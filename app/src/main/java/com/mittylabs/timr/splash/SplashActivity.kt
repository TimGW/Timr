package com.mittylabs.timr.splash

import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mittylabs.timr.databinding.ActivitySplashBinding
import com.mittylabs.timr.extensions.isAndroid12
import com.mittylabs.timr.timer.TimerActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isAndroid12()) openTimerActivity() else animateSplash()
    }

    private fun animateSplash() {
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawable = (binding.imageView.drawable as? AnimatedVectorDrawable)
        drawable?.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                super.onAnimationEnd(drawable)
                openTimerActivity()
            }
        })
        drawable?.start()
    }

    fun openTimerActivity() {
        startActivity(TimerActivity.intentBuilder(this@SplashActivity))
        finish()
    }
}
