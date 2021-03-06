package com.dfinn.wallet.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import com.dfinn.wallet.common.R
import com.dfinn.wallet.common.view.TableCellView
import com.dfinn.wallet.feature_wallet_api.presentation.mixin.fee.FeeStatus

class FeeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : TableCellView(context, attrs, defStyle) {

    init {
        setTitle(R.string.network_fee)

        setFeeStatus(FeeStatus.Loading)
    }

    fun setFeeStatus(feeStatus: FeeStatus) {
        when (feeStatus) {
            is FeeStatus.Loading -> {
                showProgress()
            }
            is FeeStatus.Error -> {
                showValue(context.getString(R.string.common_error_general_title))
            }
            is FeeStatus.Loaded -> {
                showAmount(feeStatus.feeModel.display)
            }
        }
    }
}
