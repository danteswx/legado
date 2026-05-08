package io.legado.app.ui.book.read

import androidx.annotation.StringRes
import io.legado.app.R
import kotlin.math.roundToInt

data class ReadMenuThemePreset(
    val key: String,
    @StringRes val labelRes: Int,
    val backgroundColor: Int,
    val textColor: Int,
    val bgType: Int = 0,
    val bgValue: String,
    val previewTitle: String = "\u7b2c\u4e00\u7ae0 \u6625\u591c",
    val previewBody: String = "\u591c\u8272\u5fae\u51c9\uff0c\u661f\u5149\u6d12\u843d\n\u5728\u5bc2\u9759\u7684\u5ead\u9662\u91cc..."
) {
    companion object {
        fun defaultPresets(): List<ReadMenuThemePreset> = listOf(
            ReadMenuThemePreset(
                key = "follow_system",
                labelRes = R.string.read_menu_theme_follow_system,
                backgroundColor = 0xFFF8F8FA.toInt(),
                textColor = 0xFF1D1D1F.toInt(),
                bgValue = "#F8F8FA"
            ),
            ReadMenuThemePreset(
                key = "dark",
                labelRes = R.string.dark_theme,
                backgroundColor = 0xFF101010.toInt(),
                textColor = 0xFFEDEDED.toInt(),
                bgValue = "#101010"
            ),
            ReadMenuThemePreset(
                key = "paper",
                labelRes = R.string.read_menu_theme_paper,
                backgroundColor = 0xFFF2E5D3.toInt(),
                textColor = 0xFF1F1A15.toInt(),
                bgValue = "#F2E5D3"
            ),
            ReadMenuThemePreset(
                key = "eye_green",
                labelRes = R.string.read_menu_theme_eye_green,
                backgroundColor = 0xFFE8EEDC.toInt(),
                textColor = 0xFF1E261B.toInt(),
                bgValue = "#E8EEDC"
            ),
            ReadMenuThemePreset(
                key = "quiet_blue",
                labelRes = R.string.read_menu_theme_quiet_blue,
                backgroundColor = 0xFFE8ECF5.toInt(),
                textColor = 0xFF192033.toInt(),
                bgValue = "#E8ECF5"
            ),
            ReadMenuThemePreset(
                key = "night",
                labelRes = R.string.read_menu_theme_night,
                backgroundColor = 0xFF07172A.toInt(),
                textColor = 0xFFEAF2FF.toInt(),
                bgValue = "#07172A"
            )
        )

        fun contrastTextColor(baseTextColor: Int, backgroundColor: Int, progress: Int): Int {
            val normalized = ((progress.coerceIn(0, 100) - 50) / 100f).coerceIn(-0.5f, 0.5f)
            val lightBackground = luminance(backgroundColor) >= 0.5f
            val ratio = normalized.coerceAtLeast(0f) * if (lightBackground) 1f else 1.35f
            return if (lightBackground) {
                blend(baseTextColor, 0xFF000000.toInt(), ratio)
            } else {
                blend(baseTextColor, 0xFFFFFFFF.toInt(), ratio)
            }
        }

        private fun blend(from: Int, to: Int, ratio: Float): Int {
            val boundedRatio = ratio.coerceIn(0f, 1f)
            val r = channel(from, 16) +
                    ((channel(to, 16) - channel(from, 16)) * boundedRatio).roundToInt()
            val g = channel(from, 8) +
                    ((channel(to, 8) - channel(from, 8)) * boundedRatio).roundToInt()
            val b = channel(from, 0) +
                    ((channel(to, 0) - channel(from, 0)) * boundedRatio).roundToInt()
            return (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or
                    (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
        }

        private fun channel(color: Int, shift: Int): Int = color shr shift and 0xFF

        private fun luminance(color: Int): Float {
            val r = channel(color, 16) / 255f
            val g = channel(color, 8) / 255f
            val b = channel(color, 0) / 255f
            return 0.299f * r + 0.587f * g + 0.114f * b
        }
    }
}
