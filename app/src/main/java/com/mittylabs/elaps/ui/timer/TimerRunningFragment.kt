package com.mittylabs.elaps.ui.timer

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mittylabs.elaps.databinding.FragmentTimerRunningBinding
import com.mittylabs.elaps.service.TimerService
import com.mittylabs.elaps.ui.timer.TimerState.*
import com.mittylabs.elaps.utils.EventObserver
import com.mittylabs.elaps.utils.blink
import com.mittylabs.elaps.utils.toHumanFormat


class TimerRunningFragment : Fragment() {
    private val viewModel: TimerViewModel by activityViewModels()
    private lateinit var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var binding: FragmentTimerRunningBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTimerRunningBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initButtonListeners()

        viewModel.timerState.observe(viewLifecycleOwner, { updateTimerState(it) })

        viewModel.timerStart.observe(viewLifecycleOwner, EventObserver { play(it) })

        viewModel.updateToolbarTitle("")
    }

    private fun initButtonListeners() {
        onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isPaused ->
            if (isPaused) pause() else resume()
        }
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(onCheckedChangeListener)
        binding.timerResetButton.setOnClickListener { stop() }
        binding.timerExtendButton.setOnClickListener { extend() }
        binding.timerTerminateButton.setOnClickListener {
            terminate()
            viewModel.updateOpenFragment(TimerFragment.Setup)
        }
    }

    /**
     * updateTimerState.Progress will be called every 50MS to provide a smooth progress experience,
     * so keep the function as light as possible
     */
    private fun updateTimerState(timerState: TimerState) {
        when (timerState) {
            is Started -> {
                binding.timerTextView.clearAnimation()
                updateToggleState(timerState.isPlayIconVisible)
            }
            is Progress -> {
                updateProgress(timerState.currentTimerLength, timerState.currentTimeRemaining)
            }
            is Paused -> {
                binding.timerTextView.blink()
                updateToggleState(timerState.isPlayIconVisible)
                updateProgress(timerState.currentTimerLength, timerState.currentTimeRemaining)
            }
            is Stopped -> {
                binding.timerTextView.clearAnimation()
                updateToggleState(timerState.isPlayIconVisible)
                updateProgress(timerState.initialTimerLength, timerState.initialTimerLength)
            }
            is Finished -> {
                updateToggleState(timerState.isPlayIconVisible)
                binding.timerTextView.text = timerState.elapsedTime.toHumanFormat()
            }
            is Terminated -> {
                binding.timerTextView.clearAnimation()
                updateToggleState(true)
            }
        }
    }

    /**
     * temporary disable listener to update the toggle button state
     * return immediately is state is already the same
     *
     * true = playicon is visible
     * false = pauseicon is visible
     */
    private fun updateToggleState(isPlayIconVisible: Boolean) {
        if (binding.timerStartPauseToggleButton.isChecked == isPlayIconVisible) return
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(null)
        binding.timerStartPauseToggleButton.isChecked = isPlayIconVisible
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    private fun updateProgress(length: Long, remaining: Long) {
        binding.timerTextView.text = remaining.toHumanFormat()
        binding.timerProgressBar.max = (length - MILLISECOND).toInt() // todo extract

        val progress = ((length - MILLISECOND) - (remaining - MILLISECOND)).toInt()
        if (VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.timerProgressBar.setProgress(progress, true)
        } else {
            binding.timerProgressBar.progress = progress
        }
    }

    private fun play(timeMillis: Long) {
        if (TimerService.timerState is Progress) return

        val intent = Intent(requireActivity(), TimerService::class.java).apply {
            action = TimerService.START_ACTION
            putExtra(TimerService.TIMER_LENGTH_EXTRA, timeMillis)
        }
        ContextCompat.startForegroundService(requireActivity(), intent)
    }

    private fun resume() {
        if (TimerService.timerState is Progress) return
        startTimerService(TimerService.RESUME_ACTION)
    }

    private fun pause() {
        if (TimerService.timerState !is Progress) return
        startTimerService(TimerService.PAUSE_ACTION)
    }

    private fun stop() {
        startTimerService(TimerService.STOP_ACTION)
    }

    private fun extend() {
        startTimerService(TimerService.EXTEND_ACTION)
    }

    private fun terminate() {
        if (TimerService.timerState is Terminated) return
        startTimerService(TimerService.TERMINATE_ACTION)
    }

    private fun startTimerService(intentAction: String) {
        val intent = Intent(requireActivity(), TimerService::class.java)
            .apply { action = intentAction }
        ContextCompat.startForegroundService(requireActivity(), intent)
    }

    companion object {
        fun newInstance() = TimerRunningFragment()

        const val MILLISECOND = 1000L
    }
}