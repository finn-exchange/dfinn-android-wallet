package com.dfinn.wallet.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.dfinn.wallet.common.utils.makeGone
import com.dfinn.wallet.common.utils.makeVisible
import com.dfinn.wallet.common.utils.setCompoundDrawableTint
import com.dfinn.wallet.common.utils.setTextColorRes
import com.dfinn.wallet.common.view.shape.addRipple
import com.dfinn.wallet.common.view.shape.getBlurDrawable
import com.dfinn.wallet.common.view.shape.getRoundedCornerDrawable
import com.dfinn.wallet.common.view.startTimer
import com.dfinn.wallet.common.view.stopTimer
import com.dfinn.wallet.feature_staking_impl.R
import com.dfinn.wallet.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryContentGroup
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryFiatStake
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryFiatStakeShimmer
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryShimmerGroup
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryStatus
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryStatusShimmer
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryTokenStake
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryTokenStakeShimmer

class StakeSummaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    sealed class Status(@StringRes val textRes: Int, @ColorRes val tintRes: Int) {

        object Active : Status(R.string.staking_nominator_status_active, R.color.green)

        object Inactive : Status(R.string.staking_nominator_status_inactive, R.color.white_80)

        class Waiting(val timeLeft: Long) : Status(R.string.staking_nominator_status_waiting_format, R.color.white_80)
    }

    init {
        View.inflate(context, R.layout.view_stake_summary, this)

        with(context) {
            background = getBlurDrawable()
            stakeSummaryStatus.background = addRipple(getRoundedCornerDrawable(fillColorRes = R.color.white_8))
        }
    }

    fun showStakeStatus(status: Status) {
        stakeSummaryStatusShimmer.makeGone()

        with(stakeSummaryStatus) {
            makeVisible()

            setCompoundDrawableTint(status.tintRes)
            setTextColorRes(status.tintRes)

            if (status is Status.Waiting) {
                stakeSummaryStatus.startTimer(
                    millis = status.timeLeft,
                    daysPlurals = R.plurals.staking_main_lockup_period_value,
                    customMessageFormat = status.textRes
                )
            } else {
                stakeSummaryStatus.stopTimer()
                stakeSummaryStatus.setText(status.textRes)
            }
        }
    }

    fun showStakeAmount(amountModel: AmountModel) {
        stakeSummaryTokenStake.makeVisible()
        stakeSummaryFiatStake.makeVisible()

        stakeSummaryFiatStakeShimmer.makeGone()
        stakeSummaryTokenStakeShimmer.makeGone()

        stakeSummaryTokenStake.text = amountModel.token
        stakeSummaryFiatStake.text = amountModel.fiat
    }

    fun showLoading() {
        stakeSummaryShimmerGroup.makeVisible()
        stakeSummaryContentGroup.makeGone()
    }

    fun setStatusClickListener(listener: OnClickListener) {
        stakeSummaryStatus.setOnClickListener(listener)
    }
}
