package com.mittylabs.elaps.ui.main

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller

typealias OnScrollListener = (Int) -> Unit

class SliderLayoutManager private constructor(
    context: Context,
    @RecyclerView.Orientation sliderOrientation: Int = RecyclerView.HORIZONTAL,
    private val initialIndex: Int = 0,
    private val onScrollListener: OnScrollListener? = null,
    private val scaling: Scaling? = null,
) : LinearLayoutManager(context, sliderOrientation, false) {
    private lateinit var recyclerView: RecyclerView
    private var currentPosition = 0
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, DRAGGING_UPDATE_INTERVAL_MS)
            calculateCenterIndex()
        }
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        recyclerView = view

        view.clipToPadding = false
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // set padding offset to align start/end to the middle
                setPaddingOffset(view, orientation)

                // set initial position
                scrollToPositionWithOffset(initialIndex, getInitialItemOffset(orientation))
            }
        })

        LinearSnapHelper().attachToRecyclerView(view)
    }

    override fun onScrollStateChanged(state: Int) {
        if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            handler.post(runnable)
        } else if (state == RecyclerView.SCROLL_STATE_IDLE) {
            handler.removeCallbacks(runnable)
            calculateCenterIndex()
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        if (scaling != null) scaleView()
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        if (orientation == VERTICAL && scaling != null) scaleView()
        return super.scrollVerticallyBy(dy, recycler, state)
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        if (orientation == HORIZONTAL && scaling != null) scaleView()
        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        val smoothScroller: SmoothScroller = CenterSmoothScroller(recyclerView?.context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    private fun setPaddingOffset(
        view: RecyclerView,
        orientation: Int
    ) {
        if (orientation == HORIZONTAL) {
            val padding = view.width / HALF_INT
            view.setPadding(padding, 0, padding, 0)
        } else {
            val padding = view.height / HALF_INT
            view.setPadding(0, padding, 0, padding)
        }
    }

    private fun getInitialItemOffset(
        orientation: Int
    ) = if (orientation == HORIZONTAL) {
        -(getChildAt(0)?.width?.div(HALF_INT) ?: 0)
    } else {
        -(getChildAt(0)?.height?.div(HALF_INT) ?: 0)
    }

    private fun scaleView() {
        val recyclerViewCenter = getParentCenter()
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: return
            val diff = Math.abs(recyclerViewCenter - getChildCenter(child))
            val scale = getScaling(diff)
            child.scaleX = scale
            child.scaleY = scale
        }
    }

    private fun getScaling(diff: Float): Float {
        val widthOrHeight = if (orientation == HORIZONTAL) width else height
        return when (scaling) {
            Scaling.Linear -> 1 - diff / widthOrHeight
            is Scaling.Logarithmic -> {
                1 - Math.sqrt((diff / widthOrHeight).toDouble()).toFloat() * scaling.multiplier
            }
            else -> 1F
        }
    }

    private fun getParentCenter() = (if (orientation == HORIZONTAL) width else height) / HALF

    private fun getChildCenter(child: View) = if (orientation == HORIZONTAL) {
        (getDecoratedLeft(child) + getDecoratedRight(child)) / HALF
    } else {
        (getDecoratedTop(child) + getDecoratedBottom(child)) / HALF
    }

    private fun calculateCenterIndex() {
        val recyclerViewCenter = getParentCenter()
        var minDistance = Float.MAX_VALUE
        var position = -1

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: return
            val childCenter = getChildCenter(child)

            val diff = Math.abs(recyclerViewCenter - childCenter)
            if (diff < minDistance) {
                minDistance = diff
                position = recyclerView.getChildLayoutPosition(child)
            }
        }

        if (position != -1 && currentPosition != position) {
            currentPosition = position
            onScrollListener?.invoke(currentPosition)
        }
    }

    fun smoothScroll(recyclerView: RecyclerView?, position: Int) {
        if (currentPosition == position) return
        smoothScrollToPosition(recyclerView, RecyclerView.State(), position)
    }

    sealed class Scaling {
        object Linear : Scaling()
        data class Logarithmic(val multiplier: Float = 1.0F) : Scaling()
    }

    private class CenterSmoothScroller(
        context: Context?
    ) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int
        ) = boxStart + (boxEnd - boxStart) / HALF_INT - (viewStart + (viewEnd - viewStart) / HALF_INT)
    }

    data class Builder(
        val context: Context,
        @RecyclerView.Orientation val orientation: Int
    ) {
        private var initialIndex: Int = 0
        private var onScrollListener: OnScrollListener? = null
        private var scaling: Scaling? = null

        fun setInitialIndex(initialIndex: Int) = apply {
            this.initialIndex = initialIndex
        }

        fun setOnScrollListener(onScrollListener: OnScrollListener) = apply {
            this.onScrollListener = onScrollListener
        }

        fun setScaling(scaling: Scaling) = apply {
            this.scaling = scaling
        }

        fun build() = SliderLayoutManager(context, orientation, initialIndex, onScrollListener, scaling)
    }

    companion object {
        private const val DRAGGING_UPDATE_INTERVAL_MS = 50L
        private const val HALF = 2F
        private const val HALF_INT = HALF.toInt()
    }
}