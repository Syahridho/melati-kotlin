package com.example.melati

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CekNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tabRiwayatDapatDipilihDariCek() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Riwayat").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Riwayat").assertExists()
    }
}
