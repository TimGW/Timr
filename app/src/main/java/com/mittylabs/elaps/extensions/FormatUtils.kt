package com.mittylabs.elaps.extensions

import java.util.concurrent.TimeUnit

/**
 * milliseconds to readable timer format
 */
fun Long.toHumanFormat(): String {
    val baseString = if (this < 0L) "-%02d:%02d:%02d" else "%02d:%02d:%02d"
    val time = Math.abs(this)

    return String.format(
        baseString, TimeUnit.MILLISECONDS.toHours(time),
        TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(time)
        ),
        TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(time)
        )
    )
}