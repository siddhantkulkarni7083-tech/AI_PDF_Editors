package com.example.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.example.model.PdfPageItem
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class PdfRenderResult(
    val bitmap: Bitmap,
    val pageIndex: Int,
    val totalPages: Int
)

data class PdfExportResult(
    val file: File,
    val publicUri: Uri?,
    val pageCount: Int,
    val fileName: String
)

object PdfDocumentHandler {

    /**
     * Copies the PDF from content URI to a local cache file and returns the ParcelFileDescriptor.
     */
    private fun getFileDescriptorForUri(context: Context, uri: Uri): Pair<File, ParcelFileDescriptor> {
        val cacheFile = File(context.cacheDir, "temp_render_doc.pdf")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open PDF input stream" }
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }
        val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
        return Pair(cacheFile, pfd)
    }

    /**
     * Retrieves total page count of a PDF.
     */
    fun getPageCount(context: Context, uri: Uri): Int {
        val (_, fileDescriptor) = getFileDescriptorForUri(context, uri)
        val renderer = PdfRenderer(fileDescriptor)
        try {
            return renderer.pageCount
        } finally {
            renderer.close()
            fileDescriptor.close()
        }
    }

    /**
     * Renders a specific page of a PDF as a high-res Bitmap.
     */
    fun renderPage(
        context: Context,
        uri: Uri,
        pageIndex: Int = 0,
        scaleFactor: Float = 2.0f
    ): PdfRenderResult {
        val (_, fileDescriptor) = getFileDescriptorForUri(context, uri)
        val renderer = PdfRenderer(fileDescriptor)

        try {
            val totalPages = renderer.pageCount
            require(totalPages > 0) { "PDF has no pages" }

            val safePageIndex = pageIndex.coerceIn(0, totalPages - 1)
            val page = renderer.openPage(safePageIndex)

            try {
                val targetWidth = (page.width * scaleFactor).roundToInt().coerceIn(800, 3000)
                val targetHeight = (page.height * scaleFactor).roundToInt().coerceIn(800, 4000)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                val matrix = Matrix().apply {
                    setScale(
                        targetWidth.toFloat() / page.width.toFloat(),
                        targetHeight.toFloat() / page.height.toFloat()
                    )
                }

                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                return PdfRenderResult(
                    bitmap = bitmap,
                    pageIndex = safePageIndex,
                    totalPages = totalPages
                )
            } finally {
                page.close()
            }
        } finally {
            renderer.close()
            fileDescriptor.close()
        }
    }

    /**
     * Renders a fast lightweight thumbnail for a PDF page to show in the page selector & reordering UI.
     */
    fun renderThumbnail(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        thumbWidth: Int = 300
    ): Bitmap {
        val (_, fileDescriptor) = getFileDescriptorForUri(context, uri)
        val renderer = PdfRenderer(fileDescriptor)

        try {
            val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
            val page = renderer.openPage(safeIndex)
            try {
                val ratio = page.height.toFloat() / page.width.toFloat()
                val thumbHeight = (thumbWidth * ratio).roundToInt().coerceIn(100, 600)

                val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                val matrix = Matrix().apply {
                    setScale(
                        thumbWidth.toFloat() / page.width.toFloat(),
                        thumbHeight.toFloat() / page.height.toFloat()
                    )
                }
                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            } finally {
                page.close()
            }
        } finally {
            renderer.close()
            fileDescriptor.close()
        }
    }

    /**
     * Exports a new PDF containing all selected pages in their customized serial order,
     * using the modified edited bitmaps for edited pages and rendered pages for untouched ones.
     */
    fun exportPdfDocument(
        context: Context,
        originalPdfUri: Uri,
        pageItems: List<PdfPageItem>,
        editedPages: Map<Int, Bitmap>
    ): PdfExportResult {
        val selectedItems = pageItems.filter { it.isSelected }
        require(selectedItems.isNotEmpty()) { "No pages selected for PDF export" }

        val (_, fileDescriptor) = getFileDescriptorForUri(context, originalPdfUri)
        val renderer = PdfRenderer(fileDescriptor)

        val timestamp = System.currentTimeMillis()
        val fileName = "Edited_Document_${timestamp}.pdf"
        val cacheOutputFile = File(context.cacheDir, fileName)

        val pdfDocument = PdfDocument()

        try {
            for ((serialIndex, item) in selectedItems.withIndex()) {
                val pageIndex = item.pageIndex.coerceIn(0, renderer.pageCount - 1)
                
                val pageBitmap: Bitmap = if (editedPages.containsKey(pageIndex)) {
                    editedPages[pageIndex]!!
                } else {
                    val page = renderer.openPage(pageIndex)
                    try {
                        val scale = 2.0f
                        val w = (page.width * scale).roundToInt().coerceIn(800, 3000)
                        val h = (page.height * scale).roundToInt().coerceIn(800, 4000)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        val mat = Matrix().apply {
                            setScale(w.toFloat() / page.width.toFloat(), h.toFloat() / page.height.toFloat())
                        }
                        page.render(bmp, null, mat, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    } finally {
                        page.close()
                    }
                }

                // Add Page to PdfDocument (standard 72 DPI PDF point size matching bitmap)
                val pageInfo = PdfDocument.PageInfo.Builder(pageBitmap.width, pageBitmap.height, serialIndex + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas
                canvas.drawBitmap(pageBitmap, 0f, 0f, null)
                pdfDocument.finishPage(pdfPage)
            }

            // Write out to cache file
            FileOutputStream(cacheOutputFile).use { out ->
                pdfDocument.writeTo(out)
            }

        } finally {
            pdfDocument.close()
            renderer.close()
            fileDescriptor.close()
        }

        // Save to public storage (Documents / Downloads)
        val publicUri = savePdfToPublicStorage(context, cacheOutputFile, fileName)

        return PdfExportResult(
            file = cacheOutputFile,
            publicUri = publicUri,
            pageCount = selectedItems.size,
            fileName = fileName
        )
    }

    /**
     * Saves exported PDF to Android MediaStore Documents/Downloads for access in file managers.
     */
    private fun savePdfToPublicStorage(context: Context, pdfFile: File, fileName: String): Uri? {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AI Image Text Editor")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val uri = resolver.insert(collection, values)
            if (uri != null) {
                resolver.openOutputStream(uri).use { output ->
                    if (output != null) {
                        pdfFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            }
            uri
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a Uri corresponds to a PDF file based on MIME type or extension.
     */
    fun isPdfUri(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        if (type != null && type.contains("pdf", ignoreCase = true)) {
            return true
        }
        val path = uri.path ?: uri.toString()
        return path.endsWith(".pdf", ignoreCase = true)
    }
}

