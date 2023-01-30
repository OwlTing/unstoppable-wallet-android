package io.horizontalsystems.bankwallet.owlwallet.countrypicker

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.Country
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import io.horizontalsystems.bankwallet.owlwallet.utils.getLangParam
import io.horizontalsystems.core.ILanguageManager
import kotlinx.coroutines.launch

sealed class ActionState {
    object Loading : ActionState()
    class Success(val countries: List<Country>) : ActionState()
}

class CountryPickerViewModel(
    private val languageManager: ILanguageManager,
    private val repo: OTRepository,
    private val prefs: PreferenceHelper,
) : ViewModel() {

    val uiState = mutableStateOf<List<Country>>(listOf())
    val allCountry: MutableList<Country> = mutableListOf()

    var filter: String = ""

    init {
        viewModelScope.launch {

            val result = repo.getCountries(
                lang = getLangParam(languageManager.currentLanguage),
            )

            if (result.succeeded) {
                val res = result as OTResult.Success
                if (res.data.status) {
                    allCountry.addAll(res.data.data)
                    uiState.value = res.data.data
                }
            }
        }
    }

    fun updateFilter(filter: String) {
        this.filter = filter
        if (filter.isEmpty()) {
            uiState.value = allCountry
        } else {
            uiState.value =
                allCountry.filter { country -> country.name.contains(filter, ignoreCase = true) }
        }
    }

    fun selectCountry(country: Country) {
        prefs.setAmlRegisterCountry(country)
    }
}