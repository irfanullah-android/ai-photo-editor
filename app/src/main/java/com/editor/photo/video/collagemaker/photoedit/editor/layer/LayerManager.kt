package com.editor.photo.video.collagemaker.photoedit.editor.layer

class LayerManager {
    private val layersMap = mutableMapOf<String, Layer>()
    private var selectedLayerId: String? = null

    fun getLayers(): List<Layer> = layersMap.values.sortedBy { it.zIndex }

    fun getLayerState(): LayerState = LayerState(getLayers(), selectedLayerId)

    fun addOrUpdateLayer(layer: Layer) {
        layersMap[layer.id] = layer
    }

    fun removeLayer(id: String) {
        layersMap.remove(id)
        if (selectedLayerId == id) {
            selectedLayerId = null
        }
    }

    fun selectLayer(id: String?) {
        selectedLayerId = id
    }

    fun clear() {
        layersMap.clear()
        selectedLayerId = null
    }
}
