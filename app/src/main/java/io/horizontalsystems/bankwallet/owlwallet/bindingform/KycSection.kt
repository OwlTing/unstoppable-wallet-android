package io.horizontalsystems.bankwallet.owlwallet.bindingform

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.VerifyState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import timber.log.Timber
import java.util.*

@Composable
fun KycSection(
    navController: NavController,
    viewModel: BindingFormViewModel
) {
    val kycState = viewModel.kycState

    val title = when (viewModel.getVerifyState()) {
        VerifyState.VERIFIED, VerifyState.UNFINISHED -> stringResource(R.string.Binding_Description_Finished)
        else -> stringResource(R.string.Binding_Description)
    }

    Column {
        if (viewModel.getVerifyState() == VerifyState.REJECTED) {
            Row {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_attention_red_20),
                    contentDescription = null,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.Binding_Reject_Description),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    color = ComposeAppTheme.colors.redD,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.W700,
            color = ComposeAppTheme.colors.leah,
        )

        Spacer(modifier = Modifier.height(20.dp))

        FormsInput(
            hint = stringResource(id = R.string.Binding_Kyc_Name),
            pasteEnabled = false,
            singleLine = true,
            initial = if (viewModel.isKycFinished.value) kycState.nameState?.dataOrNull else null,
            state = kycState.nameState,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            onValueChange = {
                viewModel.onNameChanged(it)
            },
            enabled = !viewModel.isKycFinished.value,
            isFinished = viewModel.isKycFinished.value
        )

        Spacer(modifier = Modifier.height(4.dp))

        FormsInput(
            modifier = Modifier.clickable(
                enabled = !viewModel.isKycFinished.value,
            ) {
                if (!viewModel.isKycFinished.value) {
                    navController.slideFromRight(R.id.countryPickerFragment)
                }
            },
            hint = stringResource(id = R.string.Binding_Kyc_Nationality),
            pasteEnabled = false,
            singleLine = true,
            initial = kycState.nationalityState?.dataOrNull?.name ?: "",
            state = kycState.nationalityState,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            onValueChange = {
                viewModel.clearCountry()
            },
            enabled = false,
            isFinished = viewModel.isKycFinished.value
        )

        Spacer(modifier = Modifier.height(4.dp))

        FormsInput(
            modifier = Modifier.clickable(
                enabled = !viewModel.isKycFinished.value,
            ) {
                if (!viewModel.isKycFinished.value) {
                    viewModel.onToggleDatePicker(true)
                }
            },
            hint = stringResource(id = R.string.Binding_Kyc_Birth),
            pasteEnabled = false,
            singleLine = true,
            initial = kycState.birthdayState?.dataOrNull,
            state = kycState.birthdayState,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            onValueChange = {
                viewModel.clearDate()
            },
            enabled = false,
            isFinished = viewModel.isKycFinished.value,
        )

        if (kycState.showDatePicker) {
            val fragmentManager =
                (LocalContext.current as? FragmentActivity)?.supportFragmentManager
            DisposableEffect(fragmentManager) {
                var datePicker: MaterialDatePicker<*>? = null
                if (fragmentManager != null) {
                    val today = MaterialDatePicker.todayInUtcMilliseconds()
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                    calendar.timeInMillis = today
                    calendar.add(Calendar.YEAR, -18)
                    val end = calendar.timeInMillis

                    calendar.timeInMillis = today
                    calendar.add(Calendar.YEAR, -100)
                    val start = calendar.timeInMillis

                    val constraintsBuilder =
                        CalendarConstraints.Builder()
                            .setStart(start)
                            .setEnd(end)
                            .setValidator(DateValidatorPointBackward.before(end))

                    datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText(R.string.Binding_Kyc_Birth)
                        .setSelection(end)
                        .setCalendarConstraints(constraintsBuilder.build())
                        .build()
                    datePicker.addOnDismissListener {
                        viewModel.onToggleDatePicker(false)
                    }
                    datePicker.addOnCancelListener {
                        viewModel.onToggleDatePicker(false)
                    }
                    datePicker.addOnPositiveButtonClickListener {
                        viewModel.onDateSelected(it)
                    }
                    datePicker.addOnNegativeButtonClickListener {
                        viewModel.onToggleDatePicker(false)
                    }
                    datePicker.show(fragmentManager, javaClass.name)
                }
                onDispose {
                    datePicker?.dismiss()
                }
            }

        }
    }
}