package com.mittylabs.elaps.utils

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.constraintlayout.widget.Group

fun View.blink() {
    val anim: Animation = AlphaAnimation(0.0f, 1.0f)
    anim.duration = 300
    anim.startOffset = 200
    anim.repeatMode = Animation.REVERSE
    anim.repeatCount = Animation.INFINITE
    startAnimation(anim)
}

fun Group.setOnClickListeners(listener: View.OnClickListener?) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}