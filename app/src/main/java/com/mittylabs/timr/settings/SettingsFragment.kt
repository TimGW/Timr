package com.mittylabs.timr.settings

import android.Manifest.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewManagerFactory
import com.mittylabs.timr.BuildConfig
import com.mittylabs.timr.R
import com.mittylabs.timr.app.SharedPrefs
import com.mittylabs.timr.extensions.snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private var rateAppPref: Preference? = null
    private var darkModePref: ListPreference? = null
    private var timerResetPref: SwitchPreferenceCompat? = null

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            timerResetPref?.isChecked = true
        } else {
            view?.snackbar(
                message = getString(R.string.permission_declined_text),
                actionMessage = getString(R.string.snackbar_open_settings),
                length = Snackbar.LENGTH_LONG
            ) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                })
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timerResetPref = (findPreference("timer_reset_walk") as? SwitchPreferenceCompat)
        darkModePref = (findPreference("dark_mode_key") as? ListPreference)
        rateAppPref = (findPreference("preferences_rate_app_key") as? Preference)

        timerPrefs()
        displayPrefs()
        aboutPrefs()
    }

    private fun timerPrefs() {
        timerResetPref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                askPermission(newValue as Boolean)
            }
    }

    private fun displayPrefs() {
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
        rateAppPref?.setOnPreferenceClickListener {
            val manager = ReviewManagerFactory.create(requireActivity())

            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        view?.snackbar(message = getString(R.string.review_flow_done))
                    }
                } else {
                    view?.snackbar(message = getString(R.string.error_generic))
                }
            }
            true
        }
    }

    private fun askPermission(isResetEnabled: Boolean): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED -> return true
            shouldShowRequestPermissionRationale(permission.ACTIVITY_RECOGNITION) -> {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.permission_declined_text)
                    .setPositiveButton(getString(R.string.permission_allow)) { _, _ ->
                        requestPermissionLauncher.launch(permission.ACTIVITY_RECOGNITION)
                    }
                    .setNegativeButton(getString(R.string.permission_deny)) { dialog, _ ->
                       dialog.cancel()
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
            else -> requestPermissionLauncher.launch(permission.ACTIVITY_RECOGNITION)
        }
        return !isResetEnabled
    }
}