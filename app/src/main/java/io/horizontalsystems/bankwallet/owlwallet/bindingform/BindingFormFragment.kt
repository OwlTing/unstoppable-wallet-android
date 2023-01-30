package io.horizontalsystems.bankwallet.owlwallet.bindingform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration

class BindingFormFragment : BaseFragment() {

    private val viewModel by viewModels<BindingFormViewModel> { BindingFormModule.Factory() }

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
                BindingFormStatusScreen(findNavController(), viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onUpdateCountry()
    }

    override fun onDestroy() {
        viewModel.clearCountry()
        super.onDestroy()
    }
}

@Composable
fun BindingFormStatusScreen(
    navController: NavController,
    viewModel: BindingFormViewModel,
) {
    val showSentDialog = remember { mutableStateOf(false) }

    val actionState = viewModel.actionState
    if (actionState != null) {
        when (actionState) {
            is ActionState.Loading ->
                HudHelper.showInProcessMessage(
                    LocalView.current,
                    R.string.Alert_Loading,
                    SnackbarDuration.INDEFINITE
                )
            is ActionState.SendSuccess -> {
                HudHelper.showSuccessMessage(
                    LocalView.current,
                    stringResource(id = R.string.Binding_Sent_Title),
                    SnackbarDuration.SHORT
                )
                showSentDialog.value = true
            }
            is ActionState.Failed ->
                HudHelper.showErrorMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Reset_Password_Failed),
                )
            is ActionState.Expired -> {
                HudHelper.showErrorMessage(
                    LocalView.current,
                    stringResource(id = R.string.Auth_Reset_Password_Failed),
                )
                navController.popBackStack(R.id.mainFragment, false)
            }
        }
    }

    var navigationIcon: @Composable (() -> Unit)? = null
    navigationIcon = {
        HsIconButton(onClick = { navController.popBackStack() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "back",
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }

    ComposeAppTheme {
        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
                .verticalScroll(rememberScrollState())
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.Binding_Title),
                navigationIcon = navigationIcon,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .padding(16.dp)
            ) {
                AccountInfo(user = viewModel.getUser())

                Spacer(modifier = Modifier.height(32.dp))

                KycSection(navController, viewModel)

                Spacer(modifier = Modifier.height(40.dp))

                WalletOptionsSection(viewModel)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                SimplePolicyCheckbox(
                    checked = viewModel.getPrivacyPolicyState(),
                    onCheckedChange = {
                        viewModel.onTogglePrivacyPolicy(it)
                    },
                    description = stringResource(R.string.Binding_Policy_Confirm_Description),
                    policy = stringResource(id = R.string.Binding_Policy_Title),
                    url = App.getPrivacyPolicyUrl(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                title = stringResource(R.string.Binding_Send),
                onClick = {
                    viewModel.send()
                },
                enabled = viewModel.isFormCompleted()
            )

            if (showSentDialog.value) {
                SimpleAlertDialog(
                    title = stringResource(id = R.string.Binding_Sent_Title),
                    message = stringResource(id = R.string.Binding_Sent_Description),
                    positiveOption = stringResource(id = R.string.Binding_Confirm),
                    onPositiveClick = {
                        showSentDialog.value = false
                        navController.popBackStack(R.id.mainFragment, false)
                    },
                    onNegativeClick = {
                        showSentDialog.value = false
                        navController.popBackStack(R.id.mainFragment, false)
                    },
                    onDismiss = {
                        showSentDialog.value = false
                        navController.popBackStack(R.id.mainFragment, false)
                    }
                )
            }
        }
    }
}
