package com.editor.photo.video.collagemaker.photoedit.models.bottomsheets

/**
 * The shape a completed doodle stroke should be rendered as.
 *
 * FREEHAND renders every recorded point as a connected stroke (normal brush drawing).
 * LINE / RECTANGLE / OVAL render a geometric shape using only the first ([DoodlePath.points].first())
 * and last ([DoodlePath.points].last()) recorded points as the shape's bounding box - mirroring how
 * the interactive PhotoEditor ShapeBuilder derives shapes from a drag gesture.
 */
enum class DoodleShapeType {
    FREEHAND, LINE, RECTANGLE, OVAL
}

/**
 * A single recorded touch point for a doodle stroke, stored in NORMALIZED coordinates
 * (0f..1f) relative to the editing surface the stroke was drawn on.
 *
 * Normalized coordinates are what make a single [DoodlePath] reproducible at any output
 * resolution: to get pixel coordinates for a bitmap of a given size, multiply:
 *   pixelX = point.x * bitmap.width
 *   pixelY = point.y * bitmap.height
 */
data class DoodlePoint(
    val x: Float,
    val y: Float
)

/**
 * Immutable, fully self-contained representation of one completed doodle stroke.
 *
 * This is the actual drawing data (not just an id/reference) so that a [DoodlePath] can be
 * independently replayed for preview, undo/redo, and high-resolution export without depending
 * on any transient UI state (e.g. the third-party PhotoEditor drawing surface).
 *
 * [strokeWidth] is stored as a fraction of the reference canvas WIDTH the stroke was captured on
 * (i.e. strokeWidthPx / referenceCanvasWidthPx), so it can be scaled correctly for any output
 * bitmap width:
 *   pixelStrokeWidth = strokeWidth * bitmap.width
 */
data class DoodlePath(
    val id: String,
    val points: List<DoodlePoint>,
    val color: Int = 0xFFFF0000.toInt(),
    val strokeWidth: Float = 0.02f,
    val alpha: Int = 255,
    val shapeType: DoodleShapeType = DoodleShapeType.FREEHAND
)
