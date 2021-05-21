package com.mittylabs.elaps.timer

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.mittylabs.elaps.databinding.FragmentTimerRunningBinding
import com.mittylabs.elaps.extensions.blink
import com.mittylabs.elaps.timer.TimerActivity.Companion.INTENT_EXTRA_TIMER
import com.mittylabs.elaps.model.TimerState.*
import com.mittylabs.elaps.extensions.toHumanFormat
import com.mittylabs.elaps.model.TimerState
import com.mittylabs.elaps.service.TimerService

class TimerRunningFragment : Fragment() {
    private val viewModel: TimerViewModel by activityViewModels()
    private lateinit var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var binding: FragmentTimerRunningBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this,
            object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { activity?.finish() }
        })
    }

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

        // deeplink intent received from notification click (not the actions).
        // notification actions call the service and the result gets
        // broadcasted and updated via the shared viewmodel
        if (arguments?.isEmpty == false) {
            updateTimerState(arguments?.getParcelable(INTENT_EXTRA_TIMER))
        }
    }

    private fun initButtonListeners() {
        onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isPaused ->
            if (isPaused) pause() else resume()
        }
        binding.timerStartPauseToggleButton.setOnCheckedChangeListener(onCheckedChangeListener)
        binding.timerResetButton.setOnClickListener { stop() }
        binding.timerExtendButton.setOnClickListener { extend() }
        binding.timerTerminateButton.setOnClickListener { v ->
            v.findNavController().navigate(TimerRunningFragmentDirections.showSetup())
            terminate()
        }
    }

    /**
     * updateTimerState.Progress will be called every 50MS to provide a smooth progress experience,
     * so keep the function as light as possible
     */
    private fun updateTimerState(timerState: TimerState?) {
        when (timerState) {
            is Started -> {
                binding.timerTextView.clearAnimation()
                updateToggleState(timerState.isPlayIconVisible)
            }
            is Progress -> {
                updateProgress(timerState.currentTimerLength, timerState.currentTimeRemaining)
                updateToggleState(timerState.isPlayIconVisible)
            }
            is Paused -> {
                updateProgress(timerState.currentTimerLength, timerState.currentTimeRemaining)
                updateToggleState(timerState.isPlayIconVisible)
                binding.timerTextView.blink()
            }
            is Stopped -> {
                binding.timerTextView.clearAnimation()
                updateProgress(timerState.initialTimerLength, timerState.initialTimerLength)
                updateToggleState(timerState.isPlayIconVisible)
            }
            is Finished -> {
                binding.timerTextView.text = timerState.elapsedTime.toHumanFormat()
                updateToggleState(timerState.isPlayIconVisible)
            }
            is Terminated, null -> { /** do nothing **/ }
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
        binding.timerProgressBar.max = (length - MILLISECOND).toInt()

        val progress = ((length - MILLISECOND) - (remaining - MILLISECOND)).toInt()
        if (VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.timerProgressBar.setProgress(progress, true)
        } else {
            binding.timerProgressBar.progress = progress
        }
    }

    private fun resume() {
        if (viewModel.timerState.value is Progress) return
        startTimerService(TimerService.RESUME_ACTION)
    }

    private fun pause() {
        if (viewModel.timerState.value !is Progress) return
        startTimerService(TimerService.PAUSE_ACTION)
    }

    private fun stop() {
        if (viewModel.timerState.value is Stopped) return
        startTimerService(TimerService.STOP_ACTION)
    }

    private fun extend() {
        startTimerService(TimerService.EXTEND_ACTION)
    }

    private fun terminate() {
        if (viewModel.timerState.value is Terminated) return
        startTimerService(TimerService.TERMINATE_ACTION)
    }

    private fun startTimerService(intentAction: String) {
        val intent = Intent(requireActivity(), TimerService::class.java)
            .apply { action = intentAction }
        ContextCompat.startForegroundService(requireActivity(), intent)
    }

    companion object {
        const val MILLISECOND = 1000L
    }
}