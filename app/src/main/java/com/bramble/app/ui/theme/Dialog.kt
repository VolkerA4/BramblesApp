package com.bramble.app.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * The app's pop-up box. It takes the same slots as Material's AlertDialog, but draws the rounded,
 * outlined box itself and centers the buttons along the bottom (Material's always sits them at the
 * right). Dismiss comes first, then confirm, so "Cancel  Save" reads left to right.
 *
 * @param fullWidthText true when the body brings its own side padding, like the date picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrambleDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    fullWidthText: Boolean = false,
    properties: DialogProperties = DialogProperties(),
) {
    val colors = MaterialTheme.colorScheme
    BasicAlertDialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            shape = BrambleCorner,
            color = colors.surfaceContainerHigh,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            border = popupBorderStroke(),
        ) {
            Column(Modifier.padding(vertical = 24.dp)) {
                if (title != null) {
                    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) { title() }
                        }
                    }
                }
                if (text != null) {
                    // weight(fill = false): a tall body shrinks and scrolls instead of pushing the buttons off screen.
                    Box(Modifier.weight(1f, fill = false).padding(horizontal = if (fullWidthText) 0.dp else 24.dp)) {
                        CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
