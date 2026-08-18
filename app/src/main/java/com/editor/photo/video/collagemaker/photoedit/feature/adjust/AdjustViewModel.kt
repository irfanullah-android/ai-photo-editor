package com.editor.photo.video.collagemaker.photoedit.feature.adjust

import androidx.lifecycle.ViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdjustViewModel : ViewModel() {

    private val _adjustments = MutableStateFlow<List<AdjustmentModel>>(emptyList())
    val adjustments: StateFlow<List<AdjustmentModel>> = _adjustments.asStateFlow()

    private val _selectedAdjustment = MutableStateFlow<AdjustmentModel?>(null)
    val selectedAdjustment: StateFlow<AdjustmentModel?> = _selectedAdjustment.asStateFlow()

    private val _adjustmentValues = MutableStateFlow<Map<AdjustmentType, Int>>(emptyMap())
    val adjustmentValues: StateFlow<Map<AdjustmentType, Int>> = _adjustmentValues.asStateFlow()

    fun initAdjustments(initialList: List<AdjustmentModel>) {
        _adjustments.value = initialList

        if (_selectedAdjustment.value == null) {
            _selectedAdjustment.value = initialList.firstOrNull { it.isSelected }
                ?: initialList.firstOrNull()
        }

        // Auto سمیت تمام ایڈجسٹمنٹس کی ابتدائی ویلیوز شامل کی گئی ہیں
        val initialValues = initialList.associate { it.type to it.value }
        _adjustmentValues.value = initialValues
    }

    fun selectAdjustment(adjustment: AdjustmentModel) {
        _selectedAdjustment.value = adjustment
    }

    fun updateValue(type: AdjustmentType, value: Int) {
        val currentValues = _adjustmentValues.value.toMutableMap()
        currentValues[type] = value
        _adjustmentValues.value = currentValues

        val currentList = _adjustments.value.map {
            if (it.type == type) it.copy(value = value) else it
        }
        _adjustments.value = currentList

        val selected = _selectedAdjustment.value
        if (selected != null && selected.type == type) {
            _selectedAdjustment.value = selected.copy(value = value)
        }
    }

    fun updateValues(values: Map<AdjustmentType, Int>) {
        val currentValues = _adjustmentValues.value.toMutableMap()
        values.forEach { (type, value) -> currentValues[type] = value }
        _adjustmentValues.value = currentValues

        val currentList = _adjustments.value.map { adj ->
            val newValue = values[adj.type]
            if (newValue != null) adj.copy(value = newValue) else adj
        }
        _adjustments.value = currentList

        val selected = _selectedAdjustment.value
        if (selected != null) {
            val newValue = values[selected.type]
            if (newValue != null) {
                _selectedAdjustment.value = selected.copy(value = newValue)
            }
        }
    }
}