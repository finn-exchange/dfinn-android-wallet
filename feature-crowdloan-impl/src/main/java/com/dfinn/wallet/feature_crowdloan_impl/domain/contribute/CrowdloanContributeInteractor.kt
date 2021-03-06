package com.dfinn.wallet.feature_crowdloan_impl.domain.contribute

import android.os.Parcelable
import com.dfinn.wallet.feature_account_api.data.extrinsic.ExtrinsicService
import com.dfinn.wallet.feature_account_api.domain.interfaces.AccountRepository
import com.dfinn.wallet.feature_account_api.domain.model.MetaAccount
import com.dfinn.wallet.feature_account_api.domain.model.accountIdIn
import com.dfinn.wallet.feature_account_api.domain.model.addressIn
import com.dfinn.wallet.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import com.dfinn.wallet.feature_crowdloan_api.data.repository.CrowdloanRepository
import com.dfinn.wallet.feature_crowdloan_api.data.repository.ParachainMetadata
import com.dfinn.wallet.feature_crowdloan_api.data.repository.hasWonAuction
import com.dfinn.wallet.feature_crowdloan_impl.data.CrowdloanSharedState
import com.dfinn.wallet.feature_crowdloan_impl.data.network.blockhain.extrinsic.contribute
import com.dfinn.wallet.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import com.dfinn.wallet.feature_crowdloan_impl.domain.contribute.custom.PrivateCrowdloanSignatureProvider.Mode
import com.dfinn.wallet.feature_crowdloan_impl.domain.main.Crowdloan
import com.dfinn.wallet.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import com.dfinn.wallet.feature_wallet_api.domain.model.planksFromAmount
import com.dfinn.wallet.runtime.multiNetwork.chain.model.Chain
import com.dfinn.wallet.runtime.repository.ChainStateRepository
import com.dfinn.wallet.runtime.state.chainAndAsset
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

typealias OnChainSubmission = suspend ExtrinsicBuilder.() -> Unit

class CrowdloanContributeInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
    private val chainStateRepository: ChainStateRepository,
    private val customContributeManager: CustomContributeManager,
    private val crowdloanSharedState: CrowdloanSharedState,
    private val crowdloanRepository: CrowdloanRepository,
) {

    fun crowdloanStateFlow(
        parachainId: ParaId,
        parachainMetadata: ParachainMetadata?,
    ): Flow<Crowdloan> = crowdloanSharedState.assetWithChain.flatMapLatest { (chain, _) ->
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = selectedMetaAccount.accountIdIn(chain)!! // TODO optional for ethereum chains

        val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)
        val blocksPerLeasePeriod = crowdloanRepository.blocksPerLeasePeriod(chain.id)

        combine(
            crowdloanRepository.fundInfoFlow(chain.id, parachainId),
            chainStateRepository.currentBlockNumberFlow(chain.id)
        ) { fundInfo, blockNumber ->
            val contribution = crowdloanRepository.getContribution(chain.id, accountId, parachainId, fundInfo.trieIndex)
            val hasWonAuction = crowdloanRepository.hasWonAuction(chain.id, fundInfo)

            mapFundInfoToCrowdloan(
                fundInfo = fundInfo,
                parachainMetadata = parachainMetadata,
                parachainId = parachainId,
                currentBlockNumber = blockNumber,
                expectedBlockTimeInMillis = expectedBlockTime,
                blocksPerLeasePeriod = blocksPerLeasePeriod,
                contribution = contribution,
                hasWonAuction = hasWonAuction
            )
        }
    }

    suspend fun estimateFee(
        crowdloan: Crowdloan,
        contribution: BigDecimal,
        bonusPayload: BonusPayload?,
        customizationPayload: Parcelable?,
    ) = formingSubmission(
        crowdloan = crowdloan,
        contribution = contribution,
        bonusPayload = bonusPayload,
        customizationPayload = customizationPayload,
        toCalculateFee = true
    ) { submission, chain, _ ->
        extrinsicService.estimateFee(chain, submission)
    }

    suspend fun contribute(
        crowdloan: Crowdloan,
        contribution: BigDecimal,
        bonusPayload: BonusPayload?,
        customizationPayload: Parcelable?,
    ): Result<String> = runCatching {
        crowdloan.parachainMetadata?.customFlow?.let {
            customContributeManager.getFactoryOrNull(it)?.submitter?.submitOffChain(customizationPayload, bonusPayload, contribution)
        }

        val txHash = formingSubmission(
            crowdloan = crowdloan,
            contribution = contribution,
            bonusPayload = bonusPayload,
            toCalculateFee = false,
            customizationPayload = customizationPayload
        ) { submission, chain, account ->
            val accountId = account.accountIdIn(chain)!!

            extrinsicService.submitExtrinsic(chain, accountId, submission)
        }.getOrThrow()

        txHash
    }

    private suspend fun <T> formingSubmission(
        crowdloan: Crowdloan,
        contribution: BigDecimal,
        bonusPayload: BonusPayload?,
        customizationPayload: Parcelable?,
        toCalculateFee: Boolean,
        finalAction: suspend (OnChainSubmission, Chain, MetaAccount) -> T,
    ): T = withContext(Dispatchers.Default) {
        val (chain, chainAsset) = crowdloanSharedState.chainAndAsset()
        val contributionInPlanks = chainAsset.planksFromAmount(contribution)
        val account = accountRepository.getSelectedMetaAccount()

        val privateSignature = crowdloan.parachainMetadata?.customFlow?.let {
            val previousContribution = crowdloan.myContribution?.amount ?: BigInteger.ZERO

            val signatureProvider = customContributeManager.getFactoryOrNull(it)?.privateCrowdloanSignatureProvider
            val address = account.addressIn(chain)!!

            signatureProvider?.provideSignature(
                chainMetadata = crowdloan.parachainMetadata,
                previousContribution = previousContribution,
                newContribution = contributionInPlanks,
                address = address,
                mode = if (toCalculateFee) Mode.FEE else Mode.SUBMIT
            )
        }

        val submitter = crowdloan.parachainMetadata?.customFlow?.let {
            customContributeManager.getFactoryOrNull(it)?.submitter
        }

        val submission: OnChainSubmission = {
            contribute(crowdloan.parachainId, contributionInPlanks, privateSignature)

            submitter?.let {
                val injection = if (toCalculateFee) submitter::injectFeeCalculation else submitter::injectOnChainSubmission

                injection(crowdloan, customizationPayload, bonusPayload, contribution, this)
            }
        }

        finalAction(submission, chain, account)
    }
}
