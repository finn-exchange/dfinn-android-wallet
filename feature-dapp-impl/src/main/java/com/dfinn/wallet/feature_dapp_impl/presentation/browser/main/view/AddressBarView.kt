package com.dfinn.wallet.feature_dapp_impl.presentation.browser.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.dfinn.wallet.common.utils.WithContextExtensions
import com.dfinn.wallet.common.utils.setVisible
import com.dfinn.wallet.feature_dapp_impl.R
import kotlinx.android.synthetic.main.view_address_bar.view.addressBarIcon
import kotlinx.android.synthetic.main.view_address_bar.view.addressBarUrl

class AddressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_address_bar, this)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        background = addRipple(getRoundedCornerDrawable(R.color.white_8))
    }

    fun setAddress(address: String) {
        addressBarUrl.text = address
    }

    fun showSecureIcon(shouldShow: Boolean) {
        addressBarIcon.setVisible(shouldShow)
    }
}
