package com.cyborgethel.shippainting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect

class AnimatedObject(
    var _img: ArrayList<Bitmap>, x: Float, y: Float, vx: Float,
    vy: Float, offset: AnimOffset, satMod: (tick: Float) -> Float = { 1f }
) {
    private val _offset: AnimOffset
    private val _fx: Float
    private val _fy: Float
    private val _vx: Float
    private val _vy: Float
    private val satMod: (tick: Float) -> Float

    init {
        _offset = offset
        _fx = x
        _fy = y
        _vx = vx
        _vy = vy
        this.satMod = satMod
    }

    fun draw(c: Canvas, startx: Int, starty: Int, tick: Float) {
        var frame = (tick * _img.size).toInt()
        if (frame >= _img.size) {
            frame = _img.size - 1
        }
        val sx = (startx + _vx * _fx).toInt() + if (_offset == null) 0 else _offset.getX(tick)
        val sy = (starty + _vy * _fy).toInt() + if (_offset == null) 0 else _offset.getY(tick)
        val r = Rect(
            sx,
            sy,
            sx + _img[frame].width,
            sy + _img[frame].height
        )

        val colorMatrix = ColorMatrix()
        val satVal = satMod(tick)
        colorMatrix.setScale(satVal, satVal, satVal, 1f)

        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        val paint = Paint()
        paint.colorFilter = colorMatrixColorFilter

        c.drawBitmap(_img[frame], null, r, paint)
    }

    interface AnimOffset {
        fun getX(tick: Float): Int
        fun getY(tick: Float): Int
    }
}