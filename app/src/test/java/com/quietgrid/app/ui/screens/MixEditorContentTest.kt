package com.quietgrid.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.MixRepository
import com.quietgrid.app.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MixEditorContentTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val existingMix = Mix(id = "mix-1", name = "Old Name", entries = emptyList())

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val mixRepository = mockk<MixRepository>(relaxed = true)

    private fun stubRepositories() {
        every { settingsRepository.settings } returns flowOf(AppSettings())

        every { mixRepository.mixes } returns flowOf(listOf(existingMix))
        every { mixRepository.activeMixId } returns flowOf(null)
        coEvery { mixRepository.saveMix(any()) } just Runs
    }

    @Test
    fun `editing the name then losing focus saves the new name`() {
        stubRepositories()

        composeRule.setContent {
            MixEditorContent(
                mixId = existingMix.id,
                onDone = {},
                settingsRepository = settingsRepository,
                mixRepository = mixRepository,
                saveScope = CoroutineScope(mainDispatcherRule.dispatcher),
                renameTrigger = 1,
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MIX_NAME_FIELD_TEST_TAG).performTextReplacement("New Name")
        composeRule.onNodeWithTag(MIX_NAME_FIELD_TEST_TAG).performImeAction()
        composeRule.waitForIdle()

        coVerify(exactly = 1) {
            mixRepository.saveMix(match { it.id == existingMix.id && it.name == "New Name" })
        }
    }
}
