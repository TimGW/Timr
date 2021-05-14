package com.mittylabs.elaps.ui.main

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import com.mittylabs.elaps.databinding.ActivityTimerBinding
import com.mittylabs.elaps.service.Timer.extend
import com.mittylabs.elaps.service.Timer.pause
import com.mittylabs.elaps.service.Timer.play
import com.mittylabs.elaps.service.Timer.stop
import com.mittylabs.elaps.service.Timer.terminate
import com.mittylabs.elaps.service.TimerService
import com.mittylabs.elaps.ui.main.TimerState.*
import com.mittylabs.elaps.utils.blink
import com.mittylabs.elaps.utils.toHumanFormat

class TimerActivity : Activity() {
    private lateinit var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var binding: ActivityTimerBinding
    private lateinit var timerState: TimerState
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == INTENT_EXTRA_TIMER) {
                updateTimerState(intent.getParcelableExtra(INTENT_EXTRA_TIMER))
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            updateTimerState(TimerService.timerState)
        }
        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initButtonListeners()

        if (savedInstanceState != null) {
            updateTimerState(savedInstanceState.getParcelable(BUNDLE_EXTRA_TIMER))
        } else if (intent.hasExtra(INTENT_EXTRA_TIMER_START)) {
            play(intent.getLongExtra(INTENT_EXTRA_TIMER_START, -1L))
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, TimerService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter().apply { addAction(INTENT_EXTRA_TIMER) })
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(BUNDLE_EXTRA_TIMER, timerState)
    }

    private fun initButtonListeners() {
        onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isPaused ->
            if (isPaused) pause() else play(timerState.initialTime)
        }
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(onCheckedChangeListener)
        binding.timerTerminateButton.setOnClickListener { terminate() }
        binding.timerResetButton.setOnClickListener { stop() }
        binding.timerExtendButton.setOnClickListener { extend() }
    }

    private fun close() {
        startActivity(TimerSetupActivity.launchingIntent(this))
        finish()
    }

    private fun updateTimerState(timerState: TimerState?) {
        timerState?.let { this.timerState = it }
        binding.timerTextView.clearAnimation()

        when (timerState) {
            is Running -> updateProgress(timerState.initialTime, timerState.remainingTime)
            is Paused -> {
                updateProgress(timerState.initialTime, timerState.remainingTime)
                binding.timerTextView.blink()
            }
            is Stopped -> updateProgress(timerState.initialTime, timerState.initialTime)
            is Finished -> binding.timerTextView.text = timerState.elapsedTime.toHumanFormat()
            is Terminated, null -> close()
        }

        // temporary disable listener to update the toggle button state
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(null)
        binding.timerStartPauseToggleButton.isChecked = timerState !is Running
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    private fun updateProgress(length: Long, remaining: Long) {
        binding.timerTextView.text = remaining.toHumanFormat()
        binding.timerProgressBar.max = (length - 1000L).toInt()
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
        const val BUNDLE_EXTRA_TIMER = "BUNDLE_EXTRA_TIMER"
        const val INTENT_EXTRA_TIMER_START = "INTENT_EXTRA_TIMER_START"
    }
}