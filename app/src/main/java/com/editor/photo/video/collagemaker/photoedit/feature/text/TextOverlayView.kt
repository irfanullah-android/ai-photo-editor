package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.editor.photo.video.collagemaker.photoedit.R
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

class TextOverlayView(context: Context) : FrameLayout(context) {

    interface Listener {
        fun onTransformChanged()
        fun onTransformCommitted()
        fun onDeleteRequested()
        fun onEditRequested()
    }

    var listener: Listener? = null

    val boxContainer = FrameLayout(context).apply {
        background = GradientDrawable().apply {
            setStroke(dp(1).coerceAtLeast(2), SELECTION_BORDER_COLOR)
            setColor(Color.TRANSPARENT)
            cornerRadius = dp(4).toFloat()
        }
    }

    val strokeText = TextView(context).apply {
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(10), dp(16), dp(10))
        textSize = 24f
    }

    val contentText = TextView(context).apply {
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(10), dp(16), dp(10))
        setTextColor(Color.WHITE)
        textSize = 24f
    }

    private val hotspotSize = dp(HOTSPOT_DP)
    private val iconSize = dp(ICON_DP)

    private val ivEdit = handleImageView(R.drawable.ic_edit_handle)
    private val ivDelete = handleImageView(R.drawable.ic_cross)
    private val ivRotate = handleImageView(R.drawable.ic_rotate)
    private val ivResize = handleImageView(R.drawable.ic_resize_handle)

    private val hotspotEdit = handleHotspot(ivEdit)
    private val hotspotDelete = handleHotspot(ivDelete)
    private val hotspotRotate = handleHotspot(ivRotate)
    private val hotspotResize = handleHotspot(ivResize)

    var scaleFactor: Float = 1f
        private set

    private enum class BodyMode { NONE, DRAG, TRANSFORM }

    private var bodyMode = BodyMode.NONE
    private var dragPointerId = MotionEvent.INVALID_POINTER_ID
    private var lastDragRawX = 0f
    private var lastDragRawY = 0f

    private var pinchPointerId1 = MotionEvent.INVALID_POINTER_ID
    private var pinchPointerId2 = MotionEvent.INVALID_POINTER_ID
    private var lastPinchDistance = 0f
    private var lastPinchAngle = 0f
    private var lastPinchMidX = 0f
    private var lastPinchMidY = 0f

    private var baseTextSizePx: Float = 48f
    private var baseStrokeWidthPx: Float = 4f
    private var currentStrokeColor: Int = Color.BLACK

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        val margin = hotspotSize / 2
        val boxParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            setMargins(margin, margin, margin, margin)
        }
        val textParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        boxContainer.addView(strokeText, textParams)
        boxContainer.addView(contentText, textParams)
        addView(boxContainer, boxParams)

        addHandle(hotspotEdit, Gravity.TOP or Gravity.START)
        addHandle(hotspotDelete, Gravity.TOP or Gravity.END)
        addHandle(hotspotRotate, Gravity.BOTTOM or Gravity.START)
        addHandle(hotspotResize, Gravity.BOTTOM or Gravity.END)

        hotspotDelete.setOnClickListener { listener?.onDeleteRequested() }
        hotspotEdit.setOnClickListener { listener?.onEditRequested() }
        hotspotRotate.setOnTouchListener { _, event -> handleRotateTouch(event) }
        hotspotResize.setOnTouchListener { _, event -> handleResizeTouch(event) }
    }

    private fun handleImageView(iconRes: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            background = context.getDrawable(R.drawable.bg_handle_circle)
            setPadding(dp(4), dp(4), dp(4), dp(4))
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
            val hotspots = listOf(hotspotEdit, hotspotDelete, hotspotRotate, hotspotResize)
            val rect = Rect()
            for (hotspot in hotspots) {
                if (hotspot.visibility == View.VISIBLE) {
                    hotspot.getHitRect(rect)
                    if (rect.contains(x, y)) {
                        return false
                    }
                }
            }
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return handleBodyTouch(event)
    }

    private fun rawXAt(event: MotionEvent, pointerIndex: Int): Float {
        return event.getX(pointerIndex) + (event.rawX - event.x)
    }

    private fun rawYAt(event: MotionEvent, pointerIndex: Int): Float {
        return event.getY(pointerIndex) + (event.rawY - event.y)
    }

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

                lastPinchDistance = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat().coerceAtLeast(1f)
                lastPinchAngle = Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
                lastPinchMidX = (x1 + x2) / 2f
                lastPinchMidY = (y1 + y2) / 2f

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

                        val distanceRatio = currentDistance / lastPinchDistance
                        scaleFactor = (scaleFactor * distanceRatio).coerceIn(0.2f, 6f)
                        applyScaleToContent()

                        rotation += angleDelta(lastPinchAngle, currentAngle)

                        translationX += (currentMidX - lastPinchMidX)
                        translationY += (currentMidY - lastPinchMidY)

                        lastPinchDistance = currentDistance
                        lastPinchAngle = currentAngle
                        lastPinchMidX = currentMidX
                        lastPinchMidY = currentMidY

                        listener?.onTransformChanged()
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

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasActive = bodyMode != BodyMode.NONE
                bodyMode = BodyMode.NONE
                dragPointerId = MotionEvent.INVALID_POINTER_ID
                pinchPointerId1 = MotionEvent.INVALID_POINTER_ID
                pinchPointerId2 = MotionEvent.INVALID_POINTER_ID
                if (wasActive) listener?.onTransformCommitted()
                return true
            }
        }
        return false
    }

    private var rotateLastAngle = 0f

    private fun handleRotateTouch(event: MotionEvent): Boolean {
        val centerScreen = centerOnScreen()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                rotateLastAngle = angleTo(centerScreen, event.rawX, event.rawY)
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentAngle = angleTo(centerOnScreen(), event.rawX, event.rawY)
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

    private var resizeLastDistance = 0f

    private fun handleResizeTouch(event: MotionEvent): Boolean {
        val centerScreen = centerOnScreen()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizeLastDistance = distanceTo(centerScreen, event.rawX, event.rawY).coerceAtLeast(1f)
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentDistance = distanceTo(centerOnScreen(), event.rawX, event.rawY).coerceAtLeast(1f)
                val ratio = currentDistance / resizeLastDistance
                scaleFactor = (scaleFactor * ratio).coerceIn(0.2f, 6f)
                applyScaleToContent()
                resizeLastDistance = currentDistance
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

    fun zoomIn(step: Float = 0.1f) {
        scaleFactor = (scaleFactor + step).coerceIn(0.2f, 6f)
        applyScaleToContent()
        listener?.onTransformChanged()
        listener?.onTransformCommitted()
    }

    fun zoomOut(step: Float = 0.1f) {
        scaleFactor = (scaleFactor - step).coerceIn(0.2f, 6f)
        applyScaleToContent()
        listener?.onTransformChanged()
        listener?.onTransformCommitted()
    }

    fun setBaseTextSizePx(px: Float) {
        baseTextSizePx = max(px, 8f)
        applyScaleToContent()
    }

    fun setStroke(strokeWidthPx: Float, strokeColor: Int) {
        baseStrokeWidthPx = strokeWidthPx.coerceAtLeast(0f)
        currentStrokeColor = strokeColor
        applyScaleToContent()
    }

    private fun applyScaleToContent() {
        val scaledTextSizePx = baseTextSizePx * scaleFactor
        contentText.textSize = scaledTextSizePx / resources.displayMetrics.scaledDensity
        strokeText.textSize = scaledTextSizePx / resources.displayMetrics.scaledDensity

        val scaledStrokeWidth = baseStrokeWidthPx * scaleFactor
        if (scaledStrokeWidth < MIN_VISIBLE_STROKE_PX) {
            strokeText.visibility = View.GONE
        } else {
            strokeText.visibility = View.VISIBLE
            strokeText.setTextColor(currentStrokeColor)
            strokeText.paint.style = Paint.Style.STROKE
            strokeText.paint.strokeWidth = scaledStrokeWidth
            strokeText.paint.strokeJoin = Paint.Join.ROUND
            strokeText.paint.strokeMiter = 4f
        }
        requestLayout()
        invalidate()
    }

    fun resetScale(newBaseScale: Float = 1f) {
        scaleFactor = newBaseScale
        applyScaleToContent()
    }

    fun setScaleFactorProgrammatic(scale: Float) {
        scaleFactor = scale.coerceIn(0.2f, 6f)
        applyScaleToContent()
    }

    fun applyAlignment(gravity: Int) {
        contentText.gravity = gravity
        strokeText.gravity = gravity
        (contentText.layoutParams as? LayoutParams)?.gravity = gravity
        (strokeText.layoutParams as? LayoutParams)?.gravity = gravity
        requestLayout()
        invalidate()
    }

    fun applyStyle(
        text: String,
        color: Int,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        typeface: Typeface
    ) {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val resolvedTypeface = Typeface.create(typeface, style)

        contentText.text = text
        contentText.setTextColor(color)
        contentText.typeface = resolvedTypeface
        contentText.paintFlags = if (isUnderline) {
            contentText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else {
            contentText.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        }

        strokeText.text = text
        strokeText.typeface = resolvedTypeface
        strokeText.paintFlags = contentText.paintFlags

        applyScaleToContent()
    }

    private fun centerOnScreen(): FloatArray {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        return floatArrayOf(loc[0] + width / 2f, loc[1] + height / 2f)
    }

    private fun angleTo(center: FloatArray, rawX: Float, rawY: Float): Float {
        return Math.toDegrees(atan2((rawY - center[1]).toDouble(), (rawX - center[0]).toDouble())).toFloat()
    }

    private fun distanceTo(center: FloatArray, rawX: Float, rawY: Float): Float {
        return hypot((rawX - center[0]).toDouble(), (rawY - center[1]).toDouble()).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val SELECTION_BORDER_COLOR = 0xFF3D8BFF.toInt()
        private const val MIN_VISIBLE_STROKE_PX = 1.5f
        private const val HOTSPOT_DP = 64
        private const val ICON_DP = 32
    }
}