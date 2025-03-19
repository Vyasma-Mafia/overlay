package com.stoum.overlay

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.getLogger(): Logger {
    return LoggerFactory.getLogger(this::class.simpleName)
}
