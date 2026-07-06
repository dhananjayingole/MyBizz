package eu.tutorials.mybizz.UIScreens

// AppDesign.kt
// Shared design tokens for MyBizz — drop this in eu.tutorials.mybizz.ui.theme
// Import AppColors / AppDimens into any screen to keep visuals consistent.

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MyBizz color system.
 * Primary  — deep slate blue: trust, stability, "this handles my money correctly"
 * Accent   — teal: used sparingly for primary actions / active states
 * Status colors are semantic and used ONLY for status (paid/unpaid/pending),
 * never as decoration, so they stay meaningful at a glance.
 */
object AppColors {
    // Core brand
    val Primary = Color(0xFF1E3A5F)          // deep slate blue
    val PrimaryDark = Color(0xFF13253F)
    val PrimaryLight = Color(0xFF2E5580)
    val Accent = Color(0xFF2E9E8F)           // teal — primary action color

    // Neutrals
    val Background = Color(0xFFF6F7FB)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEEF1F6)
    val Border = Color(0xFFE3E7EF)
    val TextPrimary = Color(0xFF1A1F2B)
    val TextSecondary = Color(0xFF6B7280)
    val TextMuted = Color(0xFF9AA1AC)

    // Semantic status
    val Success = Color(0xFF1F9254)
    val SuccessBg = Color(0xFFE6F5EC)
    val Warning = Color(0xFFB9770E)
    val WarningBg = Color(0xFFFCF1DD)
    val Danger = Color(0xFFC0392B)
    val DangerBg = Color(0xFFFBEAE8)
    val InfoBg = Color(0xFFE8EEF7)
}

object AppDimens {
    val ScreenPadding = 16.dp
    val CardPadding = 16.dp
    val SectionGap = 20.dp
    val ItemGap = 10.dp
    val CardRadius = 16.dp
    val ChipRadius = 20.dp
    val SmallRadius = 10.dp
}

object AppShapes {
    val Card = RoundedCornerShape(AppDimens.CardRadius)
    val CardSmall = RoundedCornerShape(AppDimens.SmallRadius)
    val Chip = RoundedCornerShape(AppDimens.ChipRadius)
    val Button = RoundedCornerShape(12.dp)
}