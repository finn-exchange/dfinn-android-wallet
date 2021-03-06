package com.dfinn.wallet.feature_assets.presentation.common

import androidx.annotation.StringRes
import com.dfinn.wallet.feature_assets.R
import com.dfinn.wallet.common.utils.format
import com.dfinn.wallet.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import kotlinx.android.synthetic.main.item_sheet_currency.view.itemCurrencyLabel
import kotlinx.android.synthetic.main.item_sheet_currency.view.itemCurrencyValue
import java.math.BigDecimal

fun FixedListBottomSheet.currencyItem(@StringRes label: Int, value: BigDecimal) {
    item(R.layout.item_sheet_currency) { view ->
        view.itemCurrencyLabel.setText(label)

        view.itemCurrencyValue.text = value.format()
    }
}
