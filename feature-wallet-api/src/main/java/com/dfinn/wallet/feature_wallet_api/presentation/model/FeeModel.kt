package com.dfinn.wallet.feature_wallet_api.presentation.model

import java.math.BigDecimal

class FeeModel(
    val fee: BigDecimal,
    val display: AmountModel,
)
