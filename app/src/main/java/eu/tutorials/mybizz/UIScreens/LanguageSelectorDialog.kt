package eu.tutorials.mybizz.UIScreens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.tutorials.mybizz.R
import eu.tutorials.mybizz.language.LocaleManager

/**
 * A dialog that lets the user pick one of the supported languages.
 * When confirmed it persists the choice via [LocaleManager] and restarts
 * the host Activity so every screen reflects the new language instantly.
 *
 * Drop this anywhere in your composable tree:
 *
 *   var showLanguagePicker by remember { mutableStateOf(false) }
 *   if (showLanguagePicker) {
 *       LanguageSelectorDialog(onDismiss = { showLanguagePicker = false })
 *   }
 */
@Composable
fun LanguageSelectorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Read the currently persisted language so we can pre-select it.
    val currentLanguage = remember { LocaleManager.getSelectedLanguage(context) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_language)) },
        text = {
            Column {
                LocaleManager.supportedLanguages.forEach { (code, nativeName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == code,
                            onClick = { selectedLanguage = code }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = nativeName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Persist + rebuild context, then restart to apply everywhere.
                    LocaleManager.setLocale(context, selectedLanguage)
                    activity?.let { LocaleManager.restartActivity(it) }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}