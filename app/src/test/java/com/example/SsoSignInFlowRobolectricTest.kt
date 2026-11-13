package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.di.AppContainer
import com.example.ui.screens.EnterpriseCleanHeader
import com.example.ui.screens.LiquidLogoSignInOverlay
import com.example.ui.theme.SsoTimetableTheme
import com.example.ui.viewmodel.TimetableViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SsoSignInFlowRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `signIn stages pending animation state before entering dashboard`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val container = AppContainer(context)
        val viewModel = TimetableViewModel.factory(container).create(TimetableViewModel::class.java)

        val initialized = viewModel.uiState.first { !it.isInitializing }
        assertNull(initialized.activeFaculty)

        viewModel.signInWithSsoCredentials("evance@university.edu", "faculty2026", rememberInJson = false)
        val pendingState = viewModel.uiState.first { it.pendingSignInLabel != null }
        assertNull(pendingState.activeFaculty)
        assertTrue(pendingState.pendingSignInLabel?.contains("Vance") == true)

        viewModel.completePendingSignIn()
        val signedInState = viewModel.uiState.first { it.activeFaculty != null && it.pendingSignInLabel == null }
        assertNotNull(signedInState.activeFaculty)
        assertEquals("evance@university.edu", signedInState.activeFaculty?.email)
    }

    @Test
    fun `liquid logo overlay fills and triggers completion callback`() {
        var completed = false
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            SsoTimetableTheme {
                LiquidLogoSignInOverlay(
                    facultyLabel = "Dr. Eleanor Vance",
                    onFillComplete = { completed = true }
                )
            }
        }

        composeRule.onNodeWithTag("liquid_logo_signin_overlay").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(2200L)
        composeRule.waitForIdle()
        assertTrue(completed)
    }

    @Test
    fun `enterprise clean header displays editorial eyebrow and title`() {
        composeRule.setContent {
            SsoTimetableTheme {
                EnterpriseCleanHeader(
                    title = "SSO Timetable",
                    subtitle = "Dr. Eleanor Vance · Fall 2026",
                    eyebrow = "Academic Schedule",
                    conflictCount = 0,
                    isDarkMode = false,
                    onToggleTheme = {},
                    showExportPdf = true
                )
            }
        }

        composeRule.onNodeWithTag("enterprise_clean_header").assertIsDisplayed()
        composeRule.onNodeWithText("ACADEMIC SCHEDULE").assertIsDisplayed()
        composeRule.onNodeWithText("SSO Timetable").assertIsDisplayed()
        composeRule.onNodeWithTag("export_pdf_button").assertIsDisplayed()
    }
}
