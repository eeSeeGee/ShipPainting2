package com.cyborgethel.shippainting

class CloudAnimator(var w: Int, h: Int) : AnimatedObject.AnimOffset {
    override fun getX(tick: Float): Int {
        return 0
    }

    override fun getY(tick: Float): Int {
        return 0
    }
}