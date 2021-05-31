package com.mittylabs.timr.extensions

import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import com.google.android.material.snackbar.Snackbar

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

fun Context.toast(message: String) {
    Toast.makeText(
        this,
        message,
        Toast.LENGTH_SHORT
    ).show()
}

fun View.snackbar(
    message: String = "",
    actionMessage: String = "",
    anchorView: View? = null,
    length: Int = Snackbar.LENGTH_LONG,
    action: (() -> Unit)? = null
): Snackbar {
    val snackbar = Snackbar.make(this, message, length)
    if (action != null) snackbar.setAction(actionMessage) { action.invoke() }
    if (anchorView != null) snackbar.anchorView = anchorView
    snackbar.show()
    return snackbar
}

fun isAndroid12() = Build.VERSION.CODENAME == "S" || Build.VERSION.SDK_INT >= 31