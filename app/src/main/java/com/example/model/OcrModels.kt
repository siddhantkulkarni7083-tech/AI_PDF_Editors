package com.example.model

import android.graphics.Color
import android.graphics.Rect

enum class OcrCategory(val displayName: String, val iconName: String) {
    LATITUDE("Latitude", "LocationOn"),
    LONGITUDE("Longitude", "Explore"),
    ELEVATION("Elevation", "Landscape"),
    ACCURACY("Accuracy", "GpsFixed"),
    DATE("Date", "CalendarToday"),
    TIME("Time", "AccessTime"),
    NOTE("Note", "Description"),
    OTHER("Text", "TextFields")
}

data class OcrLine(
    val id: Long,
    val text: String,
    val box: Rect,
    val confidence: Float? = null,
    val category: OcrCategory = OcrCategory.OTHER,
    val extractedValue: String? = null
)

enum class BackgroundMatchMode(val displayName: String) {
    AUTO_SMART("Auto Smart Match"),
    BANNER_MATCH("NoteCam Banner Match"),
    TEXTURE_INPAINT("Photo Texture Inpaint"),
    CLEAN_FILL("Clean Background Fill")
}

data class TextEditOptions(
    val fontSize: Float? = null,
    val textColor: Int? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val alignment: TextAlignment = TextAlignment.LEFT,
    val fontFamily: String = "sans-serif",
    val letterSpacing: Float = 0f,
    val textScaleX: Float = 1.0f,
    val expandPadding: Int = 0,
    val softnessBlur: Float = 0f,
    val opacity: Float = 1.0f,
    val bgMatchMode: BackgroundMatchMode = BackgroundMatchMode.AUTO_SMART,
    val customBgColor: Int? = null,
    val featherRadius: Int = 3,
    val addMatchingGrain: Boolean = true
)

enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

data class EditActionRecord(
    val lineId: Long,
    val box: Rect,
    val oldText: String,
    val newText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PdfPageItem(
    val pageIndex: Int,
    val serialNumber: Int,
    val isSelected: Boolean = true,
    val isEdited: Boolean = false,
    val thumbnailBitmap: android.graphics.Bitmap? = null
)

