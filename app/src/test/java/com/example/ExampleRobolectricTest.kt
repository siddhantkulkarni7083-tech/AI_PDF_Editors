package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.example.image.TextReplacementEngine
import com.example.model.TextEditOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Image Text Editor", appName)
  }

  @Test
  fun `textReplacementEngine analyzes font metrics and preserves external pixels`() {
    val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.rgb(20, 25, 35))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 28f
    }
    canvas.drawText("Date: 18/08/2026", 40f, 100f, paint)

    val box = Rect(40, 75, 280, 110)
    val metrics = TextReplacementEngine.analyzeTextRegion(bitmap, box, "Date: 18/08/2026")

    assertNotNull(metrics)
    assertTrue("Recommended size should be calculated accurately", metrics.recommendedSize in 20f..40f)

    val replaced = TextReplacementEngine.replaceText(
      original = bitmap,
      box = box,
      oldText = "Date: 18/08/2026",
      newText = "Date: 25/12/2026",
      options = TextEditOptions(fontSize = 28f)
    )

    // Verify dimensions are preserved
    assertEquals(bitmap.width, replaced.width)
    assertEquals(bitmap.height, replaced.height)

    // Verify pixel outside patch area remains 100% untouched
    val originalCornerPixel = bitmap.getPixel(10, 10)
    val replacedCornerPixel = replaced.getPixel(10, 10)
    assertEquals(originalCornerPixel, replacedCornerPixel)
  }
}

