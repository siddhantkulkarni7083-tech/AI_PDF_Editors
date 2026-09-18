package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onImportImage: () -> Unit,
    onImportPdf: () -> Unit,
    onTakePhoto: () -> Unit,
    onLoadSample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))

        // Hero Icon with Gradient Glow
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "AI Image & PDF Text Editor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Precision Localized OCR Text Inpainting for Photos, Scans & PDF Documents",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(30.dp))

        // Primary Action: Import Image
        Button(
            onClick = onImportImage,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("import_image_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Import Photo (JPG, PNG, WEBP)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        // Dedicated Action: Upload PDF Document
        FilledTonalButton(
            onClick = onImportPdf,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("import_pdf_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Text("Upload PDF Document", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        // Secondary Action: Take Photo
        OutlinedButton(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("take_photo_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Take Camera Photo", fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))

        // Quick Sample Demo
        FilledTonalButton(
            onClick = onLoadSample,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("load_sample_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try Sample GPS Stamp Photo", fontSize = 13.sp)
        }

        Spacer(Modifier.height(30.dp))

        // Feature Highlights Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Key Capabilities",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))

                FeatureItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF & Multi-Page Support",
                    description = "Upload any PDF file, browse between pages, and inpaint or replace text on any page."
                )
                Spacer(Modifier.height(10.dp))
                FeatureItem(
                    icon = Icons.Default.GpsFixed,
                    title = "GPS & Timestamp Support",
                    description = "Edit Latitude, Longitude, Altitude, Accuracy, Date & Time fields independently."
                )
                Spacer(Modifier.height(10.dp))
                FeatureItem(
                    icon = Icons.Default.CropFree,
                    title = "Localized Patch Inpainting",
                    description = "Modifies ONLY the selected text bounding box. Rest of the photograph is 100% untouched."
                )
                Spacer(Modifier.height(10.dp))
                FeatureItem(
                    icon = Icons.Default.Undo,
                    title = "Undo / Redo & Split Compare",
                    description = "Full history stack, instant reset, and before/after split slider comparison."
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
