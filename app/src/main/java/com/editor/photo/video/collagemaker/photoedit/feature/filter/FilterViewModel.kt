package com.editor.photo.video.collagemaker.photoedit.feature.filter

import androidx.lifecycle.ViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FilterViewModel : ViewModel() {
    private val _selectedFilter = MutableStateFlow(EditorFilter.NORMAL)
    val selectedFilter: StateFlow<EditorFilter> = _selectedFilter.asStateFlow()

    private val _intensity = MutableStateFlow(80)
    val intensity: StateFlow<Int> = _intensity.asStateFlow()

    fun selectFilter(filter: EditorFilter) {
        _selectedFilter.value = filter
    }

    fun setIntensity(intensity: Int) {
        _intensity.value = intensity
    }
}
