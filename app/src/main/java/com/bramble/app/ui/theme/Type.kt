package com.bramble.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.bramble.app.R

/**
 * Huninn (SIL Open Font License, see /licenses). It only has a regular weight, so Android
 * fakes bold where a style asks for it. To try another font, swap the file in res/font and the name here.
 */
val BrambleFont = FontFamily(Font(R.font.huninn_regular, FontWeight.Normal))

private val Base = Typography()

/** Material's default sizes and spacing for every text style, with the Bramble font applied. */
val BrambleTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = BrambleFont),
    displayMedium = Base.displayMedium.copy(fontFamily = BrambleFont),
    displaySmall = Base.displaySmall.copy(fontFamily = BrambleFont),
    headlineLarge = Base.headlineLarge.copy(fontFamily = BrambleFont),
    headlineMedium = Base.headlineMedium.copy(fontFamily = BrambleFont),
    headlineSmall = Base.headlineSmall.copy(fontFamily = BrambleFont),
    titleLarge = Base.titleLarge.copy(fontFamily = BrambleFont),
    titleMedium = Base.titleMedium.copy(fontFamily = BrambleFont),
    titleSmall = Base.titleSmall.copy(fontFamily = BrambleFont),
    bodyLarge = Base.bodyLarge.copy(fontFamily = BrambleFont),
    bodyMedium = Base.bodyMedium.copy(fontFamily = BrambleFont),
    bodySmall = Base.bodySmall.copy(fontFamily = BrambleFont),
    labelLarge = Base.labelLarge.copy(fontFamily = BrambleFont),
    labelMedium = Base.labelMedium.copy(fontFamily = BrambleFont),
    labelSmall = Base.labelSmall.copy(fontFamily = BrambleFont),
)
