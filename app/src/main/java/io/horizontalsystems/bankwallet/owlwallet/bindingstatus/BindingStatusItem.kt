package io.horizontalsystems.bankwallet.owlwallet.bindingstatus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_green
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey

@Composable
fun BindingStatusItem(
    index: Int,
    item: StatusItem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(16.dp)
    ) {
        CoinImage(
            iconUrl = item.wallet.coin.iconUrl,
            placeholder = item.wallet.token.iconPlaceholder,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        headline2_leah(
            text = item.wallet.coin.code,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!item.wallet.badge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ComposeAppTheme.colors.jeremy)
            ) {
                Text(
                    modifier = Modifier.padding(
                        start = 4.dp,
                        end = 4.dp,
                        bottom = 1.dp
                    ),
                    text = item.wallet.badge as String,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.microSB,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        when (item.status) {
            BindingStatus.ANOTHER_WALLET -> subhead1_grey(
                text = stringResource(R.string.Binding_Status_Not_IN_Wallet),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            BindingStatus.UNBIND -> subhead1_grey(
                text = stringResource(R.string.Binding_Status_Unbound),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            BindingStatus.BOUND -> subhead1_green(
                text = stringResource(R.string.Binding_Status_Bound),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}