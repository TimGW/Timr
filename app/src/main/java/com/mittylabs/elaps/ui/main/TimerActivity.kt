package com.mittylabs.elaps.ui.main

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import com.mittylabs.elaps.databinding.ActivityTimerBinding
import com.mittylabs.elaps.service.Timer.pause
import com.mittylabs.elaps.service.Timer.play
import com.mittylabs.elaps.service.Timer.stop
import com.mittylabs.elaps.service.Timer.terminate
import com.mittylabs.elaps.ui.main.TimerState.*
import com.mittylabs.elaps.utils.blink
import com.mittylabs.elaps.utils.toHumanFormat

class TimerActivity : Activity() {
    private lateinit var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var binding: ActivityTimerBinding
    private lateinit var timerState: TimerState
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == INTENT_EXTRA_TIMER) timerState = fetchTimerState(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initButtonListeners()

        timerState = savedInstanceState?.let {
            val state = it.getParcelable<TimerState>(INTENT_EXTRA_TIMER) as TimerState
            updateTimerState(state); state
        } ?: fetchTimerState(intent)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter().apply { addAction(INTENT_EXTRA_TIMER) })
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putParcelable(INTENT_EXTRA_TIMER, timerState)
    }

    private fun fetchTimerState(intent: Intent) =
        (intent.getParcelableExtra<TimerState>(INTENT_EXTRA_TIMER) as TimerState).also {
            updateTimerState(it)
        }

    private fun initButtonListeners() {
        binding.timerTerminateButton.setOnClickListener { terminate() }

        onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isPaused ->
            if (isPaused) pause() else play(timerState.initialTime)
        }.also {
            binding.timerStartPauseToggleButton.setOnCheckedChangeListener(it)
        }

        binding.timerResetButton.setOnClickListener { stop() }
    }

    private fun updateTimerState(timerState: TimerState) {
        binding.timerTextView.clearAnimation()

        when (timerState) {
            is Running -> updateProgress(timerState.initialTime, timerState.remainingTime)
            is Paused -> {
                updateProgress(timerState.initialTime, timerState.remainingTime)
                binding.timerTextView.blink()
            }
            is Stopped -> updateProgress(timerState.initialTime, timerState.initialTime)
            is Terminated -> finish()
            is Finished -> binding.timerTextView.text = timerState.elapsedTime.toHumanFormat()
            is Initialize -> play(timerState.initialTime)
        }

        // temporary disable listener to update the toggle button state
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(null)
        binding.timerStartPauseToggleButton.isChecked = timerState !is Running
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    private fun updateProgress(length: Long, remaining: Long) {
        binding.timerTextView.text = remaining.toHumanFormat()
        binding.timerProgressBar.max = (length - 1000L).toInt() // required to support adding 5 minutes
        binding.timerProgressBar.progress = ((length - 1000L) - (remaining - 1000L)).toInt()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_timer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
//            R.id.action_settings -> {
//                val intent = Intent(this, SettingsActivity::class.java)
//                startActivity(intent)
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun getLaunchingIntent(ctx: Context?): Intent {
            return Intent(ctx, TimerActivity::class.java)
        }

        const val INTENT_EXTRA_TIMER = "INTENT_EXTRA_TIMER"
    }
}