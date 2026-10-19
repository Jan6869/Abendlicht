@file:OptIn(ExperimentalTextApi::class)

package de.abendlicht.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.abendlicht.app.R
import de.abendlicht.app.Scene

// ── Schrift ───────────────────────────────────────────────────
// Fredoka: rund und freundlich für Titel. Nunito: weich und gut lesbar für alles andere.

private fun fredoka(w: Int) = Font(
    R.font.fredoka, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w))
)

private fun nunito(w: Int) = Font(
    R.font.nunito, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w))
)

val Fredoka = FontFamily(fredoka(400), fredoka(500), fredoka(600), fredoka(700))
val Nunito = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800))

// ── Farben ────────────────────────────────────────────────────

@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val accent: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFF7F1E6),
    surface = Color(0xFFFFFCF6),
    surfaceHigh = Color(0xFFFFFFFF),
    ink = Color(0xFF332D27),
    inkSoft = Color(0xFF8F857A),
    line = Color(0xFFEAE1D3),
    accent = Color(0xFFE8875B),
    dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF1B1A24),
    surface = Color(0xFF25232F),
    surfaceHigh = Color(0xFF2E2C3A),
    ink = Color(0xFFF4EEE5),
    inkSoft = Color(0xFFA49C93),
    line = Color(0xFF363341),
    accent = Color(0xFFF29A70),
    dark = true,
)

/** Farbwelt einer Karte. */
@Immutable
data class SceneColors(
    val top: Color,
    val bottom: Color,
    val title: Color,
    val a: Color,  // Hauptakzent
    val b: Color,  // zweiter Akzent
    val c: Color,  // dritter Akzent
    val glow: Color,
)

fun sceneColors(scene: Scene): SceneColors = when (scene) {
    Scene.WALK -> SceneColors(
        top = Color(0xFFE4F1C9), bottom = Color(0xFFB9DA8F), title = Color(0xFF3E5A28),
        a = Color(0xFF8DBB63), b = Color(0xFF66974A), c = Color(0xFFFFC95C), glow = Color(0xFFFFF4C9),
    )
    Scene.READ -> SceneColors(
        top = Color(0xFFFFF0B8), bottom = Color(0xFFFBD46C), title = Color(0xFF6B4A12),
        a = Color(0xFFE8875B), b = Color(0xFFFFFBF0), c = Color(0xFF9A6B3F), glow = Color(0xFFFFFAE3),
    )
    Scene.JOURNAL -> SceneColors(
        top = Color(0xFFDDE9F7), bottom = Color(0xFFADC8EA), title = Color(0xFF2E4A72),
        a = Color(0xFF6E92CF), b = Color(0xFFFFFAF0), c = Color(0xFFF3B562), glow = Color(0xFFFFFFFF),
    )
    Scene.STRETCH -> SceneColors(
        top = Color(0xFFFFE1D2), bottom = Color(0xFFF8AE91), title = Color(0xFF7A3520),
        a = Color(0xFF4F7BC0), b = Color(0xFFFFC93F), c = Color(0xFF7FB069), glow = Color(0xFFFFF1E8),
    )
    Scene.CALM -> SceneColors(
        top = Color(0xFFE6E0FA), bottom = Color(0xFFB5A8E6), title = Color(0xFF3E3175),
        a = Color(0xFF8C7DD6), b = Color(0xFFFFF5D6), c = Color(0xFF7AA6E0), glow = Color(0xFFFFFFFF),
    )
    Scene.GARDEN -> SceneColors(
        top = Color(0xFFD9F2E6), bottom = Color(0xFF9AD5BD), title = Color(0xFF1F5A47),
        a = Color(0xFF4FA584), b = Color(0xFFE8875B), c = Color(0xFFFF9EB0), glow = Color(0xFFF3FFF8),
    )
}

// ── Bewegung ──────────────────────────────────────────────────
// Kritisch gedämpfte Federn als Standard, ein Hauch Überschwingen nur bei „physischen“ Momenten.

fun <T> calmSpring() = spring<T>(dampingRatio = 1f, stiffness = 260f)
fun <T> liftSpring() = spring<T>(dampingRatio = 0.78f, stiffness = 340f)
fun <T> snappySpring() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun AbendlichtTheme(reduceMotion: Boolean, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(
        primary = p.accent, onPrimary = Color(0xFF2A1408), background = p.bg, surface = p.surface,
        surfaceContainerLow = p.surface, surfaceContainer = p.surfaceHigh, surfaceContainerHigh = p.surfaceHigh,
        onSurface = p.ink, onBackground = p.ink, onSurfaceVariant = p.inkSoft, outline = p.line,
        secondaryContainer = Color(0xFF3A3346), onSecondaryContainer = p.ink,
    ) else lightColorScheme(
        primary = p.accent, onPrimary = Color.White, background = p.bg, surface = p.surface,
        surfaceContainerLow = p.surface, surfaceContainer = p.surfaceHigh, surfaceContainerHigh = p.surfaceHigh,
        onSurface = p.ink, onBackground = p.ink, onSurfaceVariant = p.inkSoft, outline = p.line,
        secondaryContainer = Color(0xFFF6E6D6), onSecondaryContainer = p.ink,
    )
    val base = TextStyle(fontFamily = Nunito)
    val type = Typography(
        displaySmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.01).em),
        headlineSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 24.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(500), fontSize = 21.sp, lineHeight = 26.sp),
        titleMedium = base.copy(fontWeight = FontWeight(700), fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = base.copy(fontWeight = FontWeight(500), fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = base.copy(fontWeight = FontWeight(500), fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = base.copy(fontWeight = FontWeight(600), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em),
        labelLarge = base.copy(fontWeight = FontWeight(700), fontSize = 15.sp, lineHeight = 20.sp),
        labelMedium = base.copy(fontWeight = FontWeight(700), fontSize = 13.sp, lineHeight = 16.sp),
        labelSmall = base.copy(fontWeight = FontWeight(700), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em),
    )
    CompositionLocalProvider(LocalPalette provides p, LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
