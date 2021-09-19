package com.mittylabs.timr.timer

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.mittylabs.timr.NavGraphDirections
import com.mittylabs.timr.R
import com.mittylabs.timr.app.SharedPrefs
import com.mittylabs.timr.databinding.ActivityTimerBinding
import com.mittylabs.timr.extensions.isAndroid12
import com.mittylabs.timr.model.TimerState
import com.mittylabs.timr.service.TimerService
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
        if (isAndroid12()) delayFirstFrameForSplash()

        setContentView(binding.root)
        setSupportActionBar(binding.appbar.toolbar)


        manager = LocalBroadcastManager.getInstance(this)

        NavigationUI.setupActionBarWithNavController(
            this,
            getNavController(),
            AppBarConfiguration.Builder(R.id.timerSetupFragment, R.id.timerRunningFragment).build()
        )
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
        } else if (currentFragment == R.id.timerSetupFragment && isTimerActive) {
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

    private fun delayFirstFrameForSplash() {
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    Thread.sleep(250) // todo change to animationlistener
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            }
        )
    }

    companion object {
        const val INTENT_EXTRA_TIMER = "INTENT_EXTRA_TIMER"

        fun intentBuilder(context: Context): Intent {
            return Intent(context, TimerActivity::class.java)
        }
    }
}