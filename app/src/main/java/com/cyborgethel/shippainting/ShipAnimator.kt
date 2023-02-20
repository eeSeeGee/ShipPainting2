package com.cyborgethel.shippainting

class ShipAnimator(w: Int, var h: Int
) : AnimatedObject.AnimOffset {
    override fun getX(tick: Float): Int {
        return 0
    }

    override fun getY(tick: Float): Int {
        val highness = 0.025f * h
        return (2 * highness).toInt() + (Math.sin(tick * ShipPaintingService.MAXRAD) * highness / 3f).toInt()
    }
}