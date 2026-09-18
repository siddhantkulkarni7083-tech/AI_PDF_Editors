package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.OcrCategory
import com.example.ocr.GpsParser
import com.example.ui.HomeScreen
import com.example.ui.theme.AIImageTextEditorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun homeScreen_rendersSuccessfully() {
    composeTestRule.setContent {
      AIImageTextEditorTheme {
        HomeScreen(
          onImportImage = {},
          onImportPdf = {},
          onTakePhoto = {},
          onLoadSample = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("import_image_button").assert(hasTestTag("import_image_button"))
    composeTestRule.onNodeWithTag("import_pdf_button").assert(hasTestTag("import_pdf_button"))
    composeTestRule.onNodeWithTag("take_photo_button").assert(hasTestTag("take_photo_button"))
    composeTestRule.onNodeWithTag("load_sample_button").assert(hasTestTag("load_sample_button"))
  }

  @Test
  fun gpsParser_detectsGpsAndDateFields() {
    assertEquals(OcrCategory.LATITUDE, GpsParser.categorize("Lat: 37.7749° N").first)
    assertEquals(OcrCategory.LONGITUDE, GpsParser.categorize("Long: 122.4194° W").first)
    assertEquals(OcrCategory.DATE, GpsParser.categorize("Date: 17/08/2026").first)
    assertEquals(OcrCategory.TIME, GpsParser.categorize("Time: 14:32:05 GMT").first)
    assertEquals(OcrCategory.ELEVATION, GpsParser.categorize("Elevation: 42.5 m").first)
    assertEquals(OcrCategory.ACCURACY, GpsParser.categorize("Accuracy: 3.2m").first)
  }
}
