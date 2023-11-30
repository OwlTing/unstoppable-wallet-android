package io.horizontalsystems.bankwallet.modules.settings.main

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.contacts.ContactsFragment
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.CounterType
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Manager
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.VerifyState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeCount
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.OutlinedButtonDefault
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SimpleAlertDialog
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_green
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_red
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainSettingsViewModel = viewModel(factory = MainSettingsModule.Factory()),
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                stringResource(R.string.Settings_Title),
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingSections(viewModel, navController)
//                OwlTingSections(viewModel, navController)
                SettingsFooter(viewModel)
            }
        }
    }
}

@Composable
private fun SettingSections(
    viewModel: MainSettingsViewModel,
    navController: NavController
) {
    val showAlertManageWallet by viewModel.manageWalletShowAlertLiveData.observeAsState(false)
    val showAlertSecurityCenter by viewModel.securityCenterShowAlertLiveData.observeAsState(false)
    val showAlertAboutApp by viewModel.aboutAppShowAlertLiveData.observeAsState(false)
    val wcCounter by viewModel.wcCounterLiveData.observeAsState()
    val baseCurrency by viewModel.baseCurrencyLiveData.observeAsState()
    val language by viewModel.languageLiveData.observeAsState()
    val context = LocalContext.current

//    CellUniversalLawrenceSection(
//        listOf {
//            HsSettingCell(
//                R.string.Settings_Donate,
//                R.drawable.ic_heart_jacob_48,
//                onClick = {
//                    navController.slideFromRight(R.id.donateTokenSelectFragment)
//                }
//            )
//        }
//    )
//
//    VSpacer(32.dp)

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.SettingsSecurity_ManageKeys,
                R.drawable.ic_wallet_20,
                showAlert = showAlertManageWallet,
                onClick = {
                    navController.slideFromRight(
                        R.id.manageAccountsFragment,
                        bundleOf(ManageAccountsModule.MODE to ManageAccountsModule.Mode.Manage)
                    )
                }
            )
        }, {
            HsSettingCell(
                R.string.BlockchainSettings_Title,
                R.drawable.ic_blocks_20,
                onClick = {
                    navController.slideFromRight(R.id.blockchainSettingsFragment)
                }
            )
        },{
            HsSettingCell(
                R.string.BackupManager_Title,
                R.drawable.ic_file_24,
                onClick = {
                    navController.slideFromRight(R.id.backupManagerFragment)
                }
            )
        }
            )
    )

    VSpacer(32.dp)

