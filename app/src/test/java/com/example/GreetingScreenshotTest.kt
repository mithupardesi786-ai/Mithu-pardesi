package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Document
import com.example.ui.DocumentCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockDocument = Document(
      id = 1,
      type = "PASSPORT",
      documentNumber = "EP8843901",
      fullName = "JANE DOE",
      nationality = "UNITED KINGDOM",
      birthDate = "1995-12-08",
      expiryDate = "2512-12-30",
      issueDate = "2026-05-30",
      issuer = "UK HER MAJESTY PASSPORT OFFICE",
      photoSeed = 42,
      additionalData = "Passport Type: P | Code: GBR",
      qrCodePayload = "VERIFIED_SECURE_ID"
    )

    composeTestRule.setContent { 
      MyApplicationTheme { 
        DocumentCard(
          doc = mockDocument,
          isExpanded = true,
          onToggle = {},
          onDelete = {}
        )
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
