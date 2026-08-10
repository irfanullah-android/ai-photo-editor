package com.editor.photo.video.collagemaker.photoedit.helpers

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Smoothly scrolls selected item to the horizontal center of RecyclerView
fun RecyclerView.smoothScrollToCenter(position: Int) {
    post {
        val lm = layoutManager as? LinearLayoutManager ?: return@post
        val itemView = lm.findViewByPosition(position)

        if (itemView != null) {
            // Item is already laid out — compute exact offset
            val rvCenter = width / 2
            val itemCenter = itemView.left + itemView.width / 2
            smoothScrollBy(itemCenter - rvCenter, 0)
        } else {
            // Item is off-screen — scroll to it first, then re-center
            smoothScrollToPosition(position)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        rv.removeOnScrollListener(this)
                        rv.smoothScrollToCenter(position) // re-center precisely
                    }
                }
            })
        }
    }
}

// Instant (no animation) scroll to center — use for initial load
fun RecyclerView.scrollToCenter(position: Int) {
    post {
        val lm = layoutManager as? LinearLayoutManager ?: return@post
        val itemView = lm.findViewByPosition(position)
        if (itemView != null) {
            val rvCenter = width / 2
            val itemCenter = itemView.left + itemView.width / 2
            scrollBy(itemCenter - rvCenter, 0)
        } else {
            scrollToPosition(position)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        rv.removeOnScrollListener(this)
                        rv.scrollToCenter(position)
                    }
                }
            })
        }
    }
}