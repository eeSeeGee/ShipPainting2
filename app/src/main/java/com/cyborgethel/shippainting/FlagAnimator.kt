package com.cyborgethel.shippainting

class FlagAnimator(w: Int, var h: Int
) : AnimatedObject.AnimOffset {
    override fun getX(tick: Float): Int {
        return 0
    }

    override fun getY(tick: Float): Int {
        val highness = 13.1f * 0.025f * h
        return (2 * highness).toInt() + (Math.sin(tick * 256 * ShipPaintingService.MAXRAD) * highness / 3f).toInt()
    }

    override fun getFrameTime(): Int {
        return ShipPaintingService.MS_BETWEEN_FRAMES * 8
    }
}