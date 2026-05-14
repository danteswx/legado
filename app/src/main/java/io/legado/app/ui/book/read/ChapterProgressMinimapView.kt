package io.legado.app.ui.book.read

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import io.legado.app.utils.dpToPx
import kotlin.math.roundToInt

class ChapterProgressMinimapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onProgressChanging: ((progressRatio: Float) -> Unit)? = null
    var onProgressChanged: ((progressRatio: Float) -> Unit)? = null

    private val trackRect = RectF()
    private val thumbRect = RectF()
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f.dpToPx()
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f.dpToPx()
    }
    private val contentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 3.2f.dpToPx()
        color = ColorUtils.setAlphaComponent(Color.WHITE, 88)
        letterSpacing = 0f
    }

    private var chapterText: String = ""
    private var contentLayout: StaticLayout? = null
    private var contentLayoutWidth = -1
    private var pageCount: Int = 0
    private var progress: Int = 0
    private var dragRatio: Float? = null
    private var dragThumbTouchOffset = 0f
    private var isDragging = false

    init {
        isClickable = true
        isFocusable = true
    }

    fun updateChapter(content: String, pageCount: Int, progress: Int) {
        val safeContent = content.ifBlank { " " }
        if (chapterText != safeContent) {
            chapterText = safeContent
            contentLayout = null
        }
        updateProgress(pageCount, progress)
    }

    fun updateProgress(pageCount: Int, progress: Int) {
        val safePageCount = pageCount.coerceAtLeast(0)
        val safeProgress = progress.coerceIn(0, (safePageCount - 1).coerceAtLeast(0))
        if (this.pageCount == safePageCount && this.progress == safeProgress && !isDragging) {
            return
        }
        this.pageCount = safePageCount
        this.progress = safeProgress
        invalidate()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width != oldWidth || height != oldHeight) {
            contentLayout = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pageCount <= 1) {
            return
        }
        updateTrackRect()
        drawTrack(canvas)
        drawContent(canvas)
        drawThumb(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || pageCount <= 1) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                isPressed = true
                isDragging = true
                beginDrag(event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateDragFromY(event.y)
                return true
            }

            MotionEvent.ACTION_UP -> {
                commitDrag(event.y)
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                finishDrag()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun finishDrag() {
        parent.requestDisallowInterceptTouchEvent(false)
        isPressed = false
        isDragging = false
        dragRatio = null
        dragThumbTouchOffset = 0f
        invalidate()
    }

    private fun beginDrag(y: Float) {
        updateTrackRect()
        val thumbHeight = thumbHeight(trackRect.height())
        val travel = (trackRect.height() - thumbHeight).coerceAtLeast(0f)
        val thumbTop = trackRect.top + travel * progressRatio()
        dragThumbTouchOffset = (y - thumbTop).coerceIn(0f, thumbHeight)
        dragRatio = progressRatio()
        invalidate()
    }

    private fun updateDragFromY(y: Float) {
        updateTrackRect()
        val ratio = ratioForY(y)
        dragRatio = ratio
        invalidate()
        onProgressChanging?.invoke(ratio)
    }

    private fun commitDrag(y: Float) {
        updateDragFromY(y)
        val ratio = dragRatio ?: progressRatio()
        progress = pageForRatio(ratio)
        finishDrag()
        onProgressChanged?.invoke(ratio)
    }

    private fun ratioForY(y: Float): Float {
        val maxPage = (pageCount - 1).coerceAtLeast(0)
        if (maxPage == 0 || trackRect.height() <= 0f) {
            return 0f
        }
        val thumbHeight = thumbHeight(trackRect.height())
        val travel = (trackRect.height() - thumbHeight).coerceAtLeast(1f)
        val thumbTop = (y - dragThumbTouchOffset).coerceIn(trackRect.top, trackRect.bottom - thumbHeight)
        return ((thumbTop - trackRect.top) / travel).coerceIn(0f, 1f)
    }

    private fun pageForRatio(ratio: Float): Int {
        val maxPage = (pageCount - 1).coerceAtLeast(0)
        return (ratio * maxPage).toInt().coerceIn(0, maxPage)
    }

    private fun progressRatio(): Float {
        return if (pageCount <= 1) 0f else progress / (pageCount - 1).toFloat()
    }

    private fun updateTrackRect() {
        val horizontalInset = 5f.dpToPx()
        trackRect.set(
            paddingLeft + horizontalInset,
            paddingTop.toFloat(),
            width - paddingRight - horizontalInset,
            height - paddingBottom.toFloat()
        )
    }

    private fun drawTrack(canvas: Canvas) {
        trackPaint.color = ColorUtils.setAlphaComponent(Color.rgb(18, 24, 32), 86)
        canvas.drawRect(trackRect, trackPaint)
        outlinePaint.color = ColorUtils.setAlphaComponent(Color.WHITE, 42)
        canvas.drawRect(trackRect, outlinePaint)
    }

    private fun drawContent(canvas: Canvas) {
        val contentInsetX = 6f.dpToPx()
        val contentInsetY = 7f.dpToPx()
        val textWidth = (trackRect.width() - contentInsetX * 2).roundToInt().coerceAtLeast(1)
        val layout = chapterContentLayout(textWidth)
        val availableHeight = (trackRect.height() - contentInsetY * 2).coerceAtLeast(1f)
        val scaleY = (availableHeight / layout.height.coerceAtLeast(1)).coerceAtMost(1f)

        canvas.save()
        canvas.clipRect(trackRect)
        canvas.translate(trackRect.left + contentInsetX, trackRect.top + contentInsetY)
        canvas.scale(1f, scaleY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun chapterContentLayout(textWidth: Int): StaticLayout {
        val current = contentLayout
        if (current != null && contentLayoutWidth == textWidth) {
            return current
        }
        return StaticLayout.Builder.obtain(chapterText, 0, chapterText.length, contentPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, 0.78f)
            .build()
            .also {
                contentLayout = it
                contentLayoutWidth = textWidth
            }
    }

    private fun drawThumb(canvas: Canvas) {
        val thumbHeight = thumbHeight(trackRect.height())
        val travel = (trackRect.height() - thumbHeight).coerceAtLeast(0f)
        val ratio = dragRatio ?: progressRatio()
        val top = trackRect.top + travel * ratio.coerceIn(0f, 1f)
        thumbRect.set(
            trackRect.left + 2f.dpToPx(),
            top,
            trackRect.right - 2f.dpToPx(),
            top + thumbHeight
        )
        thumbPaint.color = ColorUtils.setAlphaComponent(Color.WHITE, if (isPressed) 96 else 72)
        canvas.drawRect(thumbRect, thumbPaint)
        thumbStrokePaint.color = ColorUtils.setAlphaComponent(Color.WHITE, if (isPressed) 236 else 200)
        canvas.drawRect(thumbRect, thumbStrokePaint)
    }

    private fun thumbHeight(trackHeight: Float): Float {
        if (pageCount <= 0) {
            return trackHeight
        }
        return (trackHeight / pageCount).coerceIn(44f.dpToPx(), trackHeight)
    }
}
