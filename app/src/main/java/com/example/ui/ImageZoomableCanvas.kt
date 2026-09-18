package com.example.ui

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OcrCategory
import com.example.model.OcrLine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageZoomableCanvas(
    bitmap: Bitmap,
    originalBitmap: Bitmap?,
    isHoldingOriginal: Boolean,
    lines: List<OcrLine>,
    selectedLine: OcrLine?,
    showBoxes: Boolean,
    activeCategoryFilter: OcrCategory?,
    onLineSelected: (OcrLine) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val displayBitmap = if (isHoldingOriginal && originalBitmap != null) originalBitmap else bitmap

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value

        val imageRatio = bitmap.width.toFloat() / max(1f, bitmap.height.toFloat())
        val containerRatio = containerWidth / max(1f, containerHeight)

        val baseWidth: Float
        val baseHeight: Float

        if (imageRatio > containerRatio) {
            baseWidth = containerWidth
            baseHeight = containerWidth / imageRatio
        } else {
            baseHeight = containerHeight
            baseWidth = containerHeight * imageRatio
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.8f, 6f)
                        val maxPanX = (baseWidth * zoomScale - baseWidth).coerceAtLeast(0f) / 2f + 200f
                        val maxPanY = (baseHeight * zoomScale - baseHeight).coerceAtLeast(0f) / 2f + 200f
                        panOffset = Offset(
                            x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                            y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                        )
                    }
                }
                .pointerInput(lines, displayBitmap, zoomScale, panOffset, baseWidth, baseHeight) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1.2f) {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            } else {
                                zoomScale = 2.2f
                            }
                        },
                        onTap = { tapOffset ->
                            // Convert tap from container center to image coordinates
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f

                            val relX = (tapOffset.x - centerX - panOffset.x) / zoomScale + baseWidth / 2f
                            val relY = (tapOffset.y - centerY - panOffset.y) / zoomScale + baseHeight / 2f

                            if (relX in 0f..baseWidth && relY in 0f..baseHeight) {
                                val imgX = (relX / baseWidth * bitmap.width).toInt()
                                val imgY = (relY / baseHeight * bitmap.height).toInt()

                                // Find tapped OCR line with a slight touch radius tolerance
                                val hit = lines.firstOrNull {
                                    val expanded = Rect(
                                        it.box.left - 6,
                                        it.box.top - 6,
                                        it.box.right + 6,
                                        it.box.bottom + 6
                                    )
                                    expanded.contains(imgX, imgY)
                                }
                                if (hit != null) {
                                    onLineSelected(hit)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(baseWidth.dp, baseHeight.dp)
                    .offset { IntOffset(panOffset.x.roundToInt(), panOffset.y.roundToInt()) }
            ) {
                // Apply Zoom transform
                withTransform({
                    scale(scaleX = zoomScale, scaleY = zoomScale, pivot = center)
                }) {
                    // Draw main image bitmap
                    val imgBmp = displayBitmap.asImageBitmap()
                    drawImage(
                        image = imgBmp,
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                    )

                    // Draw OCR Bounding Boxes if not in original preview mode
                    if (showBoxes && !isHoldingOriginal) {
                        val sx = size.width / bitmap.width.toFloat()
                        val sy = size.height / bitmap.height.toFloat()

                        lines.forEach { line ->
                            val isFilteredOut = activeCategoryFilter != null && line.category != activeCategoryFilter
                            val isSelected = line.id == selectedLine?.id

                            if (!isFilteredOut || isSelected) {
                                val left = line.box.left * sx
                                val top = line.box.top * sy
                                val right = line.box.right * sx
                                val bottom = line.box.bottom * sy
                                val boxW = max(2f, right - left)
                                val boxH = max(2f, bottom - top)

                                val boxColor = when {
                                    isSelected -> Color(0xFFFF3366)
                                    line.category == OcrCategory.LATITUDE -> Color(0xFFF59E0B)
                                    line.category == OcrCategory.LONGITUDE -> Color(0xFF10B981)
                                    line.category == OcrCategory.DATE || line.category == OcrCategory.TIME -> Color(0xFF8B5CF6)
                                    line.category == OcrCategory.ELEVATION || line.category == OcrCategory.ACCURACY -> Color(0xFF06B6D4)
                                    line.category == OcrCategory.NOTE -> Color(0xFFEC4899)
                                    else -> Color(0xFF60A5FA)
                                }

                                // Draw subtle background tint on box
                                drawRect(
                                    color = boxColor.copy(alpha = if (isSelected) 0.35f else 0.12f),
                                    topLeft = Offset(left, top),
                                    size = Size(boxW, boxH)
                                )

                                // Draw outline stroke
                                drawRect(
                                    color = boxColor.copy(alpha = if (isSelected) 1f else 0.85f),
                                    topLeft = Offset(left, top),
                                    size = Size(boxW, boxH),
                                    style = Stroke(
                                        width = if (isSelected) 3.5f else 1.8f,
                                        pathEffect = if (isSelected) null else PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                                    )
                                )

                                // Draw corner anchor markers for selected line
                                if (isSelected) {
                                    val cornerLen = min(12f, min(boxW, boxH) / 2.5f)
                                    val strokeW = 4f
                                    // Top-left
                                    drawLine(Color.White, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                                    drawLine(Color.White, Offset(left, top), Offset(left, top + cornerLen), strokeW)
                                    // Top-right
                                    drawLine(Color.White, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                                    drawLine(Color.White, Offset(right, top), Offset(right, top + cornerLen), strokeW)
                                    // Bottom-left
                                    drawLine(Color.White, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                                    drawLine(Color.White, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)
                                    // Bottom-right
                                    drawLine(Color.White, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                                    drawLine(Color.White, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay banner if holding original
        if (isHoldingOriginal) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD000000)
            ) {
                Text(
                    text = "Viewing Original Photo",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Floating Zoom Controls (Bottom Right)
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC1E2433))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(6f) }
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                }

                IconButton(
                    onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.8f) }
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    }
                ) {
                    Icon(Icons.Default.FitScreen, contentDescription = "Reset Zoom", tint = Color.White)
                }
            }
        }
    }
}
