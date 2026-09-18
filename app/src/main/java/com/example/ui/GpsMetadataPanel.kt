package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.OcrCategory
import com.example.model.OcrLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsMetadataPanel(
    lines: List<OcrLine>,
    onClose: () -> Unit,
    onSelectLine: (OcrLine) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<OcrCategory?>(null) }

    val filteredLines = remember(lines, searchQuery, selectedCategoryFilter) {
        lines.filter { line ->
            val matchesQuery = searchQuery.isBlank() || line.text.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategoryFilter == null || line.category == selectedCategoryFilter
            matchesQuery && matchesCat
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(12.dp)
                .testTag("gps_metadata_panel"),
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
                                    Icons.Default.GpsFixed,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "GPS & Metadata Inspector",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${lines.size} OCR lines recognized",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search text, coordinates, dates...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All (${lines.size})", fontSize = 11.sp) }
                    )

                    val gpsCount = lines.count { it.category == OcrCategory.LATITUDE || it.category == OcrCategory.LONGITUDE }
                    if (gpsCount > 0) {
                        FilterChip(
                            selected = selectedCategoryFilter == OcrCategory.LATITUDE || selectedCategoryFilter == OcrCategory.LONGITUDE,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == OcrCategory.LATITUDE) null else OcrCategory.LATITUDE
                            },
                            label = { Text("GPS ($gpsCount)", fontSize = 11.sp) }
                        )
                    }

                    val dateCount = lines.count { it.category == OcrCategory.DATE || it.category == OcrCategory.TIME }
                    if (dateCount > 0) {
                        FilterChip(
                            selected = selectedCategoryFilter == OcrCategory.DATE || selectedCategoryFilter == OcrCategory.TIME,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == OcrCategory.DATE) null else OcrCategory.DATE
                            },
                            label = { Text("Date/Time ($dateCount)", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // List of OCR / GPS fields
                if (filteredLines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No matching text lines found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLines, key = { it.id }) { line ->
                            GpsLineCard(
                                line = line,
                                onClick = {
                                    onSelectLine(line)
                                    onClose()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun GpsLineCard(
    line: OcrLine,
    onClick: () -> Unit
) {
    val (catColor, catIcon) = when (line.category) {
        OcrCategory.LATITUDE -> Pair(Color(0xFFF59E0B), Icons.Default.LocationOn)
        OcrCategory.LONGITUDE -> Pair(Color(0xFF10B981), Icons.Default.Explore)
        OcrCategory.ELEVATION -> Pair(Color(0xFF06B6D4), Icons.Default.Landscape)
        OcrCategory.ACCURACY -> Pair(Color(0xFF06B6D4), Icons.Default.GpsFixed)
        OcrCategory.DATE -> Pair(Color(0xFF8B5CF6), Icons.Default.CalendarToday)
        OcrCategory.TIME -> Pair(Color(0xFF8B5CF6), Icons.Default.AccessTime)
        OcrCategory.NOTE -> Pair(Color(0xFFEC4899), Icons.Default.Description)
        OcrCategory.OTHER -> Pair(Color(0xFF64748B), Icons.Default.TextFields)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ocr_line_card_${line.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = catColor.copy(alpha = 0.18f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(catIcon, null, tint = catColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        line.category.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                    if (line.confidence != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${(line.confidence * 100).toInt()}% conf",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    line.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit line",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
