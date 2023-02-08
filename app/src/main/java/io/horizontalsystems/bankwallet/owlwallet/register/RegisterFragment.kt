package io.horizontalsystems.bankwallet.owlwallet.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.SnackbarDuration

class RegisterFragment : BaseFragment() {

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
                RegisterScreen(findNavController())
            }
        }
    }
}

@Composable
fun RegisterScreen(
    navController: NavController,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val viewModel = viewModel<RegisterViewModel>(factory = RegisterModule.Factory())
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
            is ActionState.RegisterSuccess -> {
                HudHelper.showSuccessMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Logged_In),
                    SnackbarDuration.SHORT
                )
                navController.popBackStack(R.id.mainFragment, false)
                if (!actionState.isBindingSent) {
                    navController.slideFromRight(R.id.bindingFormFragment)
                }
            }
            is ActionState.Failed ->
                HudHelper.showErrorMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Reset_Password_Failed),
                )
        }
        viewModel.resetActionState()
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
                    .verticalScroll(rememberScrollState())
            ) {

                AppBar(
                    title = TranslatableString.ResString(R.string.Settings_Register_Title),
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
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                        .padding(vertical = 16.dp)
                ) {
                    Column {
                        HeaderText_leah(stringResource(id = R.string.Register_Display_Name))

                        FormsInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            initial = "",
                            hint = stringResource(id = R.string.Register_Display_Name_Hint),
                            pasteEnabled = false,
                            singleLine = true,
                            state = when (uiState.displayNameState) {
                                is DataState.Success -> uiState.displayNameState
                                is DataState.Error -> DataState.Error(Throwable())
                                else -> null
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                            ),
                            onValueChange = {
                                viewModel.onDisplayNameChanged(it)
                            },
                            enabled = actionState !is ActionState.Loading
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Column {
                        HeaderText_leah(stringResource(id = R.string.Auth_Email))

                        FormsInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            initial = "",
                            hint = stringResource(id = R.string.Register_Email_Hint),
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
                    PasswordSection(viewModel)
                    Spacer(modifier = Modifier.height(32.dp))
                    BirthdaySection(viewModel)
                    Spacer(modifier = Modifier.height(32.dp))
                    GenderOptionsSection(viewModel)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                ) {
                    SimplePolicyCheckbox(
                        checked = viewModel.getPrivacyPolicyState(),
                        onCheckedChange = {
                            viewModel.onTogglePrivacyPolicy(it)
                        },
                        description = stringResource(R.string.Register_Privacy_Hint),
                        policy = stringResource(id = R.string.Register_Privacy_Policy),
                        url = App.getPrivacyPolicyUrl(),
                    )
                }

                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    title = stringResource(R.string.Settings_Register),
                    onClick = {
                        viewModel.doRegister()
                    },
                    enabled = viewModel.isFormCompleted.value
                )
            }
        }
    }
}