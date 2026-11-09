package com.bramble.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Corner radius shared by cards, buttons and popups. */
val BrambleCorner = RoundedCornerShape(12.dp)

/**
 * Material's "extra large" shape is what pop-up dialogs (AlertDialog, the date picker) use by
 * default, so changing it here rounds every dialog at once. Everything else keeps Material's defaults.
 */
val BrambleShapes = Shapes(extraLarge = BrambleCorner)

/** The soft outline around pop-up boxes: the same one the new task card has. */
@Composable
fun popupBorderStroke(): BorderStroke = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))