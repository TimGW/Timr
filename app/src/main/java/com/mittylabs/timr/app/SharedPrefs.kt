package com.mittylabs.timr.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class SharedPrefs(
    private val spm: SharedPrefManager
) {

    fun setDarkModeSetting(darkMode: Int) {
        spm.setIntValue(SHARED_PREF_DARK_MODE, darkMode)
    }

    fun getDarkModeSetting() = spm.getIntValue(SHARED_PREF_DARK_MODE)

    fun isResetEnabled(context: Context) = spm.getBoolValue(SHARED_PREF_IS_RESET) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED


    companion object {
        const val SHARED_PREF_DARK_MODE = "SHARED_PREF_DARK_THEME"
        const val SHARED_PREF_IS_RESET = "timer_reset_walk"
    }
}
