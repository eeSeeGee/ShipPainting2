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
    var _img: ArrayList<Bitmap>, dimRect: ViewRect, viewRect: ViewRect, offset: AnimOffset?, satMod: (tick: Float) -> Float = { 1f }
) {
    private val _offset: AnimOffset?
    private val dimRect: ViewRect
    private val viewRect: ViewRect
    private val satMod: (tick: Float) -> Float

    init {
        _offset = offset
        this.dimRect = dimRect
        this.viewRect = viewRect
        this.satMod = satMod
    }

    fun draw(c: Canvas, tick: Float) {
        var frame = (tick * _img.size).toInt()
        if (frame >= _img.size) {
            frame = _img.size - 1
        }
        val srcOver = overlap(dimRect, viewRect, _offset?.getX(tick) ?: 0,_offset?.getY(tick) ?: 0)?: return
        val dstOver = overlap(viewRect, dimRect, 0, 0)?: return

        val colorMatrix = ColorMatrix()
        val satVal = satMod(tick)
        colorMatrix.setScale(satVal, satVal, satVal, 1f)

        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        val paint = Paint()
        paint.colorFilter = colorMatrixColorFilter

        c.drawBitmap(_img[frame],
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
    }
}