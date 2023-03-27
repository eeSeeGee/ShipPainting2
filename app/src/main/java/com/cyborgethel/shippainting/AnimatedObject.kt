package com.cyborgethel.shippainting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

class AnimatedObject(
    var bitmaps: ArrayList<Bitmap>, dimRect: ViewRect, viewRect: ViewRect, offset: AnimOffset?,
    satMod: (tick: Float) -> Float = { 1f }, alphaMod: (tick: Float) -> Float = { 1f }
) {
    private val animOffset: AnimOffset?
    private val dimRect: ViewRect
    private val viewRect: ViewRect
    private val satMod: (tick: Float) -> Float
    private val alphaMod: (tick: Float) -> Float

    init {
        animOffset = offset
        this.dimRect = dimRect
        this.viewRect = viewRect
        this.satMod = satMod
        this.alphaMod = alphaMod
    }

    fun draw(c: Canvas, tick: Float) {
        // Yes this IS comically overcomplicated!
        var frame = (tick * ShipPaintingService.MAX_TICKS
                / (animOffset?.getFrameTime() ?: 1)).toInt() % bitmaps.size
        if (frame >= bitmaps.size) {
            frame = bitmaps.size - 1
        }
        val modDimRect = ViewRect(dimRect.width, dimRect.height,
            dimRect.xoff + (animOffset?.getX(tick) ?: 0), dimRect.yoff + (animOffset?.getY(tick) ?: 0))
        val srcOver = overlap(modDimRect, viewRect, 0,0)?: return
        val dstOver = overlap(viewRect, modDimRect, 0, 0)?: return

        val colorMatrix = ColorMatrix()
        val satVal = satMod(tick)
        colorMatrix.setScale(satVal, satVal, satVal, alphaMod(tick))

        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        val paint = Paint()
        paint.colorFilter = colorMatrixColorFilter

        c.drawBitmap(bitmaps[frame],
            srcOver,
            dstOver, paint)
    }

    // Return the portion of a contained within b relative to a.
    fun overlap(a: ViewRect, b: ViewRect, xoff: Int, yoff: Int): Rect? {
        if (a.xoff + a.width < b.xoff || b.xoff + b.width < a.xoff ||
                a.yoff + a.height < b.yoff || b.yoff + b.height < a.yoff) {
            return null
        }

        return Rect(
            max(a.xoff, b.xoff) - a.xoff + xoff,
            max(a.yoff, b.yoff) - a.yoff + yoff,
            min(a.xoff + a.width, b.xoff + b.width) - a.xoff + xoff,
            min(a.yoff + a.height, b.yoff + b.height) - a.yoff + yoff
        )
    }

    interface AnimOffset {
        fun getX(tick: Float): Int
        fun getY(tick: Float): Int

        fun getFrameTime(): Int = 1
    }
}