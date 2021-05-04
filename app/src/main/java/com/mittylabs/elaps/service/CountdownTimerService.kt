package com.mittylabs.elaps.service

import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mittylabs.elaps.R
import com.mittylabs.elaps.ui.main.TimerRunningActivity
import com.mittylabs.elaps.ui.main.TimerSetupActivity.Companion.INTENT_EXTRA_MINUTES
import com.mittylabs.elaps.ui.millisToTimerFormat

class CountdownTimerService : Service() {
    private var timer: CounterClass? = null
    private lateinit var notificationManager: NotificationManager
    private var channelId: String = ""
    private lateinit var pendingIntent: PendingIntent
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var initialTimeInMillis: Long = 0L

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(
                "my_service",
                "My Background Service",
                notificationManager
            )
        }

        notificationBuilder = NotificationCompat.Builder(this@CountdownTimerService, channelId)
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TimerRunningActivity::class.java),
            FLAG_UPDATE_CURRENT
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initialTimeInMillis = intent?.getLongExtra(INTENT_EXTRA_MINUTES, 0L) ?: 0L
        timer = CounterClass(initialTimeInMillis, TIMER_TICK_INTERVAL)
        timer?.start()

        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(initialTimeInMillis.millisToTimerFormat())
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
        val timerInfoIntent = Intent(TIME_INFO)
        timerInfoIntent.putExtra(INTENT_EXTRA_RESULT, initialTimeInMillis)
        LocalBroadcastManager.getInstance(this@CountdownTimerService).sendBroadcast(timerInfoIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
        notificationManager: NotificationManager
    ): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(chan)
        return channelId
    }

    inner class CounterClass(private val millisInFuture: Long, countDownInterval: Long) :
        CountDownTimer(millisInFuture, countDownInterval) {

        override fun onTick(millisUntilFinished: Long) {
            val notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText(millisUntilFinished.millisToTimerFormat())
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
//                .addAction(R.drawable.ic_clear, "cancel", cancelAction())
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)

            val timerInfoIntent = Intent(TIME_INFO)
            timerInfoIntent.putExtra(INTENT_EXTRA_RESULT, millisUntilFinished)
            timerInfoIntent.putExtra(INTENT_EXTRA_INITIAL_TIME, initialTimeInMillis)

            LocalBroadcastManager.getInstance(this@CountdownTimerService)
                .sendBroadcast(timerInfoIntent)
        }

        override fun onFinish() {
            val timerInfoIntent = Intent(TIME_INFO)
            timerInfoIntent.putExtra(INTENT_EXTRA_RESULT, millisInFuture)
            LocalBroadcastManager.getInstance(this@CountdownTimerService)
                .sendBroadcast(timerInfoIntent)

            this@CountdownTimerService.stopSelf()
        }
    }

    companion object {
        const val TIME_INFO = "time_info"
        const val INTENT_EXTRA_RESULT = "INTENT_EXTRA_RESULT"
        const val INTENT_EXTRA_INITIAL_TIME = "INTENT_EXTRA_INITIAL_TIME"
        const val NOTIFICATION_ID = 1
        const val TIMER_TICK_INTERVAL = 60L
    }
}