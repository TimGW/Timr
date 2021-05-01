//package com.mittylabs.elaps.ui.main
//
//import android.app.Activity
//import android.app.ActivityManager
//import android.app.NotificationManager
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.graphics.PorterDuff
//import android.graphics.drawable.Drawable
//import android.os.Bundle
//import android.view.MenuItem
//import android.view.View
//import android.view.WindowManager
//import android.widget.ImageView
//import android.widget.ProgressBar
//import android.widget.TextView
//import android.widget.Toolbar
//import com.mittylabs.elaps.R
//import java.util.concurrent.TimeUnit
//
//class TimerRunningActivity : Activity(), View.OnClickListener {
//    private var progressCircle: ProgressBar? = null
//    private var textViewTime: TextView? = null
//    private var imageViewStartStop: ImageView? = null
//    private var toolbar: Toolbar? = null
//    private var timerSetting: TimerSetting? = null
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_timer_running)
//        setStatusBarColor(R.color.color_primary)
//        realm = Realm.getDefaultInstance()
//        timerSetting =
//            realm.where(TimerSetting::class.java).equalTo("id", Constants.DEFAULT_TIMER_ID)
//                .findFirst()
//        progressCircle = findViewById<View>(R.id.barTimer) as ProgressBar
//        val imageViewReset = findViewById<View>(R.id.timer_reset_button) as ImageView
//        textViewTime = findViewById<View>(R.id.textViewTime) as TextView
//        imageViewStartStop = findViewById<View>(R.id.timer_start_button) as ImageView
//        val stopButton = findViewById<View>(R.id.timer_stop_button) as ImageView
//        toolbar = findViewById<View>(R.id.toolbar_timer) as Toolbar
//        setupToolbar()
//        imageViewReset.setOnClickListener(this)
//        imageViewStartStop!!.setOnClickListener(this)
//        stopButton.setOnClickListener(this)
//        resetProgressBar(timerSetting!!.initialTimeMilliseconds)
//        startTimerService()
//    }
//
//    private val br: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            receiveBroadcast(intent)
//        }
//    }
//
//    /**
//     * register listening broadcast receiver to get timer onTick callbacks
//     */
//    public override fun onResume() {
//        super.onResume()
//        registerReceiver(br, IntentFilter(TimerBroadcastService.COUNTDOWN_BR))
//    }
//
//    /**
//     * unregister listening broadcast receiver to stop getting timer onTick callbacks
//     */
//    public override fun onPause() {
//        super.onPause()
//        unregisterReceiver(br)
//    }
//
//    public override fun onStop() {
//        try {
//            unregisterReceiver(br)
//        } catch (e: Exception) {
//            // Receiver was probably already stopped in onPause()
//        }
//        super.onStop()
//    }
//
//    override fun onDestroy() {
//        realm.close()
//        super.onDestroy()
//    }
//
//    /**
//     * Handles the received broadcasts from the TimerBroadcastService
//     * @param intent received from broadcast service
//     */
//    private fun receiveBroadcast(intent: Intent) {
//        if (intent.extras != null) {
//            val millisUntilFinished = intent.getLongExtra("countdown", 0)
//            val finished = intent.getBooleanExtra("finished", false)
//            /** If timer is not yet finished, update the time and progress bar  */
//            if (!finished) {
//                setProgressBar(millisUntilFinished)
//                val formattedTime = formatTime(millisUntilFinished)
//                textViewTime!!.text = formattedTime
//            } else {
//                resetTimer()
//                imageViewStartStop!!.setImageResource(R.drawable.icon_play_orange)
//            }
//        }
//    }
//
//    override fun onClick(view: View) {
//        when (view.id) {
//            R.id.timer_reset_button -> resetTimer()
//            R.id.timer_start_button -> timerStatusToggle(timerSetting!!.status)
//            R.id.timer_stop_button -> {
//                resetTimer()
//                finish()
//            }
//        }
//    }
//
//    /**
//     * - reset the displayed time to the initial time
//     * - reset the saved realm time to the initial time and set the timer status to STOP
//     * - reset the progress bar
//     * - stop the TimerBroadcastService
//     */
//    private fun resetTimer() {
//        val formattedTime = formatTime(timerSetting!!.initialTimeMilliseconds)
//        textViewTime!!.text = formattedTime
//        realm.executeTransaction(object : Transaction() {
//            fun execute(@Nonnull realm: Realm?) {
//                timerSetting!!.savedTimeMilliseconds = timerSetting!!.initialTimeMilliseconds
//                timerSetting!!.status = TimerSetting.Status.STOPPED
//            }
//        })
//        resetProgressBar(timerSetting!!.initialTimeMilliseconds)
//        imageViewStartStop!!.setImageResource(R.drawable.icon_play_orange)
//        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.cancel(NOTIFICATION_STICKY_ID)
//        stopTimerService()
//    }
//
//    private fun resetProgressBar(time: Long) {
//        val max = time.toInt() / 1000
//        progressCircle!!.max = max
//        progressCircle!!.progress = max
//    }
//
//    private fun setProgressBar(time: Long) {
//        val progress = (time / 1000).toInt()
//        progressCircle!!.progress = progress
//    }
//
//    /**
//     * handles the interactions to play/pause
//     * @param timerStatus indicates the current status
//     */
//    private fun timerStatusToggle(timerStatus: TimerSetting.Status) {
//        when (timerStatus) {
//            TimerSetting.Status.STOPPED -> {
//                resetProgressBar(timerSetting!!.initialTimeMilliseconds)
//                imageViewStartStop!!.setImageResource(R.drawable.icon_pause_orange)
//                realm.executeTransaction(object : Transaction() {
//                    fun execute(@Nonnull realm: Realm?) {
//                        timerSetting!!.status = TimerSetting.Status.STARTED
//                    }
//                })
//                startTimerService()
//            }
//            TimerSetting.Status.STARTED -> {
//                imageViewStartStop!!.setImageResource(R.drawable.icon_play_orange)
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
//                imageViewStartStop!!.setImageResource(R.drawable.icon_pause_orange)
//                realm.executeTransaction(object : Transaction() {
//                    fun execute(@Nonnull realm: Realm?) {
//                        timerSetting!!.status = TimerSetting.Status.STARTED
//                    }
//                })
//                startTimerService()
//            }
//        }
//    }
//
//    /**
//     * Start a new TimerBroadcast Service to persist the timer countdown
//     */
//    private fun startTimerService() {
//        if (!isMyServiceRunning(TimerBroadcastService::class.java)) {
//            realm.executeTransaction(object : Transaction() {
//                fun execute(@Nonnull realm: Realm?) {
//                    timerSetting!!.status = TimerSetting.Status.STARTED // update the status
//                }
//            })
//            val serviceIntent = Intent(this, TimerBroadcastService::class.java)
//            serviceIntent.putExtra("TimerValue", timerSetting!!.savedTimeMilliseconds)
//            startService(serviceIntent)
//        }
//    }
//
//    /**
//     * Stop the TimerBroadcast Service if it's running
//     */
//    private fun stopTimerService() {
//        if (isMyServiceRunning(TimerBroadcastService::class.java)) {
//            stopService(Intent(this, TimerBroadcastService::class.java))
//        }
//    }
//
//    /**
//     * check if a service is currently running
//     * @param serviceClass is always TimerBroadcastService.class
//     * @return true if the service is running
//     */
//    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
//        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
//        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
//            if (serviceClass.name == service.service.className) {
//                return true
//            }
//        }
//        return false
//    }
//
//    private fun setupToolbar() {
//        toolbar!!.title = "Timer"
//        setSupportActionBar(toolbar)
//        getSupportActionBar().setHomeButtonEnabled(true)
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true)
//        val downArrow: Drawable = ContextCompat.getDrawable(this, R.drawable.icon_chevrondown)
//        downArrow.setColorFilter(
//            ContextCompat.getColor(this, R.color.white),
//            PorterDuff.Mode.SRC_ATOP
//        )
//        getSupportActionBar().setHomeAsUpIndicator(downArrow)
//    }
//
//    private fun setStatusBarColor(color: Int) {
//        val window = this.window
//        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//        window.statusBarColor = ContextCompat.getColor(this, color)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            android.R.id.home -> onBackPressed()
//            else -> {
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
//    /**
//     * @return a unique constant ID to save the state of the presenters
//     */
//    val viewId: Int
//        get() = Constants.ACTIVITY_TIMER_ID
//
//    companion object {
//        fun getLaunchingIntent(ctx: Context?): Intent {
//            return Intent(ctx, TimerRunningActivity::class.java)
//        }
//
//        fun formatTime(time: Long): String {
//            return String.format(
//                "%02d:%02d:%02d",
//                TimeUnit.MILLISECONDS.toHours(time),
//                TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(
//                    TimeUnit.MILLISECONDS.toHours(
//                        time
//                    )
//                ),
//                TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(
//                    TimeUnit.MILLISECONDS.toMinutes(
//                        time
//                    )
//                )
//            )
//        }
//    }
//}