package com.mittylabs.elaps.ui.main

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.mittylabs.elaps.R
import kotlin.math.abs

class SliderLayoutManager(
    context: Context?,
    private val screenWidth: Int = 0
) : LinearLayoutManager(context, HORIZONTAL, false) {
    var onScroll: ((Int) -> Unit)? = null

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
        view.clipToPadding = false
        view.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                }
                val offset = (screenWidth - view.width) / 2F
                val padding = ((screenWidth / 2f) - offset).toInt()
                view.setPadding(padding, 0, padding, 0)

                return true
            }
        })

    }

    override fun onScrollStateChanged(state: Int) {
        if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            handler.post(runnable)
        } else if (state == RecyclerView.SCROLL_STATE_IDLE) {
            handler.removeCallbacks(runnable)
            calculateCenterIndex()
        }
    }

    private fun calculateCenterIndex() {
        val recyclerViewCenterX = width / 2.0f
        var minDistance = Float.MAX_VALUE
        var position = -1

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: return
            val childCenterX = (getDecoratedLeft(child) + getDecoratedRight(child)) / 2.0f
            val diff =  Math.abs(recyclerViewCenterX - childCenterX)

            if (diff < minDistance) {
                minDistance = diff
                position = i
            }
        }

        if (position != -1 && currentPosition != position) {
            currentPosition = getChildAt(position)?.tag as? Int ?: return
            onScroll?.invoke(currentPosition)
        }
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

    private class CenterSmoothScroller(
        context: Context?
    ): LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int
        ) = boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
    }

    fun smoothScroll(recyclerView: RecyclerView?, position: Int) {
        if (currentPosition == position) return
        smoothScrollToPosition(recyclerView, RecyclerView.State(), position)
    }

    companion object {
        private const val DRAGGING_UPDATE_INTERVAL_MS = 50L
    }
}