package com.bhagavatam.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Interface font for English. */
val Jakarta = FontFamily(
    variable(R.font.jakarta, 400), variable(R.font.jakarta, 500),
    variable(R.font.jakarta, 600), variable(R.font.jakarta, 700),
)

/**
 * English translation and IAST reading text. Stand-in for Lexicon, which is a licensed typeface:
 * to use it, add lexicon_regular.ttf (and bold) to res/font and point this family at them.
 */
val EnglishReading = FontFamily(
    variable(R.font.literata, 400), variable(R.font.literata, 500), variable(R.font.literata, 600),
)

/** Sanskrit shlokas in Devanagari: Noto Sans Devanagari. */
val NotoDevanagari = FontFamily(
    variable(R.font.noto_sans_devanagari, 400), variable(R.font.noto_sans_devanagari, 500),
    variable(R.font.noto_sans_devanagari, 600), variable(R.font.noto_sans_devanagari, 700),
)

/** Hindi translation text. */
val TiroHindi = FontFamily(Font(R.font.tiro_hindi))

/** Bengali translation text and Sanskrit in Bengali script: Noto Serif Bengali. */
val NotoSerifBengali = FontFamily(
    variable(R.font.noto_serif_bengali, 400), variable(R.font.noto_serif_bengali, 500), variable(R.font.noto_serif_bengali, 600),
)

/** Interface font when the app language is Hindi. */
val Mukta = FontFamily(
    Font(R.font.mukta_regular, FontWeight.Normal), Font(R.font.mukta_semibold, FontWeight.SemiBold),
    Font(R.font.mukta_semibold, FontWeight.Bold),
)

/** Interface font when the app language is Bengali. */
val HindSiliguri = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal), Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_semibold, FontWeight.Bold),
)

fun appTypography(ui: FontFamily) = Typography(
    displaySmall = TextStyle(fontFamily = ui, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontFamily = ui, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = ui, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = ui, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = ui, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = ui, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = ui, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = ui, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
