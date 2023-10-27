package io.horizontalsystems.bankwallet.owlwallet.login

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.SnackbarDuration
import timber.log.Timber

class LoginFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                LoginScreen(findNavController())
            }
        }
    }
}

@Composable
fun LoginScreen(
    navController: NavController,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val viewModel = viewModel<LoginViewModel>(factory = LoginModule.Factory())
    val uiState = viewModel.uiState
    val actionState = viewModel.actionState

    if (actionState != null) {
        when (actionState) {
            is ActionState.Loading ->
                HudHelper.showInProcessMessage(
                    LocalView.current,
                    R.string.Alert_Loading,
                    SnackbarDuration.INDEFINITE
                )
            is ActionState.LoginSuccess -> {
                HudHelper.showSuccessMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Logged_In),
                    SnackbarDuration.SHORT
                )
                navController.popBackStack()
                if (!actionState.isBindingSent) {
                    navController.slideFromRight(R.id.bindingFormFragment)
                }
            }
            is ActionState.Failed ->
                HudHelper.showErrorMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Reset_Password_Failed),
                )
            is ActionState.AccountDeleted ->
                HudHelper.showErrorMessage(
                    LocalView.current,
                    stringResource(id = R.string.Settings_Account_Deleted_Error),
                )
        }
        viewModel.resetActionState()
    }

    val context = LocalContext.current

    ComposeAppTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .padding(paddingValues)
                    .fillMaxHeight()
                    .background(color = ComposeAppTheme.colors.tyler)
            ) {
                val qrScannerLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val scannedText =
                                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                            viewModel.doLoginByToken(scannedText)
                        }
                    }

                AppBar(
                    title = stringResource(R.string.Settings_Login),
                    navigationIcon = {
                        HsIconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                        }
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Settings_Scan),
                            icon = R.drawable.ic_qr_scan_20,
                            showIconAndTitle = true,
                            onClick = {
                                qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                            }
                        )
                    )
                )

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Column {

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .height(30.dp)
                        ) {
                            Image(
                                painter = painterResource(id = if (isSystemInDarkTheme()) R.drawable.ic_login_logo_dark else R.drawable.ic_login_logo),
                                contentDescription = "",
                                contentScale = ContentScale.FillHeight,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        HeaderText_leah(stringResource(id = R.string.Auth_Email))

                        FormsInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            initial = "",
                            hint = stringResource(id = R.string.Auth_Email_Hint),
                            pasteEnabled = false,
                            singleLine = true,
                            state = when (uiState.emailState) {
                                is DataState.Success -> uiState.emailState
                                is DataState.Error -> DataState.Error(Throwable(stringResource(R.string.Auth_Invalid_Email)))
                                else -> null
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                            ),
                            onValueChange = {
                                viewModel.onEmailChanged(it)
                            },
                            enabled = actionState !is ActionState.Loading
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Column {
                        HeaderText_leah(stringResource(id = R.string.Auth_Password))

                        FormsInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            initial = "",
                            hint = stringResource(id = R.string.Auth_Password_Hint),
                            pasteEnabled = false,
                            singleLine = true,
                            state = uiState.passwordState,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            onValueChange = {
                                viewModel.onPasswordChanged(it)
                            },
                            enabled = actionState !is ActionState.Loading
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        title = stringResource(R.string.Settings_Login),
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.doLogin()
                        },
                        enabled = uiState.canLogin
                    )

                    TextButtonYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        title = stringResource(R.string.Settings_Register),
                        onClick = {
                            navController.slideFromRight(R.id.registerFragment)
                        },
                    )

                    TextButtonYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        title = stringResource(R.string.Auth_Forgot_Password) + "？",
                        onClick = {
                            focusManager.clearFocus()
                            navController.slideFromRight(R.id.forgotPasswordFragment)
                        },
                    )
                }
            }
        }
    }
}
