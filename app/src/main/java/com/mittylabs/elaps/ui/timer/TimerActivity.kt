package com.mittylabs.elaps.ui.timer

import android.content.*
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mittylabs.elaps.R
import com.mittylabs.elaps.databinding.ActivityTimerBinding
import com.mittylabs.elaps.service.TimerService
import com.mittylabs.elaps.utils.EventObserver
import org.koin.android.viewmodel.ext.android.viewModel

class TimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerBinding
    private val timerSetupFragment: Fragment by lazy { TimerSetupFragment.newInstance() }
    private val timerRunningFragment: Fragment by lazy { TimerRunningFragment.newInstance() }
    private val viewModel by viewModel<TimerViewModel>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == INTENT_EXTRA_TIMER) {
                val state = intent.getParcelableExtra(INTENT_EXTRA_TIMER) as? TimerState ?: return
                viewModel.updateTimerState(state)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appbar.toolbar)

        viewModel.openFragment.observe(this, EventObserver{
            when (it) {
                TimerFragment.Running -> {
                    openFragment(timerRunningFragment)
                    viewModel.updateTimerState(TimerService.timerState)
                }
                TimerFragment.Setup -> openFragment(timerSetupFragment)
            }
        })

        viewModel.toolbarTitle.observe(this, {
            supportActionBar?.title = it
        })

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(INTENT_EXTRA_TIMER)
        })
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            replace(R.id.content_frame, fragment)
            commit()
        }
    }

    companion object {
        const val INTENT_EXTRA_TIMER = "INTENT_EXTRA_TIMER"
    }
}