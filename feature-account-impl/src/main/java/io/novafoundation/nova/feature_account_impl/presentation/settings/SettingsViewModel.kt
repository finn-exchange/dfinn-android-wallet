package io.novafoundation.nova.feature_account_impl.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.coingecko.FiatChooserEvent
import io.novafoundation.nova.common.data.network.coingecko.FiatCurrency
import io.novafoundation.nova.common.domain.GetAvailableFiatCurrencies
import io.novafoundation.nova.common.domain.SelectedFiat
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.*
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountChosenNavDirection
import io.novafoundation.nova.feature_account_impl.presentation.language.mapper.mapLanguageToLanguageModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val appLinksProvider: AppLinksProvider,
    private val addressIconGenerator: AddressIconGenerator,
    private val getAvailableFiatCurrencies: GetAvailableFiatCurrencies,
    private val selectedFiat: SelectedFiat
) : BaseViewModel(), Browserable {

    private val _showFiatChooser = MutableLiveData<FiatChooserEvent>()
    val showFiatChooser: LiveData<FiatChooserEvent> = _showFiatChooser

    val selectedAccountFlow = interactor.selectedMetaAccountFlow()
        .inBackground()
        .share()

    val accountIconFlow = selectedAccountFlow.map {
        addressIconGenerator.createAddressIcon(it.substrateAccountId, AddressIconGenerator.SIZE_BIG)
    }
        .inBackground()
        .share()

    val selectedLanguageFlow = flowOf {
        val language = interactor.getSelectedLanguage()

        mapLanguageToLanguageModel(language)
    }
        .inBackground()
        .share()

    @OptIn(ExperimentalStdlibApi::class)
    val selectedFiatLiveData: LiveData<String> = selectedFiat.flow().asLiveData().map { it.uppercase() }

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val _openEmailEvent = MutableLiveData<Event<String>>()
    val openEmailEvent: LiveData<Event<String>> = _openEmailEvent

    fun walletsClicked() {
        router.openAccounts(AccountChosenNavDirection.MAIN)
    }

    fun networksClicked() {
        router.openNodes()
    }

    fun languagesClicked() {
        router.openLanguages()
    }

    fun changePinCodeClicked() {
        router.openChangePinCode()
    }

    fun websiteClicked() {
        openLink(appLinksProvider.website)
    }

    fun githubClicked() {
        openLink(appLinksProvider.github)
    }

    fun termsClicked() {
        openLink(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openLink(appLinksProvider.privacyUrl)
    }

    fun accountActionsClicked() = launch {
        router.openAccountDetails(selectedAccountFlow.first().id)
    }

    fun currencyClicked() {
        viewModelScope.launch {
            val currencies = getAvailableFiatCurrencies()
            if (currencies.isEmpty()) return@launch
            val selected = selectedFiat.get()
            val selectedItem = currencies.first { it.id == selected }
            _showFiatChooser.value = FiatChooserEvent(DynamicListBottomSheet.Payload(currencies, selectedItem))
        }
    }

    fun onFiatSelected(item: FiatCurrency) {
        viewModelScope.launch {
            selectedFiat.set(item.id)
        }
    }

    private fun openLink(link: String) {
        openBrowserEvent.value = link.event()
    }
}
