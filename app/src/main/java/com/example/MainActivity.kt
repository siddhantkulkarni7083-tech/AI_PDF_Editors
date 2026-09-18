package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.ui.BeforeAfterComparisonDialog
import com.example.ui.EditTextConfirmationDialog
import com.example.ui.EditorScreen
import com.example.ui.GpsMetadataPanel
import com.example.ui.HomeScreen
import com.example.ui.theme.AIImageTextEditorTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ImageEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AIImageTextEditorTheme {
                MainAppScreen(
                    viewModel = viewModel,
                    onShowToast = { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: ImageEditorViewModel,
    onShowToast: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Gallery picker for JPG, JPEG, PNG, WEBP
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadFromUri(it) }
    }

    // PDF Document Picker
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadFromUri(it) }
    }

    // Camera launcher for taking photos
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            viewModel.loadSampleImage()
        }
    }

    // Listen for one-off events
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            onShowToast(event)
        }
    }

    // Show status toasts
    LaunchedEffect(uiState.statusToast) {
        uiState.statusToast?.let { msg ->
            onShowToast(msg)
            viewModel.clearToast()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.currentBitmap == null) {
            HomeScreen(
                onImportImage = { galleryLauncher.launch("image/*") },
                onImportPdf = { pdfLauncher.launch("application/pdf") },
                onTakePhoto = { cameraLauncher.launch(null) },
                onLoadSample = { viewModel.loadSampleImage() }
            )
        } else {
            EditorScreen(
                bitmap = uiState.currentBitmap!!,
                originalBitmap = uiState.originalBitmap,
                lines = uiState.ocrLines,
                selectedLine = uiState.selectedLine,
                busy = uiState.isBusy,
                busyMessage = uiState.busyMessage,
                statusMessage = uiState.statusToast,
                undoCount = uiState.undoStack.size,
                redoCount = uiState.redoStack.size,
                isPdf = uiState.isPdf,
                currentPdfPage = uiState.currentPdfPage,
                totalPdfPages = uiState.totalPdfPages,
                onPrevPdfPage = { viewModel.prevPdfPage() },
                onNextPdfPage = { viewModel.nextPdfPage() },
                onOpenPdfManager = { viewModel.openPdfManager() },
                onBack = { viewModel.clearImage() },
                onSelectLine = { line -> viewModel.selectLineForEdit(line) },
                onSave = {
                    if (uiState.isPdf) {
                        viewModel.openPdfManager()
                    } else {
                        viewModel.saveImageToGallery()
                    }
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onRescanOcr = { viewModel.rescanOcr() },
                onOpenGpsPanel = { viewModel.openGpsPanel() },
                onOpenBeforeAfter = { viewModel.openBeforeAfter() },
                onResetToOriginal = { viewModel.resetToOriginal() },
                onClearSelection = { viewModel.clearSelection() }
            )
        }

        // Dialog 1: Explicit Text Confirmation & Editing Dialog (Requirement 4, 10, 19, 20)
        if (uiState.isEditingLine && uiState.currentEditLine != null) {
            EditTextConfirmationDialog(
                line = uiState.currentEditLine!!,
                initialText = uiState.currentEditLine!!.text,
                bitmap = uiState.currentBitmap,
                surroundingLines = uiState.ocrLines,
                onCancel = { viewModel.dismissEditDialog() },
                onConfirm = { newText, options ->
                    viewModel.confirmTextReplacement(uiState.currentEditLine!!, newText, options)
                }
            )
        }

        // Dialog 2: GPS / Metadata Inspector Panel (Requirement 5, 17, 18)
        if (uiState.showGpsPanel) {
            GpsMetadataPanel(
                lines = uiState.ocrLines,
                onClose = { viewModel.closeGpsPanel() },
                onSelectLine = { line -> viewModel.selectLineForEdit(line) }
            )
        }

        // Dialog 3: Before / After Comparison Slider Dialog (Requirement 13)
        if (uiState.showBeforeAfter) {
            BeforeAfterComparisonDialog(
                original = uiState.originalBitmap,
                edited = uiState.currentBitmap,
                onClose = { viewModel.closeBeforeAfter() }
            )
        }

        // Dialog 4: PDF Page Selector, Serial Reordering & Export Dialog
        if (uiState.showPdfPageManager) {
            com.example.ui.PdfPageManagerDialog(
                pages = uiState.pdfPages,
                currentPageIndex = uiState.currentPdfPage,
                exportResult = uiState.exportedPdfResult,
                onTogglePageSelect = { pageIndex -> viewModel.togglePdfPageSelection(pageIndex) },
                onSelectAll = { selectAll -> viewModel.setAllPdfPagesSelection(selectAll) },
                onMovePageUp = { listIndex -> viewModel.movePdfPageUp(listIndex) },
                onMovePageDown = { listIndex -> viewModel.movePdfPageDown(listIndex) },
                onSelectPageForEdit = { pageIndex -> viewModel.goToPdfPage(pageIndex) },
                onExportPdf = { viewModel.exportEditedPdf() },
                onClearExportResult = { viewModel.clearExportResult() },
                onDismiss = { viewModel.closePdfManager() }
            )
        }
    }
}
