package com.mittylabs.elaps.timer

import android.content.*
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.mittylabs.elaps.NavGraphDirections
import com.mittylabs.elaps.R
import com.mittylabs.elaps.app.ElapsApp.Companion.TAG
import com.mittylabs.elaps.app.SharedPrefs
import com.mittylabs.elaps.databinding.ActivityTimerBinding
import com.mittylabs.elaps.model.TimerState
import com.mittylabs.elaps.service.TimerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private lateinit var binding: ActivityTimerBinding
    private val viewModel by viewModels<TimerViewModel>()
    private var navController: NavController? = null
    private lateinit var manager: LocalBroadcastManager

    // state updates received from service broadcasts when app is in foreground
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == INTENT_EXTRA_TIMER) {
                val state = intent.getParcelableExtra(INTENT_EXTRA_TIMER) as? TimerState

                state?.let { viewModel.updateTimerState(it) }
                navigate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.appbar.toolbar)

        manager = LocalBroadcastManager.getInstance(this)

        NavigationUI.setupActionBarWithNavController(
            this,
            getNavController(),
            AppBarConfiguration.Builder(R.id.timerSetupFragment, R.id.timerRunningFragment).build()
        )


        // todo animated drawable for splashscreen api Android S
//        val drawable= ContextCompat.getDrawable(this, R.drawable.ic_timer_animated) as Animatable
//        drawable.stop()
//        drawable.start()
    }

    override fun onResume() {
        super.onResume()

        manager.registerReceiver(receiver, IntentFilter(INTENT_EXTRA_TIMER))

        navigate()
    }

    override fun onPause() {
        super.onPause()

        manager.unregisterReceiver(receiver)
    }

    override fun onSupportNavigateUp(): Boolean {
        return getNavController().navigateUp() || super.onSupportNavigateUp()
    }

    private fun navigate() {
        val isTimerActive = TimerService.timerState != TimerState.Terminated
        val currentFragment = getNavController().currentDestination?.id

        if (currentFragment == R.id.timerRunningFragment && !isTimerActive) {
            getNavController().navigate(NavGraphDirections.actionGlobalTimerSetupFragment())
        } else if(currentFragment == R.id.timerSetupFragment && isTimerActive) {
            viewModel.updateTimerState(TimerService.timerState)
            getNavController().navigate(NavGraphDirections.actionGlobalTimerRunningFragment())
        }
    }

    private fun getNavController(): NavController {
        return if (navController == null) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            (navHostFragment as NavHostFragment).navController
        } else {
            navController as NavController
        }
    }

    companion object {
        const val INTENT_EXTRA_TIMER = "INTENT_EXTRA_TIMER"
    }
}