package com.dfinn.wallet.feature_staking_impl.domain.validations.rewardDestination

import com.dfinn.wallet.common.validation.ValidationSystem
import com.dfinn.wallet.feature_staking_impl.domain.validations.AccountRequiredValidation
import com.dfinn.wallet.feature_wallet_api.domain.validation.EnoughToPayFeesValidation

typealias RewardDestinationFeeValidation = EnoughToPayFeesValidation<RewardDestinationValidationPayload, RewardDestinationValidationFailure>
typealias RewardDestinationControllerRequiredValidation = AccountRequiredValidation<RewardDestinationValidationPayload, RewardDestinationValidationFailure>
typealias RewardDestinationValidationSystem = ValidationSystem<RewardDestinationValidationPayload, RewardDestinationValidationFailure>
