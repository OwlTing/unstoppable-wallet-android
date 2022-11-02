package io.horizontalsystems.bankwallet.owlwallet.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
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
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.currentState) {
        is SnackBarState.Loading ->
            HudHelper.showInProcessMessage(
                LocalView.current,
                R.string.Alert_Loading,
                io.horizontalsystems.snackbar.SnackbarDuration.INDEFINITE
            )
        is SnackBarState.LoginSuccess -> {
            HudHelper.showSuccessMessage(
                LocalView.current,
                stringResource(id = R.string.Auth_Logged_In),
                io.horizontalsystems.snackbar.SnackbarDuration.SHORT
            )
            navController.popBackStack()
        }
        is SnackBarState.ResetPasswordSuccess -> {
            HudHelper.showSuccessMessage(
                LocalView.current,
                stringResource(id = R.string.Auth_Reset_Password_Success),
                io.horizontalsystems.snackbar.SnackbarDuration.SHORT
            )
        }
        is SnackBarState.Failed ->
            HudHelper.showErrorMessage(
                LocalView.current,
                stringResource(id = R.string.Auth_Reset_Password_Failed),
            )
        else -> null
    }

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
                var menuItems: List<MenuItem> = listOf()
                var navigationIcon: @Composable (() -> Unit)? = null

                navigationIcon = {
                    HsIconButton(onClick = {
                        Timber.d("onBackClick")
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
                AppBar(
                    title = TranslatableString.ResString(R.string.Settings_Login),
                    navigationIcon = navigationIcon,
                    menuItems = menuItems
                )

                Column {
                    HeaderText(stringResource(id = R.string.Auth_Email))

                    FormsInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        initial = "",
                        hint = stringResource(id = R.string.Auth_Email_Hint),
                        pasteEnabled = false,
                        singleLine = true,
                        state = if (uiState.emailState is DataState.Error) DataState.Error(
                            Throwable(stringResource(id = R.string.Auth_Invalid_Email))
                        ) else uiState.emailState,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                        ),
                        onValueChange = {
                            viewModel.onEmailChanged(it)
                        },
                        enabled = uiState.currentState !is SnackBarState.Loading
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column {
                    HeaderText(stringResource(id = R.string.Auth_Password))

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
                        enabled = uiState.currentState !is SnackBarState.Loading
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
                        viewModel.doLogin(
                            uiState.emailState!!.dataOrNull!!,
                            uiState.passwordState!!.dataOrNull!!
                        )
                    },
                    enabled = uiState.canLogin
                )
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    title = stringResource(R.string.Auth_Forget_Password),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.resetPassword(uiState.emailState!!.dataOrNull!!)
                    },
                    enabled = uiState.emailState != null
                            && uiState.emailState!!.dataOrNull != null
                            && uiState.currentState !is SnackBarState.Loading
                )
            }
        }
    }
}