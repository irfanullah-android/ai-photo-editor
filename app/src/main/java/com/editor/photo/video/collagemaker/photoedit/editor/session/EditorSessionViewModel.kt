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
import kotlinx.coroutines.Job
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

    private val _activeTextId = MutableStateFlow<String?>(null)
    val activeTextId: StateFlow<String?> = _activeTextId.asStateFlow()

    private val _activeStickerId = MutableStateFlow<String?>(null)
    val activeStickerId: StateFlow<String?> = _activeStickerId.asStateFlow()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null   // <-- keep this too, unchanged

    fun loadImage(uri: Uri) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { _uiState.value = EditorUiState.Loading }
            loadImageUseCase(uri)
            withContext(Dispatchers.Main) {
                _editorState.value = EditorState(baseImageUri = uri.toString())
            }
            refreshPreviewInternal()
        }
    }

    fun refreshPreview() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.Default) {
            refreshPreviewInternal()
        }
    }

    fun applyAdjustment(adjustments: Map<AdjustmentType, Int>) {
        viewModelScope.launch(Dispatchers.Default) {
            applyAdjustmentUseCase(adjustments)
            refreshPreviewInternal()
        }
    }

    fun applyEffect(effectType: EffectType, intensity: Float) {
        viewModelScope.launch(Dispatchers.Default) {
            applyEffectUseCase(effectType, intensity)
            refreshPreviewInternal()
        }
    }

    fun applyFilter(filter: EditorFilter, intensity: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            applyFilterUseCase(filter, intensity)
            refreshPreviewInternal()
        }
    }

    fun applyFrame(frame: FrameLayer?) {
        viewModelScope.launch(Dispatchers.Default) {
            applyFrameUseCase(frame)
            refreshPreviewInternal()
        }
    }

    fun undo() {
        viewModelScope.launch(Dispatchers.Default) {
            if (undoUseCase()) {
                refreshPreviewInternal()
            }
        }
    }

    fun redo() {
        viewModelScope.launch(Dispatchers.Default) {
            if (redoUseCase()) {
                refreshPreviewInternal()
            }
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
        viewModelScope.launch(Dispatchers.Default) {
            if (state.rotation != 0f || state.flipHorizontal || state.flipVertical) {
                val op = EditOperation.Rotate(state.rotation, state.flipHorizontal, state.flipVertical)
                editorRepository.addOperation(op)
                withContext(Dispatchers.Main) {
                    _editorState.value = _editorState.value.copy(
                        rotation = 0f,
                        flipHorizontal = false,
                        flipVertical = false
                    )
                }
            }
            if (state.canvasZoom != 1f) {
                val zoomOp = EditOperation.Canvas(state.aspectRatio, state.canvasZoom, state.imageTranslationX)
                editorRepository.addOperation(zoomOp)
                withContext(Dispatchers.Main) {
                    _editorState.value = _editorState.value.copy(canvasZoom = 1f)
                }
            }
            refreshPreviewInternal()
        }
    }

    private suspend fun computeEditingState(): EditorUiState.Editing {
        val activeTextIdValue = _activeTextId.value
        val activeStickerIdValue = _activeStickerId.value
        val preview = editorRepository.renderPreview(
            excludeTextId = activeTextIdValue,
            excludeStickerId = activeStickerIdValue
        )
        val original = editorRepository.getOriginalBitmap()
        val canUndo = editorRepository.canUndo()
        val canRedo = editorRepository.canRedo()
        return EditorUiState.Editing(
            previewBitmap = preview,
            canUndo = canUndo,
            canRedo = canRedo,
            originalBitmap = original
        )
    }

    /**
     * Non-blocking internal refresh that cancels prior stale render jobs.
     */
    private suspend fun refreshPreviewInternal() {
        val newState = computeEditingState()
        withContext(Dispatchers.Main) {
            _uiState.value = newState
        }
    }



    fun exportImage(filename: String) {
        viewModelScope.launch {
            _uiState.value = EditorUiState.Processing("Saving to gallery...")
            val outputUri = withContext(Dispatchers.IO) {
                exportImageUseCase(filename)
            }
            if (outputUri != null) {
                _uiState.value = EditorUiState.ExportSuccess(outputUri)
            } else {
                _uiState.value = EditorUiState.Error("Export failed")
            }
        }
    }

    fun removeBackground() {
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _uiState.value = EditorUiState.Processing("Removing background...")
            }
            val bitmap = editorRepository.renderPreview() ?: editorRepository.getOriginalBitmap()
            if (bitmap != null) {
                val result = removeBackgroundUseCase(bitmap)
                if (result != null) {
                    refreshPreviewInternal()
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.value = EditorUiState.Error("Background removal failed")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = EditorUiState.Error("No image loaded")
                }
            }
        }
    }

    fun applyCrop(croppedUri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            applyCropUseCase(croppedUri)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(baseImageUri = croppedUri.toString())
            }
            refreshPreviewInternal()
        }
    }

    fun addDoodle(path: DoodlePath) {
        viewModelScope.launch(Dispatchers.Default) {
            applyDoodleUseCase(path)
            refreshPreviewInternal()
        }
    }

    fun addText(text: TextLayer) {
        viewModelScope.launch(Dispatchers.Default) {
            applyTextUseCase.add(text)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    textLayers = _editorState.value.textLayers + text
                )
            }
            refreshPreviewInternal()
        }
    }

    fun updateText(text: TextLayer) {
        viewModelScope.launch(Dispatchers.Default) {
            applyTextUseCase.update(text)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    textLayers = _editorState.value.textLayers.map { if (it.id == text.id) text else it }
                )
            }
            refreshPreviewInternal()
        }
    }

    fun removeText(textId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            applyTextUseCase.remove(textId)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    textLayers = _editorState.value.textLayers.filterNot { it.id == textId }
                )
                if (_activeTextId.value == textId) {
                    _activeTextId.value = null
                }
            }
            refreshPreviewInternal()
        }
    }

    fun submitTextInput(existingTextId: String?, text: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val existing = existingTextId?.let { id ->
                _editorState.value.textLayers.firstOrNull { it.id == id }
            }
            if (existing != null) {
                val updated = existing.copy(text = text)
                applyTextUseCase.update(updated)
                withContext(Dispatchers.Main) {
                    _editorState.value = _editorState.value.copy(
                        textLayers = _editorState.value.textLayers.map { if (it.id == updated.id) updated else it }
                    )
                    _activeTextId.value = updated.id
                }
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
                withContext(Dispatchers.Main) {
                    _editorState.value = _editorState.value.copy(
                        textLayers = _editorState.value.textLayers + newLayer
                    )
                    _activeTextId.value = newLayer.id
                }
            }
            refreshPreviewInternal()
        }
    }

    fun setActiveTextId(id: String?) {
        val changed = _activeTextId.value != id
        _activeTextId.value = id
        if (changed) {
            refreshPreview()
        }
    }

    fun addSticker(sticker: StickerLayer) {
        viewModelScope.launch(Dispatchers.Default) {
            applyStickerUseCase.add(sticker)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    stickerLayers = _editorState.value.stickerLayers + sticker
                )
                _activeStickerId.value = sticker.id
            }
            refreshPreviewInternal()
        }
    }

    fun updateSticker(sticker: StickerLayer) {
        viewModelScope.launch(Dispatchers.Default) {
            applyStickerUseCase.update(sticker)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    stickerLayers = _editorState.value.stickerLayers.map {
                        if (it.id == sticker.id) sticker else it
                    }
                )
            }
            refreshPreviewInternal()
        }
    }

    fun removeSticker(stickerId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            applyStickerUseCase.remove(stickerId)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    stickerLayers = _editorState.value.stickerLayers.filterNot { it.id == stickerId }
                )
                if (_activeStickerId.value == stickerId) {
                    _activeStickerId.value = null
                }
            }
            refreshPreviewInternal()
        }
    }

    fun setActiveStickerId(id: String?) {
        val changed = _activeStickerId.value != id
        _activeStickerId.value = id
        if (changed) {
            refreshPreview()
        }
    }

    suspend fun getHighResBitmapWithoutFrame(): Bitmap? = withContext(Dispatchers.Default) {
        editorRepository.renderPreviewWithoutFrame()
    }

    fun syncPreviewBitmap(bitmap: Bitmap) {
        val current = _uiState.value
        if (current is EditorUiState.Editing) {
            _uiState.value = current.copy(previewBitmap = bitmap)
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        editorRepository.clear()
        cacheRepository.clearCache()
        aiRepository.release()
    }
}