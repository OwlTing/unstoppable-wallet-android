package io.horizontalsystems.bankwallet.owlwallet.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText_leah
import io.horizontalsystems.bankwallet.ui.compose.components.HsRadioButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

@Composable
fun GenderOptionsSection(
    viewModel: RegisterViewModel
) {
    val genderOption = viewModel.genderOption
    Column {
        HeaderText_leah(stringResource(id = R.string.Register_Gender))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable {
                    viewModel.onGenderOptionChanged(GenderOption.MALE)
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HsRadioButton(
                selected = genderOption == GenderOption.MALE,
                onClick = {
                    viewModel.onGenderOptionChanged(GenderOption.MALE)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                body_leah(text = stringResource(R.string.Register_Gender_Male))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable {
                    viewModel.onGenderOptionChanged(GenderOption.FEMALE)
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HsRadioButton(
                selected = genderOption == GenderOption.FEMALE,
                onClick = {
                    viewModel.onGenderOptionChanged(GenderOption.FEMALE)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                body_leah(text = stringResource(R.string.Register_Gender_Female))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable {
                    viewModel.onGenderOptionChanged(GenderOption.RATHER_NOT_SAY)
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HsRadioButton(
                selected = genderOption == GenderOption.RATHER_NOT_SAY,
                onClick = {
                    viewModel.onGenderOptionChanged(GenderOption.RATHER_NOT_SAY)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                body_leah(text = stringResource(R.string.Register_Gender_Rather_Not_Say))
            }
        }
    }
}