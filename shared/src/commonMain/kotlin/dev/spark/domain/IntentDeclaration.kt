package dev.spark.domain

import kotlinx.datetime.Instant

data class IntentDeclaration(
    val id: Long = 0,
    val timestamp: Instant,
    val declaredApp: String,
    val declaredDurationMinutes: Int,
    val actualDurationMinutes: Int? = null,
    val wasEnforced: Boolean = false,
    val enforcementType: EnforcementMode? = null,
    val wasOverridden: Boolean = false,
    val emotionalCheckInId: Long? = null
)
