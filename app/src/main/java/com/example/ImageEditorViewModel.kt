package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.image.ImageRepository
import com.example.image.TextReplacementEngine
import com.example.model.OcrCategory
import com.example.model.OcrLine
import com.example.model.TextEditOptions
import com.example.ocr.OcrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImageEditorUiState(
    val currentBitmap: Bitmap? = null,
    val originalBitmap: Bitmap? = null,
    val ocrLines: List<OcrLine> = emptyList(),
    val selectedLine: OcrLine? = null,
    val isBusy: Boolean = false,
    val busyMessage: String = "",
    val statusToast: String? = null,
    val showGpsPanel: Boolean = false,
    val showBeforeAfter: Boolean = false,
    val showPdfPageManager: Boolean = false,
    val undoStack: List<Bitmap> = emptyList(),
    val redoStack: List<Bitmap> = emptyList(),
    val isEditingLine: Boolean = false,
    val currentEditLine: OcrLine? = null,
    val isPdf: Boolean = false,
    val pdfUri: Uri? = null,
    val currentPdfPage: Int = 0,
    val totalPdfPages: Int = 1,
    val pdfPages: List<com.example.model.PdfPageItem> = emptyList(),
    val editedPdfPages: Map<Int, Bitmap> = emptyMap(),
    val exportedPdfResult: com.example.image.PdfExportResult? = null
)

class ImageEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageEditorUiState())
    val uiState: StateFlow<ImageEditorUiState> = _uiState.asStateFlow()

    private val ocrManager = OcrManager()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val isPdf = withContext(Dispatchers.IO) {
                com.example.image.PdfDocumentHandler.isPdfUri(context, uri)
            }

            _uiState.update {
                it.copy(
                    isBusy = true,
                    busyMessage = if (isPdf) "Rendering PDF document..." else "Loading image..."
                )
            }

            try {
                if (isPdf) {
                    val pdfResult = withContext(Dispatchers.IO) {
                        com.example.image.PdfDocumentHandler.renderPage(context, uri, pageIndex = 0)
                    }
                    val totalPages = pdfResult.totalPages
                    val initialPagesList = (0 until totalPages).map { pageIdx ->
                        com.example.model.PdfPageItem(
                            pageIndex = pageIdx,
                            serialNumber = pageIdx + 1,
                            isSelected = true,
                            isEdited = false
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isPdf = true,
                            pdfUri = uri,
                            currentPdfPage = 0,
                            totalPdfPages = totalPages,
                            pdfPages = initialPagesList,
                            editedPdfPages = emptyMap(),
                            exportedPdfResult = null
                        )
                    }
                    setLoadedBitmap(pdfResult.bitmap)

                    // Load thumbnails in background
                    loadPdfThumbnails(uri, totalPages)
                } else {
                    val bitmap = withContext(Dispatchers.IO) {
                        ImageRepository.loadBitmap(context.contentResolver, uri)
                    }
                    _uiState.update {
                        it.copy(
                            isPdf = false,
                            pdfUri = null,
                            currentPdfPage = 0,
                            totalPdfPages = 1,
                            pdfPages = emptyList(),
                            editedPdfPages = emptyMap(),
                            exportedPdfResult = null
                        )
                    }
                    setLoadedBitmap(bitmap)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusToast = "Failed to load document: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    private fun loadPdfThumbnails(uri: Uri, totalPages: Int) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val updatedPages = _uiState.value.pdfPages.toMutableList()
            for (i in 0 until totalPages) {
                try {
                    val thumb = withContext(Dispatchers.IO) {
                        com.example.image.PdfDocumentHandler.renderThumbnail(context, uri, i, 260)
                    }
                    val idx = updatedPages.indexOfFirst { it.pageIndex == i }
                    if (idx != -1) {
                        updatedPages[idx] = updatedPages[idx].copy(thumbnailBitmap = thumb)
                        _uiState.update { it.copy(pdfPages = updatedPages.toList()) }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun goToPdfPage(pageIndex: Int) {
        val uri = _uiState.value.pdfUri ?: return
        val total = _uiState.value.totalPdfPages
        val targetIndex = pageIndex.coerceIn(0, total - 1)
        if (targetIndex == _uiState.value.currentPdfPage && _uiState.value.currentBitmap != null) return

        // First save current edited page in map if it was edited
        val currentEditedMap = _uiState.value.editedPdfPages

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    busyMessage = "Loading PDF page ${targetIndex + 1} of $total..."
                )
            }
            try {
                val context = getApplication<Application>()
                val pageBitmap = if (currentEditedMap.containsKey(targetIndex)) {
                    currentEditedMap[targetIndex]!!
                } else {
                    val result = withContext(Dispatchers.IO) {
                        com.example.image.PdfDocumentHandler.renderPage(context, uri, pageIndex = targetIndex)
                    }
                    result.bitmap
                }

                _uiState.update {
                    it.copy(
                        currentPdfPage = targetIndex
                    )
                }
                setLoadedBitmap(pageBitmap)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusToast = "Failed to render page: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun prevPdfPage() {
        goToPdfPage(_uiState.value.currentPdfPage - 1)
    }

    fun nextPdfPage() {
        goToPdfPage(_uiState.value.currentPdfPage + 1)
    }

    fun openPdfManager() {
        _uiState.update { it.copy(showPdfPageManager = true) }
    }

    fun closePdfManager() {
        _uiState.update { it.copy(showPdfPageManager = false) }
    }

    fun togglePdfPageSelection(pageIndex: Int) {
        val currentList = _uiState.value.pdfPages.map {
            if (it.pageIndex == pageIndex) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.update { it.copy(pdfPages = currentList) }
    }

    fun setAllPdfPagesSelection(selected: Boolean) {
        val currentList = _uiState.value.pdfPages.map { it.copy(isSelected = selected) }
        _uiState.update { it.copy(pdfPages = currentList) }
    }

    fun movePdfPageUp(listIndex: Int) {
        if (listIndex <= 0) return
        val currentList = _uiState.value.pdfPages.toMutableList()
        val item = currentList.removeAt(listIndex)
        currentList.add(listIndex - 1, item)
        // Re-assign serial numbers
        val reordered = currentList.mapIndexed { idx, p -> p.copy(serialNumber = idx + 1) }
        _uiState.update { it.copy(pdfPages = reordered) }
    }

    fun movePdfPageDown(listIndex: Int) {
        val currentList = _uiState.value.pdfPages.toMutableList()
        if (listIndex >= currentList.size - 1) return
        val item = currentList.removeAt(listIndex)
        currentList.add(listIndex + 1, item)
        // Re-assign serial numbers
        val reordered = currentList.mapIndexed { idx, p -> p.copy(serialNumber = idx + 1) }
        _uiState.update { it.copy(pdfPages = reordered) }
    }

    fun exportEditedPdf() {
        val uri = _uiState.value.pdfUri ?: return
        val pages = _uiState.value.pdfPages
        val editedMap = _uiState.value.editedPdfPages

        val selectedPages = pages.filter { it.isSelected }
        if (selectedPages.isEmpty()) {
            _uiState.update { it.copy(statusToast = "Please select at least one page to export") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    busyMessage = "Compiling and exporting ${selectedPages.size} PDF pages..."
                )
            }
            try {
                val context = getApplication<Application>()
                val exportResult = withContext(Dispatchers.IO) {
                    com.example.image.PdfDocumentHandler.exportPdfDocument(
                        context = context,
                        originalPdfUri = uri,
                        pageItems = pages,
                        editedPages = editedMap
                    )
                }

                _uiState.update {
                    it.copy(
                        isBusy = false,
                        exportedPdfResult = exportResult,
                        statusToast = "Exported ${exportResult.pageCount} pages to Documents/AI Image Text Editor!"
                    )
                }
                _eventFlow.emit("PDF exported successfully (${exportResult.pageCount} pages)!")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusToast = "PDF Export failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportedPdfResult = null) }
    }

    fun loadSampleImage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Generating GPS sample photo...") }
            try {
                val sampleBitmap = withContext(Dispatchers.Default) {
                    ImageRepository.createSampleGpsImage()
                }
                setLoadedBitmap(sampleBitmap)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusToast = "Failed to generate sample: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun setLoadedBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentBitmap = bitmap,
                    originalBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false),
                    undoStack = emptyList(),
                    redoStack = emptyList(),
                    selectedLine = null,
                    isEditingLine = false,
                    currentEditLine = null,
                    isBusy = true,
                    busyMessage = "Running OCR Text Detection..."
                )
            }

            // Run OCR text detection
            runOcrOnBitmap(bitmap)
        }
    }

    fun rescanOcr() {
        val current = _uiState.value.currentBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Rescanning text lines...") }
            runOcrOnBitmap(current)
        }
    }

    private suspend fun runOcrOnBitmap(bitmap: Bitmap) {
        try {
            val lines = withContext(Dispatchers.Default) {
                ocrManager.recognize(bitmap)
            }
            _uiState.update {
                it.copy(
                    ocrLines = lines,
                    isBusy = false,
                    statusToast = "Detected ${lines.size} text lines"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isBusy = false,
                    statusToast = "OCR Detection note: ${e.localizedMessage ?: "No text recognized"}"
                )
            }
        }
    }

    fun selectLineForEdit(line: OcrLine) {
        _uiState.update {
            it.copy(
                selectedLine = line,
                currentEditLine = line,
                isEditingLine = true
            )
        }
    }

    fun dismissEditDialog() {
        _uiState.update {
            it.copy(
                isEditingLine = false,
                currentEditLine = null
            )
        }
    }

    /**
     * Executes localized replacement strictly within the bounding box after explicit user confirmation.
     */
    fun confirmTextReplacement(
        targetLine: OcrLine,
        newText: String,
        options: TextEditOptions
    ) {
        val currentBmp = _uiState.value.currentBitmap ?: return
        _uiState.update {
            it.copy(
                isEditingLine = false,
                currentEditLine = null,
                isBusy = true,
                busyMessage = "Reconstructing background & replacing text..."
            )
        }

        viewModelScope.launch {
            try {
                val (updatedBitmap, updatedLines) = withContext(Dispatchers.Default) {
                    val newBmp = TextReplacementEngine.replaceText(
                        original = currentBmp,
                        box = targetLine.box,
                        oldText = targetLine.text,
                        newText = newText,
                        options = options,
                        surroundingLines = _uiState.value.ocrLines
                    )

                    // Update the text in our local OCR lines list
                    val newLinesList = _uiState.value.ocrLines.map { line ->
                        if (line.id == targetLine.id) {
                            line.copy(text = newText)
                        } else {
                            line
                        }
                    }

                    Pair(newBmp, newLinesList)
                }

                _uiState.update { state ->
                    val newUndo = state.undoStack + listOf(currentBmp)
                    val newEditedMap = if (state.isPdf) {
                        state.editedPdfPages + (state.currentPdfPage to updatedBitmap)
                    } else {
                        state.editedPdfPages
                    }
                    val newPdfPages = if (state.isPdf) {
                        state.pdfPages.map { p ->
                            if (p.pageIndex == state.currentPdfPage) p.copy(isEdited = true, thumbnailBitmap = updatedBitmap) else p
                        }
                    } else {
                        state.pdfPages
                    }

                    state.copy(
                        currentBitmap = updatedBitmap,
                        ocrLines = updatedLines,
                        undoStack = newUndo,
                        redoStack = emptyList(), // clear redo on new action
                        selectedLine = null,
                        isBusy = false,
                        editedPdfPages = newEditedMap,
                        pdfPages = newPdfPages,
                        statusToast = "Updated text successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusToast = "Error during replacement: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun undo() {
        val state = _uiState.value
        if (state.undoStack.isEmpty() || state.currentBitmap == null) return

        val previous = state.undoStack.last()
        val newUndo = state.undoStack.dropLast(1)
        val newRedo = listOf(state.currentBitmap) + state.redoStack

        _uiState.update {
            it.copy(
                currentBitmap = previous,
                undoStack = newUndo,
                redoStack = newRedo,
                statusToast = "Undo applied",
                selectedLine = null
            )
        }
        rescanOcr()
    }

    fun redo() {
        val state = _uiState.value
        if (state.redoStack.isEmpty() || state.currentBitmap == null) return

        val next = state.redoStack.first()
        val newRedo = state.redoStack.drop(1)
        val newUndo = state.undoStack + listOf(state.currentBitmap)

        _uiState.update {
            it.copy(
                currentBitmap = next,
                undoStack = newUndo,
                redoStack = newRedo,
                statusToast = "Redo applied",
                selectedLine = null
            )
        }
        rescanOcr()
    }

    fun resetToOriginal() {
        val state = _uiState.value
        val original = state.originalBitmap ?: return
        if (state.currentBitmap == null) return

        val newUndo = state.undoStack + listOf(state.currentBitmap)
        _uiState.update {
            it.copy(
                currentBitmap = original.copy(original.config ?: Bitmap.Config.ARGB_8888, true),
                undoStack = newUndo,
                redoStack = emptyList(),
                statusToast = "Reset to original image",
                selectedLine = null
            )
        }
        rescanOcr()
    }

    fun saveImageToGallery() {
        val currentBmp = _uiState.value.currentBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Saving image to Gallery...") }
            try {
                val context = getApplication<Application>()
                val savedUri = withContext(Dispatchers.IO) {
                    ImageRepository.saveToGallery(context.contentResolver, currentBmp)
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusToast = "Saved to Gallery (Pictures/AI Image Text Editor)!"
                    )
                }
                _eventFlow.emit("Image saved to Gallery successfully!")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusToast = "Save failed: ${e.localizedMessage ?: "Storage error"}"
                    )
                }
            }
        }
    }

    fun openGpsPanel() {
        _uiState.update { it.copy(showGpsPanel = true) }
    }

    fun closeGpsPanel() {
        _uiState.update { it.copy(showGpsPanel = false) }
    }

    fun openBeforeAfter() {
        _uiState.update { it.copy(showBeforeAfter = true) }
    }

    fun closeBeforeAfter() {
        _uiState.update { it.copy(showBeforeAfter = false) }
    }

    fun clearImage() {
        _uiState.update { ImageEditorUiState() }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedLine = null) }
    }

    fun clearToast() {
        _uiState.update { it.copy(statusToast = null) }
    }

    override fun onCleared() {
        super.onCleared()
        ocrManager.close()
    }
}
