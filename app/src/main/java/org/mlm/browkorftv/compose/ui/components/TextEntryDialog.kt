package org.mlm.browkorftv.compose.ui.components

import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.*

@Composable
fun TextEntryDialog(
    title: String,
    initial: String,
    hint: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                AndroidView(
                    factory = {
                        EditText(it).apply {
                            setText(initial)
                            setSelection(text.length)
                            hint?.let { h -> setHint(h) }

                            // Try to show IME (may not always show on TV, but works with many TV keyboards)
                            post {
                                requestFocus()
                                val imm = context.getSystemService(InputMethodManager::class.java)
                                imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                    },
                    update = { et ->
                        // Keep Compose state in sync (avoid loops)
                        if (et.text.toString() != value) {
                            et.setText(value)
                            et.setSelection(et.text.length)
                        }
                        et.setOnEditorActionListener { v, _, _ ->
                            value = (v as EditText).text.toString()
                            false
                        }
                        et.addTextChangedListener(
                            object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    value = s?.toString().orEmpty()
                                }
                                override fun afterTextChanged(s: Editable?) {}
                            }
                        )
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(value) }) { Text("OK") }
                }
            }
        }
    }
}