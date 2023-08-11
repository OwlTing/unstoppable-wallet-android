package io.horizontalsystems.bankwallet.modules.versioncheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.owlwallet.utils.UpdateAction
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah

@Composable
fun VersionCheck(
    updateAction: UpdateAction,
    onUpdateClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancelClick
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color = ComposeAppTheme.colors.lawrence)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            title3_leah(
                text = stringResource(
                    if (updateAction == UpdateAction.Immediate)
                        R.string.VersionCheck_Immediate_Title
                    else
                        R.string.VersionCheck_Flexible_Title
                )
            )
            Spacer(Modifier.height(12.dp))
            body_leah(
                text = stringResource(
                    if (updateAction == UpdateAction.Immediate)
                        R.string.VersionCheck_Immediate_Description
                    else
                        R.string.VersionCheck_Flexible_Description
                )
            )
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (updateAction != UpdateAction.Immediate) {
                    ButtonPrimaryTransparent(
                        onClick = onCancelClick,
                        title = stringResource(R.string.VersionCheck_No)
                    )

                    Spacer(Modifier.width(8.dp))
                }

                ButtonPrimaryYellow(
                    onClick = onUpdateClick,
                    title = stringResource(R.string.VersionCheck_Update)
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_RateApp() {
    ComposeAppTheme {
        VersionCheck(UpdateAction.Immediate, {}, {})
    }
}
