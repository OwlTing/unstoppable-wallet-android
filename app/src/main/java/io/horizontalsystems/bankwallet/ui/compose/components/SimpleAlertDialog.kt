package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun SimpleAlertDialog(
    title: String,
    message: String? = null,
    positiveOption: String,
    onPositiveClick: () -> Unit,
    negativeOption: String? = null,
    onNegativeClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val negativeButton: (@Composable () -> Unit)? = if (negativeOption != null) {
        {
            TextButton(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                ),
                onClick = onNegativeClick
            ) {
                Text(text = negativeOption)
            }
        }
    } else null

    val dialogMessage: (@Composable () -> Unit)? = if (message != null) {
        {
            Text(message)
        }
    } else null

    AlertDialog(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = ComposeAppTheme.colors.yellowD,
                ),
                onClick = onPositiveClick
            ) {
                Text(text = positiveOption)
            }
        },
        dismissButton = negativeButton,
        title = { Text(text = title, style = ComposeAppTheme.typography.headline2) },
        text = dialogMessage,
        backgroundColor = ComposeAppTheme.colors.tyler,
        contentColor = ComposeAppTheme.colors.leah,
    )
}
