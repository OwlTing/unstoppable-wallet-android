package io.horizontalsystems.bankwallet.modules.send.stellar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressInputModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.address.AddressViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.SendScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import timber.log.Timber

@Composable
fun SendStellarScreen(
    title: String,
    navController: NavController,
    viewModel: SendStellarViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val addressError = uiState.addressError
    val amountCaution = uiState.amountCaution
    var proceedEnabled = uiState.proceedEnabled
    val amountInputType = amountInputModeViewModel.inputType

    val paymentAddressViewModel =
        viewModel<AddressParserViewModel>(factory = AddressParserModule.Factory(wallet.token.blockchainType))
    val amountUnique = paymentAddressViewModel.amountUnique

    val addressViewModel = viewModel<AddressViewModel>(
        factory = AddressInputModule.FactoryToken(wallet.token.tokenQuery, wallet.coin.code, null),
        key = "address_view_model_${wallet.token.tokenQuery.id}"
    )
    when (addressViewModel.inputState) {
        is DataState.Success -> {

        }

        is DataState.Error -> {
            Timber.e("AddressViewModel error: ${addressViewModel.inputState}")
            when ((addressViewModel.inputState as DataState.Error).error) {
                is AddressValidationException.Unsupported -> {
                    proceedEnabled = false
                }
            }
        }

        DataState.Loading -> {
            proceedEnabled = false
        }

        null -> {
            proceedEnabled = false
        }
    }



    ComposeAppTheme {
        val fullCoin = wallet.token.fullCoin
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            title = title,
            onCloseClick = { navController.popBackStack() }
        ) {
            AvailableBalance(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputType = amountInputType,
                rate = viewModel.coinRate
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAmountInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                focusRequester = focusRequester,
                availableBalance = availableBalance,
                caution = amountCaution,
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                onClickHint = {
                    amountInputModeViewModel.onToggleInputType()
                },
                onValueChange = {
                    viewModel.onEnterAmount(it)
                },
                inputType = amountInputType,
                rate = viewModel.coinRate,
                amountUnique = amountUnique
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                tokenQuery = wallet.token.tokenQuery,
                coinCode = wallet.coin.code,
                error = addressError,
                textPreprocessor = paymentAddressViewModel,
                navController = navController,

                ) {
                viewModel.onAddressSport(it)
            }

            Spacer(modifier = Modifier.height(12.dp))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = viewModel.memo,
                hint = stringResource(id = R.string.Stellar_Memo),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
                onValueChange = {
                    viewModel.onEnterMemo(it)
                },
            )

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {

                    viewModel.onNavigateToConfirmation()

                    navController.slideFromRight(
                        R.id.sendConfirmation,
                        SendConfirmationFragment.prepareParams(
                            SendConfirmationFragment.Type.Stellar,
                            sendEntryPointDestId
                        )
                    )
                },
                enabled = proceedEnabled
            )
        }
    }
}
