package com.editor.photo.video.collagemaker.photoedit.feature.enhance

import androidx.lifecycle.ViewModel
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.EnhanceEngine
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorEnhance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EnhanceViewModel : ViewModel() {
    private val _selectedTool = MutableStateFlow(EditorEnhance.EXPOSURE)
    val selectedTool: StateFlow<EditorEnhance> = _selectedTool.asStateFlow()

    private val _toolValues = MutableStateFlow<Map<EditorEnhance, Float>>(emptyMap())
    val toolValues: StateFlow<Map<EditorEnhance, Float>> = _toolValues.asStateFlow()

    fun selectTool(tool: EditorEnhance) {
        _selectedTool.value = tool
        if (_toolValues.value[tool] == null) {
            val current = _toolValues.value.toMutableMap()
            current[tool] = 0f
            _toolValues.value = current
        }
    }

    fun updateValue(tool: EditorEnhance, value: Float) {
        val current = _toolValues.value.toMutableMap()
        current[tool] = value
        _toolValues.value = current
    }


    fun buildEnhanceValues(): EnhanceEngine.EnhanceValues {
        var values = EnhanceEngine.EnhanceValues()
        for ((tool, value) in _toolValues.value) {
            values = values.with(tool, value)
        }
        return values
    }
}