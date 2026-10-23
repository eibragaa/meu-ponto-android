package br.com.jean.meuponto.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tema, tokens de cor e tipografia do app.
 */

// Tokens da identidade visual. São a única fonte de cor do app: um composable deve usar estes
// valores ou MaterialTheme.colorScheme, nunca um hex solto (ver docs/engineering-standards.md).
internal val Ink = Color(0xFF112032)
internal val InkSoft = Color(0xFF304052)
internal val Teal = Color(0xFF157C70)
internal val TealDark = Color(0xFF0B5C53)
internal val Mint = Color(0xFFD8F1E2)
internal val Coral = Color(0xFFFF8A72)
internal val Cream = Color(0xFFF8F7F2)
internal val Mist = Color(0xFFE9EFEC)
internal val Line = Color(0xFFD6E0DB)
internal val Muted = Color(0xFF6B7773)

/**
 * Escala tipográfica do app: uma fonte só (sans-serif do sistema) e tamanhos fixos por papel,
 * para que o MaterialTheme aplique a mesma hierarquia em todas as telas.
 */
private val AppTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
/**
 * Tema do app. Os esquemas claro/escuro são nomeados por papel do Material 3; a marca (teal/coral)
 * aparece em `primary`/`secondary` e as superfícies vêm dos tokens acima.
 *
 * [darkTheme] é decidido pelo usuário (persistido em preferências) e cai no tema do sistema na
 * primeira execução.
 */
internal fun MeuPontoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            androidx.compose.material3.darkColorScheme(
                primary = Color(0xFF74D9C6),
                onPrimary = Color(0xFF00382F),
                primaryContainer = Color(0xFF075448),
                onPrimaryContainer = Color(0xFF9AF5E2),
                secondary = Color(0xFFFFA38F),
                onSecondary = Color(0xFF4A160D),
                tertiary = Color(0xFF9BDCCB),
                background = Color(0xFF0C141A),
                onBackground = Color(0xFFE8F0ED),
                surface = Color(0xFF121E25),
                onSurface = Color(0xFFE8F0ED),
                surfaceVariant = Color(0xFF203038),
                onSurfaceVariant = Color(0xFFB6C7C2),
                outline = Color(0xFF42565D)
            )
        } else {
            androidx.compose.material3.lightColorScheme(
                primary = Teal,
                onPrimary = Color.White,
                primaryContainer = Mint,
                onPrimaryContainer = TealDark,
                secondary = Coral,
                onSecondary = Ink,
                background = Cream,
                onBackground = Ink,
                surface = Color.White,
                onSurface = Ink,
                surfaceVariant = Mist,
                onSurfaceVariant = InkSoft,
                outline = Line
            )
        },
        typography = AppTypography,
        shapes = androidx.compose.material3.Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp)),
        content = content
    )
}
