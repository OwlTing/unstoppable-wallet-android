package io.horizontalsystems.bankwallet.owlwallet.bindingform

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import timber.log.Timber

@Composable
fun BindingPolicy(viewModel: BindingFormViewModel) {
    CellLawrence(
        enabled = false,
        onClick = {}
    ) {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        HsCheckbox(
            checked = viewModel.getPrivacyPolicyState(),
            onCheckedChange = { checked ->
                viewModel.onTogglePrivacyPolicy(checked)
            },
        )
        Spacer(Modifier.width(20.dp))
        subhead2_leah(text = stringResource(R.string.Binding_Policy_Confirm_Description))
        TextButton(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = ComposeAppTheme.colors.yellowD,
            ),
            onClick = {
                try {
                    uriHandler.openUri(App.getPrivacyPolicyUrl())
                } catch (e: Exception) {
                    Timber.e(e, "Failed to open uri")
                }
            },
        ) {
            Text(stringResource(id = R.string.Binding_Policy_Title))
        }
    }
}