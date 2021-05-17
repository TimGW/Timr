package com.mittylabs.elaps.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.mittylabs.elaps.R
import com.mittylabs.elaps.databinding.ActivityTimerSettingsBinding
import com.mittylabs.elaps.settings.SettingsActivity
import com.mittylabs.elaps.ui.main.TimerActivity.Companion.INTENT_EXTRA_TIMER_START
import com.mittylabs.elaps.utils.setOnClickListeners
import com.mittylabs.sliderpickerlibrary.SliderLayoutManager

class TimerSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerSettingsBinding
    private lateinit var sliderLayoutManager: SliderLayoutManager
    private var scrollPosition: Int = MINUTES_30 - 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            scrollPosition = savedInstanceState.getInt(BUNDLE_EXTRA_SCROLL_POS, 0)
            updateSelection(scrollPosition)
        }

        setupRecyclerView()
        setupMinuteButtons()

        binding.timerStartButton.setOnClickListener { startRunningTimerActivity() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_EXTRA_SCROLL_POS, scrollPosition)
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
        sliderLayoutManager = SliderLayoutManager.Builder(this, HORIZONTAL)
            .setInitialIndex(scrollPosition)
            .setOnScrollListener { scrollPosition = it; updateSelection(scrollPosition) }
            .build()

        binding.recyclerView.adapter = TimerAdapter().apply {
            onItemClick = { sliderLayoutManager.smoothScroll(binding.recyclerView, it) }
        }
        binding.recyclerView.layoutManager = sliderLayoutManager
    }

    private fun updateSelection(index: Int) {
        val minutes = index + 1
        val text = if (minutes > 1) R.string.minutes_text else R.string.minute_text
        binding.minutes.text = getString(text)
        binding.timerSelectedMinutes.text = minutes.toString()
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

        const val BUNDLE_EXTRA_SCROLL_POS = "BUNDLE_EXTRA_SCROLL_POS"
    }
}
