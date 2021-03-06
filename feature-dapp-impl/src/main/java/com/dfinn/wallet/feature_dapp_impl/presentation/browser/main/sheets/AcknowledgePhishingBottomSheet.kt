package com.dfinn.wallet.feature_dapp_impl.presentation.browser.main.sheets

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.dfinn.wallet.common.utils.DialogExtensions
import com.dfinn.wallet.feature_dapp_impl.R
import com.dfinn.wallet.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation
import kotlinx.android.synthetic.main.bottom_sheet_scam_alert.phishingAlertAcknowledge

class AcknowledgePhishingBottomSheet(
    context: Context,
    private val confirmation: DappPendingConfirmation<*>,
) : BottomSheetDialog(context, R.style.BottomSheetDialog), DialogExtensions {

    override val dialogInterface: DialogInterface
        get() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_sheet_scam_alert)
        super.onCreate(savedInstanceState)

        setCancelable(false)

        phishingAlertAcknowledge.setDismissingClickListener {
            confirmation.onConfirm()
        }
    }
}
