package com.mittylabs.elaps.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import androidx.recyclerview.widget.LinearSnapHelper
import com.mittylabs.elaps.R
import com.mittylabs.elaps.databinding.ActivityTimerSettingsBinding

class TimerSetupActivity : Activity() {
    private lateinit var binding: ActivityTimerSettingsBinding
    private lateinit var sliderLayoutManager: SliderLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupRadioButtons()

        binding.timerStartButton.setOnClickListener { startRunningTimerActivity() }
    }

    private fun setupRecyclerView() {
        sliderLayoutManager = SliderLayoutManager(
            this,
            windowManager.currentWindowMetrics.bounds.width()
        ).apply {
            onScroll = { binding.timerSelectedMinutes.text = (it + 1).toString() }
        }

        binding.recyclerView.adapter = TimerAdapter().apply {
            onItemClick = { sliderLayoutManager.smoothScroll(binding.recyclerView, it) }
        }
        binding.recyclerView.layoutManager = sliderLayoutManager

        LinearSnapHelper().attachToRecyclerView(binding.recyclerView)
    }

    private fun setupRadioButtons() {
        binding.minuteButtons.setOnCheckedChangeListener { _, checkedId ->
            val minutes = when (checkedId) {
                R.id.minutes_20 -> MINUTES_20
                R.id.minutes_30 -> MINUTES_30
                R.id.minutes_45 -> MINUTES_45
                R.id.minutes_60 -> MINUTES_60
                R.id.minutes_90 -> MINUTES_90
                else -> MINUTES_30
            }
            sliderLayoutManager.smoothScroll(binding.recyclerView, minutes - 1)
        }.also { binding.minutes45.isChecked = true }
    }

    private fun startRunningTimerActivity() {
        finish()

        val intent = TimerActivity.getLaunchingIntent(this)
        val time = binding.timerSelectedMinutes.text.toString().toLong() * 1000L //* 60L
        intent.putExtra(INTENT_EXTRA_TIMER_LENGTH_MILLISECONDS, time)

        startActivity(intent)
    }

    companion object {
        fun launchingIntent(ctx: Context?): Intent {
            return Intent(ctx, TimerSetupActivity::class.java)
        }

        private const val MINUTES_20 = 20
        private const val MINUTES_30 = 30
        private const val MINUTES_45 = 45
        private const val MINUTES_60 = 60
        private const val MINUTES_90 = 90

        const val INTENT_EXTRA_TIMER_LENGTH_MILLISECONDS = "TIMER_LENGTH_MILLISECONDS"
    }
}
