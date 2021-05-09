package com.mittylabs.elaps.ui.main

import android.app.Activity
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.Toast
import com.mittylabs.blink
import com.mittylabs.elaps.databinding.ActivityTimerBinding
import com.mittylabs.elaps.prefs.SharedPrefs
import com.mittylabs.elaps.service.TimerController
import com.mittylabs.elaps.ui.main.TimerSetupActivity.Companion.INTENT_EXTRA_TIMER_LENGTH_MILLISECONDS
import com.mittylabs.elaps.ui.main.TimerState.*
import com.mittylabs.elaps.ui.toHumanFormat
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.properties.Delegates

class TimerActivity : Activity() {
    private lateinit var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var binding: ActivityTimerBinding
    private lateinit var timer: TimerController.Builder

    private val sharedPrefs: SharedPrefs by inject()

    private val timerLengthMillis by lazy {
        intent.getLongExtra(INTENT_EXTRA_TIMER_LENGTH_MILLISECONDS, 30000L)
    }

    private var timerRemainingMillis by Delegates.notNull<Long>()
    private lateinit var timerState: TimerState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        timerRemainingMillis = timerLengthMillis
        timer = TimerController.Builder(this).setContentIntent(
            Intent(this, TimerActivity::class.java).let { intent ->
                getActivity(this, 0, intent, FLAG_UPDATE_CURRENT)
            }
        )

        initTimerListeners()
        initButtonListeners()

        binding.timerProgressBar.max = timerLengthMillis.toInt()
        binding.timerTextView.text = timerLengthMillis.toHumanFormat()
    }

    override fun onResume() {
        super.onResume()

//        updateProgress(sharedPrefs.getTimerLength(), sharedPrefs.getTimeRemaining())
//        if (::timerState.isInitialized) updateTimerState(sharedPrefs.getTimerState())
    }

    override fun onPause() {
        super.onPause()

//        sharedPrefs.setTimeRemaining(timerRemainingMillis)
//        sharedPrefs.setTimerLength(timerLengthMillis)
//        if (::timerState.isInitialized) sharedPrefs.setTimerState(timerState)
    }

    private fun initTimerListeners() {
        timer.setOnTickListener { timerLengthMillis, millisUntilFinished ->
            timerRemainingMillis = millisUntilFinished
            updateProgress(timerLengthMillis, millisUntilFinished, true)

        }.setOnStateChangedListener { timerState ->
            this.timerState = timerState
            updateTimerState(timerState)

        }.setOnFinishListener {
            updateProgress(timerLengthMillis, timerLengthMillis)
            Toast.makeText(this, "timer finished", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initButtonListeners() {
        binding.timerTerminateButton.setOnClickListener {
            timer.terminate()
            finish()
            startActivity(TimerSetupActivity.launchingIntent(this@TimerActivity))
        }

        onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isPaused ->
            if (isPaused) timer.pause() else timer.play(timerLengthMillis)
        }.also {
            binding.timerStartPauseToggleButton.setOnCheckedChangeListener(it)
        }

        binding.timerResetButton.setOnClickListener {
            timer.stop()
            updateProgress(timerLengthMillis, timerLengthMillis)
        }
    }

    private fun updateTimerState(timerState: TimerState) {
        when (timerState) {
            PAUSED -> binding.timerTextView.blink()
            else -> binding.timerTextView.clearAnimation()
        }

        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(null)
        binding.timerStartPauseToggleButton.isChecked = timerState == PAUSED
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    private fun updateProgress(timerLength: Long, timerRemaining: Long, animate: Boolean = false) {
        binding.timerProgressBar.max = timerLength.toInt()
        binding.timerTextView.text = timerRemaining.toHumanFormat()

        val progress = (timerLengthMillis - timerRemaining)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            binding.timerProgressBar.setProgress(progress.toInt(), animate)
        } else {
            binding.timerProgressBar.progress = progress.toInt()
        }
    }

    private fun updateProgressBar(initialTime: Long, remainingTime: Long) {

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

//                fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long {
//            val wakeUpTime = (nowSeconds + secondsRemaining) * 1000
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            val intent = Intent(context, TimerExpiredReceiver::class.java)
//            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
//            PrefUtil.setAlarmSetTime(nowSeconds, context)
//            return wakeUpTime
//        }
//
//        fun removeAlarm(context: Context) {
//            val intent = Intent(context, TimerExpiredReceiver::class.java)
//            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            alarmManager.cancel(pendingIntent)
//            PrefUtil.setAlarmSetTime(0, context)
//        }
    }
}