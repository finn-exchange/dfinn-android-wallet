package com.dfinn.wallet.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import com.dfinn.wallet.common.di.scope.FeatureScope
import com.dfinn.wallet.common.validation.CompositeValidation
import com.dfinn.wallet.feature_staking_api.domain.api.StakingRepository
import com.dfinn.wallet.feature_staking_impl.domain.validations.unbond.CrossExistentialValidation
import com.dfinn.wallet.feature_staking_impl.domain.validations.unbond.EnoughToUnbondValidation
import com.dfinn.wallet.feature_staking_impl.domain.validations.unbond.NotZeroUnbondValidation
import com.dfinn.wallet.feature_staking_impl.domain.validations.unbond.UnbondFeeValidation
import com.dfinn.wallet.feature_staking_impl.domain.validations.unbond.UnbondLimitValidation
import com.dfinn.wallet.feature_staking_impl.domain.validations.unbond.UnbondValidationFailure
import com.dfinn.wallet.feature_staking_impl.domain.validations.unbond.UnbondValidationSystem
import com.dfinn.wallet.feature_wallet_api.domain.interfaces.WalletConstants

@Module
class UnbondValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation() = UnbondFeeValidation(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.asset.transferable },
        errorProducer = { UnbondValidationFailure.CannotPayFees }
    )

    @FeatureScope
    @Provides
    fun provideNotZeroUnbondValidation() = NotZeroUnbondValidation(
        amountExtractor = { it.amount },
        errorProvider = { UnbondValidationFailure.ZeroUnbond }
    )

    @FeatureScope
    @Provides
    fun provideUnbondLimitValidation(
        stakingRepository: StakingRepository
    ) = UnbondLimitValidation(
        stakingRepository = stakingRepository,
        stashStateProducer = { it.stash },
        errorProducer = UnbondValidationFailure::UnbondLimitReached
    )

    @FeatureScope
    @Provides
    fun provideEnoughToUnbondValidation() = EnoughToUnbondValidation()

    @FeatureScope
    @Provides
    fun provideCrossExistentialValidation(
        walletConstants: WalletConstants
    ) = CrossExistentialValidation(walletConstants)

    @FeatureScope
    @Provides
    fun provideUnbondValidationSystem(
        unbondFeeValidation: UnbondFeeValidation,
        notZeroUnbondValidation: NotZeroUnbondValidation,
        unbondLimitValidation: UnbondLimitValidation,
        enoughToUnbondValidation: EnoughToUnbondValidation,
        crossExistentialValidation: CrossExistentialValidation
    ) = UnbondValidationSystem(
        CompositeValidation(
            validations = listOf(
                unbondFeeValidation,
                notZeroUnbondValidation,
                unbondLimitValidation,
                enoughToUnbondValidation,
                crossExistentialValidation
            )
        )
    )
}
