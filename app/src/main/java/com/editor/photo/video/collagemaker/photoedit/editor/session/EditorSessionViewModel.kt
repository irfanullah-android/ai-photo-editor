package com.editor.photo.video.collagemaker.photoedit.editor.session

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AIRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.CacheRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyAdjustmentUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyCropUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyDoodleUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyEffectUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyEnhanceUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyFilterUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyFrameUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyStickerUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ApplyTextUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.ExportImageUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.LoadImageUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.RedoUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.RemoveBackgroundUseCase
import com.editor.photo.video.collagemaker.photoedit.domain.usecase.UndoUseCase
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodlePath
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorEnhance
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorFilter
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorState
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorUiState
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectType
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorTool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorSessionViewModel @Inject constructor(
    private val editorRepository: EditorRepository,
    private val cacheRepository: CacheRepository,
    private val aiRepository: AIRepository,
    private val loadImageUseCase: LoadImageUseCase,
    private val undoUseCase: UndoUseCase,
    private val redoUseCase: RedoUseCase,
    private val applyCropUseCase: ApplyCropUseCase,
    private val applyAdjustmentUseCase: ApplyAdjustmentUseCase,
    private val applyEffectUseCase: ApplyEffectUseCase,
    private val applyFilterUseCase: ApplyFilterUseCase,
    private val applyEnhanceUseCase: ApplyEnhanceUseCase,
    private val applyFrameUseCase: ApplyFrameUseCase,
    private val removeBackgroundUseCase: RemoveBackgroundUseCase,
    private val exportImageUseCase: ExportImageUseCase,
    private val applyDoodleUseCase: ApplyDoodleUseCase,
    private val applyTextUseCase: ApplyTextUseCase,
    private val applyStickerUseCase: ApplyStickerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _activeTool = MutableStateFlow(EditorTool.NONE)
    val activeTool: StateFlow<EditorTool> = _activeTool.asStateFlow()

    // ── Active text layer (Phase 1 → Phase 2 bridge) ───────────────────────
    // Jab Phase 1 ("Enter text") "Done" hoti hai, ye us layer ka id set kar deta hai jo
    // PhotoStudioFragment ke REAL photo view par overlay ke taur par render hoga aur jise
    // TextEditorBottomSheet ke tools control karenge. Koi separate "draft" state nahi —
    // har change seedha [_editorState].textLayers mein commit hoti hai (real-time).
    private val _activeTextId = MutableStateFlow<String?>(null)
    val activeTextId: StateFlow<String?> = _activeTextId.asStateFlow()

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Loading
            loadImageUseCase(uri)
            _editorState.value = EditorState(baseImageUri = uri.toString())
            refreshPreview()
        }
    }

    fun applyAdjustment(adjustments: Map<AdjustmentType, Int>) {
        applyAdjustmentUseCase(adjustments)
        refreshPreview()
    }

    fun applyEffect(effectType: EffectType, intensity: Float) {
        applyEffectUseCase(effectType, intensity)
        refreshPreview()
    }

    fun applyFilter(filter: EditorFilter, intensity: Int) {
        applyFilterUseCase(filter, intensity)
        refreshPreview()
    }

    fun applyEnhance(enhanceType: EditorEnhance, intensity: Float) {
        applyEnhanceUseCase(enhanceType, intensity)
        refreshPreview()
    }

    fun applyFrame(frame: FrameLayer?) {
        applyFrameUseCase(frame)
        refreshPreview()
    }

    fun undo() {
        if (undoUseCase()) {
            refreshPreview()
        }
    }

    fun redo() {
        if (redoUseCase()) {
            refreshPreview()
        }
    }

    fun setActiveTool(tool: EditorTool) {
        _activeTool.value = tool
    }

    fun setRotation(angle: Float) {
        _editorState.value = _editorState.value.copy(rotation = angle % 360f)
    }

    fun rotate90() {
        _editorState.value = _editorState.value.copy(rotation = (_editorState.value.rotation + 90f) % 360f)
    }

    fun flipHorizontal() {
        _editorState.value = _editorState.value.copy(flipHorizontal = !_editorState.value.flipHorizontal)
    }

    fun flipVertical() {
        _editorState.value = _editorState.value.copy(flipVertical = !_editorState.value.flipVertical)
    }

    fun setCanvasZoom(zoom: Float) {
        _editorState.value = _editorState.value.copy(canvasZoom = zoom)
    }

    fun setAspectRatio(ratio: Float?) {
        _editorState.value = _editorState.value.copy(aspectRatio = ratio)
    }

    fun setImageTranslationX(x: Float) {
        _editorState.value = _editorState.value.copy(imageTranslationX = x)
    }

    fun saveCanvasState() {
        val state = _editorState.value
        if (state.rotation != 0f || state.flipHorizontal || state.flipVertical) {
            val op = EditOperation.Rotate(state.rotation, state.flipHorizontal, state.flipVertical)
            editorRepository.addOperation(op)
            _editorState.value = state.copy(rotation = 0f, flipHorizontal = false, flipVertical = false)
            refreshPreview()
        }
    }

    fun refreshPreview() {
        android.util.Log.d("TXT_DBG", "refreshPreview called", Throwable())
        viewModelScope.launch(Dispatchers.Default) {
            val activeId = _activeTextId.value
            val preview = editorRepository.renderPreview(excludeTextId = activeId)
            val original = editorRepository.getOriginalBitmap()
            val canUndo = editorRepository.canUndo()
            val canRedo = editorRepository.canRedo()
            withContext(Dispatchers.Main) {
                _uiState.value = EditorUiState.Editing(
                    previewBitmap = preview,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    originalBitmap = original
                )
            }
        }
    }

    fun exportImage(filename: String) {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Processing("Saving to gallery...")
            val outputUri = exportImageUseCase(filename)
            if (outputUri != null) {
                _uiState.value = EditorUiState.ExportSuccess(outputUri)
            } else {
                _uiState.value = EditorUiState.Error("Export failed")
            }
        }
    }

    fun removeBackground() {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Processing("Removing background...")
            val bitmap = editorRepository.renderPreview() ?: editorRepository.getOriginalBitmap()
            if (bitmap != null) {
                val result = removeBackgroundUseCase(bitmap)
                if (result != null) {
                    refreshPreview()
                } else {
                    _uiState.value = EditorUiState.Error("Background removal failed")
                }
            } else {
                _uiState.value = EditorUiState.Error("No image loaded")
            }
        }
    }

    fun applyCrop(croppedUri: Uri) {
        applyCropUseCase(croppedUri)
        refreshPreview()
    }

    /**
     * Commits ONE completed doodle stroke as a single history operation.
     * Callers (e.g. DoodleBottomSheet) must call this once per finished stroke
     * (on touch-up), never per touch-move, so undo/redo stays coherent.
     */
    fun addDoodle(path: DoodlePath) {
        applyDoodleUseCase(path)
        refreshPreview()
    }


    fun addText(text: TextLayer) {
        applyTextUseCase.add(text)
        _editorState.value = _editorState.value.copy(
            textLayers = _editorState.value.textLayers + text
        )
        refreshPreview()
    }


    fun updateText(text: TextLayer) {
        applyTextUseCase.update(text)
        _editorState.value = _editorState.value.copy(
            textLayers = _editorState.value.textLayers.map { if (it.id == text.id) text else it }
        )
        refreshPreview()
    }

    /**
     * Removes a text layer by id.
     */
    fun removeText(textId: String) {
        applyTextUseCase.remove(textId)
        _editorState.value = _editorState.value.copy(
            textLayers = _editorState.value.textLayers.filterNot { it.id == textId }
        )
        if (_activeTextId.value == textId) {
            _activeTextId.value = null
        }
        refreshPreview()
    }


    fun submitTextInput(existingTextId: String?, text: String) {
        val existing = existingTextId?.let { id ->
            _editorState.value.textLayers.firstOrNull { it.id == id }
        }
        if (existing != null) {
            val updated = existing.copy(text = text)
            applyTextUseCase.update(updated)
            _editorState.value = _editorState.value.copy(
                textLayers = _editorState.value.textLayers.map { if (it.id == updated.id) updated else it }
            )
            _activeTextId.value = updated.id
        } else {
            val newLayer = TextLayer(
                id = UUID.randomUUID().toString(),
                text = text,
                x = 0.5f,
                y = 0.5f,
                size = 0.08f,
                color = Color.WHITE,
                alpha = 255,
                rotation = 0f
            )
            applyTextUseCase.add(newLayer)
            _editorState.value = _editorState.value.copy(
                textLayers = _editorState.value.textLayers + newLayer
            )
            _activeTextId.value = newLayer.id
        }
        refreshPreview()
    }

    /** Overlay par tap-to-select ke liye (abhi optional — future use ke liye rakha hai). */
    fun setActiveTextId(id: String?) {
        val changed = _activeTextId.value != id
        _activeTextId.value = id
        if (changed) {
            refreshPreview()
        }
    }

    /**
     * Adds a new sticker (emoji) layer to the editor.
     * Coordinates in [sticker] must be in normalized space (0f..1f).
     */
    fun addSticker(sticker: StickerLayer) {
        applyStickerUseCase.add(sticker)
        refreshPreview()
    }

    /**
     * Updates an existing sticker layer (same id).
     */
    fun updateSticker(sticker: StickerLayer) {
        applyStickerUseCase.update(sticker)
        refreshPreview()
    }

    /**
     * Removes a sticker layer by id.
     */
    fun removeSticker(stickerId: String) {
        applyStickerUseCase.remove(stickerId)
        refreshPreview()
    }

    suspend fun getHighResBitmapWithoutFrame(): Bitmap? {
        return editorRepository.renderPreviewWithoutFrame()
    }

    fun syncPreviewBitmap(bitmap: Bitmap) {
        val current = _uiState.value
        if (current is EditorUiState.Editing) {
            _uiState.value = current.copy(previewBitmap = bitmap)
        }
    }

    override fun onCleared() {
        super.onCleared()
        editorRepository.clear()
        cacheRepository.clearCache()
        aiRepository.release()
    }
}