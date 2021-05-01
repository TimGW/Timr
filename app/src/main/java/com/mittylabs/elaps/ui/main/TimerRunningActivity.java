//package com.mittylabs.elaps.ui.main;
//
//import android.app.Activity;
//import android.app.ActivityManager;
//import android.app.NotificationManager;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.graphics.PorterDuff;
//import android.graphics.drawable.Drawable;
//import android.os.Build;
//import android.os.Bundle;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toolbar;
//
//import androidx.annotation.Nullable;
//
//import com.mittylabs.elaps.R;
//
//import java.util.concurrent.TimeUnit;
//
//public class TimerRunningActivity extends Activity implements View.OnClickListener {
//
//    private ProgressBar progressCircle;
//    private TextView textViewTime;
//    private ImageView imageViewStartStop;
//    private Toolbar toolbar;
//    private TimerSetting timerSetting;
//
//    public static Intent getLaunchingIntent(final Context ctx) {
//        return new Intent(ctx, TimerRunningActivity.class);
//    }
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_timer_running);
//        setStatusBarColor(R.color.color_primary);
//
//        realm = Realm.getDefaultInstance();
//        timerSetting = realm.where(TimerSetting.class).equalTo("id", Constants.DEFAULT_TIMER_ID).findFirst();
//
//        progressCircle = (ProgressBar) findViewById(R.id.barTimer);
//        ImageView imageViewReset = (ImageView) findViewById(R.id.timer_reset_button);
//        textViewTime = (TextView) findViewById(R.id.textViewTime);
//        imageViewStartStop = (ImageView) findViewById(R.id.timer_start_button);
//        ImageView stopButton = (ImageView) findViewById(R.id.timer_stop_button);
//        toolbar = (Toolbar) findViewById(R.id.toolbar_timer);
//        setupToolbar();
//
//        imageViewReset.setOnClickListener(this);
//        imageViewStartStop.setOnClickListener(this);
//        stopButton.setOnClickListener(this);
//
//        resetProgressBar(timerSetting.getInitialTimeMilliseconds());
//        startTimerService();
//    }
//
//    private final BroadcastReceiver br = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            receiveBroadcast(intent);
//        }
//    };
//
//    /**
//     * register listening broadcast receiver to get timer onTick callbacks
//     */
//    @Override
//    public void onResume() {
//        super.onResume();
//        registerReceiver(br, new IntentFilter(TimerBroadcastService.COUNTDOWN_BR));
//    }
//
//    /**
//     * unregister listening broadcast receiver to stop getting timer onTick callbacks
//     */
//    @Override
//    public void onPause() {
//        super.onPause();
//        unregisterReceiver(br);
//    }
//
//    @Override
//    public void onStop() {
//        try {
//            unregisterReceiver(br);
//        } catch (Exception e) {
//            // Receiver was probably already stopped in onPause()
//        }
//        super.onStop();
//    }
//
//    @Override
//    protected void onDestroy() {
//        realm.close();
//        super.onDestroy();
//    }
//
//    /**
//     * Handles the received broadcasts from the TimerBroadcastService
//     * @param intent received from broadcast service
//     */
//    private void receiveBroadcast(Intent intent) {
//        if (intent.getExtras() != null) {
//            final long millisUntilFinished = intent.getLongExtra("countdown", 0);
//            boolean finished = intent.getBooleanExtra("finished", false);
//
//            /** If timer is not yet finished, update the time and progress bar */
//            if (!finished) {
//                setProgressBar(millisUntilFinished);
//                String formattedTime = formatTime(millisUntilFinished);
//                textViewTime.setText(formattedTime);
//            }
//            /** If timer is finished, reset the timer */
//            else {
//                resetTimer();
//                imageViewStartStop.setImageResource(R.drawable.icon_play_orange);
//            }
//        }
//    }
//
//    @Override
//    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.timer_reset_button:
//                resetTimer();
//                break;
//            case R.id.timer_start_button:
//                timerStatusToggle(timerSetting.getStatus());
//                break;
//            case R.id.timer_stop_button:
//                resetTimer();
//                finish();
//                break;
//        }
//    }
//
//    /**
//     * - reset the displayed time to the initial time
//     * - reset the saved realm time to the initial time and set the timer status to STOP
//     * - reset the progress bar
//     * - stop the TimerBroadcastService
//     */
//    private void resetTimer() {
//        String formattedTime = formatTime(timerSetting.getInitialTimeMilliseconds());
//        textViewTime.setText(formattedTime);
//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(@Nonnull Realm realm) {
//                timerSetting.setSavedTimeMilliseconds(timerSetting.getInitialTimeMilliseconds());
//                timerSetting.setStatus(TimerSetting.Status.STOPPED);
//            }
//        });
//        resetProgressBar(timerSetting.getInitialTimeMilliseconds());
//        imageViewStartStop.setImageResource(R.drawable.icon_play_orange);
//        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.cancel(NOTIFICATION_STICKY_ID);
//        stopTimerService();
//    }
//
//    private void resetProgressBar(long time) {
//        int max = (int) time / 1000;
//        progressCircle.setMax(max);
//        progressCircle.setProgress(max);
//    }
//
//    private void setProgressBar(long time) {
//        int progress = (int) (time / 1000);
//        progressCircle.setProgress(progress);
//    }
//
//    public static String formatTime(long time) {
//        return String.format("%02d:%02d:%02d",
//                TimeUnit.MILLISECONDS.toHours(time),
//                TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
//                TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));
//    }
//
//    /**
//     * handles the interactions to play/pause
//     * @param timerStatus indicates the current status
//     */
//    private void timerStatusToggle(TimerSetting.Status timerStatus) {
//        switch (timerStatus) {
//            case STOPPED: // press play to start
//                resetProgressBar(timerSetting.getInitialTimeMilliseconds());
//                imageViewStartStop.setImageResource(R.drawable.icon_pause_orange);
//                realm.executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(@Nonnull Realm realm) {
//                        timerSetting.setStatus(TimerSetting.Status.STARTED);
//                    }
//                });
//                startTimerService();
//                break;
//            case STARTED: // press pause to pause
//                imageViewStartStop.setImageResource(R.drawable.icon_play_orange);
//                realm.executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(@Nonnull Realm realm) {
//                        timerSetting.setStatus(TimerSetting.Status.PAUSED);
//                    }
//                });
//                NotificationUtilts.alowNotificationToBeRemoved("Time left: " + formatTime(timerSetting.getSavedTimeMilliseconds()) , this);
//                stopTimerService();
//                break;
//            case PAUSED: // unpause
//                imageViewStartStop.setImageResource(R.drawable.icon_pause_orange);
//                realm.executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(@Nonnull Realm realm) {
//                        timerSetting.setStatus(TimerSetting.Status.STARTED);
//                    }
//                });
//                startTimerService();
//                break;
//        }
//    }
//
//    /**
//     * Start a new TimerBroadcast Service to persist the timer countdown
//     */
//    private void startTimerService() {
//        if (!isMyServiceRunning(TimerBroadcastService.class)) {
//            realm.executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(@Nonnull Realm realm) {
//                    timerSetting.setStatus(TimerSetting.Status.STARTED); // update the status
//                }
//            });
//            Intent serviceIntent = new Intent(this, TimerBroadcastService.class);
//            serviceIntent.putExtra("TimerValue", timerSetting.getSavedTimeMilliseconds());
//            startService(serviceIntent);
//        }
//    }
//
//    /**
//     * Stop the TimerBroadcast Service if it's running
//     */
//    private void stopTimerService() {
//        if (isMyServiceRunning(TimerBroadcastService.class)) {
//            stopService(new Intent(this, TimerBroadcastService.class));
//        }
//    }
//
//    /**
//     * check if a service is currently running
//     * @param serviceClass is always TimerBroadcastService.class
//     * @return true if the service is running
//     */
//    private boolean isMyServiceRunning(Class<?> serviceClass) {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private void setupToolbar() {
//        toolbar.setTitle("Timer");
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setHomeButtonEnabled(true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        final Drawable downArrow = ContextCompat.getDrawable(this, R.drawable.icon_chevrondown);
//        downArrow.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
//        getSupportActionBar().setHomeAsUpIndicator(downArrow);
//    }
//
//    private void setStatusBarColor(int color) {
//            Window window = this.getWindow();
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(ContextCompat.getColor(this, color));
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                onBackPressed();
//                break;
//            default:
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    /**
//     * @return a unique constant ID to save the state of the presenters
//     */
//    @Override
//    public int getViewId() {
//        return Constants.ACTIVITY_TIMER_ID;
//    }
//}
