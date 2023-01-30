package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import io.horizontalsystems.bankwallet.R
import java.util.*

@Composable
fun DatePicker(
    onDismiss: (() -> Unit),
    onCancel: (() -> Unit),
    onPositiveButtonClick: ((selection: Long) -> Unit),
    onNegativeButtonClick: (() -> Unit),
) {
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
                onDismiss()
            }
            datePicker.addOnCancelListener {
                onCancel()
            }
            datePicker.addOnPositiveButtonClickListener {
                onPositiveButtonClick(it)
            }
            datePicker.addOnNegativeButtonClickListener {
                onNegativeButtonClick()
            }
            datePicker.show(fragmentManager, javaClass.name)
        }
        onDispose {
            datePicker?.dismiss()
        }
    }
}