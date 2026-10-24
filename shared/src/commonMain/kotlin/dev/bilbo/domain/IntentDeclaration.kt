package dev.bilbo.domain

import kotlin.time.Instant

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
