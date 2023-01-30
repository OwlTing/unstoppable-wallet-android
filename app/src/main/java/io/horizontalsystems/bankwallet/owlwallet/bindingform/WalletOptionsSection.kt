package io.horizontalsystems.bankwallet.owlwallet.bindingform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import timber.log.Timber

@Composable
fun WalletOptionsSection(
    viewModel: BindingFormViewModel
) {
    Timber.d("WalletOptionsSection")
    Text(
        stringResource(R.string.Binding_Wallet_Account_Title),
        fontSize = 16.sp,
        fontWeight = FontWeight.W700,
        color = ComposeAppTheme.colors.leah,
    )

    Spacer(modifier = Modifier.height(20.dp))

    viewModel.walletOptions.forEachIndexed { index, option ->
        WalletOption(index, option, viewModel)
    }
}

@Composable
fun WalletOption(
    index: Int,
    wallet: Wallet,
    viewModel: BindingFormViewModel,
) {

    val checked = viewModel.walletSelections[index]

    CellLawrence(
        onClick = {
            viewModel.onToggleWalletOption(index, !checked)
        }
    ) {
        HsCheckbox(
            checked = checked,
            onCheckedChange = { checked ->
                viewModel.onToggleWalletOption(index, checked)
            },
        )
        Spacer(Modifier.width(20.dp))
        CoinImage(
            iconUrl = wallet.coin.iconUrl,
            placeholder = wallet.token.iconPlaceholder,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        headline2_leah(
            text = wallet.coin.code,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!wallet.badge.isNullOrBlank()) {
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
                    text = wallet.badge as String,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.microSB,
                    maxLines = 1,
                )
            }
        }
    }
}
