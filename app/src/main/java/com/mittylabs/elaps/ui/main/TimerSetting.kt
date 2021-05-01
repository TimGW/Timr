package com.mittylabs.elaps.ui.main

/**
 * Class to store some of the timer settings required to keep it persistent
 *
 * Created by tim.gortworst on 03/10/2017.
 */
data class TimerSetting(
    //    @PrimaryKey
    var id: Int = 0,
    var savedTimeMilliseconds: Long = 0L,
    var initialTimeMilliseconds: Long = 0L,
    var status: Status = Status.STOPPED
) {
    enum class Status {
        STARTED, PAUSED, STOPPED
    }
}