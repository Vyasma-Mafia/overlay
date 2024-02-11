package com.stoum.overlay

import java.util.logging.Logger

fun Any.getLogger(): Logger {
    return Logger.getLogger(this::class.simpleName)
}