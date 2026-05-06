package io.legado.app.ui.main.bookshelf

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.dpToPx

class BookshelfShelfDecoration(
    context: Context,
    private val spanCountProvider: () -> Int
) : RecyclerView.ItemDecoration() {

    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sideShadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerShadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plankPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plankFrontPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cellRect = RectF()
    private val sideRect = RectF()
    private val plankRect = RectF()
    private val backRect = RectF()
    private val sideWidth = 26.dpToPx().toFloat()
    private val topInset = 28.dpToPx().toFloat()
    private val bookToPlankGap = 14.dpToPx().toFloat()
    private val plankHeight = 30.dpToPx().toFloat()
    private val frontHeight = 10.dpToPx().toFloat()
    private val bottomSpacing = 18.dpToPx()

    init {
        val density = context.resources.displayMetrics.density
        grainPaint.color = 0x1A7A4A24
        grainPaint.strokeWidth = density.coerceAtLeast(1f)
        sideShadePaint.color = 0x8046281A.toInt()
        innerShadePaint.color = 0x332A160D
        shadowPaint.color = 0x55331B10
        highlightPaint.color = 0x8AFFF4D6.toInt()
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!AppConfig.bookshelfShelfEffect) return
        val spanCount = spanCountProvider()
        if (spanCount < 2 || parent.childCount == 0) return

        val rows = linkedMapOf<Int, RowBounds>()
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            val row = position / spanCount
            val cover = child.findViewById<View>(R.id.iv_cover)
            val coverTop = cover?.let { child.top + it.top + child.translationY }
                ?: (child.top + child.translationY)
            val coverBottom = cover?.let { child.top + it.bottom + child.translationY }
                ?: (child.bottom + child.translationY)
            rows[row] = rows[row]?.include(coverTop, coverBottom)
                ?: RowBounds(coverTop, coverBottom)
        }

        val left = parent.paddingLeft.toFloat()
        val right = (parent.width - parent.paddingRight).toFloat()
        rows.values.forEach { bounds ->
            drawShelfCell(canvas, left, right, bounds)
        }
    }

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (!AppConfig.bookshelfShelfEffect || spanCountProvider() < 2) return
        outRect.bottom += bottomSpacing
    }

    private fun drawShelfCell(canvas: Canvas, left: Float, right: Float, bounds: RowBounds) {
        val cellTop = bounds.top - topInset
        val plankTop = bounds.bottom + bookToPlankGap
        val plankBottom = plankTop + plankHeight
        val contentLeft = left + sideWidth
        val contentRight = right - sideWidth

        backRect.set(contentLeft, cellTop, contentRight, plankBottom)
        backPaint.shader = LinearGradient(
            contentLeft,
            cellTop,
            contentRight,
            plankBottom,
            intArrayOf(0xFFD4BC8F.toInt(), 0xFFF0DDB6.toInt(), 0xFFC7A679.toInt()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(backRect, backPaint)
        backPaint.shader = null

        drawWoodGrain(canvas, contentLeft, contentRight, cellTop, plankBottom)

        sideRect.set(left, cellTop, contentLeft, plankBottom)
        canvas.drawRect(sideRect, sideShadePaint)
        sideRect.set(contentRight, cellTop, right, plankBottom)
        canvas.drawRect(sideRect, sideShadePaint)

        canvas.drawRect(contentLeft, cellTop, contentLeft + 10.dpToPx(), plankBottom, innerShadePaint)
        canvas.drawRect(contentRight - 10.dpToPx(), cellTop, contentRight, plankBottom, innerShadePaint)
        canvas.drawRect(contentLeft, cellTop, contentRight, cellTop + 10.dpToPx(), innerShadePaint)

        plankRect.set(left, plankTop - 5.dpToPx(), right, plankBottom)
        plankPaint.shader = LinearGradient(
            0f,
            plankTop,
            0f,
            plankBottom,
            intArrayOf(0xFFF4D39E.toInt(), 0xFFC49A67.toInt(), 0xFF7C4E2D.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(plankRect, plankPaint)
        plankPaint.shader = null

        plankRect.set(left, plankTop - 5.dpToPx(), right, plankTop - 1.dpToPx())
        canvas.drawRect(plankRect, highlightPaint)
        plankRect.set(left, plankBottom - frontHeight, right, plankBottom)
        plankFrontPaint.shader = LinearGradient(
            0f,
            plankBottom - frontHeight,
            0f,
            plankBottom,
            0xFFB88755.toInt(),
            0xFF4D2C1A.toInt(),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(plankRect, plankFrontPaint)
        plankFrontPaint.shader = null

        cellRect.set(left, plankBottom - 4.dpToPx(), right, plankBottom + 4.dpToPx())
        canvas.drawRect(cellRect, shadowPaint)
    }

    private fun drawWoodGrain(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ) {
        val width = right - left
        val height = bottom - top
        var x = left + 10.dpToPx()
        var index = 0
        while (x < right - 8.dpToPx()) {
            val wobble = ((index % 5) - 2) * 1.6f.dpToPx()
            canvas.drawLine(x, top + 8.dpToPx(), x + wobble, bottom - 8.dpToPx(), grainPaint)
            x += width / 18f
            index++
        }
        var y = top + height * 0.18f
        repeat(3) {
            canvas.drawLine(left + 8.dpToPx(), y, right - 8.dpToPx(), y + 1.dpToPx(), grainPaint)
            y += height * 0.24f
        }
    }

    private data class RowBounds(
        val top: Float,
        val bottom: Float
    ) {
        fun include(otherTop: Float, otherBottom: Float): RowBounds {
            return RowBounds(minOf(top, otherTop), maxOf(bottom, otherBottom))
        }
    }
}
