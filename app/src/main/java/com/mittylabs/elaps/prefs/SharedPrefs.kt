package com.mittylabs.elaps.prefs

class SharedPrefs(
    private val spm: SharedPrefManager
) {

    fun setDarkModeSetting(darkMode: Int) {
        spm.setIntValue(SHARED_PREF_DARK_MODE, darkMode)
    }

    fun getDarkModeSetting() = spm.getIntValue(SHARED_PREF_DARK_MODE)

    fun setTimerServiceRunning(isTimerServiceRunning: Boolean) {
        spm.setBoolValue(SHARED_PREF_SERVICE_RUNNING, isTimerServiceRunning)
    }

    fun isTimerServiceRunning() = spm.getBoolValue(SHARED_PREF_SERVICE_RUNNING)

    companion object {
        const val SHARED_PREF_DARK_MODE = "SHARED_PREF_DARK_THEME"
        const val SHARED_PREF_SERVICE_RUNNING = "SHARED_PREF_SERVICE_RUNNING"
    }
}
