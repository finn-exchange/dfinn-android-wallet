package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala

import io.novafoundation.nova.common.base.BaseException
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.feature_account_api.data.secrets.sign
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaDirectContributeRequest
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaLiquidContributeRequest
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.extrinsic.systemRemarkWithEvent
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.state.chainAsset
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class AcalaContributeInteractor(
    private val acalaApi: AcalaApi,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
    private val selectedAssetState: SingleAssetSharedState,
) {

    suspend fun registerContributionOffChain(
        amount: BigDecimal,
        contributionType: ContributionType,
        referralCode: String?,
    ): Result<Unit> = runCatching {
        httpExceptionHandler.wrap {
            val selectedMetaAccount = accountRepository.getSelectedMetaAccount()

            val (chain, chainAsset) = selectedAssetState.chainAndAsset()

            val statement = getStatement(chain).statement

            val accountIdInCurrentChain = selectedMetaAccount.accountIdIn(chain)!!
            // api requires polkadot address even in rococo testnet
            val addressInPolkadot = chainRegistry.getChain(ChainGeneses.POLKADOT).addressOf(accountIdInCurrentChain)
            val amountInPlanks = chainAsset.planksFromAmount(amount)

            when (contributionType) {
                ContributionType.DIRECT -> {
                    val request = AcalaDirectContributeRequest(
                        address = addressInPolkadot,
                        amount = amountInPlanks,
                        referral = referralCode,
                        signature = secretStoreV2.sign(selectedMetaAccount, chain, statement)
                    )

                    acalaApi.directContribute(
                        baseUrl = AcalaApi.getBaseUrl(chain),
                        authHeader = AcalaApi.getAuthHeader(chain),
                        body = request
                    )
                }

                ContributionType.LIQUID -> {
                    val request = AcalaLiquidContributeRequest(
                        address = addressInPolkadot,
                        amount = amountInPlanks,
                        referral = referralCode
                    )

                    acalaApi.liquidContribute(
                        baseUrl = AcalaApi.getBaseUrl(chain),
                        authHeader = AcalaApi.getAuthHeader(chain),
                        body = request
                    )
                }
            }
        }
    }

    suspend fun isReferralValid(referralCode: String) = try {
        val chain = selectedAssetState.chain()

        httpExceptionHandler.wrap {
            acalaApi.isReferralValid(
                baseUrl = AcalaApi.getBaseUrl(chain),
                authHeader = AcalaApi.getAuthHeader(chain),
                referral = referralCode
            ).result
        }
    } catch (e: BaseException) {
        if (e.kind == BaseException.Kind.HTTP) {
            false // acala api return an error http code for some invalid codes, so catch it here
        } else {
            throw e
        }
    }

    suspend fun injectOnChainSubmission(
        contributionType: ContributionType,
        referralCode: String?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) = with(extrinsicBuilder) {
        if (contributionType == ContributionType.LIQUID) {
            reset()

            val (chain, chainAsset) = selectedAssetState.chainAndAsset()
            val amountInPlanks = chainAsset.planksFromAmount(amount)

            val statement = httpExceptionHandler.wrap { getStatement(chain) }
            val proxyAccountId = chain.accountIdOf(statement.proxyAddress)

            transfer(proxyAccountId, amountInPlanks)
            systemRemarkWithEvent(statement.statement)
            referralCode?.let { systemRemarkWithEvent(referralRemark(it)) }
        }
    }

    suspend fun injectFeeCalculation(
        contributionType: ContributionType,
        referralCode: String?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) = with(extrinsicBuilder) {
        if (contributionType == ContributionType.LIQUID) {
            reset()

            val chainAsset = selectedAssetState.chainAsset()
            val amountInPlanks = chainAsset.planksFromAmount(amount)

            val fakeDestination = ByteArray(32)
            transfer(accountId = fakeDestination, amount = amountInPlanks)

            val fakeAgreementRemark = ByteArray(185) // acala agreement is 185 bytes
            systemRemarkWithEvent(fakeAgreementRemark)

            referralCode?.let { systemRemarkWithEvent(referralRemark(referralCode)) }
        }
    }

    private suspend fun getStatement(
        chain: Chain,
    ) = acalaApi.getStatement(
        baseUrl = AcalaApi.getBaseUrl(chain),
        authHeader = AcalaApi.getAuthHeader(chain)
    )

    private fun referralRemark(referralCode: String) = "referrer:$referralCode"
}