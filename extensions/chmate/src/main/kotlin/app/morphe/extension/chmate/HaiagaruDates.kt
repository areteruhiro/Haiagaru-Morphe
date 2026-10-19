package app.morphe.extension.chmate

import android.app.Activity
import androidx.activity.ComponentDialog
import androidx.compose.material3.*
import androidx.compose.ui.platform.ComposeView
import java.util.Calendar
import java.util.TimeZone

object HaiagaruDates {
    fun interface Selected { fun select(year: Int, month: Int, day: Int) }
    @OptIn(ExperimentalMaterial3Api::class)
    @JvmStatic fun show(activity: Activity, initial: Calendar, selected: Selected) {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
        }
        val dialog = ComponentDialog(HaiagaruComposeActivity.dialogContext(activity))
        dialog.setContentView(ComposeView(dialog.context).apply {
            setContent { HaiagaruUi.Theme {
                val state = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
                DatePickerDialog(onDismissRequest = { dialog.dismiss() },
                    dismissButton = { TextButton(onClick = { dialog.dismiss() }) { Text("キャンセル") } },
                    confirmButton = { TextButton(onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                            selected.select(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
                        }
                        dialog.dismiss()
                    }) { Text("選択") } }) { DatePicker(state) }
            } }
        })
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }
}
