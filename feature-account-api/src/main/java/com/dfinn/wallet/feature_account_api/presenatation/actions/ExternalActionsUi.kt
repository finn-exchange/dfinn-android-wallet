package com.dfinn.wallet.feature_account_api.presenatation.actions

import android.content.Context
import com.dfinn.wallet.common.base.BaseFragment
import com.dfinn.wallet.common.base.BaseViewModel
import com.dfinn.wallet.common.mixin.impl.observeBrowserEvents

fun <T> BaseFragment<T>.setupExternalActions(viewModel: T) where T : BaseViewModel, T : ExternalActions {
    setupExternalActions(viewModel) { context, payload ->
        ExternalActionsSheet(
            context,
            payload,
            viewModel::copyAddressClicked,
            viewModel::viewExternalClicked
        )
    }
}

inline fun <T> BaseFragment<T>.setupExternalActions(
    viewModel: T,
    crossinline customSheetCreator: (Context, ExternalActions.Payload) -> ExternalActionsSheet,
) where T : BaseViewModel, T : ExternalActions {
    observeBrowserEvents(viewModel)

    viewModel.showExternalActionsEvent.observeEvent {
        customSheetCreator(requireContext(), it)
            .show()
    }
}

fun <T> T.copyAddressClicked(address: String) where T : BaseViewModel, T : ExternalActions {
    copyAddress(address, ::showMessage)
}
