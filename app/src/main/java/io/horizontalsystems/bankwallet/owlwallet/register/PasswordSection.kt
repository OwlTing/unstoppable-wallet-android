package io.horizontalsystems.bankwallet.owlwallet.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah

@Composable
fun PasswordSection(
    viewModel: RegisterViewModel
) {
    val uiState = viewModel.uiState
    val actionState = viewModel.actionState

    Column {
        HeaderText_leah(stringResource(id = R.string.Auth_Password))
        FormsInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            initial = "",
            hint = stringResource(id = R.string.Register_Password_Hint),
            pasteEnabled = false,
            singleLine = true,
            state = uiState.passwordState,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = {
                viewModel.onPasswordChanged(it)
            },
            enabled = actionState !is ActionState.Loading
        )
        Spacer(modifier = Modifier.height(10.dp))
        FormsInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            initial = "",
            hint = stringResource(id = R.string.Register_Password_Hint2),
            pasteEnabled = false,
            singleLine = true,
            state = uiState.password2State,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = {
                viewModel.onPassword2Changed(it)
            },
            enabled = actionState !is ActionState.Loading
        )
        Spacer(modifier = Modifier.height(10.dp))
        subhead2_leah(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.Register_Password_Description)
        )
//        if ((uiState.passwordState != null || uiState.password2State != null) &&
//            (uiState.passwordState !is DataState.Success || uiState.password2State !is DataState.Success)
//        ) {
//            Spacer(modifier = Modifier.height(10.dp))
//            subhead2_red(
//                modifier = Modifier.padding(horizontal = 16.dp),
//                text = stringResource(R.string.Register_Password_Invalid)
//            )
//        }
    }
}