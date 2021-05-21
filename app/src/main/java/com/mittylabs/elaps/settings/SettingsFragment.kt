package com.mittylabs.elaps.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.mittylabs.elaps.R
import com.mittylabs.elaps.app.SharedPrefs
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
        (findPreference("preferences_rate_app_key") as? Preference)?.setOnPreferenceClickListener {
            val activity = activity ?: return@setOnPreferenceClickListener false

            val intent = try {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${activity.packageName}")
                )
            } catch (ex: ActivityNotFoundException) {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
                )
            }
            startActivity(intent)
            true
        }
    }
}