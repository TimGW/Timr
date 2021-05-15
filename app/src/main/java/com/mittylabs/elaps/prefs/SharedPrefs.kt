package com.mittylabs.elaps.prefs

class SharedPrefs(
    private val spm: SharedPrefManager
) {

    fun setDarkModeSetting(darkMode: Int) { spm.setIntValue(SHARED_PREF_DARK_MODE, darkMode) }
    fun getDarkModeSetting() = spm.getIntValue(SHARED_PREF_DARK_MODE)

    companion object {
        const val SHARED_PREF_DARK_MODE = "SHARED_PREF_DARK_THEME"
    }
}
