package com.mittylabs.elaps.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.play.core.review.ReviewManagerFactory
import com.mittylabs.elaps.R
import com.mittylabs.elaps.app.SharedPrefs
import com.mittylabs.elaps.extensions.snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timerPrefs()
        displayPrefs()
        aboutPrefs()
    }

    override fun onStart() {
        super.onStart()
        setUpButtonVisible(true)
    }

    override fun onStop() {
        super.onStop()
        setUpButtonVisible(false)
    }

    private fun setUpButtonVisible(isVisible: Boolean) = (activity as? AppCompatActivity)
        ?.supportActionBar?.setDisplayHomeAsUpEnabled(isVisible)

    private fun timerPrefs() {
        // todo extra timer preferecens
    }

    private fun displayPrefs() {
        val darkModePref = (findPreference("dark_mode_key") as? ListPreference)

        darkModePref?.summary = resources
            .getStringArray(R.array.night_mode_items)[sharedPrefs.getDarkModeSetting()]

        darkModePref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val darkModeSetting =
                    (newValue as String).toIntOrNull() ?: return@OnPreferenceChangeListener false
                val nightMode = when (darkModeSetting) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
                sharedPrefs.setDarkModeSetting(darkModeSetting)
                darkModePref?.summary =
                    resources.getStringArray(R.array.night_mode_items)[darkModeSetting]
                true
            }

    }

    private fun aboutPrefs() {
        val rootView = activity?.findViewById<View>(android.R.id.content) ?: return

        (findPreference("preferences_rate_app_key") as? Preference)?.setOnPreferenceClickListener {
            val manager = ReviewManagerFactory.create(requireActivity())

            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        rootView.snackbar(message = getString(R.string.review_flow_done))
                    }
                } else {
                    rootView.snackbar(message = getString(R.string.error_generic))
                }
            }
            true
        }
    }
}