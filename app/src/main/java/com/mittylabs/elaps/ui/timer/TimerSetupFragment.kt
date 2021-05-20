package com.mittylabs.elaps.ui.timer

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.mittylabs.elaps.R
import com.mittylabs.elaps.databinding.FragmentTimerSettingsBinding
import com.mittylabs.elaps.settings.SettingsActivity
import com.mittylabs.elaps.utils.setOnClickListeners
import com.mittylabs.sliderpickerlibrary.SliderLayoutManager


class TimerSetupFragment : Fragment() {
    private val viewModel: TimerViewModel by activityViewModels()
    private lateinit var binding: FragmentTimerSettingsBinding
    private lateinit var sliderLayoutManager: SliderLayoutManager
    private var scrollPosition: Int = MINUTES_30 - 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTimerSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            scrollPosition = savedInstanceState.getInt(BUNDLE_EXTRA_SCROLL_POS, 0)
        }

        setupRecyclerView()
        setupMinuteButtons()
        updateSelection(scrollPosition)

        binding.timerStartButton.setOnClickListener { startRunningTimerActivity() }

        viewModel.updateToolbarTitle(getString(R.string.title_timer))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_EXTRA_SCROLL_POS, scrollPosition)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            R.id.action_settings -> {
                startActivity(SettingsActivity.launchingIntent(requireActivity()))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        sliderLayoutManager = SliderLayoutManager.Builder(requireActivity(), HORIZONTAL)
            .setInitialIndex(scrollPosition)
            .setOnScrollListener { scrollPosition = it; updateSelection(scrollPosition) }
            .build()

        binding.recyclerView.adapter = TimerAdapter().apply {
            onItemClick = { sliderLayoutManager.smoothScroll(it) }
        }
        binding.recyclerView.layoutManager = sliderLayoutManager
    }

    private fun updateSelection(index: Int) {
        val minutes = index + 1
        val text = if (minutes > 1) R.string.minutes_text else R.string.minute_text
        binding.minutes.text = getString(text)
        binding.timerSelectedMinutes.text = minutes.toString()
    }

    private fun setupMinuteButtons() {
        binding.minuteButtons.setOnClickListeners {
            val minutes = when (it.id) {
                R.id.minutes_20 -> MINUTES_20
                R.id.minutes_30 -> MINUTES_30
                R.id.minutes_45 -> MINUTES_45
                R.id.minutes_60 -> MINUTES_60
                R.id.minutes_90 -> MINUTES_90
                else -> MINUTES_30
            }
            sliderLayoutManager.smoothScroll(minutes - 1)
        }
    }

    private fun startRunningTimerActivity() {
        val time = binding.timerSelectedMinutes.text.toString().toLong() * 1000L * 60L
        viewModel.updateTimerStart(time)
        viewModel.updateOpenFragment(TimerFragment.Running)
    }

    companion object {
        fun newInstance() = TimerSetupFragment()

        private const val MINUTES_20 = 20
        private const val MINUTES_30 = 30
        private const val MINUTES_45 = 45
        private const val MINUTES_60 = 60
        private const val MINUTES_90 = 90

        const val BUNDLE_EXTRA_SCROLL_POS = "BUNDLE_EXTRA_SCROLL_POS"
    }
}
