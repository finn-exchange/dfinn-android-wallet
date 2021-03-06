package com.dfinn.wallet.feature_staking_impl.domain.validations.setup

import com.dfinn.wallet.feature_staking_impl.domain.validations.MaxNominatorsReachedValidation
import com.dfinn.wallet.feature_wallet_api.domain.validation.EnoughToPayFeesValidation

typealias SetupStakingFeeValidation = EnoughToPayFeesValidation<SetupStakingPayload, SetupStakingValidationFailure>
typealias SetupStakingMaximumNominatorsValidation = MaxNominatorsReachedValidation<SetupStakingPayload, SetupStakingValidationFailure>
