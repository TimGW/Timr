package com.mittylabs.elaps.timer

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.mittylabs.elaps.NavGraphDirections
import com.mittylabs.elaps.R
import com.mittylabs.elaps.databinding.ActivityTimerBinding
import com.mittylabs.elaps.model.TimerState
import com.mittylabs.elaps.service.TimerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerBinding
    private val viewModel by viewModels<TimerViewModel>()

    // state received from service broadcasts when app is in foreground
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == INTENT_EXTRA_TIMER) {
                viewModel.updateTimerState(
                    intent.getParcelableExtra(INTENT_EXTRA_TIMER) as? TimerState
                )
            }
        }
    }

    // Defines callbacks for service binding when app is killed
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            val service = (iBinder as TimerService.LocalBinder).getService()

            // timer is just starting, let fragment handle the navigation
            if (service.timerState is TimerState.Started) return

            // service is resumed, open the correct fragment
            if (service.timerState !is TimerState.Terminated) {
                viewModel.updateTimerState(service.timerState)
                navigateTo(NavGraphDirections.actionGlobalTimerRunningFragment())
            } else {
                navigateTo(NavGraphDirections.actionGlobalTimerSetupFragment())
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appbar.toolbar)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, 0) // don't auto create
        }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(INTENT_EXTRA_TIMER))
    }

    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun navigateTo(action: NavDirections) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val navController = (navHostFragment as NavHostFragment).navController
        navController.navigate(action)
    }

    companion object {
        const val INTENT_EXTRA_TIMER = "INTENT_EXTRA_TIMER"
    }
}