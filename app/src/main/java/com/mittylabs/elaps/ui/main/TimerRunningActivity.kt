package com.mittylabs.elaps.ui.main

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mittylabs.elaps.databinding.ActivityTimerRunningBinding
import com.mittylabs.elaps.service.CountdownTimerService
import com.mittylabs.elaps.service.CountdownTimerService.Companion.INTENT_EXTRA_INITIAL_TIME
import com.mittylabs.elaps.service.CountdownTimerService.Companion.INTENT_EXTRA_RESULT
import com.mittylabs.elaps.ui.main.TimerSetupActivity.Companion.INTENT_EXTRA_MINUTES
import com.mittylabs.elaps.ui.toTimerFormat


class TimerRunningActivity : Activity() {
    private lateinit var receiver: TimerStatusReceiver
    private lateinit var binding: ActivityTimerRunningBinding

    private val timerLengthMinutes by lazy {
        minToMillis(intent.getIntExtra(INTENT_EXTRA_MINUTES, 0))
    }
    private var timerState = TimerState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerRunningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiver = TimerStatusReceiver()

        binding.timerResetButton.setOnClickListener {
            resetProgressBar()
        }
        binding.timerStartButton.setOnClickListener {
            startService()
        }
        binding.timerStopButton.setOnClickListener {
            stopService()
        }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(CountdownTimerService.TIME_INFO))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun startService() {
        val intent = Intent(this, CountdownTimerService::class.java)
        intent.putExtra(INTENT_EXTRA_MINUTES, timerLengthMinutes)
        startService(intent)
    }

    private fun stopService() {
        val intent = Intent(this, CountdownTimerService::class.java)
        stopService(intent)
    }

    inner class TimerStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CountdownTimerService.TIME_INFO) {
                if (intent.hasExtra(INTENT_EXTRA_RESULT)) {
                    val time = intent.getLongExtra(INTENT_EXTRA_RESULT, 0L)
                    val initialTime = intent.getLongExtra(INTENT_EXTRA_INITIAL_TIME, 0L)
                    updateProgressBar(initialTime, time)
                    binding.textViewTime.text = time.toTimerFormat()
                }
            }
        }
    }

//    /**
//     * - reset the displayed time to the initial time
//     * - reset the saved realm time to the initial time and set the timer status to STOP
//     * - reset the progress bar
//     * - stop the TimerBroadcastService
//     */
//    private fun resetTimer() {
//        val formattedTime = formatTime(timerSetting!!.initialTimeMilliseconds)
//        binding.textViewTime!!.text = formattedTime
//        realm.executeTransaction(object : Transaction() {
//            fun execute(@Nonnull realm: Realm?) {
//                timerSetting!!.savedTimeMilliseconds = timerSetting!!.initialTimeMilliseconds
//                timerSetting!!.status = TimerSetting.Status.STOPPED
//            }
//        })
//        resetProgressBar(timerSetting!!.initialTimeMilliseconds)
//        binding.timerStartButton!!.setImageResource(R.drawable.icon_play_orange)
//        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.cancel(NOTIFICATION_STICKY_ID)
//        stopTimerService()
//    }

    private fun resetProgressBar() {
        binding.barTimer.max = timerLengthMinutes.toInt()
        binding.barTimer.progress = 0
    }

    private fun updateProgressBar(initialTime: Long, time: Long) {
        binding.barTimer.max = initialTime.toInt()

        val result = (initialTime - time)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            binding.barTimer.setProgress(result.toInt(), true)
        } else {
            binding.barTimer.progress = result.toInt()
        }
    }

//    /**
//     * handles the interactions to play/pause
//     * @param timerStatus indicates the current status
//     */
//    private fun timerStatusToggle(timerStatus: TimerSetting.Status) {
//        when (timerStatus) {
//            TimerSetting.Status.STOPPED -> {
//                resetProgressBar(timerSetting!!.initialTimeMilliseconds)
//                binding.timerStartButton!!.setImageResource(R.drawable.icon_pause_orange)
//                realm.executeTransaction(object : Transaction() {
//                    fun execute(@Nonnull realm: Realm?) {
//                        timerSetting!!.status = TimerSetting.Status.STARTED
//                    }
//                })
//                startTimerService()
//            }
//            TimerSetting.Status.STARTED -> {
//                binding.timerStartButton!!.setImageResource(R.drawable.icon_play_orange)
//                realm.executeTransaction(object : Transaction() {
//                    fun execute(@Nonnull realm: Realm?) {
//                        timerSetting!!.status = TimerSetting.Status.PAUSED
//                    }
//                })
//                NotificationUtilts.alowNotificationToBeRemoved(
//                    "Time left: " + formatTime(
//                        timerSetting!!.savedTimeMilliseconds
//                    ), this
//                )
//                stopTimerService()
//            }
//            TimerSetting.Status.PAUSED -> {
//                binding.timerStartButton!!.setImageResource(R.drawable.icon_pause_orange)
//                realm.executeTransaction(object : Transaction() {
//                    fun execute(@Nonnull realm: Realm?) {
//                        timerSetting!!.status = TimerSetting.Status.STARTED
//                    }
//                })
//                startTimerService()
//            }
//        }
//    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun minToMillis(minutes: Int): Long = minutes * 60L * 1000L

    companion object {
        fun getLaunchingIntent(ctx: Context?): Intent {
            return Intent(ctx, TimerRunningActivity::class.java)
        }
//
//        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long {
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