package com.dfinn.wallet.feature_dapp_impl.domain.search

import com.dfinn.wallet.common.list.GroupedList
import com.dfinn.wallet.feature_dapp_api.data.repository.DAppMetadataRepository
import com.dfinn.wallet.feature_dapp_impl.data.repository.FavouritesDAppRepository
import com.dfinn.wallet.feature_dapp_impl.domain.common.buildUrlToDappMapping
import com.dfinn.wallet.feature_dapp_impl.domain.common.createDAppComparator
import com.dfinn.wallet.feature_dapp_impl.util.Urls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchDappInteractor(
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val favouritesDAppRepository: FavouritesDAppRepository,
) {

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun searchDapps(query: String): GroupedList<DappSearchGroup, DappSearchResult> = withContext(Dispatchers.Default) {
        val dAppMetadatas = dAppMetadataRepository.getDAppMetadatas()
        val favouriteDApps = favouritesDAppRepository.getFavourites()

        val dAppByUrlMapping = buildUrlToDappMapping(dAppMetadatas, favouriteDApps)
        val allDApps = dAppByUrlMapping.values

        val dappsGroupContent = allDApps.filter { query.isEmpty() || query.lowercase() in it.name.lowercase() }
            .sortedWith(createDAppComparator())
            .map(DappSearchResult::Dapp)

        val searchGroupContent = when {
            query.isEmpty() -> null
            Urls.isValidWebUrl(query) -> DappSearchResult.Url(Urls.ensureHttpsProtocol(query))
            else -> DappSearchResult.Search(query, searchUrlFor(query))
        }

        buildMap {
            searchGroupContent?.let {
                put(DappSearchGroup.SEARCH, listOf(searchGroupContent))
            }

            if (dappsGroupContent.isNotEmpty()) {
                put(DappSearchGroup.DAPPS, dappsGroupContent)
            }
        }
    }

    private fun searchUrlFor(query: String): String = "https://duckduckgo.com/?q=$query"
}
