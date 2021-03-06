package com.dfinn.wallet.feature_staking_impl.presentation.validators.parcel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ValidatorDetailsParcelModel(
    val accountIdHex: String,
    val isSlashed: Boolean,
    val stake: ValidatorStakeParcelModel,
    val identity: IdentityParcelModel?,
) : Parcelable
