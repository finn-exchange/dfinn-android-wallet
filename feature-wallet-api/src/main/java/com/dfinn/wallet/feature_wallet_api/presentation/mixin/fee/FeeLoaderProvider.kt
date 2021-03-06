package com.dfinn.wallet.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.MutableLiveData
import com.dfinn.wallet.common.mixin.api.RetryPayload
import com.dfinn.wallet.common.resources.ResourceManager
import com.dfinn.wallet.common.utils.Event
import com.dfinn.wallet.feature_wallet_api.R
import com.dfinn.wallet.feature_wallet_api.data.mappers.mapFeeToFeeModel
import com.dfinn.wallet.feature_wallet_api.domain.model.Token
import com.dfinn.wallet.feature_wallet_api.domain.model.amountFromPlanks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class FeeLoaderProviderFactory(
    private val resourceManager: ResourceManager,
) : FeeLoaderMixin.Factory {

    override fun create(tokenFlow: Flow<Token>, configuration: FeeLoaderMixin.Configuration): FeeLoaderMixin.Presentation {
        return FeeLoaderProvider(resourceManager, configuration, tokenFlow)
    }
}

class FeeLoaderProvider(
    private val resourceManager: ResourceManager,
    private val configuration: FeeLoaderMixin.Configuration,
    private val tokenFlow: Flow<Token>,
) : FeeLoaderMixin.Presentation {

    override val feeLiveData = MutableLiveData<FeeStatus>()

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    override suspend fun loadFeeSuspending(
        retryScope: CoroutineScope,
        feeConstructor: suspend (Token) -> BigInteger,
        onRetryCancelled: () -> Unit,
    ): Unit = withContext(Dispatchers.Default) {
        feeLiveData.postValue(FeeStatus.Loading)

        val token = tokenFlow.first()

        val feeResult = runCatching {
            feeConstructor(token)
        }

        val value = if (feeResult.isSuccess) {
            val feeInPlanks = feeResult.getOrThrow()
            val fee = token.amountFromPlanks(feeInPlanks)
            val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

            FeeStatus.Loaded(feeModel)
        } else {
            val exception = feeResult.exceptionOrNull()

            if (exception is CancellationException) {
                null
            } else {
                retryEvent.postValue(
                    Event(
                        RetryPayload(
                            title = resourceManager.getString(R.string.choose_amount_network_error),
                            message = resourceManager.getString(R.string.choose_amount_error_fee),
                            onRetry = { loadFee(retryScope, feeConstructor, onRetryCancelled) },
                            onCancel = onRetryCancelled
                        )
                    )
                )

                exception?.printStackTrace()

                FeeStatus.Error
            }
        }

        value?.run { feeLiveData.postValue(this) }
    }

    override fun loadFee(
        coroutineScope: CoroutineScope,
        feeConstructor: suspend (Token) -> BigInteger,
        onRetryCancelled: () -> Unit,
    ) {
        coroutineScope.launch {
            loadFeeSuspending(coroutineScope, feeConstructor, onRetryCancelled)
        }
    }

    override suspend fun setFee(fee: BigDecimal) {
        val token = tokenFlow.first()
        val feeModel = mapFeeToFeeModel(fee, token, includeZeroFiat = configuration.showZeroFiat)

        feeLiveData.postValue(FeeStatus.Loaded(feeModel))
    }

    override fun requireFee(
        block: (BigDecimal) -> Unit,
        onError: (title: String, message: String) -> Unit,
    ) {
        val feeStatus = feeLiveData.value

        if (feeStatus is FeeStatus.Loaded) {
            block(feeStatus.feeModel.fee)
        } else {
            onError(
                resourceManager.getString(R.string.fee_not_yet_loaded_title),
                resourceManager.getString(R.string.fee_not_yet_loaded_message)
            )
        }
    }
}
