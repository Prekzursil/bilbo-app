package dev.bilbo.domain

import kotlinx.datetime.Instant

data class EmotionalCheckIn(
    val id: Long = 0,
    val timestamp: Instant,
    val preSessionEmotion: Emotion,
    val postSessionMood: Emotion? = null,
    val linkedIntentId: Long? = null
)

enum class Emotion {
    HAPPY, CALM, BORED, STRESSED, ANXIOUS, SAD, LONELY
}
