package io.legado.app.ui.book.read

import android.content.Context
import androidx.annotation.StringRes
import io.legado.app.constant.PageAnim
import io.legado.app.R
import kotlin.math.roundToInt

data class ReadMenuThemePreset(
    val key: String,
    @param:StringRes val labelRes: Int,
    val backgroundColor: Int,
    val textColor: Int,
    val bgType: Int = 0,
    val bgValue: String,
    @param:StringRes val layoutLabelRes: Int = R.string.read_menu_theme_layout_balanced,
    @param:StringRes val pageTurnLabelRes: Int = R.string.page_anim_cover,
    @param:StringRes val backgroundLabelRes: Int = labelRes,
    val textSize: Int = 20,
    val textWeight: Int = 50,
    val letterSpacing: Float = 0.1f,
    val lineSpacingExtra: Int = 12,
    val paragraphSpacing: Int = 2,
    val pageAnim: Int = PageAnim.coverPageAnim,
    val pageAnimationSpeed: Int = 300,
    val bgBrightness: Int = 50,
    val bgSaturation: Int = 50,
    val bgAlpha: Int = 100,
    val paperInkStrength: Int = 0,
    val previewTitle: String = "\u7b2c\u4e00\u7ae0 \u6625\u591c",
    val previewBody: String = "\u591c\u8272\u5fae\u51c9\uff0c\u661f\u5149\u6d12\u843d\n\u5728\u5bc2\u9759\u7684\u5ead\u9662\u91cc..."
) {
    fun summaryText(context: Context): String {
        return context.getString(
            R.string.read_menu_theme_suite_summary,
            context.getString(layoutLabelRes),
            context.getString(pageTurnLabelRes),
            context.getString(backgroundLabelRes)
        )
    }

    companion object {
        const val DEFAULT_PREVIEW_TITLE = "\u7b2c\u4e00\u7ae0 \u6625\u591c"
        const val DEFAULT_PREVIEW_BODY = "\u591c\u8272\u5fae\u51c9\uff0c\u661f\u5149\u6d12\u843d\n\u5728\u5bc2\u9759\u7684\u5ead\u9662\u91cc..."

        fun defaultPresets(): List<ReadMenuThemePreset> = listOf(
            ReadMenuThemePreset(
                key = "follow_system",
                labelRes = R.string.read_menu_theme_follow_system,
                backgroundColor = 0xFFF8F8FA.toInt(),
                textColor = 0xFF1D1D1F.toInt(),
                bgValue = "#F8F8FA",
                layoutLabelRes = R.string.read_menu_theme_layout_balanced,
                pageTurnLabelRes = R.string.page_anim_cover,
                backgroundLabelRes = R.string.read_menu_theme_follow_system
            ),
            ReadMenuThemePreset(
                key = "dark",
                labelRes = R.string.dark_theme,
                backgroundColor = 0xFF101010.toInt(),
                textColor = 0xFFEDEDED.toInt(),
                bgValue = "#101010",
                layoutLabelRes = R.string.read_menu_theme_layout_compact,
                pageTurnLabelRes = R.string.page_anim_none,
                backgroundLabelRes = R.string.dark_theme,
                textSize = 19,
                textWeight = 45,
                lineSpacingExtra = 10,
                paragraphSpacing = 1,
                pageAnim = PageAnim.noAnim,
                pageAnimationSpeed = 180,
                bgBrightness = 45,
                bgSaturation = 45
            ),
            ReadMenuThemePreset(
                key = "paper",
                labelRes = R.string.read_menu_theme_paper,
                backgroundColor = 0xFFF2E5D3.toInt(),
                textColor = 0xFF1F1A15.toInt(),
                bgValue = "#F2E5D3",
                layoutLabelRes = R.string.read_menu_theme_layout_comfort,
                pageTurnLabelRes = R.string.page_anim_simulation,
                backgroundLabelRes = R.string.read_menu_theme_paper,
                textSize = 21,
                lineSpacingExtra = 14,
                paragraphSpacing = 3,
                pageAnim = PageAnim.simulationPageAnim,
                pageAnimationSpeed = 360,
                bgBrightness = 52,
                bgSaturation = 54,
                paperInkStrength = 35
            ),
            ReadMenuThemePreset(
                key = "eye_green",
                labelRes = R.string.read_menu_theme_eye_green,
                backgroundColor = 0xFFE8EEDC.toInt(),
                textColor = 0xFF1E261B.toInt(),
                bgValue = "#E8EEDC",
                layoutLabelRes = R.string.read_menu_theme_layout_comfort,
                pageTurnLabelRes = R.string.page_anim_slide,
                backgroundLabelRes = R.string.read_menu_theme_eye_green,
                textSize = 20,
                lineSpacingExtra = 14,
                paragraphSpacing = 3,
                pageAnim = PageAnim.slidePageAnim,
                pageAnimationSpeed = 320,
                bgBrightness = 48,
                bgSaturation = 48
            ),
            ReadMenuThemePreset(
                key = "quiet_blue",
                labelRes = R.string.read_menu_theme_quiet_blue,
                backgroundColor = 0xFFE8ECF5.toInt(),
                textColor = 0xFF192033.toInt(),
                bgValue = "#E8ECF5",
                layoutLabelRes = R.string.read_menu_theme_layout_airy,
                pageTurnLabelRes = R.string.page_anim_linked_cover,
                backgroundLabelRes = R.string.read_menu_theme_quiet_blue,
                textSize = 21,
                letterSpacing = 0.12f,
                lineSpacingExtra = 16,
                paragraphSpacing = 4,
                pageAnim = PageAnim.linkedCoverPageAnim,
                pageAnimationSpeed = 340,
                bgBrightness = 50,
                bgSaturation = 46
            ),
            ReadMenuThemePreset(
                key = "night",
                labelRes = R.string.read_menu_theme_night,
                backgroundColor = 0xFF07172A.toInt(),
                textColor = 0xFFEAF2FF.toInt(),
                bgValue = "#07172A",
                layoutLabelRes = R.string.read_menu_theme_layout_compact,
                pageTurnLabelRes = R.string.page_anim_none,
                backgroundLabelRes = R.string.read_menu_theme_night,
                textSize = 19,
                textWeight = 45,
                lineSpacingExtra = 10,
                paragraphSpacing = 1,
                pageAnim = PageAnim.noAnim,
                pageAnimationSpeed = 180,
                bgBrightness = 42,
                bgSaturation = 42
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
