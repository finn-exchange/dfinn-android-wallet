package com.dfinn.wallet.common.utils

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

// TODO waiting for multiple receivers feature, probably in Kotlin 1.7
interface WithCoroutineScopeExtensions {

    val coroutineScope: CoroutineScope

    fun <T> Flow<T>.share() = shareIn(coroutineScope, started = SharingStarted.Eagerly, replay = 1)

    fun <T> Flow<T>.shareLazily() = shareIn(coroutineScope, started = SharingStarted.Lazily, replay = 1)

    fun <T> Flow<T>.shareInBackground() = inBackground().share()

    fun <T> Flow<T>.asLiveData(): LiveData<T> {
        return asLiveData(coroutineScope)
    }
}

fun WithCoroutineScopeExtensions(coroutineScope: CoroutineScope) = object : WithCoroutineScopeExtensions {
    override val coroutineScope: CoroutineScope = coroutineScope
}
