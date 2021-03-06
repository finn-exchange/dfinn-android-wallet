package com.dfinn.wallet.feature_staking_impl.presentation.payouts.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class PendingPayoutParcelable(
    val validatorInfo: ValidatorInfoParcelable,
    val era: BigInteger,
    val amountInPlanks: BigInteger,
    val timeLeftCalculatedAt: Long,
    val timeLeft: Long,
    val closeToExpire: Boolean,
) : Parcelable {
    @Parcelize
    class ValidatorInfoParcelable(
        val address: String,
        val identityName: String?,
    ) : Parcelable
}
