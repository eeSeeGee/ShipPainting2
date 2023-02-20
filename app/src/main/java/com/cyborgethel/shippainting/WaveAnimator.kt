package com.cyborgethel.shippainting

class WaveAnimator(var w: Int, var h: Int) : AnimatedObject.AnimOffset {
    override fun getX(tick: Float): Int {
        val wideness = 0.01f * w
        return (Math.sin(tick * ShipPaintingService.MAXRAD) * wideness).toInt()
    }

    override fun getY(tick: Float): Int {
        val highness = 0.015f * h
        return (highness / 2f).toInt() + (Math.sin(2 * tick * ShipPaintingService.MAXRAD) * highness / 3f).toInt()
    }
}