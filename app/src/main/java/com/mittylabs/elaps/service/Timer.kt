package com.mittylabs.elaps.service

import android.content.Context

interface Timer {
    fun play(context: Context, timeMillis: Long)
    fun pause(context: Context)
    fun stop(context: Context)
    fun terminate(context: Context)
}