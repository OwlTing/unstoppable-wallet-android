package io.horizontalsystems.bankwallet.modules.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.utils.MainTabManager
import io.horizontalsystems.bankwallet.owlwallet.utils.VersionChecker
import io.horizontalsystems.bankwallet.owlwallet.utils.UpdateAction
import io.horizontalsystems.bankwallet.owlwallet.utils.getLangParam
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Manager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.core.IPinComponent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(
    private val pinComponent: IPinComponent,
    rateAppManager: IRateAppManager,
    private val backupManager: IBackupManager,
    private val termsManager: ITermsManager,
    private val accountManager: IAccountManager,
    private val releaseNotesManager: ReleaseNotesManager,
    private val localStorage: ILocalStorage,
    wc2SessionManager: WC2SessionManager,
    wc2Manager: WC2Manager,
    private val wcDeepLink: String?,
    private val versionHelper: VersionChecker,
    val mainTabManager: MainTabManager,
    private val repo: OTRepository,
    private val languageManager: LanguageManager,
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private var wc2PendingRequestsCount = 0
    private var marketsTabEnabled = localStorage.marketsTabEnabledFlow.value
    private var transactionsEnabled = isTransactionsTabEnabled()
    private var settingsBadge: MainModule.BadgeType? = null
    private val launchPage: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    private var currentMainTab: MainNavigation?
        get() = localStorage.mainTab
        set(value) {
            localStorage.mainTab = value
        }

    private var relaunchBySettingChange: Boolean
        get() = localStorage.relaunchBySettingChange
        set(value) {
            localStorage.relaunchBySettingChange = value
        }

    private val items: List<MainNavigation>
        get() = if (marketsTabEnabled) {
            listOf(
                MainNavigation.Market,
                MainNavigation.Balance,
                MainNavigation.Transactions,
                MainNavigation.Settings,
            )
        } else {
            listOf(
                MainNavigation.Balance,
                MainNavigation.Transactions,
                MainNavigation.Settings,
            )
        }

    private var selectedPageIndex = getPageIndexToOpen()
    private var mainNavItems = navigationItems()
    private var showRateAppDialog = false
    private var contentHidden = pinComponent.isLocked
    private var showWhatsNew = false
    private var activeWallet = accountManager.activeAccount
    private var wcSupportState: WC2Manager.SupportState? = null
    private var torEnabled = localStorage.torEnabled
    private var versionCheckAction = UpdateAction.Nothing

    val wallets: List<Account>
        get() = accountManager.accounts.filter { !it.isWatchAccount }

    val watchWallets: List<Account>
        get() = accountManager.accounts.filter { it.isWatchAccount }

    var uiState by mutableStateOf(
        MainModule.UiState(
            selectedPageIndex = selectedPageIndex,
            mainNavItems = mainNavItems,
            showRateAppDialog = showRateAppDialog,
            contentHidden = contentHidden,
            showWhatsNew = showWhatsNew,
            activeWallet = activeWallet,
            wcSupportState = wcSupportState,
            torEnabled = torEnabled,
            versionCheckAction = versionCheckAction
        )
    )
        private set

    init {
        localStorage.marketsTabEnabledFlow.collectWith(viewModelScope) {
            marketsTabEnabled = it
            syncNavigation()
        }

        termsManager.termsAcceptedSignalFlow.collectWith(viewModelScope) {
            updateSettingsBadge()
        }

        wc2SessionManager.pendingRequestCountFlow.collectWith(viewModelScope) {
            wc2PendingRequestsCount = it
            updateSettingsBadge()
        }

        rateAppManager.showRateAppFlow.collectWith(viewModelScope) {
            showRateAppDialog = it
            syncState()
        }

        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            updateSettingsBadge()
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            updateSettingsBadge()
        })

        disposables.add(accountManager.accountsFlowable.subscribe {
            updateTransactionsTabEnabled()
            updateSettingsBadge()
        })

        viewModelScope.launch {
            accountManager.activeAccountStateFlow.collect {
                if (it is ActiveAccountState.ActiveAccount) {
                    updateTransactionsTabEnabled()
                }
            }
        }

        wcDeepLink?.let {
            wcSupportState = wc2Manager.getWalletConnectSupportState()
            syncState()
        }

        accountManager.activeAccountStateFlow.collectWith(viewModelScope) {
            (it as? ActiveAccountState.ActiveAccount)?.let { state ->
                activeWallet = state.account
                syncState()
            }
        }

        updateSettingsBadge()
        updateTransactionsTabEnabled()
        showWhatsNew()

        mainTabManager.currentTabFlow.collectWith(viewModelScope) {
            Timber.d("setCurrentTab $it")
            onSelect(it)
        }

        viewModelScope.launch {
            val newAction = versionHelper.check()
            if (newAction != versionCheckAction) {
                versionCheckAction = newAction
                syncState()
            }
        }
    }

    private fun isTransactionsTabEnabled(): Boolean =
        !accountManager.isAccountsEmpty && accountManager.activeAccount?.type !is AccountType.Cex


    override fun onCleared() {
        disposables.clear()
    }

    fun whatsNewShown() {
        showWhatsNew = false
        syncState()
    }

    fun closeRateDialog() {
        showRateAppDialog = false
        syncState()
    }

    fun closeVersionCheckDialog() {
        versionCheckAction = UpdateAction.Nothing
        syncState()
    }

    fun onSelect(account: Account) {
        accountManager.setActiveAccountId(account.id)
        activeWallet = account
        syncState()
    }

    fun onResume() {
        contentHidden = pinComponent.isLocked

        viewModelScope.launch {
            repo.getUserMeta(getLangParam(languageManager.currentLanguage))
        }
        syncState()
    }

    fun onSelect(mainNavItem: MainNavigation) {
        if (mainNavItem != MainNavigation.Settings) {
            currentMainTab = mainNavItem
        }
        selectedPageIndex = items.indexOf(mainNavItem)
        syncNavigation()
    }

    private fun updateTransactionsTabEnabled() {
        transactionsEnabled = isTransactionsTabEnabled()
        syncNavigation()
    }

    fun wcSupportStateHandled() {
        wcSupportState = null
        syncState()
    }

    private fun syncState() {
        uiState = MainModule.UiState(
            selectedPageIndex = selectedPageIndex,
            mainNavItems = mainNavItems,
            showRateAppDialog = showRateAppDialog,
            contentHidden = contentHidden,
            showWhatsNew = showWhatsNew,
            activeWallet = activeWallet,
            wcSupportState = wcSupportState,
            torEnabled = torEnabled,
            versionCheckAction = versionCheckAction,
        )
    }

    private fun navigationItems(): List<MainModule.NavigationViewItem> {
        return items.mapIndexed { index, mainNavItem ->
            getNavItem(mainNavItem, index == selectedPageIndex)
        }
    }

    private fun getNavItem(item: MainNavigation, selected: Boolean) = when (item) {
        MainNavigation.Market -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = true,
            )
        }
        MainNavigation.Transactions -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = transactionsEnabled,
            )
        }
        MainNavigation.Settings -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = true,
                badge = settingsBadge
            )
        }
        MainNavigation.Balance -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = true,
            )
        }
    }

    private fun getPageIndexToOpen(): Int {
        val page = when {
            wcDeepLink != null -> {
                MainNavigation.Settings
            }
            relaunchBySettingChange -> {
                relaunchBySettingChange = false
                MainNavigation.Settings
            }
            !marketsTabEnabled -> {
                MainNavigation.Balance
            }
            else -> when (launchPage) {
                LaunchPage.Market,
                LaunchPage.Watchlist -> MainNavigation.Market
                LaunchPage.Balance -> MainNavigation.Balance
                LaunchPage.Auto -> currentMainTab ?: MainNavigation.Balance
            }
        }
        return items.indexOf(page)
    }

    private fun syncNavigation() {
        mainNavItems = navigationItems()
        if (selectedPageIndex >= mainNavItems.size) {
            selectedPageIndex = mainNavItems.size - 1
        }
        syncState()
    }

    private fun showWhatsNew() {
        viewModelScope.launch {
            if (releaseNotesManager.shouldShowChangeLog()) {
                delay(2000)
                showWhatsNew = true
                syncState()
            }
        }
    }

    private fun updateSettingsBadge() {
        val showDotBadge =
            !(backupManager.allBackedUp /*&& termsManager.allTermsAccepted*/ && pinComponent.isPinSet) /*|| accountManager.hasNonStandardAccount*/

        settingsBadge = if (wc2PendingRequestsCount > 0) {
            MainModule.BadgeType.BadgeNumber(wc2PendingRequestsCount)
        } else if (showDotBadge) {
            MainModule.BadgeType.BadgeDot
        } else {
            null
        }
        syncNavigation()
    }

}
