package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
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

    private val handleSize = dp(28)
    private val ivEdit = handleImageView(R.drawable.ic_edit_handle)
    private val ivDelete = handleImageView(R.drawable.ic_cross)
    private val ivRotate = handleImageView(R.drawable.ic_rotate)
    private val ivResize = handleImageView(R.drawable.ic_resize_handle)

    var scaleFactor: Float = 1f
        private set

    private var dragging = false
    private var startRawX = 0f
    private var startRawY = 0f
    private var startTranslationX = 0f
    private var startTranslationY = 0f

    private var baseTextSizePx: Float = 48f
    private var baseStrokeWidthPx: Float = 4f
    private var currentStrokeColor: Int = Color.BLACK

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        val margin = handleSize / 2
        val boxParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            setMargins(margin, margin, margin, margin)
        }
        val textParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        boxContainer.addView(strokeText, textParams)
        boxContainer.addView(contentText, textParams)
        addView(boxContainer, boxParams)

        addHandle(ivEdit, Gravity.TOP or Gravity.START)
        addHandle(ivDelete, Gravity.TOP or Gravity.END)
        addHandle(ivRotate, Gravity.BOTTOM or Gravity.START)
        addHandle(ivResize, Gravity.BOTTOM or Gravity.END)

        contentText.setOnTouchListener { _, event -> handleBodyTouch(event) }
        strokeText.setOnTouchListener { _, event -> handleBodyTouch(event) }
        boxContainer.setOnTouchListener { _, event -> handleBodyTouch(event) }

        ivDelete.setOnClickListener { listener?.onDeleteRequested() }
        ivEdit.setOnClickListener { listener?.onEditRequested() }
        ivRotate.setOnTouchListener { _, event -> handleRotateTouch(event) }
        ivResize.setOnTouchListener { _, event -> handleResizeTouch(event) }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            val handles = listOf(ivEdit, ivDelete, ivRotate, ivResize)
            for (handle in handles) {
                if (handle.visibility == View.VISIBLE) {
                    val rect = android.graphics.Rect()
                    handle.getHitRect(rect)
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

    private fun handleImageView(iconRes: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            background = context.getDrawable(R.drawable.bg_handle_circle)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
    }

    private fun addHandle(view: ImageView, gravity: Int) {
        val params = LayoutParams(handleSize, handleSize, gravity)
        addView(view, params)
    }

    private fun handleBodyTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                startRawX = event.rawX
                startRawY = event.rawY
                startTranslationX = translationX
                startTranslationY = translationY
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                parent?.requestDisallowInterceptTouchEvent(true)
                translationX = startTranslationX + (event.rawX - startRawX)
                translationY = startTranslationY + (event.rawY - startRawY)
                listener?.onTransformChanged()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    listener?.onTransformCommitted()
                }
                return true
            }
        }
        return false
    }

    private var rotateStartAngle = 0f
    private var rotateStartRotation = 0f

    private fun handleRotateTouch(event: MotionEvent): Boolean {
        val centerScreen = centerOnScreen()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                rotateStartAngle = angleTo(centerScreen, event.rawX, event.rawY)
                rotateStartRotation = rotation
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentAngle = angleTo(centerScreen, event.rawX, event.rawY)
                rotation = rotateStartRotation + (currentAngle - rotateStartAngle)
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

    private var resizeStartDistance = 0f
    private var resizeStartScale = 1f

    private fun handleResizeTouch(event: MotionEvent): Boolean {
        val centerScreen = centerOnScreen()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizeStartDistance = distanceTo(centerScreen, event.rawX, event.rawY).coerceAtLeast(1f)
                resizeStartScale = scaleFactor
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val currentDistance = distanceTo(centerScreen, event.rawX, event.rawY)
                val ratio = currentDistance / resizeStartDistance
                scaleFactor = (resizeStartScale * ratio).coerceIn(0.2f, 6f)
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
    }
}