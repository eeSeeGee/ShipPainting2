package com.cyborgethel.shippainting

class CloudAnimator(var w: Int) : AnimatedObject.AnimOffset {
    override fun getX(tick: Float): Int {
        return (w.toFloat() * tick).toInt()
    }

    override fun getY(tick: Float): Int {
        return 0
    }
}