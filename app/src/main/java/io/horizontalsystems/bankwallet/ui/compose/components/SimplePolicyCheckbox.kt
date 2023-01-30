package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import timber.log.Timber

@Composable
fun SimplePolicyCheckbox(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    description: String,
    policy: String,
    url: String,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val uriHandler = LocalUriHandler.current
        HsCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(Modifier.width(20.dp))

        val annotatedString = buildAnnotatedString {
            pushStringAnnotation(tag = "description", annotation = "")
            withStyle(
                style = SpanStyle(
                    color = ComposeAppTheme.colors.leah
                )
            ) {
                append(description)
            }
            pop()
            append(" ")
            pushStringAnnotation(tag = "policy", annotation = url)
            withStyle(
                style = SpanStyle(
                    color = ComposeAppTheme.colors.yellowD,
                )
            ) {
                append(policy)
            }
            pop()
        }

        ClickableText(
            text = annotatedString,
            style = ComposeAppTheme.typography.body,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "policy", start = offset, end = offset)
                    .firstOrNull()?.let {
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to open uri")
                        }
                    }
            }
        )
    }
}
