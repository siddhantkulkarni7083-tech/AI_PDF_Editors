package com.example.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.image.TextReplacementEngine
import com.example.model.BackgroundMatchMode
import com.example.model.OcrCategory
import com.example.model.OcrLine
import com.example.model.TextAlignment
import com.example.model.TextEditOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextConfirmationDialog(
    line: OcrLine,
    initialText: String,
    bitmap: Bitmap? = null,
    surroundingLines: List<OcrLine> = emptyList(),
    onCancel: () -> Unit,
    onConfirm: (newText: String, options: TextEditOptions) -> Unit
) {
    var textValue by remember { mutableStateOf(initialText) }
    var selectedColorIndex by remember { mutableIntStateOf(0) } // 0 = Auto
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var alignment by remember { mutableStateOf(TextAlignment.LEFT) }
    var fontFamily by remember { mutableStateOf("sans-serif") }
    
    // Auto-detected font metrics
    val autoMetrics = remember(line, bitmap) {
        if (bitmap != null && !line.box.isEmpty) {
            TextReplacementEngine.analyzeTextRegion(bitmap, line.box, line.text, surroundingLines)
        } else null
    }

    val defaultAutoFontSize = autoMetrics?.recommendedSize ?: max(14f, line.box.height() * 0.75f)
    
    // Custom font size override
    var isCustomFontSize by remember { mutableStateOf(false) }
    var customFontSize by remember { mutableFloatStateOf(defaultAutoFontSize) }
    var letterSpacing by remember { mutableFloatStateOf(0.0f) }
    var textScaleX by remember { mutableFloatStateOf(1.0f) }
    var expandPadding by remember { mutableIntStateOf(0) }
    
    // Background Matching & Inpainting Controls
    var bgMatchMode by remember { mutableStateOf(BackgroundMatchMode.AUTO_SMART) }
    var featherRadius by remember { mutableIntStateOf(3) }
    var addMatchingGrain by remember { mutableStateOf(true) }
    var selectedBgColorIndex by remember { mutableIntStateOf(0) } // 0 = Auto Sampled
    
    var showAdvancedSettings by remember { mutableStateOf(true) }

    // Live preview bitmap state
    var livePreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val colorPresets = listOf(
        Pair("Auto", null),
        Pair("White", AndroidColor.WHITE),
        Pair("Black", AndroidColor.BLACK),
        Pair("Off-White", AndroidColor.rgb(240, 240, 240)),
        Pair("Dark Gray", AndroidColor.DKGRAY),
        Pair("Yellow", AndroidColor.rgb(255, 235, 59)),
        Pair("Cyan", AndroidColor.rgb(0, 229, 255)),
        Pair("Red", AndroidColor.rgb(255, 59, 48))
    )

    val bgColorPresets = listOf(
        Pair("Auto Sampled", null),
        Pair("Dark Banner", AndroidColor.rgb(20, 25, 35)),
        Pair("Black", AndroidColor.BLACK),
        Pair("Translucent Box", AndroidColor.argb(180, 10, 15, 25)),
        Pair("White/Paper", AndroidColor.WHITE)
    )

    // Update live preview whenever options change
    LaunchedEffect(
        textValue,
        selectedColorIndex,
        isBold,
        isItalic,
        alignment,
        fontFamily,
        isCustomFontSize,
        customFontSize,
        letterSpacing,
        textScaleX,
        expandPadding,
        bgMatchMode,
        featherRadius,
        addMatchingGrain,
        selectedBgColorIndex
    ) {
        if (bitmap != null && !line.box.isEmpty) {
            withContext(Dispatchers.Default) {
                try {
                    val chosenColor = colorPresets[selectedColorIndex].second
                    val chosenBgColor = bgColorPresets[selectedBgColorIndex].second
                    val opts = TextEditOptions(
                        fontSize = if (isCustomFontSize) customFontSize else null,
                        textColor = chosenColor,
                        isBold = isBold,
                        isItalic = isItalic,
                        alignment = alignment,
                        fontFamily = fontFamily,
                        letterSpacing = letterSpacing,
                        textScaleX = textScaleX,
                        expandPadding = expandPadding,
                        bgMatchMode = bgMatchMode,
                        customBgColor = chosenBgColor,
                        featherRadius = featherRadius,
                        addMatchingGrain = addMatchingGrain
                    )
                    val preview = TextReplacementEngine.renderPreviewCrop(
                        original = bitmap,
                        box = line.box,
                        oldText = line.text,
                        newText = textValue,
                        options = opts,
                        surroundingLines = surroundingLines
                    )
                    withContext(Dispatchers.Main) {
                        livePreviewBitmap = preview
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(10.dp)
                .testTag("edit_text_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (line.category) {
                                OcrCategory.LATITUDE -> Color(0xFFF59E0B)
                                OcrCategory.LONGITUDE -> Color(0xFF10B981)
                                OcrCategory.DATE, OcrCategory.TIME -> Color(0xFF8B5CF6)
                                OcrCategory.ELEVATION, OcrCategory.ACCURACY -> Color(0xFF06B6D4)
                                OcrCategory.NOTE -> Color(0xFFEC4899)
                                else -> MaterialTheme.colorScheme.primary
                            }.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = line.category.displayName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Edit Overlay Text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Live Real-Time Rendering Preview Card
                    if (livePreviewBitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LIVE RESULT PREVIEW:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Pixel-exact NoteCam match",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 60.dp, max = 110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = livePreviewBitmap!!.asImageBitmap(),
                                        contentDescription = "Live Inpainting Preview",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Original Reference Text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Original OCR Text:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Replacement Input Field
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replacement_text_input"),
                        label = { Text("New Replacement Text") },
                        placeholder = { Text("Enter replacement text...") },
                        trailingIcon = {
                            if (textValue != line.text) {
                                IconButton(onClick = { textValue = line.text }) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset to original")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Helper Buttons for Date / Time
                    when (line.category) {
                        OcrCategory.DATE -> {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val today = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
                                        textValue = replaceDateInText(textValue, today)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Today, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Today (DD/MM/YYYY)", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                        textValue = replaceDateInText(textValue, today)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("ISO (YYYY-MM-DD)", fontSize = 11.sp)
                                }
                            }
                        }
                        OcrCategory.TIME -> {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val now = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                                        textValue = replaceTimeInText(textValue, now)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Now (24h)", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val now = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
                                        textValue = replaceTimeInText(textValue, now)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("12-Hour AM/PM", fontSize = 11.sp)
                                }
                            }
                        }
                        OcrCategory.LATITUDE, OcrCategory.LONGITUDE -> {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        textValue = toggleHemisphere(textValue, line.category)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (line.category == OcrCategory.LATITUDE) "Toggle N/S" else "Toggle E/W", fontSize = 12.sp)
                                }
                            }
                        }
                        else -> {}
                    }

                    Spacer(Modifier.height(14.dp))

                    // ==========================================
                    // TEXT SIZE & METRICS SECTION (Dedicated)
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.FormatSize,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Text Size & Scale",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(
                                        selected = !isCustomFontSize,
                                        onClick = {
                                            isCustomFontSize = false
                                            customFontSize = defaultAutoFontSize
                                        },
                                        label = {
                                            Text(
                                                "Auto: ${String.format(Locale.US, "%.1f", defaultAutoFontSize)}px",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        modifier = Modifier.testTag("auto_text_size_chip")
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Font size slider & step controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isCustomFontSize) "Custom: ${String.format(Locale.US, "%.1f", customFontSize)} px" else "Auto Calibrated Size",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isCustomFontSize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            isCustomFontSize = true
                                            customFontSize = max(8f, customFontSize - 1.0f)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, "Decrease Size", modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            isCustomFontSize = true
                                            customFontSize += 1.0f
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, "Increase Size", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Slider(
                                value = if (isCustomFontSize) customFontSize else defaultAutoFontSize,
                                onValueChange = {
                                    isCustomFontSize = true
                                    customFontSize = it
                                },
                                valueRange = 8f..80f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("font_size_slider")
                            )

                            // Horizontal Scale (textScaleX) & Letter Spacing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Letter Spacing: ${String.format(Locale.US, "%.2f", letterSpacing)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = letterSpacing,
                                        onValueChange = { letterSpacing = it },
                                        valueRange = -0.05f..0.20f
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Width Scale: ${String.format(Locale.US, "%.2f", textScaleX)}x",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = textScaleX,
                                        onValueChange = { textScaleX = it },
                                        valueRange = 0.80f..1.30f
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ==========================================
                    // BACKGROUND & INPAINTING MATCH SECTION
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoFixHigh,
                                        null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Background & Inpaint Match",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "Seamless Blend",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Background Matching Mode Selection
                            Text("Reconstruction Mode:", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BackgroundMatchMode.values().forEach { mode ->
                                    val isSelected = bgMatchMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { bgMatchMode = mode },
                                        label = {
                                            Text(
                                                when (mode) {
                                                    BackgroundMatchMode.AUTO_SMART -> "Auto Smart"
                                                    BackgroundMatchMode.BANNER_MATCH -> "NoteCam Banner"
                                                    BackgroundMatchMode.TEXTURE_INPAINT -> "Photo Texture"
                                                    BackgroundMatchMode.CLEAN_FILL -> "Clean Fill"
                                                },
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Background Color Sampling / Override
                            Text("Background Color:", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bgColorPresets.forEachIndexed { index, (label, _) ->
                                    val isSelected = selectedBgColorIndex == index
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedBgColorIndex = index },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Edge Feathering / Softness Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Edge Feathering / Softness: ${featherRadius}px",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Zero box seams",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Slider(
                                value = featherRadius.toFloat(),
                                onValueChange = { featherRadius = it.roundToInt() },
                                valueRange = 1f..8f,
                                steps = 6
                            )

                            // Matching Camera Noise / Grain Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Match Camera Sensor Grain",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Adds micro-texture matching original photo noise",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = addMatchingGrain,
                                    onCheckedChange = { addMatchingGrain = it }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Advanced Styling Accordion
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAdvancedSettings = !showAdvancedSettings },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatPaint, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Font Family, Color & Alignment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    if (showAdvancedSettings) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            // Text Color Presets
                            Text("Text Color:", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                colorPresets.take(5).forEachIndexed { index, (label, _) ->
                                    val isSelected = selectedColorIndex == index
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedColorIndex = index },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Font Weight & Style
                            Text("Font Weight & Style:", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !isBold,
                                    onClick = { isBold = false },
                                    label = { Text("Regular (Matches Lat/Long)", fontSize = 11.sp) }
                                )

                                FilterChip(
                                    selected = isBold,
                                    onClick = { isBold = true },
                                    label = { Text("Bold", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.FormatBold, null, modifier = Modifier.size(16.dp)) }
                                )

                                FilterChip(
                                    selected = isItalic,
                                    onClick = { isItalic = !isItalic },
                                    label = { Text("Italic", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.FormatItalic, null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Font Family
                            Text("Font Typeface:", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = fontFamily == "sans-serif",
                                    onClick = { fontFamily = "sans-serif" },
                                    label = { Text("Sans-Serif (NoteCam)", fontSize = 11.sp) }
                                )

                                FilterChip(
                                    selected = fontFamily == "monospace",
                                    onClick = { fontFamily = "monospace" },
                                    label = { Text("Monospace", fontSize = 11.sp) }
                                )

                                FilterChip(
                                    selected = fontFamily == "serif",
                                    onClick = { fontFamily = "serif" },
                                    label = { Text("Serif", fontSize = 11.sp) }
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Alignment
                            Text("Alignment:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextAlignment.values().forEach { alignOption ->
                                    FilterChip(
                                        selected = alignment == alignOption,
                                        onClick = { alignment = alignOption },
                                        label = { Text(alignOption.name.lowercase().capitalize(Locale.ROOT)) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Extra Padding for Inpainting
                            Text(
                                "Inpainting Boundary Margin: +${expandPadding}px",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = expandPadding.toFloat(),
                                onValueChange = { expandPadding = it.toInt() },
                                valueRange = 0f..16f,
                                steps = 15
                            )
                        }
                    }
                }

                // Explicit Confirmation Footer
                Divider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("dialog_cancel_button")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val chosenColor = colorPresets[selectedColorIndex].second
                            val chosenBgColor = bgColorPresets[selectedBgColorIndex].second
                            val options = TextEditOptions(
                                fontSize = if (isCustomFontSize) customFontSize else null,
                                textColor = chosenColor,
                                isBold = isBold,
                                isItalic = isItalic,
                                alignment = alignment,
                                fontFamily = fontFamily,
                                letterSpacing = letterSpacing,
                                textScaleX = textScaleX,
                                expandPadding = expandPadding,
                                bgMatchMode = bgMatchMode,
                                customBgColor = chosenBgColor,
                                featherRadius = featherRadius,
                                addMatchingGrain = addMatchingGrain
                            )
                            onConfirm(textValue, options)
                        },
                        modifier = Modifier.testTag("dialog_confirm_button")
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm & Replace")
                    }
                }
            }
        }
    }
}

private fun replaceDateInText(current: String, newDate: String): String {
    val dateRegex = Regex("""\b\d{1,4}[-/.]\d{1,2}[-/.]\d{1,4}\b""")
    return if (dateRegex.containsMatchIn(current)) {
        dateRegex.replace(current, newDate)
    } else {
        if (current.contains("date", ignoreCase = true)) {
            "Date: $newDate"
        } else {
            newDate
        }
    }
}

private fun replaceTimeInText(current: String, newTime: String): String {
    val timeRegex = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?(?:\s*(?:AM|PM|am|pm|GMT|UTC))?\b""")
    return if (timeRegex.containsMatchIn(current)) {
        timeRegex.replace(current, newTime)
    } else {
        if (current.contains("time", ignoreCase = true)) {
            "Time: $newTime"
        } else {
            newTime
        }
    }
}

private fun toggleHemisphere(current: String, category: OcrCategory): String {
    return if (category == OcrCategory.LATITUDE) {
        when {
            current.contains("N") -> current.replace("N", "S")
            current.contains("S") -> current.replace("S", "N")
            else -> current
        }
    } else {
        when {
            current.contains("E") -> current.replace("E", "W")
            current.contains("W") -> current.replace("W", "E")
            else -> current
        }
    }
}

