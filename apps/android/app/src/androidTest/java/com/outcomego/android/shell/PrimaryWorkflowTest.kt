package com.outcomego.android.shell

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.outcomego.android.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrimaryWorkflowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun signInDirectoryOpenChatAndSend() {
        compose.onNodeWithTag("preview-workspace").performClick()

        compose.onNodeWithTag("directory-title").assertIsDisplayed()
        compose.onNodeWithTag("conversation-bot-1").performClick()
        compose.onNodeWithText("Good morning. What are we making progress on today?").assertIsDisplayed()

        compose.onNodeWithTag("message-composer").performTextInput("A quick update")
        compose.onNodeWithTag("send-message").performClick()
        compose.onNodeWithTag("message-composer").assertIsDisplayed()
    }

    @Test
    fun draftSurvivesLeavingAndReopeningConversation() {
        compose.onNodeWithTag("preview-workspace").performClick()
        compose.onNodeWithTag("conversation-bot-1").performClick()
        compose.onNodeWithTag("message-composer").performTextInput("Keep this note")

        compose.onNodeWithTag("back-to-conversations").performClick()
        compose.onNodeWithTag("conversation-bot-1").performClick()
        compose.onNodeWithText("Keep this note").assertIsDisplayed()
    }

    @Test
    fun composerCollapsesWhenUnfocusedAndSplitsWhenFocused() {
        compose.onNodeWithTag("preview-workspace").performClick()
        compose.onNodeWithTag("conversation-bot-1").performClick()

        val containerUnfocused = compose.onNodeWithTag("composer-container").getUnclippedBoundsInRoot()
        val attachUnfocused = compose.onNodeWithTag("attach-composer").getUnclippedBoundsInRoot()
        val inputUnfocused = compose.onNodeWithTag("message-composer").getUnclippedBoundsInRoot()
        val voiceUnfocused = compose.onNodeWithTag("voice-composer").getUnclippedBoundsInRoot()

        assertTrue(attachUnfocused.left >= containerUnfocused.left)
        assertTrue(attachUnfocused.right <= inputUnfocused.left)
        assertTrue(inputUnfocused.right <= voiceUnfocused.left)
        assertTrue(voiceUnfocused.right <= containerUnfocused.right)
        compose.onNodeWithTag("send-message").assertDoesNotExist()

        compose.onNodeWithTag("message-composer").performClick()
        compose.waitForIdle()

        val attachFocused = compose.onNodeWithTag("attach-composer").getUnclippedBoundsInRoot()
        val inputFocused = compose.onNodeWithTag("message-composer").getUnclippedBoundsInRoot()
        val voiceFocused = compose.onNodeWithTag("voice-composer").getUnclippedBoundsInRoot()
        assertTrue(inputFocused.bottom <= attachFocused.top)
        assertTrue(inputFocused.bottom <= voiceFocused.top)

        compose.onNodeWithTag("message-composer").performTextInput("Hello")
        compose.onNodeWithTag("voice-composer").assertDoesNotExist()
        compose.onNodeWithTag("send-message").assertIsDisplayed().performClick()
        compose.onNodeWithTag("voice-composer").assertIsDisplayed()
        compose.onNodeWithTag("send-message").assertDoesNotExist()
    }
}
