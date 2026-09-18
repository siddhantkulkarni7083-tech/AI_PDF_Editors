package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.model.OcrCategory
import com.example.model.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): List<OcrLine> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val lines = mutableListOf<OcrLine>()
                    var idCounter = 1L

                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            val text = line.text.trim()
                            val box = line.boundingBox ?: Rect()
                            if (text.isNotBlank() && !box.isEmpty) {
                                val (category, extracted) = GpsParser.categorize(text)
                                val avgConfidence = if (line.elements.isNotEmpty()) {
                                    line.elements.mapNotNull { it.confidence }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
                                } else null

                                lines.add(
                                    OcrLine(
                                        id = idCounter++,
                                        text = text,
                                        box = box,
                                        confidence = avgConfidence,
                                        category = category,
                                        extractedValue = extracted
                                    )
                                )
                            }
                        }
                    }

                    // Sort lines from top to bottom, then left to right
                    lines.sortWith(compareBy({ it.box.top }, { it.box.left }))

                    if (cont.isActive) {
                        cont.resume(lines)
                    }
                }
                .addOnFailureListener { error ->
                    if (cont.isActive) {
                        cont.resumeWithException(error)
                    }
                }
        }

    fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
