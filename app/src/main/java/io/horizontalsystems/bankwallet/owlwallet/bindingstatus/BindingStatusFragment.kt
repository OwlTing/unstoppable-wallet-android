package io.horizontalsystems.bankwallet.owlwallet.bindingstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.SnackbarDuration

class BindingStatusFragment : BaseFragment() {

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
                BindingStatusScreen(findNavController())
            }
        }
    }
}

@Composable
fun BindingStatusScreen(
    navController: NavController,
) {
    val viewModel = viewModel<BindingStatusViewModel>(factory = BindingStatusModule.Factory())
    val actionState = viewModel.actionState

    if (actionState != null) {
        when (actionState) {
            is ActionState.Loading ->
                HudHelper.showInProcessMessage(
                    LocalView.current,
                    R.string.Alert_Loading,
                    SnackbarDuration.INDEFINITE
                )
            is ActionState.UnbindAllSuccess -> {
                HudHelper.showSuccessMessage(
                    LocalView.current,
                    stringResource(id = R.string.Binding_Unbind),
                    SnackbarDuration.SHORT
                )
                navController.popBackStack()
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
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = stringResource(R.string.Binding_Title),
                navigationIcon = navigationIcon,
            )

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                viewModel.items.forEachIndexed { index, item ->
                    Spacer(modifier = Modifier.height(10.dp))
                    BindingStatusItem(index, item)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val showRebindDialog = remember { mutableStateOf(false) }
                TextButtonYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    title = stringResource(R.string.Binding_Rebind),
                    onClick = {
                        showRebindDialog.value = true
                    },
                    enabled = actionState != ActionState.Loading
                )

                Spacer(modifier = Modifier.height(16.dp))

                val showUnbindDialog = remember { mutableStateOf(false) }
                OutlinedButtonDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    title = stringResource(R.string.Binding_Unbind), onClick = {
                        showUnbindDialog.value = true
                    },
                    enabled = actionState != ActionState.Loading && viewModel.hasWalletToUnbind.value
                )

                if (showRebindDialog.value) {
                    SimpleAlertDialog(
                        title = stringResource(R.string.Binding_Rebind_Title),
                        message = stringResource(id = R.string.Binding_Rebind_Description),
                        positiveOption = stringResource(id = R.string.Binding_Confirm),
                        onPositiveClick = {
                            showRebindDialog.value = false
                            navController.slideFromRight(R.id.bindingFormFragment)
                        },
                        negativeOption = stringResource(id = R.string.Binding_Cancel),
                        onNegativeClick = {
                            showRebindDialog.value = false
                        },
                        onDismiss = { showRebindDialog.value = false }
                    )
                }

                if (showUnbindDialog.value) {
                    SimpleAlertDialog(
                        title = stringResource(R.string.Binding_Unbind_Description),
                        positiveOption = stringResource(id = R.string.Binding_Unbind),
                        onPositiveClick = {
                            showUnbindDialog.value = false
                            viewModel.unbindAll()
                        },
                        negativeOption = stringResource(id = R.string.Binding_Back),
                        onNegativeClick = {
                            showUnbindDialog.value = false
                        },
                        onDismiss = { showUnbindDialog.value = false }
                    )
                }
            }
        }
    }
}



