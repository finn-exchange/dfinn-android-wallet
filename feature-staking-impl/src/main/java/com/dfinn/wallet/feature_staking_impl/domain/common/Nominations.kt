package com.dfinn.wallet.feature_staking_impl.domain.common

import com.dfinn.wallet.feature_staking_api.domain.model.Nominations
import java.math.BigInteger

fun Nominations.isWaiting(activeEraIndex: BigInteger): Boolean {
    return submittedInEra >= activeEraIndex
}
