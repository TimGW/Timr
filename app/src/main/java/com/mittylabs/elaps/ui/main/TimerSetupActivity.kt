package com.mittylabs.elaps.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearSnapHelper
import com.mittylabs.elaps.R
import com.mittylabs.elaps.databinding.ActivityTimerSettingsBinding
import com.mittylabs.elaps.settings.SettingsActivity
import com.mittylabs.elaps.ui.main.TimerActivity.Companion.INTENT_EXTRA_TIMER_START
import com.mittylabs.elaps.utils.setOnClickListeners

class TimerSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerSettingsBinding
    private lateinit var sliderLayoutManager: SliderLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupMinuteButtons()

        binding.timerStartButton.setOnClickListener { startRunningTimerActivity() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_settings -> {
                startActivity(SettingsActivity.launchingIntent(this))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        sliderLayoutManager = SliderLayoutManager(this, MINUTES_30 - 1).apply {
            onScroll = { binding.timerSelectedMinutes.text = (it + 1).toString() }
        }

        binding.recyclerView.adapter = TimerAdapter().apply {
            onItemClick = { sliderLayoutManager.smoothScroll(binding.recyclerView, it) }
        }
        binding.recyclerView.layoutManager = sliderLayoutManager

        LinearSnapHelper().attachToRecyclerView(binding.recyclerView)
    }

    private fun setupMinuteButtons() {
        binding.minuteButtons.setOnClickListeners {
            val minutes = when (it.id) {
                R.id.minutes_20 -> MINUTES_20
                R.id.minutes_30 -> MINUTES_30
                R.id.minutes_45 -> MINUTES_45
                R.id.minutes_60 -> MINUTES_60
                R.id.minutes_90 -> MINUTES_90
                else -> MINUTES_30
            }
            sliderLayoutManager.smoothScroll(binding.recyclerView, minutes - 1)
        }
    }

    private fun startRunningTimerActivity() {
        val intent = TimerActivity.getLaunchingIntent(this)
        val time = binding.timerSelectedMinutes.text.toString().toLong() * 1000L * 60L
        intent.putExtra(INTENT_EXTRA_TIMER_START, time)

        startActivity(intent)
        finish()
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
    }
}
