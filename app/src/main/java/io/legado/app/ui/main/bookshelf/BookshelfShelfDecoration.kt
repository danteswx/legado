package io.legado.app.ui.main.bookshelf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.UiCorner
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor

class BookshelfShelfDecoration(
    context: Context,
    private val spanCountProvider: () -> Int
) : RecyclerView.ItemDecoration() {

    private val shelfPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shelfRect = RectF()
    private val shelfHeight = 14.dpToPx().toFloat()
    private val shelfDepth = 6.dpToPx().toFloat()
    private val horizontalInset = 10.dpToPx().toFloat()
    private val radius = UiCorner.scaledDp(8f)

    init {
        val cardColor = context.getCompatColor(R.color.background_card)
        val textColor = context.getCompatColor(R.color.primaryText)
        shelfPaint.color = ColorUtils.setAlphaComponent(cardColor, 235)
        shadowPaint.color = ColorUtils.setAlphaComponent(textColor, 28)
        highlightPaint.color = ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), 72)
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!AppConfig.bookshelfShelfEffect) return
        val spanCount = spanCountProvider()
        if (spanCount < 2 || parent.childCount == 0) return

        val rowBottoms = linkedMapOf<Int, Float>()
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            val row = position / spanCount
            val coverBottom = child.findViewById<View>(R.id.iv_cover)?.let { cover ->
                child.top + cover.bottom + child.translationY
            } ?: (child.bottom + child.translationY)
            rowBottoms[row] = maxOf(rowBottoms[row] ?: Float.MIN_VALUE, coverBottom)
        }

        val left = parent.paddingLeft + horizontalInset
        val right = parent.width - parent.paddingRight - horizontalInset
        rowBottoms.values.forEach { coverBottom ->
            val top = coverBottom - shelfDepth
            val bottom = top + shelfHeight
            shelfRect.set(left, top, right, bottom)
            canvas.drawRoundRect(shelfRect, radius, radius, shadowPaint)
            shelfRect.set(left, top - 1.dpToPx(), right, top + 2.dpToPx())
            canvas.drawRoundRect(shelfRect, radius, radius, highlightPaint)
            shelfRect.set(left, top, right, bottom - 2.dpToPx())
            canvas.drawRoundRect(shelfRect, radius, radius, shelfPaint)
        }
    }
}
