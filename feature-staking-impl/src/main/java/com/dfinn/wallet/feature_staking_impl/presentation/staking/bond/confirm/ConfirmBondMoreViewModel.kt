package com.dfinn.wallet.feature_staking_impl.presentation.staking.bond.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dfinn.wallet.common.address.AddressIconGenerator
import com.dfinn.wallet.common.base.BaseViewModel
import com.dfinn.wallet.common.mixin.api.Validatable
import com.dfinn.wallet.common.mixin.hints.ResourcesHintsMixinFactory
import com.dfinn.wallet.common.resources.ResourceManager
import com.dfinn.wallet.common.utils.flowOf
import com.dfinn.wallet.common.utils.requireException
import com.dfinn.wallet.common.validation.ValidationExecutor
import com.dfinn.wallet.common.validation.progressConsumer
import com.dfinn.wallet.feature_account_api.presenatation.account.icon.createAccountAddressModel
import com.dfinn.wallet.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import com.dfinn.wallet.feature_account_api.presenatation.actions.ExternalActions
import com.dfinn.wallet.feature_staking_impl.R
import com.dfinn.wallet.feature_staking_impl.domain.StakingInteractor
import com.dfinn.wallet.feature_staking_impl.domain.staking.bond.BondMoreInteractor
import com.dfinn.wallet.feature_staking_impl.domain.validations.bond.BondMoreValidationPayload
import com.dfinn.wallet.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import com.dfinn.wallet.feature_staking_impl.presentation.StakingRouter
import com.dfinn.wallet.feature_staking_impl.presentation.staking.bond.bondMoreValidationFailure
import com.dfinn.wallet.feature_wallet_api.data.mappers.mapFeeToFeeModel
import com.dfinn.wallet.feature_wallet_api.domain.model.planksFromAmount
import com.dfinn.wallet.feature_wallet_api.presentation.mixin.fee.FeeStatus
import com.dfinn.wallet.feature_wallet_api.presentation.model.mapAmountToAmountModel
import com.dfinn.wallet.runtime.state.SingleAssetSharedState
import com.dfinn.wallet.runtime.state.chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmBondMoreViewModel(
    private val router: StakingRouter,
    interactor: StakingInteractor,
    private val bondMoreInteractor: BondMoreInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val iconGenerator: AddressIconGenerator,
    private val validationSystem: BondMoreValidationSystem,
    private val externalActions: ExternalActions.Presentation,
    private val payload: ConfirmBondMorePayload,
    private val selectedAssetState: SingleAssetSharedState,
    walletUiUseCase: WalletUiUseCase,
    hintsMixinFactory: ResourcesHintsMixinFactory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    val hintsMixin = hintsMixinFactory.create(
        coroutineScope = this,
        hintsRes = listOf(R.string.staking_hint_reward_bond_more_v2_2_0)
    )

    private val assetFlow = interactor.assetFlow(payload.stashAddress)
        .shareInBackground()

    val amountModelFlow = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeStatusFlow = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(payload.fee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .shareInBackground()

    val originAddressModelFlow = flowOf {
        iconGenerator.createAccountAddressModel(selectedAssetState.chain(), payload.stashAddress)
    }
        .shareInBackground()

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(payload.stashAddress), selectedAssetState.chain())
    }

    private fun maybeGoToNext() = launch {
        val payload = BondMoreValidationPayload(
            stashAddress = payload.stashAddress,
            fee = payload.fee,
            amount = payload.amount,
            chainAsset = assetFlow.first().token.configuration
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { bondMoreValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction()
        }
    }

    private fun sendTransaction() = launch {
        val token = assetFlow.first().token
        val amountInPlanks = token.planksFromAmount(payload.amount)

        val result = bondMoreInteractor.bondMore(payload.stashAddress, amountInPlanks)

        _showNextProgress.value = false

        if (result.isSuccess) {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))

            finishFlow()
        } else {
            showError(result.requireException())
        }
    }

    private fun finishFlow() {
        router.returnToMain()
    }
}
