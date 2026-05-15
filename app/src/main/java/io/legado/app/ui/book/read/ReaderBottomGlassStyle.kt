package io.legado.app.ui.book.read

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.qmdeve.liquidglass.widget.LiquidGlassView
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx

internal object ReaderBottomGlassStyle {

    fun glassLevel(): Float {
        return (AppConfig.frostedGlassLevel / 100f).coerceIn(0.45f, 1f)
    }

    fun shell(context: Context, glassLevel: Float, cornerRadius: Float): GradientDrawable {
        val isDark = useDarkGlass()
        val surfaceColor = if (isDark) {
            darkSurfaceColor()
        } else {
            ColorUtils.blendColors(Color.WHITE, context.accentColor, 0.05f)
        }
        val topAlpha = if (isDark) {
            0.42f + glassLevel * 0.14f
        } else {
            0.22f + glassLevel * 0.12f
        }
        val centerAlpha = if (isDark) {
            0.35f + glassLevel * 0.13f
        } else {
            0.16f + glassLevel * 0.09f
        }
        val bottomAlpha = if (isDark) {
            0.26f + glassLevel * 0.11f
        } else {
            0.10f + glassLevel * 0.07f
        }
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.withAlpha(surfaceColor, topAlpha.coerceAtMost(if (isDark) 0.56f else 0.34f)),
                ColorUtils.withAlpha(surfaceColor, centerAlpha.coerceAtMost(if (isDark) 0.48f else 0.25f)),
                ColorUtils.withAlpha(surfaceColor, bottomAlpha.coerceAtMost(if (isDark) 0.37f else 0.17f))
            )
        ).apply {
            this.cornerRadius = cornerRadius
        }
    }

    fun fallbackShell(context: Context, glassLevel: Float, cornerRadius: Float): GradientDrawable {
        val isDark = useDarkGlass()
        val surfaceColor = if (isDark) {
            darkSurfaceColor()
        } else {
            ColorUtils.blendColors(Color.WHITE, context.accentColor, 0.05f)
        }
        val alpha = if (isDark) {
            0.46f + glassLevel * 0.14f
        } else {
            0.20f + glassLevel * 0.14f
        }
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius
            setColor(ColorUtils.withAlpha(surfaceColor, alpha.coerceAtMost(if (isDark) 0.60f else 0.34f)))
        }
    }

    fun configureLiquidGlass(
        liquidGlassView: LiquidGlassView,
        target: ViewGroup,
        cornerRadius: Float,
        bindTarget: Boolean,
        glassLevel: Float = glassLevel()
    ): Boolean {
        if (!ViewCompat.isLaidOut(target) || !ViewCompat.isLaidOut(liquidGlassView)) {
            return false
        }
        if (bindTarget) {
            liquidGlassView.bind(target)
        }
        liquidGlassView.setCornerRadius(cornerRadius)
        liquidGlassView.setRefractionHeight((12f + glassLevel * 8f).dpToPx())
        liquidGlassView.setRefractionOffset((34f + glassLevel * 18f).dpToPx())
        liquidGlassView.setBlurRadius((22f + glassLevel * 30f).dpToPx())
        liquidGlassView.setDispersion((0.18f + glassLevel * 0.16f).coerceAtMost(0.42f))
        liquidGlassView.setTintAlpha(tintAlpha(glassLevel))
        tintColor().let { tintColor ->
            liquidGlassView.setTintColorRed(tintColor[0])
            liquidGlassView.setTintColorGreen(tintColor[1])
            liquidGlassView.setTintColorBlue(tintColor[2])
        }
        liquidGlassView.setDraggableEnabled(false)
        liquidGlassView.setElasticEnabled(false)
        liquidGlassView.setTouchEffectEnabled(false)
        liquidGlassView.isClickable = false
        liquidGlassView.isFocusable = false
        liquidGlassView.invalidate()
        return true
    }

    fun darkSurfaceColor(): Int {
        return Color.rgb(8, 10, 14)
    }

    fun tintAlpha(glassLevel: Float): Float {
        return if (useDarkGlass()) {
            (0.16f + glassLevel * 0.18f).coerceAtMost(0.34f)
        } else {
            (0.08f + glassLevel * 0.14f).coerceAtMost(0.22f)
        }
    }

    fun tintColor(): FloatArray {
        return if (useDarkGlass()) {
            floatArrayOf(0.08f, 0.10f, 0.14f)
        } else {
            floatArrayOf(0.70f, 0.79f, 0.86f)
        }
    }

    fun useDarkGlass(): Boolean {
        return AppConfig.isNightTheme && !AppConfig.isEInkMode
    }
}
