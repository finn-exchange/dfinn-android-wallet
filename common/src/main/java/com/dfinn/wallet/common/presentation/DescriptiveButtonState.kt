package com.dfinn.wallet.common.presentation

sealed class DescriptiveButtonState {

    class Enabled(val action: String) : DescriptiveButtonState()

    class Disabled(val reason: String) : DescriptiveButtonState()

    object Loading : DescriptiveButtonState()

    object Gone : DescriptiveButtonState()
}
