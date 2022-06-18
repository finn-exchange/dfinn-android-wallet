package com.dfinn.wallet.common.mixin.api

import androidx.lifecycle.LiveData

interface NetworkStateMixin : NetworkStateUi

interface NetworkStateUi {
    val showConnectingBarLiveData: LiveData<Boolean>
}
