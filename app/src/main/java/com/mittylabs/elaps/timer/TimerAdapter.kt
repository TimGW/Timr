package com.mittylabs.elaps.timer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mittylabs.elaps.R
import java.util.*

class TimerAdapter : RecyclerView.Adapter<TimerAdapter.ViewHolder>() {
    private val selectorItems: MutableList<Int> = ArrayList()

    var onItemClick: ((Int) -> Unit)? = null

    init {
        for (i in 1..90) {
            selectorItems.add(i) // add 90 minutes to the slider
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == 1) {
            R.layout.list_item_timer_slider_long  // long layout for every 5th child
        } else {
            R.layout.list_item_timer_slider
        }

        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(layout, parent, false)
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) { }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return 1
        return if ((position + 1) % 5 == 0) 1 else 2
    }

    override fun getItemCount(): Int {
        return selectorItems.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener { onItemClick?.invoke(adapterPosition) }
        }
    }
}