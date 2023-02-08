package io.horizontalsystems.bankwallet.owlwallet.forgotpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.SnackbarDuration

class ForgotPasswordFragment : BaseFragment() {

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
                ForgotPasswordScreen(findNavController())
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val viewModel = viewModel<ForgotPasswordViewModel>(factory = ForgotPasswordModule.Factory())
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
            is ActionState.ResetPasswordSuccess -> {
                HudHelper.showSuccessMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Reset_Password_Success),
                    SnackbarDuration.SHORT
                )
                navController.popBackStack()
            }
            is ActionState.Failed ->
                HudHelper.showErrorMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Reset_Password_Failed),
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
                AppBar(
                    title = TranslatableString.ResString(R.string.Auth_Forgot_Password),
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
                    }
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
                    Text(
                        stringResource(R.string.Auth_Forgot_Password_Hint),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W700,
                        color = ComposeAppTheme.colors.leah,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    HeaderText(stringResource(id = R.string.Auth_Email))

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

                Spacer(modifier = Modifier.height(30.dp))

                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    title = stringResource(R.string.Binding_Send),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.resetPassword()
                    },
                    enabled = uiState.canResetPassword
                )
            }
        }
    }
}