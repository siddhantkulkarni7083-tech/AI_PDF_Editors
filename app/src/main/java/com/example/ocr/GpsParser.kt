package com.example.ocr

import com.example.model.OcrCategory
import java.util.regex.Pattern

object GpsParser {

    private val LAT_PATTERN = Pattern.compile(
        """(?i)(?:lat(?:itude)?\s*[:=]?\s*|(?<=\b))([+-]?\d{1,2}(?:\.\d+)?)\s*(?:°|deg)?\s*([NS])?"""
    )
    private val LON_PATTERN = Pattern.compile(
        """(?i)(?:long(?:itude)?\s*[:=]?\s*|lng\s*[:=]?\s*|(?<=\b))([+-]?\d{1,3}(?:\.\d+)?)\s*(?:°|deg)?\s*([EW])?"""
    )
    private val ELEVATION_PATTERN = Pattern.compile(
        """(?i)(?:elev(?:ation)?|alt(?:itude)?|height)\s*[:=]?\s*([+-]?\d+(?:\.\d+)?)\s*(m|ft|meters|feet)?"""
    )
    private val ACCURACY_PATTERN = Pattern.compile(
        """(?i)(?:acc(?:uracy)?|precision)\s*[:=]?\s*(\d+(?:\.\d+)?)\s*(m|ft|meters|feet)?"""
    )
    private val DATE_PATTERN = Pattern.compile(
        """\b(?:(?:\d{1,2}[-/.])(?:\d{1,2}[-/.])(?:\d{2,4})|(?:\d{4}[-/.])(?:\d{1,2}[-/.])(?:\d{1,2})|(?:(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{4}))\b""",
        Pattern.CASE_INSENSITIVE
    )
    private val TIME_PATTERN = Pattern.compile(
        """\b\d{1,2}:\d{2}(?::\d{2})?(?:\s*(?:AM|PM|am|pm|GMT|UTC|[+-]\d{2,4}))?\b"""
    )
    private val NOTE_PATTERN = Pattern.compile(
        """(?i)^(?:note|memo|remark|site|project|job|name|title|location)\s*[:=]\s*(.+)"""
    )

    fun categorize(text: String): Pair<OcrCategory, String?> {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()

        // 1. Latitude check
        if (lower.contains("latitude") || lower.contains("lat:") || lower.contains("lat ") || lower.contains("lat.")) {
            return Pair(OcrCategory.LATITUDE, extractMatch(trimmed, LAT_PATTERN))
        }
        // 2. Longitude check
        if (lower.contains("longitude") || lower.contains("long:") || lower.contains("lng:") || lower.contains("long ") || lower.contains("lng ")) {
            return Pair(OcrCategory.LONGITUDE, extractMatch(trimmed, LON_PATTERN))
        }
        // 3. Elevation check
        if (lower.contains("elevation") || lower.contains("elev:") || lower.contains("alt:") || lower.contains("altitude") || lower.contains("elev ")) {
            return Pair(OcrCategory.ELEVATION, extractMatch(trimmed, ELEVATION_PATTERN))
        }
        // 4. Accuracy check
        if (lower.contains("accuracy") || lower.contains("acc:") || lower.contains("precision")) {
            return Pair(OcrCategory.ACCURACY, extractMatch(trimmed, ACCURACY_PATTERN))
        }
        // 5. Note / Label check
        if (NOTE_PATTERN.matcher(trimmed).find()) {
            return Pair(OcrCategory.NOTE, extractMatch(trimmed, NOTE_PATTERN))
        }
        // 6. Standalone coordinates like 18.5204° N or 73.8567° E
        if (trimmed.endsWith("N", ignoreCase = true) || trimmed.endsWith("S", ignoreCase = true)) {
            val m = LAT_PATTERN.matcher(trimmed)
            if (m.find()) return Pair(OcrCategory.LATITUDE, m.group(0))
        }
        if (trimmed.endsWith("E", ignoreCase = true) || trimmed.endsWith("W", ignoreCase = true)) {
            val m = LON_PATTERN.matcher(trimmed)
            if (m.find()) return Pair(OcrCategory.LONGITUDE, m.group(0))
        }
        // 7. Date check
        val dateMatcher = DATE_PATTERN.matcher(trimmed)
        if (dateMatcher.find()) {
            return Pair(OcrCategory.DATE, dateMatcher.group(0))
        }
        // 8. Time check
        val timeMatcher = TIME_PATTERN.matcher(trimmed)
        if (timeMatcher.find()) {
            return Pair(OcrCategory.TIME, timeMatcher.group(0))
        }

        return Pair(OcrCategory.OTHER, null)
    }

    private fun extractMatch(text: String, pattern: Pattern): String? {
        val m = pattern.matcher(text)
        return if (m.find()) m.group(0) else text
    }
}
