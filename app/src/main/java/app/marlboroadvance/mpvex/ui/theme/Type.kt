package app.marlboroadvance.mpvex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// The system typeface with Material3's sizes, but lightened: the default
// type scale sets the title and label styles to Medium weight, which
// renders noticeably thicker than the reference player (mpvRx) the design
// follows. Only the weights change — no sizes, no line heights, and the
// font family stays the system default.
private val defaultTypography = Typography()

val AppTypography = Typography(
  titleMedium = defaultTypography.titleMedium.copy(fontWeight = FontWeight.Normal),
  titleSmall = defaultTypography.titleSmall.copy(fontWeight = FontWeight.Normal),
  labelLarge = defaultTypography.labelLarge.copy(fontWeight = FontWeight.Normal),
  labelMedium = defaultTypography.labelMedium.copy(fontWeight = FontWeight.Normal),
  labelSmall = defaultTypography.labelSmall.copy(fontWeight = FontWeight.Normal),
)
