package io.horizontalsystems.bankwallet.owlwallet.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.DatePicker
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText_leah

@Composable
fun BirthdaySection(
    viewModel: RegisterViewModel
) {
    val uiState = viewModel.uiState

    Column {
        HeaderText_leah(stringResource(id = R.string.Register_Birthday))

        FormsInput(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable {
                    viewModel.onToggleDatePicker(true)
                },
            hint = stringResource(id = R.string.Binding_Kyc_Birth),
            pasteEnabled = false,
            singleLine = true,
            initial = uiState.birthdayState?.dataOrNull,
            state = uiState.birthdayState,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            onValueChange = {
                viewModel.clearDate()
            },
            enabled = false
        )

        if (uiState.showDatePicker) {
            DatePicker(
                onDismiss = {
                    viewModel.onToggleDatePicker(false)
                },
                onCancel = {
                    viewModel.onToggleDatePicker(false)
                },
                onPositiveButtonClick = {
                    viewModel.onDateSelected(it)
                },
                onNegativeButtonClick = {
                    viewModel.onToggleDatePicker(false)
                }
            )
        }
    }
}
