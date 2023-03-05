package com.cyborgethel.shippainting

import kotlin.math.pow

class WaveAnimator(var w: Int, var h: Int, var level: Int) : AnimatedObject.AnimOffset {
    override fun getX(tick: Float): Int {
        var x = (w.toFloat() * tick * 8 / 2f.pow(level)).toInt()
        while (x > w / 2) {
            x -= w / 2
        }
        return x
    }

    override fun getY(tick: Float): Int {
        val highness = 0.03f * h
        return (highness / 2f).toInt() + (Math.sin(level * 4 * tick * ShipPaintingService.MAXRAD) * highness / 3f).toInt()
    }
}