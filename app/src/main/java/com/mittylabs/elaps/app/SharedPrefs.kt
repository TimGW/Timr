package com.mittylabs.elaps.app

class SharedPrefs(
    private val spm: SharedPrefManager
) {

    fun setDarkModeSetting(darkMode: Int) {
        spm.setIntValue(SHARED_PREF_DARK_MODE, darkMode)
    }

    fun getDarkModeSetting() = spm.getIntValue(SHARED_PREF_DARK_MODE)

    fun getIsResetEnabled() = spm.getBoolValue(SHARED_PREF_IS_RESET)

    companion object {
        const val SHARED_PREF_DARK_MODE = "SHARED_PREF_DARK_THEME"
        const val SHARED_PREF_IS_RESET = "timer_reset_walk"
        const val SHARED_PREF_IS_RESET_HA = "timer_reset_walk_ha"
    }
}
