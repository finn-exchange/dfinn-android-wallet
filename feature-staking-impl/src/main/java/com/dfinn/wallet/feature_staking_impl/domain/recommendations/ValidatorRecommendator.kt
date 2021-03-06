package com.dfinn.wallet.feature_staking_impl.domain.recommendations

import com.dfinn.wallet.common.utils.applyFilters
import com.dfinn.wallet.feature_staking_api.domain.model.Validator
import com.dfinn.wallet.feature_staking_impl.domain.recommendations.settings.RecommendationSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ValidatorRecommendator(val availableValidators: List<Validator>) {

    suspend fun recommendations(settings: RecommendationSettings) = withContext(Dispatchers.Default) {
        val all = availableValidators.applyFilters(settings.allFilters)
            .sortedWith(settings.sorting)

        val postprocessed = settings.postProcessors.fold(all) { acc, postProcessor ->
            postProcessor(acc)
        }

        settings.limit?.let(postprocessed::take) ?: postprocessed
    }
}
