package io.legado.app.lib.theme

import android.content.Context
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.dpToPx

object UiCorner {

    fun panelRadius(context: Context): Float {
        return context.resources.getDimension(R.dimen.ui_panel_radius) * AppConfig.uiCornerScale
    }

    fun actionRadius(context: Context): Float {
        return context.resources.getDimension(R.dimen.ui_action_radius) * AppConfig.uiCornerScale
    }

    fun scaledDp(value: Float): Float {
        return value.dpToPx() * AppConfig.uiCornerScale
    }
}
