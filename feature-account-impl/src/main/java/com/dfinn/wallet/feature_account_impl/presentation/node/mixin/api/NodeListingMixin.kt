package com.dfinn.wallet.feature_account_impl.presentation.node.mixin.api

import androidx.lifecycle.LiveData

interface NodeListingMixin {

    val groupedNodeModelsLiveData: LiveData<List<Any>>
}
