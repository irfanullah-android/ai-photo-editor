package com.editor.photo.video.collagemaker.photoedit.feature.sticker

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.editor.photo.video.collagemaker.photoedit.R
import kotlin.math.atan2
import kotlin.math.hypot

class StickerOverlayView(context: Context) : FrameLayout(context) {

    interface Listener {
        fun onTransformChanged()
        fun onTransformCommitted()
        fun onDeleteRequested()
    }

    var listener: Listener? = null

    private val framePadding = dp(4)
    private val baseStrokeWidthPx = dp(2f)
    private val baseCornerRadiusPx = dp(6).toFloat()

    private val borderDrawable = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke(baseStrokeWidthPx, SELECTION_BORDER_COLOR)
        cornerRadius = baseCornerRadiusPx
    }

    private val selectionBorder = View(context).apply {
        background = borderDrawable
    }

    val boxContainer = FrameLayout(context)

    private val contentText = TextView(context).apply {
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextColor(Color.BLACK)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private val contentImage = ImageView(context).apply {
        visibility = View.GONE
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private val hotspotSize = dp(HOTSPOT_DP)
    private val iconSize = dp(ICON_DP)

    private val ivDelete = handleImageView(R.drawable.ic_sticker_delete_handle)
    private val ivRotate = handleImageView(R.drawable.ic_sticker_rotate_handle)
    private val ivResize = handleImageView(R.drawable.ic_sticker_resize_handle)

    private val hotspotDelete = handleHotspot(ivDelete)
    private val hotspotRotate = handleHotspot(ivRotate)
    private val hotspotResize = handleHotspot(ivResize)

    /**
     * SINGLE SOURCE OF TRUTH for user-applied scale.
     *
     * `baseEmojiSizePx` (the actual laid-out pixel size of the content box)
     * is fixed and derived only from the image width — it must NEVER be
     * multiplied by this value. `scaleFactor` is the only place the user's
     * zoom/resize is represented, and it is what gets persisted 1:1 into
     * StickerLayer.scale. This avoids "double scaling" across renders.
     */
    var scaleFactor: Float = 1f
        private set

    private enum class BodyMode { NONE, DRAG, TRANSFORM }

    private var bodyMode = BodyMode.NONE
    private var dragPointerId = MotionEvent.INVALID_POINTER_ID
    private var lastDragRawX = 0f
    private var lastDragRawY = 0f

    private var pinchPointerId1 = MotionEvent.INVALID_POINTER_ID
    private var pinchPointerId2 = MotionEvent.INVALID_POINTER_ID

    private var pinchStartDistance = 1f
    private var pinchStartScale = 1f
    private var pinchStartFingerAngle = 0f
    private var pinchStartMidX = 0f
    private var pinchStartMidY = 0f
    private var pinchStartTranslationX = 0f
    private var pinchStartTranslationY = 0f

    private var lastPinchAngle = 0f

    private var baseEmojiSizePx: Float = dp(DEFAULT_STICKER_SIZE_DP).toFloat()

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        val margin = hotspotSize / 2
        val frameSize = frameSizePx()
        val borderParams = LayoutParams(frameSize, frameSize, Gravity.CENTER).apply {
            setMargins(margin, margin, margin, margin)
        }
        addView(selectionBorder, borderParams)

        val contentSide = baseEmojiSizePx.toInt()
        val boxParams = LayoutParams(contentSide, contentSide, Gravity.CENTER)
        val contentParams = LayoutParams(contentSide, contentSide, Gravity.CENTER)
        boxContainer.addView(contentText, contentParams)
        boxContainer.addView(contentImage, LayoutParams(contentSide, contentSide, Gravity.CENTER))
        addView(boxContainer, boxParams)

        addHandle(hotspotDelete, Gravity.TOP or Gravity.END)
        addHandle(hotspotRotate, Gravity.BOTTOM or Gravity.START)
        addHandle(hotspotResize, Gravity.BOTTOM or Gravity.END)

        hotspotDelete.setOnClickListener { listener?.onDeleteRequested() }
        hotspotRotate.setOnTouchListener { _, event -> handleRotateTouch(event) }
        hotspotResize.setOnTouchListener { _, event -> handleResizeTouch(event) }

        boxContainer.visibility = View.VISIBLE
        updatePivots()
        applyScaleToContent()
    }

    private fun handleImageView(iconRes: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            background = context.getDrawable(R.drawable.bg_sticker_handle_circle)
            val p = dp(4)
            setPadding(p, p, p, p)
            elevation = dp(2).toFloat()
        }
    }

    private fun handleHotspot(icon: ImageView): FrameLayout {
        return FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            val iconParams = LayoutParams(iconSize, iconSize, Gravity.CENTER)
            addView(icon, iconParams)
        }
    }

    private fun addHandle(hotspot: FrameLayout, gravity: Int) {
        val params = LayoutParams(hotspotSize, hotspotSize, gravity)
        addView(hotspot, params)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            val hotspots = listOf(hotspotDelete, hotspotRotate, hotspotResize)
            val rect = Rect()
            for (hotspot in hotspots) {
                if (hotspot.visibility == View.VISIBLE) {
                    hotspot.getHitRect(rect)
                    if (rect.contains(x, y)) return false
                }
            }
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = handleBodyTouch(event)

    private fun rawXAt(event: MotionEvent, pointerIndex: Int) = event.getRawX(pointerIndex)

    private fun rawYAt(event: MotionEvent, pointerIndex: Int) = event.getRawY(pointerIndex)

    private fun angleDelta(from: Float, to: Float): Float {
        var delta = (to - from) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun handleBodyTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                bodyMode = BodyMode.DRAG
                dragPointerId = event.getPointerId(0)
                lastDragRawX = rawXAt(event, 0)
                lastDragRawY = rawYAt(event, 0)
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onTransformChanged()
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val newIndex = event.actionIndex
                val existingId = if (dragPointerId != MotionEvent.INVALID_POINTER_ID) {
                    dragPointerId
                } else {
                    event.getPointerId(if (newIndex == 0) 1 else 0)
                }
                val newId = event.getPointerId(newIndex)
                if (existingId == newId) return true

                pinchPointerId1 = existingId
                pinchPointerId2 = newId
                bodyMode = BodyMode.TRANSFORM

                val i1 = event.findPointerIndex(pinchPointerId1)
                val i2 = event.findPointerIndex(pinchPointerId2)
                if (i1 == -1 || i2 == -1) return true

                val x1 = rawXAt(event, i1); val y1 = rawYAt(event, i1)
                val x2 = rawXAt(event, i2); val y2 = rawYAt(event, i2)

                pinchStartDistance = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat().coerceAtLeast(1f)
                pinchStartScale = scaleFactor
                pinchStartFingerAngle = Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
                pinchStartMidX = (x1 + x2) / 2f
                pinchStartMidY = (y1 + y2) / 2f
                pinchStartTranslationX = translationX
                pinchStartTranslationY = translationY

                lastPinchAngle = pinchStartFingerAngle

                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                when (bodyMode) {
                    BodyMode.DRAG -> {
                        val index = event.findPointerIndex(dragPointerId)
                        if (index == -1) return true
                        val currentX = rawXAt(event, index)
                        val currentY = rawYAt(event, index)

                        translationX += (currentX - lastDragRawX)
                        translationY += (currentY - lastDragRawY)
                        lastDragRawX = currentX
                        lastDragRawY = currentY
                        listener?.onTransformChanged()
                        return true
                    }

                    BodyMode.TRANSFORM -> {
                        val i1 = event.findPointerIndex(pinchPointerId1)
                        val i2 = event.findPointerIndex(pinchPointerId2)
                        if (i1 == -1 || i2 == -1) return true

                        val x1 = rawXAt(event, i1); val y1 = rawYAt(event, i1)
                        val x2 = rawXAt(event, i2); val y2 = rawYAt(event, i2)

                        val currentDistance = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat().coerceAtLeast(1f)
                        val currentAngle = Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
                        val currentMidX = (x1 + x2) / 2f
                        val currentMidY = (y1 + y2) / 2f

                        val distanceRatio = currentDistance / pinchStartDistance
                        scaleFactor = (pinchStartScale * distanceRatio).coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
                        applyScaleToContent()

                        rotation += angleDelta(lastPinchAngle, currentAngle)
                        lastPinchAngle = currentAngle

                        translationX = pinchStartTranslationX + (currentMidX - pinchStartMidX)
                        translationY = pinchStartTranslationY + (currentMidY - pinchStartMidY)

                        listener?.onTransformChanged()
                        return true
                    }

                    BodyMode.NONE -> return false
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val liftedId = event.getPointerId(event.actionIndex)
                if (bodyMode == BodyMode.TRANSFORM &&
                    (liftedId == pinchPointerId1 || liftedId == pinchPointerId2)
                ) {
                    val remainingId = if (liftedId == pinchPointerId1) pinchPointerId2 else pinchPointerId1
                    val remainingIndex = event.findPointerIndex(remainingId)
                    if (remainingIndex != -1) {
                        bodyMode = BodyMode.DRAG
                        dragPointerId = remainingId
                        lastDragRawX = rawXAt(event, remainingIndex)
                        lastDragRawY = rawYAt(event, remainingIndex)
                    } else {
                        bodyMode = BodyMode.NONE
                    }
                }
                pinchPointerId1 = MotionEvent.INVALID_POINTER_ID
                pinchPointerId2 = MotionEvent.INVALID_POINTER_ID
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasActive = bodyMode != BodyMode.NONE
                bodyMode = BodyMode.NONE
                dragPointerId = MotionEvent.INVALID_POINTER_ID
                pinchPointerId1 = MotionEvent.INVALID_POINTER_ID
                pinchPointerId2 = MotionEvent.INVALID_POINTER_ID
                if (wasActive) {
                    listener?.onTransformCommitted()
                }
                return true
            }
        }
        return false
    }

    private var rotateCenter = floatArrayOf(0f, 0f)
    private var rotateLastAngle = 0f

    private fun handleRotateTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                rotateCenter = centerOnScreen()
                rotateLastAngle = angleTo(rotateCenter, event.rawX, event.rawY)
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentAngle = angleTo(rotateCenter, event.rawX, event.rawY)
                rotation += angleDelta(rotateLastAngle, currentAngle)
                rotateLastAngle = currentAngle
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                listener?.onTransformCommitted()
                return true
            }
        }
        return false
    }

    private var resizeCenter = floatArrayOf(0f, 0f)
    private var resizeInitialDistance = 1f
    private var resizeInitialScale = 1f

    private fun handleResizeTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizeCenter = centerOnScreen()
                resizeInitialDistance = distanceTo(resizeCenter, event.rawX, event.rawY).coerceAtLeast(1f)
                resizeInitialScale = scaleFactor
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentDistance = distanceTo(resizeCenter, event.rawX, event.rawY).coerceAtLeast(1f)
                scaleFactor = (resizeInitialScale * (currentDistance / resizeInitialDistance))
                    .coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
                applyScaleToContent()
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                listener?.onTransformCommitted()
                return true
            }
        }
        return false
    }

    fun setEmoji(emoji: String) {
        contentImage.visibility = View.GONE
        contentText.visibility = View.VISIBLE
        contentText.text = emoji
        applyScaleToContent()
    }

    fun setStickerDrawable(resId: Int) {
        contentText.visibility = View.GONE
        contentImage.visibility = View.VISIBLE
        contentImage.setImageResource(resId)
        applyScaleToContent()
    }

    /**
     * Sets the FIXED base content size in px. This must be derived only from
     * layout/image geometry (e.g. imageRect.width() * ratio) — never from
     * layer.scale. scaleFactor is layered on top of this via applyScaleToContent().
     *
     * Safe to call every render: if the size hasn't actually changed, layout
     * params are left untouched and no relayout is triggered.
     */
    fun setBaseEmojiSizePx(px: Float) {
        val newSize = px.coerceAtLeast(dp(24).toFloat())
        if (newSize == baseEmojiSizePx) return

        baseEmojiSizePx = newSize
        val side = baseEmojiSizePx.toInt()
        val frameSize = frameSizePx()

        (contentText.layoutParams as? LayoutParams)?.let {
            it.width = side
            it.height = side
            contentText.layoutParams = it
        }
        (contentImage.layoutParams as? LayoutParams)?.let {
            it.width = side
            it.height = side
            contentImage.layoutParams = it
        }
        (boxContainer.layoutParams as? LayoutParams)?.let {
            it.width = side
            it.height = side
            boxContainer.layoutParams = it
        }
        (selectionBorder.layoutParams as? LayoutParams)?.let {
            it.width = frameSize
            it.height = frameSize
            selectionBorder.layoutParams = it
        }

        updatePivots()
        applyScaleToContent()
    }

    private fun updatePivots() {
        val side = baseEmojiSizePx
        boxContainer.pivotX = side / 2f
        boxContainer.pivotY = side / 2f

        val frame = frameSizePx().toFloat()
        selectionBorder.pivotX = frame / 2f
        selectionBorder.pivotY = frame / 2f
    }

    private fun applyScaleToContent() {
        boxContainer.scaleX = scaleFactor
        boxContainer.scaleY = scaleFactor

        selectionBorder.scaleX = scaleFactor
        selectionBorder.scaleY = scaleFactor

        val invScale = 1f / scaleFactor
        borderDrawable.setStroke((baseStrokeWidthPx * invScale).toInt().coerceAtLeast(1), SELECTION_BORDER_COLOR)
        borderDrawable.cornerRadius = baseCornerRadiusPx * invScale

        contentText.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseEmojiSizePx * EMOJI_FILL_RATIO)

        val frameHalf = frameSizePx() / 2f
        val extent = frameHalf * (scaleFactor - 1f)

        hotspotDelete.translationX = extent
        hotspotDelete.translationY = -extent
        hotspotRotate.translationX = -extent
        hotspotRotate.translationY = extent
        hotspotResize.translationX = extent
        hotspotResize.translationY = extent
    }

    fun setScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
        if (clamped == scaleFactor) return
        scaleFactor = clamped
        applyScaleToContent()
    }

    private fun frameSizePx(): Int = baseEmojiSizePx.toInt() + framePadding * 2

    private fun centerOnScreen(): FloatArray {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        return floatArrayOf(loc[0] + width / 2f, loc[1] + height / 2f)
    }

    private fun angleTo(center: FloatArray, rawX: Float, rawY: Float): Float =
        Math.toDegrees(atan2((rawY - center[1]).toDouble(), (rawX - center[0]).toDouble())).toFloat()

    private fun distanceTo(center: FloatArray, rawX: Float, rawY: Float): Float =
        hypot((rawX - center[0]).toDouble(), (rawY - center[1]).toDouble()).toFloat()

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

    private fun dp(value: Int): Int = dp(value.toFloat())

    companion object {
        private const val SELECTION_BORDER_COLOR = 0xFF3D8BFF.toInt()
        private const val HOTSPOT_DP = 44
        private const val ICON_DP = 24
        private const val DEFAULT_STICKER_SIZE_DP = 180
        private const val EMOJI_FILL_RATIO = 0.85f

        /** Single source of truth for scale bounds — used by gestures AND setScale(). */
        const val MIN_SCALE_FACTOR = 0.3f
        const val MAX_SCALE_FACTOR = 5f
    }
}