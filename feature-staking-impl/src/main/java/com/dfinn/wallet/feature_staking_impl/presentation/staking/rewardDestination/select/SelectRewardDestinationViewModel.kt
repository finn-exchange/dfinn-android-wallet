package com.dfinn.wallet.feature_staking_impl.presentation.staking.rewardDestination.select

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dfinn.wallet.common.base.BaseViewModel
import com.dfinn.wallet.common.mixin.api.Retriable
import com.dfinn.wallet.common.mixin.api.Validatable
import com.dfinn.wallet.common.resources.ResourceManager
import com.dfinn.wallet.common.utils.inBackground
import com.dfinn.wallet.common.validation.ValidationExecutor
import com.dfinn.wallet.common.validation.progressConsumer
import com.dfinn.wallet.feature_staking_api.domain.model.RewardDestination
import com.dfinn.wallet.feature_staking_api.domain.model.StakingState
import com.dfinn.wallet.feature_staking_impl.data.mappers.mapRewardDestinationModelToRewardDestination
import com.dfinn.wallet.feature_staking_impl.domain.StakingInteractor
import com.dfinn.wallet.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import com.dfinn.wallet.feature_staking_impl.domain.staking.rewardDestination.ChangeRewardDestinationInteractor
import com.dfinn.wallet.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationPayload
import com.dfinn.wallet.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationSystem
import com.dfinn.wallet.feature_staking_impl.presentation.StakingRouter
import com.dfinn.wallet.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationMixin
import com.dfinn.wallet.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationModel
import com.dfinn.wallet.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload
import com.dfinn.wallet.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.RewardDestinationParcelModel
import com.dfinn.wallet.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SelectRewardDestinationViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val changeRewardDestinationInteractor: ChangeRewardDestinationInteractor,
    private val validationSystem: RewardDestinationValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val rewardDestinationMixin: RewardDestinationMixin.Presentation,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    RewardDestinationMixin by rewardDestinationMixin {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val rewardCalculator = viewModelScope.async { rewardCalculatorFactory.create() }

    private val rewardDestinationFlow = rewardDestinationMixin.rewardDestinationModelFlow
        .map { mapRewardDestinationModelToRewardDestination(it) }
        .inBackground()
        .share()

    private val stashStateFlow = interactor.selectedAccountStakingStateFlow()
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val controllerAssetFlow = stashStateFlow
        .flatMapLatest { interactor.assetFlow(it.controllerAddress) }
        .inBackground()
        .share()

    val continueAvailable = rewardDestinationMixin.rewardDestinationChangedFlow
        .asLiveData()

    init {
        rewardDestinationFlow.combine(stashStateFlow) { rewardDestination, stashState ->
            loadFee(rewardDestination, stashState)
        }.launchIn(viewModelScope)

        controllerAssetFlow.onEach {
            rewardDestinationMixin.updateReturns(rewardCalculator(), it, it.bonded)
        }.launchIn(viewModelScope)

        stashStateFlow.onEach(rewardDestinationMixin::loadActiveRewardDestination)
            .launchIn(viewModelScope)
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private fun loadFee(rewardDestination: RewardDestination, stashState: StakingState.Stash) {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { changeRewardDestinationInteractor.estimateFee(stashState, rewardDestination) },
            onRetryCancelled = ::backClicked
        )
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val payload = RewardDestinationValidationPayload(
                availableControllerBalance = controllerAssetFlow.first().transferable,
                fee = fee,
                stashState = stashStateFlow.first()
            )

            val rewardDestination = rewardDestinationModelFlow.first()

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { rewardDestinationValidationFailure(resourceManager, it) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                _showNextProgress.value = false

                goToNextStep(rewardDestination, it.fee)
            }
        }
    }

    private fun goToNextStep(
        rewardDestination: RewardDestinationModel,
        fee: BigDecimal
    ) {
        val payload = ConfirmRewardDestinationPayload(
            fee = fee,
            rewardDestination = mapRewardDestinationModelToRewardDestinationParcelModel(rewardDestination)
        )

        router.openConfirmRewardDestination(payload)
    }

    private fun mapRewardDestinationModelToRewardDestinationParcelModel(rewardDestination: RewardDestinationModel): RewardDestinationParcelModel {
        return when (rewardDestination) {
            RewardDestinationModel.Restake -> RewardDestinationParcelModel.Restake
            is RewardDestinationModel.Payout -> RewardDestinationParcelModel.Payout(rewardDestination.destination.address)
        }
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )

    private suspend fun rewardCalculator() = rewardCalculator.await()
}
