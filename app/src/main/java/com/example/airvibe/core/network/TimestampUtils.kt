package com.example.airvibe.core.network

import java.time.Instant

internal fun Long.toIsoTimestamp(): String = Instant.ofEpochMilli(this).toString()

internal fun String?.toEpochMillisOr(fallback: Long = System.currentTimeMillis()): Long =
    this?.let {
        runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
    } ?: fallback
