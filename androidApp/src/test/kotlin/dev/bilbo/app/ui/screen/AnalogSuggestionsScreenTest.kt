package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.SuggestionCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [AnalogSuggestionsScreen].
 *
 * Covers:
 *  - header text always visible
 *  - back button shown when onBack != null, hidden when null
 *  - back button fires callback
 *  - FAB opens add-dialog
 *  - dialog dismiss hides dialog
 *  - active suggestion cards rendered when provided
 *  - custom suggestion section header + delete button
 *  - delete callback fires with correct id
 *  - empty active/custom lists render without crash
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnalogSuggestionsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun makeSuggestion(id: Long, text: String, custom: Boolean = false) =
        AnalogSuggestion(
            id = id,
            text = text,
            category = SuggestionCategory.READING,
            tags = emptyList(),
            isCustom = custom,
        )

    @Test
    fun `header text is always rendered`() {
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Analog Alternatives").assertExists()
        composeRule.onNodeWithText("Need inspiration?").assertExists()
        composeRule.onNodeWithText("Step away from the screen — here are some ideas.").assertExists()
    }

    @Test
    fun `back button not shown when onBack is null`() {
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                    onBack = null,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun `back button shown and fires callback when onBack provided`() {
        var backCount = 0
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                    onBack = { backCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    @Test
    fun `FAB click opens add-custom dialog`() {
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Add your own").performClick()
        composeRule.waitForIdle()
        // Dialog shows a "Save" or "Cancel" button – either indicates it opened
        composeRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun `dismissing add dialog hides it`() {
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Add your own").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun `custom suggestions section header and delete button rendered`() {
        val customSugg = makeSuggestion(id = 42L, text = "Read a novel", custom = true)
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(customSuggestions = listOf(customSugg)),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Custom Suggestions").assertExists()
        composeRule.onNodeWithText("Read a novel").assertExists()
        composeRule.onNodeWithContentDescription("Delete").assertExists()
    }

    @Test
    fun `delete callback fires with correct id`() {
        val deletedIds = mutableListOf<Long>()
        val customSugg = makeSuggestion(id = 99L, text = "Walk in the park", custom = true)
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(customSuggestions = listOf(customSugg)),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = { deletedIds.add(it) },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()
        assertTrue(deletedIds.contains(99L))
    }

    @Test
    fun `empty state renders without crash`() {
        composeRule.setContent {
            BilboTheme {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(
                        activeSuggestions = emptyList(),
                        customSuggestions = emptyList(),
                    ),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Analog Alternatives").assertExists()
    }
}