//    CellUniversalLawrenceSection(
//        listOf {
//            HsSettingCell(
//                R.string.Settings_WalletConnect,
//                R.drawable.ic_wallet_connect_20,
//                value = (wcCounter as? CounterType.SessionCounter)?.number?.toString(),
//                counterBadge = (wcCounter as? CounterType.PendingRequestCounter)?.number?.toString(),
//                onClick = {
//                    when (val state = viewModel.getWalletConnectSupportState()) {
//                        WC2Manager.SupportState.Supported -> {
//                            navController.slideFromRight(R.id.wallet_connect_graph)
//                        }
//                        WC2Manager.SupportState.NotSupportedDueToNoActiveAccount -> {
//                            navController.slideFromBottom(R.id.wcErrorNoAccountFragment)
//                        }
//                        is WC2Manager.SupportState.NotSupportedDueToNonBackedUpAccount -> {
//                            val text = Translator.getString(R.string.WalletConnect_Error_NeedBackup)
//                            navController.slideFromBottom(
//                                R.id.backupRequiredDialog,
//                                BackupRequiredDialog.prepareParams(state.account, text)
//                            )
//                        }
//                        is WC2Manager.SupportState.NotSupported -> {
//                            navController.slideFromBottom(
//                                R.id.wcAccountTypeNotSupportedDialog,
//                                WCAccountTypeNotSupportedDialog.prepareParams(state.accountTypeDescription)
//                            )
//                        }
//                    }
//                }
//            )
//        }
//    )
//
//    VSpacer(32.dp)

    CellUniversalLawrenceSection(
        listOf(
            {
                HsSettingCell(
                    R.string.Settings_SecurityCenter,
                    R.drawable.ic_security,
                    showAlert = showAlertSecurityCenter,
                    onClick = {
                        navController.slideFromRight(R.id.securitySettingsFragment)
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Contacts,
                    R.drawable.ic_user_20,
                    onClick = {
                        navController.slideFromRight(R.id.contactsFragment, ContactsFragment.prepareParams(Mode.Full))
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Settings_Appearance,
                    R.drawable.ic_brush_20,
                    onClick = {
                        navController.slideFromRight(R.id.appearanceFragment)
                    }
                )
            },
            /*{
                HsSettingCell(
                    R.string.Settings_BaseCurrency,
                    R.drawable.ic_currency,
                    value = baseCurrency?.code,
                    onClick = {
                        navController.slideFromRight(R.id.baseCurrencySettingsFragment)
                    }
                )
            },*/
            {
                HsSettingCell(
                    R.string.Settings_Language,
                    R.drawable.ic_language,
                    value = language,
                    onClick = {
                        navController.slideFromRight(R.id.languageSettingsFragment)
                    }
                )
            },
        )
    )

    VSpacer(32.dp)

//    CellUniversalLawrenceSection(
//        listOf {
//            HsSettingCell(
//                R.string.Settings_ExperimentalFeatures,
//                R.drawable.ic_experimental,
//                onClick = {
//                    navController.slideFromRight(R.id.experimentalFeaturesFragment)
//                }
//            )
//        }
//    )
//
//    VSpacer(32.dp)

//    CellUniversalLawrenceSection(
//        listOf({
//            HsSettingCell(
//                R.string.Settings_Faq,
//                R.drawable.ic_faq_20,
//                onClick = {
//                    navController.slideFromRight(R.id.faqListFragment)
//                }
//            )
//        }, {
//            HsSettingCell(
//                R.string.Guides_Title,
//                R.drawable.ic_academy_20,
//                onClick = {
//                    navController.slideFromRight(R.id.academyFragment)
//                }
//            )
//        })
//    )
//
//    VSpacer(32.dp)

//    CellUniversalLawrenceSection(
//        listOf {
//            HsSettingCell(
//                R.string.SettingsAboutApp_Title,
//                R.drawable.ic_about_app_20,
//                showAlert = showAlertAboutApp,
//                onClick = {
//                    navController.slideFromRight(R.id.aboutAppFragment)
//                }
//            )
//        }
//    )
//
//    VSpacer(32.dp)
//
//    CellUniversalLawrenceSection(
//        listOf({
//            HsSettingCell(
//                R.string.Settings_RateUs,
//                R.drawable.ic_star_20,
//                onClick = { RateAppManager.openPlayMarket(context) }
//            )
//        }, {
//            HsSettingCell(
//                R.string.Settings_ShareThisWallet,
//                R.drawable.ic_share_20,
//                onClick = { shareAppLink(viewModel.appWebPageLink, context) }
//            )
//        }, {
//            HsSettingCell(
//                R.string.SettingsContact_Title,
//                R.drawable.ic_mail_24,
//                onClick = { navController.slideFromBottom(R.id.contactOptionsDialog) },
//            )
//        })
//    )
//
//    VSpacer(32.dp)
}

@Composable
private fun OwlTingSections(
    viewModel: MainSettingsViewModel,
    navController: NavController
) {
    val loginState by viewModel.loginState.collectAsState()
    val verifyState by viewModel.verifyState.collectAsState()
    val snackBarState by viewModel.snackBarState.collectAsState()

    when (snackBarState) {
        is SnackBarState.Loading ->
            HudHelper.showInProcessMessage(
                LocalView.current,
                R.string.Alert_Loading,
                SnackbarDuration.INDEFINITE
            )
        is SnackBarState.LogoutSuccess -> {
            HudHelper.showSuccessMessage(
                LocalView.current,
                stringResource(id = R.string.Auth_Logged_Out),
                SnackbarDuration.SHORT
            )
        }
        is SnackBarState.DeleteSuccess -> {
            HudHelper.showSuccessMessage(
                LocalView.current,
                stringResource(id = R.string.Settings_Account_Deleted),
                SnackbarDuration.SHORT
            )
        }
        is SnackBarState.Failed -> {
            HudHelper.showErrorMessage(
                LocalView.current,
                (snackBarState as SnackBarState.Failed).msg
            )
        }
        else -> null
    }

    val showNoWalletDialog = remember { mutableStateOf(false) }
    val showNoEvmDialog = remember { mutableStateOf(false) }
    val stateString = when (verifyState) {
        VerifyState.VERIFIED -> stringResource(id = R.string.Binding_Status_Bound)
        VerifyState.UNVERIFIED -> stringResource(id = R.string.Binding_Status_Verifying)
        VerifyState.REJECTED -> stringResource(id = R.string.Binding_Status_Reject)
        else -> stringResource(id = R.string.Binding_Status_Unbound)
    }
    val stateType = when (verifyState) {
        VerifyState.VERIFIED -> CellValueType.POSITIVE
        else -> CellValueType.NEGATIVE
    }
    CellSingleLineLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.Binding_Title,
                R.drawable.ic_bind,
                value = stateString,
                valueType = stateType,
                onClick = {
                    if (viewModel.canLogin()) {
                        if (viewModel.canBind()) {
                            if (loginState) {
                                when (verifyState) {
                                    VerifyState.VERIFIED -> navController.slideFromRight(R.id.bindingStatusFragment)
                                    VerifyState.REJECTED, VerifyState.UNFINISHED, VerifyState.NOT_FOUND -> navController.slideFromRight(
                                        R.id.bindingFormFragment
                                    )
                                    else -> {}
                                }
                            } else {
                                navController.slideFromRight(R.id.loginFragment)
                            }
                        } else {
                            showNoEvmDialog.value = true
                        }
                    } else {
                        showNoWalletDialog.value = true
                    }
                },
                showArrow = verifyState != VerifyState.UNVERIFIED
            )
        }
    )

    VSpacer(32.dp)

    AnimatedVisibility(
        visible = !loginState,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            title = stringResource(R.string.Settings_Login_Binding),
            onClick = {
                if (viewModel.canLogin()) {
                    if (viewModel.canBind()) {
                        navController.slideFromRight(R.id.loginFragment)
                    } else {
                        showNoEvmDialog.value = true
                    }
                } else {
                    showNoWalletDialog.value = true
                }
            },
        )
    }

    AnimatedVisibility(
        visible = loginState,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        OutlinedButtonDefault(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            title = stringResource(R.string.Settings_Logout), onClick = {
                viewModel.doLogout()
            },
            enabled = snackBarState !is SnackBarState.Loading
        )
    }

    VSpacer(32.dp)

    if (showNoWalletDialog.value) {
        SimpleAlertDialog(
            title = stringResource(R.string.Settings_No_Wallet_Title),
            message = stringResource(id = R.string.Settings_No_Wallet_Description),
            positiveOption = stringResource(id = R.string.Settings_No_Wallet_Confirm),
            onPositiveClick = {
                showNoWalletDialog.value = false
                viewModel.setCurrentTab(MainModule.MainNavigation.Balance)
            },
            negativeOption = stringResource(id = R.string.Settings_No_Wallet_Cancel),
            onNegativeClick = {
                showNoWalletDialog.value = false
            },
            onDismiss = { showNoWalletDialog.value = false }
        )
    }
    if (showNoEvmDialog.value) {
        SimpleAlertDialog(
            title = stringResource(R.string.Settings_No_Evm_Wallet_Title),
            message = stringResource(id = R.string.Settings_No_Evm_Wallet_Description),
            positiveOption = stringResource(id = R.string.Settings_No_Wallet_Confirm),
            onPositiveClick = {
                showNoEvmDialog.value = false
                viewModel.setCurrentTab(MainModule.MainNavigation.Balance)
            },
            negativeOption = stringResource(id = R.string.Settings_No_Wallet_Cancel),
            onNegativeClick = {
                showNoEvmDialog.value = false
            },
            onDismiss = { showNoEvmDialog.value = false }
        )
    }
}

