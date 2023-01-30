package io.horizontalsystems.bankwallet.owlwallet.bindingform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.User
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun AccountInfo(
    user: User
) {
    Row {
        AsyncImage(
            model = user.avatar,
            contentDescription = "avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                user.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                color = ComposeAppTheme.colors.leah,
            )
            Text(
                user.email,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400,
                color = ComposeAppTheme.colors.leah,
            )
        }
    }
}