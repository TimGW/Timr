package com.mittylabs.elaps.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        val screenWidth = windowManager.currentWindowMetrics.bounds.width()
        val itemPadding = resources.getDimension(R.dimen.padding_list_item_timer_slider)
        val adapter = TimerAdapter().apply {
            onItemClick = { sliderLayoutManager.smoothScroll(binding.recyclerView, it) }
        }

        sliderLayoutManager = SliderLayoutManager(this, screenWidth, itemPadding).apply {
            onScroll = { binding.timerSelectedMinutes.text = (it + 1).toString() }
        }

        binding.recyclerView.adapter = adapter
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
        }.run {
            binding.minutes30.isChecked = true
        }
    }

    private fun startRunningTimerActivity() {
        finish()
//        startActivity(TimerRunningActivity.getLaunchingIntent(TimerSetupActivity.this));
    }

    //   fun setTimerMinutes(minutes: Int) {
//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(@NonNull Realm realm) {
//                TimerSetting timerSetting = new TimerSetting();
//                timerSetting.setId(Constants.DEFAULT_TIMER_ID);
//
//                long timeInMilliseconds = convertToMilis(minutes);
//                timerSetting.setInitialTimeMilliseconds(timeInMilliseconds);
//                timerSetting.setSavedTimeMilliseconds(timeInMilliseconds);
//                timerSetting.setStatus(TimerSetting.Status.STOPPED);
//
//                realm.copyToRealmOrUpdate(timerSetting);
//            }
//        });
//        presentMinutes(minutes)
//    }
//
//    fun timerMinutes() {
//        val timerSetting: TimerSetting? =
//            null //realm.where(TimerSetting.class).equalTo("id", Constants.DEFAULT_TIMER_ID).findFirst();
//        if (timerSetting != null) {
//            val seconds = convertToSeconds(timerSetting.savedTimeMilliseconds)
//            presentMinutes(seconds)
//        } else {
//            presentMinutes(15)
//        }
//    }
//
//    private fun convertToMilis(seconds: Int): Long {
//        return (seconds * 60 * 1000).toLong()
//    }
//
//    private fun convertToSeconds(milis: Long): Int {
//        return (milis / 60 / 1000).toInt()
//    }

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