enum class CellValueType {
    NORMAL,
    POSITIVE,
    NEGATIVE,
}

@Composable
fun HsSettingCell(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    value: String? = null,
    valueType: CellValueType = CellValueType.NORMAL,
    counterBadge: String? = null,
    showAlert: Boolean = false,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        Image(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
        )
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        if (counterBadge != null) {
            BadgeCount(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = counterBadge
            )
        } else if (value != null) {
//                subhead1_grey(
//                    text = value,
//                    maxLines = 1,
//                    modifier = Modifier.padding(horizontal = 8.dp)
//                )
            when (valueType) {
                CellValueType.POSITIVE -> subhead1_green(
                    text = value,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                CellValueType.NEGATIVE -> subhead1_red(
                    text = value,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                else -> subhead1_grey(
                    text = value,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        if (showAlert) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.ic_attention_red_20),
                contentDescription = null,
            )
            Spacer(Modifier.width(12.dp))
        }
        if (showArrow) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun SettingsFooter(viewModel: MainSettingsViewModel) {
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        caption_grey(
            text = stringResource(
                R.string.Settings_InfoTitleWithVersion,
                viewModel.appVersion
            )
        )

        Spacer(Modifier.height(32.dp))

        val showDeleteDialog = remember { mutableStateOf(false) }
        if (loginState) {
            Text(
                text = stringResource(R.string.Settings_Delete_Account),
                style = ComposeAppTheme.typography.micro,
                color = ComposeAppTheme.colors.grey,
                modifier = Modifier.clickable {
                    showDeleteDialog.value = true
                }
            )
        }

        if (showDeleteDialog.value) {
            SimpleAlertDialog(
                title = stringResource(R.string.Settings_Delete_Account),
                message = stringResource(R.string.Settings_Delete_Account_Description),
                positiveOption = stringResource(id = R.string.Settings_Delete),
                onPositiveClick = {
                    showDeleteDialog.value = false
                    viewModel.deleteAccount()
                },
                negativeOption = stringResource(id = R.string.Binding_Cancel),
                onNegativeClick = {
                    showDeleteDialog.value = false
                },
                onDismiss = { showDeleteDialog.value = false }
            )
        }
//        caption_grey(text = stringResource(R.string.Settings_InfoTitleWithVersion, appVersion).uppercase())
//        Divider(
//            modifier = Modifier
//                .width(100.dp)
//                .padding(top = 8.dp, bottom = 4.5.dp),
//            thickness = 0.5.dp,
//            color = ComposeAppTheme.colors.steel20
//        )
//        Text(
//            text = stringResource(R.string.Settings_InfoSubtitle),
//            style = ComposeAppTheme.typography.micro,
//            color = ComposeAppTheme.colors.grey,
//        )
//        Image(
//            modifier = Modifier
//                .padding(top = 32.dp)
//                .size(32.dp)
//                .clickable {
//                    LinkHelper.openLinkInAppBrowser(context, companyWebPage)
//                },
//            painter = painterResource(id = R.drawable.ic_company_logo),
//            contentDescription = null,
//        )
//        caption_grey(
//            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
//            text = stringResource(R.string.Settings_CompanyName),
//        )
    }
}

private fun shareAppLink(appLink: String, context: Context) {
    val shareMessage = Translator.getString(R.string.SettingsShare_Text) + "\n" + appLink + "\n"
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            Translator.getString(R.string.SettingsShare_Title)
        )
    )
}

@Preview
@Composable
private fun previewSettingsScreen() {
    ComposeAppTheme {
        Column {
            CellSingleLineLawrenceSection(
                listOf({
                    HsSettingCell(
                        R.string.Settings_Faq,
                        R.drawable.ic_faq_20,
                        showAlert = true,
                        onClick = { }
                    )
                }, {
                    HsSettingCell(
                        R.string.Guides_Title,
                        R.drawable.ic_academy_20,
                        onClick = { }
                    )
                })
            )

            Spacer(Modifier.height(32.dp))

            CellSingleLineLawrenceSection(
                listOf {
                    HsSettingCell(
                        R.string.Settings_WalletConnect,
                        R.drawable.ic_wallet_connect_20,
                        counterBadge = "13",
                        onClick = { }
                    )
                }
            )
        }
    }
}
