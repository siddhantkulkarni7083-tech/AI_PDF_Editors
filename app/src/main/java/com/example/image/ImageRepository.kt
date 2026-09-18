package com.example.image

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ImageRepository {

    fun loadBitmap(resolver: ContentResolver, uri: Uri): Bitmap {
        var inputStream: InputStream? = null
        try {
            // First check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri).use { stream ->
                requireNotNull(stream) { "Unable to open image stream" }
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Calculate sample size if extremely massive to prevent OOM
            var sampleSize = 1
            val maxDimension = 4096
            val maxSide = max(options.outWidth, options.outHeight)
            while (maxSide / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = sampleSize
                inMutable = true
            }

            val decodedBitmap = resolver.openInputStream(uri).use { stream ->
                requireNotNull(stream) { "Unable to decode image" }
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: error("Failed to decode image into bitmap")

            // Read EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                resolver.openInputStream(uri).use { stream ->
                    if (stream != null) {
                        val exif = ExifInterface(stream)
                        orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                }
            } catch (_: Exception) {}

            return applyExifRotation(decodedBitmap, orientation)
        } finally {
            inputStream?.close()
        }
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    fun saveToGallery(
        resolver: ContentResolver,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): Uri {
        val timestamp = System.currentTimeMillis()
        val filename = "AI_TextEdit_${timestamp}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/AI Image Text Editor"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("Could not create media item in Gallery")

        try {
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "Could not open stream for writing" }
                bitmap.compress(format, 98, output)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, completeValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /**
     * Generates a realistic sample photo with GPS overlay text for testing.
     */
    fun createSampleGpsImage(): Bitmap {
        val width = 1280
        val height = 960
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw scenery backdrop (sky + ground + mountains + sun)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sky gradient
        for (y in 0 until height / 2) {
            val ratio = y.toFloat() / (height / 2f)
            val r = (100 + (180 - 100) * ratio).toInt()
            val g = (160 + (220 - 160) * ratio).toInt()
            val b = (230 + (255 - 230) * ratio).toInt()
            paint.color = Color.rgb(r, g, b)
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }

        // Sun
        paint.color = Color.rgb(255, 230, 140)
        canvas.drawCircle(width * 0.75f, height * 0.22f, 70f, paint)

        // Mountains
        paint.color = Color.rgb(90, 110, 140)
        val path = android.graphics.Path().apply {
            moveTo(0f, height * 0.52f)
            lineTo(width * 0.25f, height * 0.32f)
            lineTo(width * 0.5f, height * 0.55f)
            lineTo(width * 0.78f, height * 0.28f)
            lineTo(width.toFloat(), height * 0.5f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(path, paint)

        // Field / Ground
        for (y in (height / 2) until height) {
            val ratio = (y - height / 2).toFloat() / (height / 2f)
            val r = (70 + (40 - 70) * ratio).toInt()
            val g = (140 + (100 - 140) * ratio).toInt()
            val b = (60 + (40 - 60) * ratio).toInt()
            paint.color = Color.rgb(r, g, b)
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }

        // GPS Stamp Overlay Box (bottom left)
        val boxLeft = 40f
        val boxTop = height - 290f
        val boxRight = width - 40f
        val boxBottom = height - 40f

        // Semi-transparent dark background banner for GPS
        paint.color = Color.argb(180, 20, 25, 35)
        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 16f, 16f, paint)

        // Accent border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.argb(120, 255, 255, 255)
        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 16f, 16f, paint)

        // Draw Stamp Lines
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 28f

        var startY = boxTop + 45f
        val startX = boxLeft + 24f
        val lineSpacing = 36f

        val sampleDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val sampleTime = SimpleDateFormat("HH:mm:ss 'GMT'", Locale.US).format(Date())

        val lines = listOf(
            "Latitude: 18.520430° N",
            "Longitude: 73.856743° E",
            "Elevation: 560.4 m  Accuracy: 3.5 m",
            "Date: $sampleDate  Time: $sampleTime",
            "Note: Construction Survey Site #A-104",
            "Address: Shivajinagar, Pune, Maharashtra 411005"
        )

        for (line in lines) {
            canvas.drawText(line, startX, startY, paint)
            startY += lineSpacing
        }

        return bitmap
    }
}
