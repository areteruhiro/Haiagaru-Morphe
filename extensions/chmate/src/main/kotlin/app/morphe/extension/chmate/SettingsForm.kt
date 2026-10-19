package app.morphe.extension.chmate

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Adapts the existing preference bindings to Compose controls, without mounting legacy Views.
 * Original listeners remain the single writer, preserving validation and restart behavior. */
internal data class BoundSetting(val control: View, val label: String) {
    val keywords: String get() = label + " " + when (control) {
        is EditText -> control.hint?.toString().orEmpty() // Never index entered text or credentials.
        is TextView -> control.text.toString()
        else -> ""
    }
}

internal fun prepareSettingsBindings(root: ViewGroup) {
    for (index in 0 until root.childCount) {
        val view = root.getChildAt(index)
        if (view is LiveSettings.Choice) {
            // Deliver the initial selection normally sent by an attached Spinner.
            // LiveSettings suppresses saves for unchanged selections.
            view.onItemSelectedListener?.onItemSelected(view, null, view.selectedItemPosition, view.selectedItemId)
        } else if (view is ViewGroup) prepareSettingsBindings(view)
    }
}

internal fun settingsRows(root: ViewGroup): List<BoundSetting> {
    val result = mutableListOf<BoundSetting>()
    fun collect(parent: ViewGroup) {
        var index = 0
        while (index < parent.childCount) {
            val view = parent.getChildAt(index++)
            if (view.visibility != View.VISIBLE) continue
            val next = parent.getChildAt(index)
            if (view is TextView && view !is Button && view !is EditText &&
                !view.hasOnClickListeners() && (next is EditText || next is Spinner)) {
                index++
                if (next.visibility == View.VISIBLE) result.add(BoundSetting(next, view.text.toString()))
            } else if (view is ViewGroup && view !is Spinner) collect(view)
            else if (view is TextView || view is Spinner || view is ImageView) {
                val label = if (view is EditText) view.hint?.toString().orEmpty()
                    else if (view is TextView) view.text.toString() else view.contentDescription?.toString().orEmpty()
                result.add(BoundSetting(view, label))
            }
        }
    }
    collect(root)
    return result
}

@Composable
internal fun BoundSettingRow(item: BoundSetting, changed: () -> Unit) {
    val view = item.control
    when (view) {
        is CompoundButton -> {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .toggleable(view.isChecked, enabled = view.isEnabled, role = Role.Switch) {
                    view.isChecked = it; changed()
                }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(item.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    color = if (view.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(checked = view.isChecked, onCheckedChange = null, enabled = view.isEnabled)
            }
        }
        is EditText -> BoundTextField(view, item.label, changed = changed)
        is ImageView -> view.drawable?.let { drawable ->
            val bitmap = remember(drawable) { drawable.toBitmap(width = 96, height = 96).asImageBitmap() }
            Image(bitmap, view.contentDescription?.toString(), modifier = Modifier.size(56.dp).padding(8.dp),
                colorFilter = if (view.tag == false) androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface) else null)
        }
        is Spinner -> {
            var choosing by remember(view) { mutableStateOf(false) }
            ListItem(headlineContent = { Text(item.label) },
                supportingContent = { Text(view.selectedItem?.toString().orEmpty()) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.clickable(enabled = view.isEnabled) { choosing = true })
            if (choosing) AlertDialog(onDismissRequest = { choosing = false },
                title = { Text(item.label) }, text = {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(view.count) { index ->
                            Row(Modifier.fillMaxWidth().clickable {
                                view.setSelection(index)
                                // Offscreen Spinners have no layout pass to deliver selection callbacks.
                                view.onItemSelectedListener?.onItemSelected(view, null, index, view.getItemIdAtPosition(index))
                                choosing = false; changed()
                            }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = view.selectedItemPosition == index, onClick = null)
                                Text(view.getItemAtPosition(index).toString(), Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { choosing = false }) { Text("閉じる") } })
        }
        is Button -> ListItem(headlineContent = { Text(item.label) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth().clickable(enabled = view.isEnabled) { view.performClick(); changed() })
        is TextView -> {
            if (view.hasOnClickListeners()) TextButton(onClick = { view.performClick(); changed() },
                modifier = Modifier.fillMaxWidth()) { Text(view.text.toString()) }
            else if (view.text.isNotBlank()) Text(view.text.toString(), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
internal fun BoundTextField(view: EditText, title: String, enabled: Boolean = view.isEnabled, changed: () -> Unit = {}) {
    // TextFieldValue preserves cursor/selection/composition while the native binding validates text.
    var value by remember(view) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(view.text.toString())) }
    val native = view.text.toString()
    if (value.text != native) value = androidx.compose.ui.text.input.TextFieldValue(native)
    val numeric = view.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER
    val secret = view.inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_TEXT_VARIATION_PASSWORD
    OutlinedTextField(value = value, onValueChange = {
        value = it
        if (it.text != view.text.toString()) view.setText(it.text)
        changed()
    }, label = { Text(title.ifBlank { "入力" }) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        enabled = enabled, singleLine = view.maxLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        isError = view.error != null,
        supportingText = if (view.error != null) ({ Text(view.error.toString()) }) else null,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
}
