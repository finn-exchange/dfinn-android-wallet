package com.dfinn.wallet.feature_dapp_impl.domain.browser.addToFavourites

import com.dfinn.wallet.feature_dapp_api.data.repository.DAppMetadataRepository
import com.dfinn.wallet.feature_dapp_impl.data.model.FavouriteDApp
import com.dfinn.wallet.feature_dapp_impl.data.repository.FavouritesDAppRepository
import com.dfinn.wallet.feature_dapp_impl.util.Urls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddToFavouritesInteractor(
    private val favouritesDAppRepository: FavouritesDAppRepository,
    private val dAppMetadataRepository: DAppMetadataRepository,
) {

    suspend fun addToFavourites(favouriteDApp: FavouriteDApp) = withContext(Dispatchers.Default) {
        favouritesDAppRepository.addFavourite(favouriteDApp)
    }

    suspend fun resolveFavouriteDAppDisplay(url: String, suppliedLabel: String?) = withContext(Dispatchers.Default) {
        val dAppMetadataExactMatch = dAppMetadataRepository.findDAppMetadataByExactUrlMatch(url)
        val dAppMetadataBaseUrlMatches = dAppMetadataRepository.findDAppMetadatasByBaseUrlMatch(baseUrl = Urls.normalizeUrl(url))

        // we don't want to use base url match if there more than one candidate
        val dAppMetadataBaseUrlSingleMatch = dAppMetadataBaseUrlMatches.singleOrNull()

        FavouriteDApp(
            url = url,
            label = dAppMetadataExactMatch?.name ?: suppliedLabel ?: Urls.hostOf(url),
            icon = dAppMetadataExactMatch?.iconLink ?: dAppMetadataBaseUrlSingleMatch?.iconLink
        )
    }
}
