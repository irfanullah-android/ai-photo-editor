package com.example.collage_maker.ui.screens
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.collage_maker.model.SlotImage

/**
 * Slot image jo directly pinch se zoom aur drag se pan hoti hai.
 * Koi button ya mode nahi — bas gesture karo.
 *
 * Scale: 1x (original) → 5x (max zoom)
 * Pan:   scale ke hisaab se auto-limit hota hai
 */
@Composable
fun ZoomableSlotImage(
    slotImage: SlotImage,
    onTransform: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state — instant feel, no recomposition lag
    var scale   by remember(slotImage.slotIndex) { mutableStateOf(slotImage.scale.coerceAtLeast(1f)) }
    var offsetX by remember(slotImage.slotIndex) { mutableStateOf(slotImage.offsetX) }
    var offsetY by remember(slotImage.slotIndex) { mutableStateOf(slotImage.offsetY) }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(slotImage.uri)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(slotImage.slotIndex) {
                detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->

                    // Scale: 1x se 5x tak
                    val newScale = (scale * zoom).coerceIn(1f, 5f)

                    // Jitna zoom, utna pan allowed
                    val maxPan     = 300f * (newScale - 1f)
                    val newOffsetX = (offsetX + pan.x).coerceIn(-maxPan, maxPan)
                    val newOffsetY = (offsetY + pan.y).coerceIn(-maxPan, maxPan)

                    scale   = newScale
                    offsetX = newOffsetX
                    offsetY = newOffsetY

                    // ViewModel ko update karo — export mein bhi sahi position aayegi
                    onTransform(scale, offsetX, offsetY)
                }
            }
            .graphicsLayer {
                scaleX       = scale
                scaleY       = scale
                translationX = offsetX
                translationY = offsetY
            }
    )
}