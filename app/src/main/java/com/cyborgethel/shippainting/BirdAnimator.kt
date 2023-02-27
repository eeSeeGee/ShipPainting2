package com.cyborgethel.shippainting

class BirdAnimator(var w: Int, var h: Int, var offset: Float
) : AnimatedObject.AnimOffset {
    override fun getX(tick: Float): Int {
        val sideness = 0.8f * w
        return (Math.sin((tick + offset) * 256 * ShipPaintingService.MAXRAD) * sideness / 3f).toInt()
    }

    override fun getY(tick: Float): Int {
        val highness = 0.6f * h
        return (Math.sin((tick + offset) * 128 * ShipPaintingService.MAXRAD) * highness / 3f).toInt()
    }
}