package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OcrCategory
import com.example.model.OcrLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    bitmap: Bitmap,
    originalBitmap: Bitmap?,
    lines: List<OcrLine>,
    selectedLine: OcrLine?,
    busy: Boolean,
    busyMessage: String,
    statusMessage: String?,
    undoCount: Int,
    redoCount: Int,
    isPdf: Boolean = false,
    currentPdfPage: Int = 0,
    totalPdfPages: Int = 1,
    onPrevPdfPage: () -> Unit = {},
    onNextPdfPage: () -> Unit = {},
    onOpenPdfManager: () -> Unit = {},
    onBack: () -> Unit,
    onSelectLine: (OcrLine) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRescanOcr: () -> Unit,
    onOpenGpsPanel: () -> Unit,
    onOpenBeforeAfter: () -> Unit,
    onResetToOriginal: () -> Unit,
    onClearSelection: () -> Unit
) {
    var showBoxes by remember { mutableStateOf(true) }
    var activeCategoryFilter by remember { mutableStateOf<OcrCategory?>(null) }
    var isHoldingOriginal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isPdf) "PDF Text Editor" else "AI Image Text Editor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isPdf) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        "PDF",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            if (isPdf && totalPdfPages > 1) {
                                "Page ${currentPdfPage + 1} of $totalPdfPages • ${bitmap.width}×${bitmap.height} px • ${lines.size} lines"
                            } else {
                                "${bitmap.width} × ${bitmap.height} px • ${lines.size} text lines"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isPdf) {
                        // PDF Manage Pages & Export action
                        FilledTonalButton(
                            onClick = onOpenPdfManager,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("manage_pdf_pages_button")
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pages & Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (totalPdfPages > 1) {
                            IconButton(
                                onClick = onPrevPdfPage,
                                enabled = currentPdfPage > 0 && !busy,
                                modifier = Modifier.testTag("prev_pdf_page_button")
                            ) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Page")
                            }

                            IconButton(
                                onClick = onNextPdfPage,
                                enabled = currentPdfPage < totalPdfPages - 1 && !busy,
                                modifier = Modifier.testTag("next_pdf_page_button")
                            ) {
                                Icon(Icons.Default.NavigateNext, contentDescription = "Next Page")
                            }
                        }
                    }

                    IconButton(
                        onClick = onUndo,
                        enabled = undoCount > 0 && !busy,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (undoCount > 0) {
                                    Badge { Text("$undoCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                        }
                    }

                    IconButton(
                        onClick = onRedo,
                        enabled = redoCount > 0 && !busy,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (redoCount > 0) {
                                    Badge { Text("$redoCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Redo, contentDescription = "Redo")
                        }
                    }

                    IconButton(
                        onClick = onSave,
                        enabled = !busy,
                        modifier = Modifier.testTag("save_button")
                    ) {
                        Icon(
                            if (isPdf) Icons.Default.FileDownload else Icons.Default.Save,
                            contentDescription = if (isPdf) "Save/Export" else "Save to Gallery",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Divider()

                // Filter Category Chips (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeCategoryFilter == null,
                        onClick = { activeCategoryFilter = null },
                        label = { Text("All (${lines.size})", fontSize = 12.sp) }
                    )

                    val gpsLinesCount = lines.count { it.category == OcrCategory.LATITUDE || it.category == OcrCategory.LONGITUDE }
                    if (gpsLinesCount > 0) {
                        FilterChip(
                            selected = activeCategoryFilter == OcrCategory.LATITUDE || activeCategoryFilter == OcrCategory.LONGITUDE,
                            onClick = {
                                activeCategoryFilter = if (activeCategoryFilter == OcrCategory.LATITUDE) null else OcrCategory.LATITUDE
                            },
                            label = { Text("GPS ($gpsLinesCount)", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    val dateLinesCount = lines.count { it.category == OcrCategory.DATE || it.category == OcrCategory.TIME }
                    if (dateLinesCount > 0) {
                        FilterChip(
                            selected = activeCategoryFilter == OcrCategory.DATE || activeCategoryFilter == OcrCategory.TIME,
                            onClick = {
                                activeCategoryFilter = if (activeCategoryFilter == OcrCategory.DATE) null else OcrCategory.DATE
                            },
                            label = { Text("Date/Time ($dateLinesCount)", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    val noteLinesCount = lines.count { it.category == OcrCategory.NOTE }
                    if (noteLinesCount > 0) {
                        FilterChip(
                            selected = activeCategoryFilter == OcrCategory.NOTE,
                            onClick = {
                                activeCategoryFilter = if (activeCategoryFilter == OcrCategory.NOTE) null else OcrCategory.NOTE
                            },
                            label = { Text("Notes ($noteLinesCount)", fontSize = 12.sp) }
                        )
                    }

                    FilterChip(
                        selected = !showBoxes,
                        onClick = { showBoxes = !showBoxes },
                        label = { Text(if (showBoxes) "Hide Boxes" else "Show Boxes", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                if (showBoxes) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                // Bottom Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // GPS / Date Panel Button
                    FilledTonalButton(
                        onClick = onOpenGpsPanel,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("gps_panel_button")
                    ) {
                        Icon(Icons.Default.GpsFixed, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("GPS / Fields", fontSize = 12.sp)
                    }

                    // Before / After Split Compare Button
                    OutlinedButton(
                        onClick = onOpenBeforeAfter,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("compare_button")
                    ) {
                        Icon(Icons.Default.Compare, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compare", fontSize = 12.sp)
                    }

                    // OCR Rescan
                    IconButton(
                        onClick = onRescanOcr,
                        enabled = !busy,
                        modifier = Modifier.testTag("rescan_ocr_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan OCR")
                    }

                    // Reset to original
                    IconButton(
                        onClick = onResetToOriginal,
                        enabled = undoCount > 0 && !busy,
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset to Original", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Interactive Zoomable & Pannable Canvas
            ImageZoomableCanvas(
                bitmap = bitmap,
                originalBitmap = originalBitmap,
                isHoldingOriginal = isHoldingOriginal,
                lines = lines,
                selectedLine = selectedLine,
                showBoxes = showBoxes,
                activeCategoryFilter = activeCategoryFilter,
                onLineSelected = onSelectLine
            )

            // Top Guidance Banner or Status Banner
            AnimatedVisibility(
                visible = statusMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                statusMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xEE1E2433),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Quick Floating Prompt if no line is selected
            if (selectedLine == null && lines.isNotEmpty() && !busy) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xCC000000),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Tap any highlighted text line to edit",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Loading / Busy Overlay with animated indicator
            if (busy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = busyMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
