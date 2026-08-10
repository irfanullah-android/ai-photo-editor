package com.editor.photo.video.collagemaker.photoedit.models.bottomsheets

import android.graphics.Bitmap
import android.net.Uri

/**
 * EditorUiState - Fragment کا single observable state۔
 *
 * Fragment کے پاس صرف یہی آتا ہے۔ کوئی boolean flag نہیں،
 * کوئی bitmap comparison نہیں — صرف render کرو۔
 *
 * Sealed hierarchy: ہر state clearly typed ہے۔
 */
sealed class EditorUiState {

    /** ابتدائی state — کوئی image load نہیں ہوئی */
    object Idle : EditorUiState()

    /** Image load یا processing جاری ہے */
    object Loading : EditorUiState()

    /**
     * Editing mode — Fragment کو یہ سب render کرنا ہے۔
     *
     * @param previewBitmap  ImageProcessor کا output — photoEditorView.source پر لگاؤ
     * @param canUndo        Undo button enable/disable
     * @param canRedo        Redo button enable/disable
     * @param isComparing    True = original دکھاؤ؛ controls hide کرو
     * @param originalBitmap Compare mode کے لیے original — صرف display، کوئی processing نہیں
     * @param exportedUri    Export success کے بعد set ہوتا ہے
     */
    data class Editing(
        val previewBitmap: Bitmap? = null,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val isComparing: Boolean = false,
        val originalBitmap: Bitmap? = null,
        val exportedUri: Uri? = null
    ) : EditorUiState()

    /**
     * Export یا background processing جاری ہے —
     * Fragment progress bar دکھائے، UI disable کرے
     */
    data class Processing(val message: String = "Processing...") : EditorUiState()

    /** کوئی error آ گئی — Fragment toast/snackbar دکھائے */
    data class Error(val message: String) : EditorUiState()

    /** Export complete — Fragment dialog دکھائے */
    data class ExportSuccess(val uri: Uri) : EditorUiState()
}