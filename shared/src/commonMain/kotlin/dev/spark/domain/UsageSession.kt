package dev.spark.domain

import kotlinx.datetime.Instant

data class UsageSession(
    val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val category: AppCategory,
    val startTime: Instant,
    val endTime: Instant?,
    val durationSeconds: Long,
    val wasTracked: Boolean = true
)
