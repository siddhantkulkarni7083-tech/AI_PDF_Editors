package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BeforeAfterComparisonDialog(
    original: Bitmap?,
    edited: Bitmap?,
    onClose: () -> Unit
) {
    var isSplitView by remember { mutableStateOf(true) }
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(12.dp)
                .testTag("before_after_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Compare,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Before / After Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Mode Tabs
                TabRow(
                    selectedTabIndex = if (isSplitView) 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = isSplitView,
                        onClick = { isSplitView = true },
                        text = { Text("Split Slider", fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Compare, null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = !isSplitView,
                        onClick = { isSplitView = false },
                        text = { Text("Side by Side", fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.ViewColumn, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Content View
                if (original != null && edited != null) {
                    if (isSplitView) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            val origImg = remember(original) { original.asImageBitmap() }
                            val editImg = remember(edited) { edited.asImageBitmap() }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val newFrac = (splitFraction + dragAmount.x / size.width).coerceIn(0.05f, 0.95f)
                                            splitFraction = newFrac
                                        }
                                    }
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val splitX = canvasWidth * splitFraction

                                // 1. Draw Edited Bitmap on entire canvas
                                drawImage(
                                    image = editImg,
                                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                                )

                                // 2. Draw Original Bitmap clipped to the left side
                                clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasHeight) {
                                    drawImage(
                                        image = origImg,
                                        dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                                    )
                                }

                                // 3. Draw vertical divider line
                                drawLine(
                                    color = Color.White,
                                    start = Offset(splitX, 0f),
                                    end = Offset(splitX, canvasHeight),
                                    strokeWidth = 3.dp.toPx()
                                )

                                // 4. Draw Center Drag Handle Circle
                                drawCircle(
                                    color = Color.White,
                                    radius = 16.dp.toPx(),
                                    center = Offset(splitX, canvasHeight / 2f)
                                )
                                drawCircle(
                                    color = Color(0xFF6366F1),
                                    radius = 12.dp.toPx(),
                                    center = Offset(splitX, canvasHeight / 2f)
                                )
                            }

                            // Badges (Original on Left, Edited on Right)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xCC000000)
                            ) {
                                Text("Original", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xCC6366F1)
                            ) {
                                Text("Edited", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    } else {
                        // Side by Side View
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xCC000000),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text("Original", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = original.asImageBitmap(),
                                        contentDescription = "Original",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xCC6366F1),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text("Edited", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = edited.asImageBitmap(),
                                        contentDescription = "Edited",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Preview")
                }
            }
        }
    }
}
