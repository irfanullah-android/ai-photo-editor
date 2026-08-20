package com.editor.photo.video.collagemaker.photoedit.feature.sticker

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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

    private var bodyTransformChanged = false
    private var rotateChanged = false
    private var resizeChanged = false

    private val borderDrawable = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke(baseStrokeWidthPx, SELECTION_BORDER_COLOR)
        cornerRadius = baseCornerRadiusPx
    }

    private val selectionBorder = View(context).apply {
        background = borderDrawable
        isClickable = false
        isFocusable = false
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
     * baseEmojiSizePx is the fixed base size.
     * scaleFactor is the absolute user-applied scale.
     *
     * StickerLayer.scale is persisted 1:1 from this value.
     *
     * IMPORTANT:
     * Never multiply layer.scale by scaleFactor.
     */
    var scaleFactor: Float = 1f
        private set

    private enum class BodyMode {
        NONE,
        DRAG,
        TRANSFORM
    }

    private var bodyMode = BodyMode.NONE

    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    // Transform safety slop flags
    private var isTransformStarted = false
    private var handleDragging = false
    private var initialDownX = 0f
    private var initialDownY = 0f

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

    private var baseEmojiSizePx = dp(DEFAULT_STICKER_SIZE_DP).toFloat()

    private var rotateCenter = floatArrayOf(0f, 0f)
    private var rotateLastAngle = 0f

    private var resizeCenter = floatArrayOf(0f, 0f)
    private var resizeInitialDistance = 1f
    private var resizeInitialScale = 1f

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

        hotspotDelete.setOnClickListener {
            listener?.onDeleteRequested()
        }

        hotspotRotate.setOnTouchListener { _, event ->
            handleRotateTouch(event)
        }

        hotspotResize.setOnTouchListener { _, event ->
            handleResizeTouch(event)
        }

        boxContainer.visibility = View.VISIBLE

        updatePivots()
        applyScaleToContent()
    }

    private fun handleImageView(iconRes: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            background = context.getDrawable(R.drawable.bg_sticker_handle_circle)
            val padding = dp(4)
            setPadding(padding, padding, padding, padding)
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

    /**
     * Expand the touch bounds of this root view out to the translated hotspots
     * so that touches outside the unscaled center aren't dropped by the parent container.
     */
    override fun getHitRect(outRect: Rect) {
        super.getHitRect(outRect)
        val scale = scaleFactor.coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
        if (scale > 1f) {
            val frameHalf = frameSizePx() / 2f
            val extent = frameHalf * (scale - 1f)
            val inflateAmount = (extent + hotspotSize).toInt()
            outRect.inset(-inflateAmount, -inflateAmount)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val x = ev.x
            val y = ev.y

            if (isPointInsideHotspot(hotspotDelete, x, y)) return false
            if (isPointInsideHotspot(hotspotRotate, x, y)) return false
            if (isPointInsideHotspot(hotspotResize, x, y)) return false

            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    /**
     * Perfected local hit detection.
     * event.x and event.y are already passed un-rotated by the parent logic.
     * hotspot.x and hotspot.y are perfect local translations. No global Window mapping required!
     */
    private fun isPointInsideHotspot(hotspot: View, x: Float, y: Float): Boolean {
        if (hotspot.visibility != View.VISIBLE) return false

        val left = hotspot.x
        val top = hotspot.y
        val right = left + hotspot.width
        val bottom = top + hotspot.height

        return x >= left && x <= right && y >= top && y <= bottom
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return handleBodyTouch(event)
    }

    private fun rawXAt(event: MotionEvent, pointerIndex: Int): Float = event.getRawX(pointerIndex)
    private fun rawYAt(event: MotionEvent, pointerIndex: Int): Float = event.getRawY(pointerIndex)

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

                initialDownX = lastDragRawX
                initialDownY = lastDragRawY

                isTransformStarted = false
                bodyTransformChanged = false

                parent?.requestDisallowInterceptTouchEvent(true)

                // Note: Deliberately DO NOT call onTransformChanged() on simple tap
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

                val x1 = rawXAt(event, i1)
                val y1 = rawYAt(event, i1)
                val x2 = rawXAt(event, i2)
                val y2 = rawYAt(event, i2)

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

                        if (!isTransformStarted) {
                            val dx = currentX - initialDownX
                            val dy = currentY - initialDownY
                            if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                                isTransformStarted = true
                                bodyTransformChanged = true
                                listener?.onTransformChanged()
                            }
                        }

                        if (isTransformStarted) {
                            val deltaX = currentX - lastDragRawX
                            val deltaY = currentY - lastDragRawY
                            if (deltaX != 0f || deltaY != 0f) {
                                translationX += deltaX
                                translationY += deltaY
                                bodyTransformChanged = true
                                listener?.onTransformChanged()
                            }
                        }

                        lastDragRawX = currentX
                        lastDragRawY = currentY
                        return true
                    }

                    BodyMode.TRANSFORM -> {
                        val i1 = event.findPointerIndex(pinchPointerId1)
                        val i2 = event.findPointerIndex(pinchPointerId2)

                        if (i1 == -1 || i2 == -1) return true

                        val x1 = rawXAt(event, i1)
                        val y1 = rawYAt(event, i1)
                        val x2 = rawXAt(event, i2)
                        val y2 = rawYAt(event, i2)

                        val currentDistance = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat().coerceAtLeast(1f)
                        val currentAngle = Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
                        val currentMidX = (x1 + x2) / 2f
                        val currentMidY = (y1 + y2) / 2f

                        if (!isTransformStarted) {
                            val distDiff = Math.abs(currentDistance - pinchStartDistance)
                            val angleDiff = Math.abs(angleDelta(pinchStartFingerAngle, currentAngle))
                            val midXDiff = Math.abs(currentMidX - pinchStartMidX)
                            val midYDiff = Math.abs(currentMidY - pinchStartMidY)

                            if (distDiff > touchSlop || angleDiff > 2f || midXDiff > touchSlop || midYDiff > touchSlop) {
                                isTransformStarted = true
                                bodyTransformChanged = true
                                listener?.onTransformChanged()
                            }
                        }

                        if (isTransformStarted) {
                            val distanceRatio = currentDistance / pinchStartDistance
                            scaleFactor = (pinchStartScale * distanceRatio).coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
                            applyScaleToContent()

                            rotation += angleDelta(lastPinchAngle, currentAngle)
                            translationX = pinchStartTranslationX + (currentMidX - pinchStartMidX)

                            bodyTransformChanged = true
                            listener?.onTransformChanged()
                        }

                        lastPinchAngle = currentAngle
                        return true
                    }
                    BodyMode.NONE -> return false
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val liftedId = event.getPointerId(event.actionIndex)

                if (bodyMode == BodyMode.TRANSFORM && (liftedId == pinchPointerId1 || liftedId == pinchPointerId2)) {
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

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                val shouldCommit = bodyTransformChanged

                bodyMode = BodyMode.NONE
                bodyTransformChanged = false
                isTransformStarted = false

                dragPointerId = MotionEvent.INVALID_POINTER_ID
                pinchPointerId1 = MotionEvent.INVALID_POINTER_ID
                pinchPointerId2 = MotionEvent.INVALID_POINTER_ID

                if (shouldCommit) {
                    listener?.onTransformCommitted()
                }

                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return false
    }

    private fun handleRotateTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                rotateCenter = centerOnScreen()
                rotateLastAngle = angleTo(rotateCenter, event.rawX, event.rawY)
                rotateChanged = false
                handleDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentAngle = angleTo(rotateCenter, event.rawX, event.rawY)
                val delta = angleDelta(rotateLastAngle, currentAngle)

                if (!handleDragging) {
                    if (Math.abs(delta) > 1f) {
                        handleDragging = true
                        rotateChanged = true
                        listener?.onTransformChanged()
                    }
                }

                if (handleDragging) {
                    rotation += delta
                    rotateChanged = true
                    listener?.onTransformChanged()
                }

                rotateLastAngle = currentAngle
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (rotateChanged) {
                    listener?.onTransformCommitted()
                }
                rotateChanged = false
                handleDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return false
    }

    private fun handleResizeTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizeCenter = centerOnScreen()
                resizeInitialDistance = distanceTo(resizeCenter, event.rawX, event.rawY).coerceAtLeast(1f)
                resizeInitialScale = scaleFactor
                resizeChanged = false
                handleDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentDistance = distanceTo(resizeCenter, event.rawX, event.rawY).coerceAtLeast(1f)

                if (!handleDragging) {
                    if (Math.abs(currentDistance - resizeInitialDistance) > touchSlop) {
                        handleDragging = true
                        resizeChanged = true
                        listener?.onTransformChanged()
                    }
                }

                if (handleDragging) {
                    val newScale = (resizeInitialScale * (currentDistance / resizeInitialDistance)).coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
                    if (newScale != scaleFactor) {
                        scaleFactor = newScale
                        applyScaleToContent()
                        resizeChanged = true
                        listener?.onTransformChanged()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (resizeChanged) {
                    listener?.onTransformCommitted()
                }
                resizeChanged = false
                handleDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
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

        // Ensure root view rotates around its direct center correctly
        pivotX = frame / 2f
        pivotY = frame / 2f
    }

    private fun applyScaleToContent() {
        val scale = scaleFactor.coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)

        boxContainer.scaleX = scale
        boxContainer.scaleY = scale
        selectionBorder.scaleX = scale
        selectionBorder.scaleY = scale

        val invScale = 1f / scale

        borderDrawable.setStroke((baseStrokeWidthPx * invScale).toInt().coerceAtLeast(1), SELECTION_BORDER_COLOR)
        borderDrawable.cornerRadius = baseCornerRadiusPx * invScale

        contentText.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseEmojiSizePx * EMOJI_FILL_RATIO)

        val frameHalf = frameSizePx() / 2f
        val extent = frameHalf * (scale - 1f)

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

    private fun frameSizePx(): Int {
        return baseEmojiSizePx.toInt() + framePadding * 2
    }

    /**
     * Perfected Center Calculation.
     * Prevents rotation matrix distortion by calculating center exactly based on translation/pivot
     * rather than depending entirely on bounded un-rotated mapping.
     */
    private fun centerOnScreen(): FloatArray {
        val loc = IntArray(2)
        val p = parent as? View
        if (p != null) {
            p.getLocationOnScreen(loc)
            return floatArrayOf(
                loc[0] + x + pivotX,
                loc[1] + y + pivotY
            )
        }

        getLocationOnScreen(loc)
        return floatArrayOf(loc[0] + width / 2f, loc[1] + height / 2f)
    }

    private fun angleTo(center: FloatArray, rawX: Float, rawY: Float): Float {
        return Math.toDegrees(atan2((rawY - center[1]).toDouble(), (rawX - center[0]).toDouble())).toFloat()
    }

    private fun distanceTo(center: FloatArray, rawX: Float, rawY: Float): Float {
        return hypot((rawX - center[0]).toDouble(), (rawY - center[1]).toDouble()).toFloat()
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
    }

    private fun dp(value: Int): Int = dp(value.toFloat())

    companion object {
        private const val SELECTION_BORDER_COLOR = 0xFF3D8BFF.toInt()
        private const val HOTSPOT_DP = 44
        private const val ICON_DP = 24
        private const val DEFAULT_STICKER_SIZE_DP = 180
        private const val EMOJI_FILL_RATIO = 0.85f

        const val MIN_SCALE_FACTOR = 0.3f
        const val MAX_SCALE_FACTOR = 5f
    }
}