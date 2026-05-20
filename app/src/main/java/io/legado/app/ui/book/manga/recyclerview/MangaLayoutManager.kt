package io.legado.app.ui.book.manga.recyclerview

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class MangaLayoutManager(private val context: Context) :
    LinearLayoutManager(context) {

    private val extraLayoutSpace = context.resources.displayMetrics.heightPixels * 3 / 4

    fun smoothScrollToPositionWithOffset(position: Int, offset: Int) {
        val scroller = MangaSmoothScroller(context).apply {
            targetPosition = position
            this.offset = offset
        }
        startSmoothScroll(scroller)
    }

    @Deprecated("Deprecated in Java")
    override fun getExtraLayoutSpace(state: RecyclerView.State?): Int {
        return extraLayoutSpace
    }

    private class MangaSmoothScroller(context: Context) : LinearSmoothScroller(context) {
        var offset = 0

        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }

        override fun getHorizontalSnapPreference(): Int {
            return SNAP_TO_START
        }

        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int
        ): Int {
            return boxStart - viewStart + offset
        }
    }

}
