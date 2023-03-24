package com.cyborgethel.shippainting

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.*
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.ref.WeakReference
import java.util.Calendar
import kotlin.collections.ArrayList
import kotlin.math.log10
import kotlin.math.max

class ShipPaintingService : WallpaperService() {
    private val animHandler = AnimHandler(this)
    @Volatile private var newSurfaceLimits: SurfaceLimits? = null
    private var currentSurfaceLimits: SurfaceLimits? = null

    private var animatedObjects: ArrayList<AnimatedObject> = ArrayList()
    var shipBackground: Bitmap? = null
    private var vx = 0f
    private var vy = 0f
    private var midx = 0
    private var midy = 0
    private var visible = false
    private var dayNight = true
    private var fixTime = false
    private var fixedTime = 0

    override fun onCreateEngine(): Engine {
        return ShipEngine()
    }

    internal inner class ShipEngine : Engine() {
        private val shipAnimRunnable = Runnable {
            run {
                Handler(mainLooper).post {
                    run {
                        drawFrame()
                    }
                }
            }
        }

        private var totalDrawTime = 0L
        private var totalBgDrawTime = 0L
        private var framesCycled = 0

        fun scheduleRedraw() {
            cancelRedraw()
            animHandler.post(shipAnimRunnable)
        }

        fun scheduleRedrawLater(minusTimeMs: Long) {
            cancelRedraw()
            animHandler.postDelayed(shipAnimRunnable, max(MS_BETWEEN_FRAMES.toLong() - minusTimeMs, 0))
        }

        fun cancelRedraw() {
            animHandler.removeCallbacks(shipAnimRunnable)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
        }

        override fun onDestroy() {
            super.onDestroy()
            cancelRedraw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this@ShipPaintingService.visible = visible

            val prefs = PreferenceManager.getDefaultSharedPreferences(this@ShipPaintingService)
            dayNight = prefs.getBoolean("day_night", true)
            fixTime = prefs.getBoolean("fix_time", false)
            fixedTime = prefs.getInt("time_of_day", 0)

            if (visible) {
                scheduleRedraw()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder, format: Int,
            width: Int, height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.i("onSurfaceChanged", String.format("size: w:%d h:%d", width, height))

            newSurfaceLimits = SurfaceLimits(width, height)
            scheduleRedraw()
        }

        private fun calculateDrawableSurfaces(width: Int, height: Int) = runBlocking {
            val startTime = System.currentTimeMillis()

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(
                resources,
                R.drawable.sky, options
            )

            val imageHeight = options.outHeight
            val imageWidth = options.outWidth
            val xScale = width.toFloat() / imageWidth.toFloat()
            val yScale = height.toFloat() / imageHeight.toFloat()
            var scale = xScale
            if (xScale * imageHeight.toFloat() < height) {
                scale = yScale
            }
            shipBackground = decodeSampledBitmapFromResource(
                R.drawable.sky,
                null,
                (scale * imageWidth.toFloat()).toInt(),
                (scale * imageHeight.toFloat()).toInt()
            )
            Log.i(
                "calculateDrawableSurfaces", String.format(
                    "bg size: w:%d h:%d",
                    shipBackground!!.width, shipBackground!!.height
                )
            )
            midx = shipBackground!!.width / 2
            midy = shipBackground!!.height / 2

            val viewRect = ViewRect(width, height, midx - width / 2, midy - height / 2)

            val resources = listOf(
                R.drawable.bird1,
                R.drawable.bird2,
                R.drawable.bird3,
                R.drawable.ship,
                R.drawable.flag1,
                R.drawable.flag2,
                R.drawable.flag3,
                R.drawable.flag4,
                R.drawable.flag5,
                R.drawable.flag6,
                R.drawable.flag7,
                R.drawable.flag8,
                R.drawable.flag9,
                R.drawable.maincrest,
                R.drawable.rearcrest,
                R.drawable.backwave,
                R.drawable.back2wave,
                R.drawable.front2wave,
                R.drawable.frontwave,
                R.drawable.clouds
            )

            val jobs: List<Deferred<Bitmap>> = resources.map {
                async(Dispatchers.IO) {
                    decodeSampledBitmapFromResource(it, scale)
                }
            }
            val results = jobs.awaitAll()

            val bitmapMap = resources.zip(results).toMap()

            val bird1 = bitmapMap.get(R.drawable.bird1)!!
            val bird2 = bitmapMap.get(R.drawable.bird2)!!
            val bird3 = bitmapMap.get(R.drawable.bird3)!!
            val ship = bitmapMap.get(R.drawable.ship)!!
            val flag1 = bitmapMap.get(R.drawable.flag1)!!
            val flag2 = bitmapMap.get(R.drawable.flag2)!!
            val flag3 = bitmapMap.get(R.drawable.flag3)!!
            val flag4 = bitmapMap.get(R.drawable.flag4)!!
            val flag5 = bitmapMap.get(R.drawable.flag5)!!
            val flag6 = bitmapMap.get(R.drawable.flag6)!!
            val flag7 = bitmapMap.get(R.drawable.flag7)!!
            val flag8 = bitmapMap.get(R.drawable.flag8)!!
            val flag9 = bitmapMap.get(R.drawable.flag9)!!
            val frontcrest = bitmapMap.get(R.drawable.maincrest)!!
            val rearcrest = bitmapMap.get(R.drawable.rearcrest)!!
            val wave1 = bitmapMap.get(R.drawable.backwave)!!
            val wave2 = bitmapMap.get(R.drawable.back2wave)!!
            val wave3 = bitmapMap.get(R.drawable.front2wave)!!
            val wave4 = bitmapMap.get(R.drawable.frontwave)!!
            val clouds = bitmapMap.get(R.drawable.clouds)!!

            Log.i("calculateDrawableSurfaces", String.format("Loaded resources in %d", System.currentTimeMillis() - startTime))

            vx = shipBackground!!.width.toFloat()
            vy = shipBackground!!.height.toFloat()
            animatedObjects = ArrayList()
            val cl1 = arrayListOf(clouds)
            animatedObjects.addAll(
                listOf(
                    AnimatedObject(
                        cl1, ViewRect(clouds.width, clouds.height,
                            (CLOUD1X*scale).toInt(), (CLOUD1Y*scale).toInt()),
                        viewRect, CloudAnimator((clouds.width.toFloat()*scale).toInt()), fader(MEDIUM_RATIO)
                    ),
                    AnimatedObject(
                        cl1, ViewRect(clouds.width, clouds.height,
                            ((CLOUD1X - shipBackground!!.width)*scale).toInt(), (CLOUD1Y*scale).toInt()),
                        viewRect, CloudAnimator((clouds.width.toFloat()*scale).toInt()), fader(MEDIUM_RATIO)
                    ),
                    AnimatedObject(
                        cl1, ViewRect(clouds.width, clouds.height,
                            ((CLOUD1X - 2 * shipBackground!!.width)*scale).toInt(), (CLOUD1Y*scale).toInt()),
                        viewRect, CloudAnimator((clouds.width.toFloat()*scale).toInt()), fader(MEDIUM_RATIO)
                    )
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(frontcrest), ViewRect(frontcrest.width, frontcrest.height,
                        (FRONTCRESTX*scale).toInt(), (FRONTCRESTY*scale).toInt()),
                    viewRect, null, fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(rearcrest), ViewRect(rearcrest.width, rearcrest.height,
                        (REARCRESTX*scale).toInt(), (REARCRESTY*scale).toInt()),
                    viewRect, null, fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(bird1), ViewRect(bird1.width, bird1.height,
                        (BIRD1X*scale).toInt(), (BIRD1Y*scale).toInt()),
                    viewRect, BirdAnimator(bird1.width, bird1.height, 0.1f), fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(bird2), ViewRect(bird2.width, bird2.height,
                        (BIRD2X*scale).toInt(), (BIRD2Y*scale).toInt()),
                    viewRect, BirdAnimator(bird2.width, bird2.height, 0.3f), fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(bird3), ViewRect(bird3.width, bird3.height,
                        (BIRD3X*scale).toInt(), (BIRD3Y*scale).toInt()),
                    viewRect, BirdAnimator(bird3.width, bird3.height, 0.7f), fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(ship), ViewRect(ship.width, ship.height,
                        (SHIPX*scale).toInt(), (SHIPY*scale).toInt()),
                    viewRect, ShipAnimator(ship.width, ship.height), fader(LIGHT_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(flag1, flag2, flag3, flag4, flag5, flag6, flag7, flag8, flag9),
                    ViewRect(flag1.width, flag1.height,
                        (FLAGX*scale).toInt(), (FLAGY*scale).toInt()),
                    viewRect, FlagAnimator(flag1.width, flag1.height), fader(LIGHT_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(wave1), ViewRect(wave1.width, wave1.height,
                        (WAVE1X*scale).toInt(), (WAVE1Y*scale).toInt()),
                    viewRect, WaveAnimator((wave1.width.toFloat()*scale).toInt(), (wave1.height.toFloat()*scale).toInt(), 4), fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(wave2), ViewRect(wave2.width, wave2.height,
                        (WAVE2X*scale).toInt(), (WAVE2Y*scale).toInt()),
                    viewRect, WaveAnimator((wave2.width.toFloat()*scale).toInt(), (wave2.height.toFloat()*scale).toInt(), 3), fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(wave3), ViewRect(wave3.width, wave3.height,
                        (WAVE3X*scale).toInt(), (WAVE3Y*scale).toInt()),
                    viewRect, WaveAnimator((wave3.width.toFloat()*scale).toInt(), (wave3.height.toFloat()*scale).toInt(), 2), fader(MEDIUM_RATIO)
                )
            )

            animatedObjects.add(
                AnimatedObject(
                    arrayListOf(wave4), ViewRect(wave4.width, wave4.height,
                        (WAVE4X*scale).toInt(), (WAVE4Y*scale).toInt()),
                    viewRect, WaveAnimator((wave4.width.toFloat()*scale).toInt(), (wave4.height.toFloat()*scale).toInt(), 1), fader(MEDIUM_RATIO)
                )
            )

            val doneTime = System.currentTimeMillis()
            Log.i("calculateDrawableSurfaces", String.format("Calculated drawable surfaces in %d", doneTime - startTime))
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
        }

        fun drawFrame() {
            if (!visible) {
                return
            }

            val sl = newSurfaceLimits.also { newSurfaceLimits = null }
            if (sl != null && currentSurfaceLimits != sl) {
                currentSurfaceLimits = sl
                calculateDrawableSurfaces(sl.width, sl.height)
            }

            val startTime = System.currentTimeMillis()

            val holder = surfaceHolder
            var c: Canvas? = null
            try {
                c = holder.lockHardwareCanvas()
                if (c != null) {
                    val tick = (SystemClock.elapsedRealtime() % MAX_TICKS).toFloat() / MAX_TICKS.toFloat()

                    val colorMatrix = ColorMatrix()
                    val satVal = fader(DARK_RATIO)(tick)
                    colorMatrix.setScale(satVal, satVal, satVal, 1f)

                    val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
                    val paint = Paint()
                    paint.colorFilter = colorMatrixColorFilter

                    c.drawBitmap(
                        shipBackground!!,
                        Rect(
                            midx - c.width / 2, midy - c.height / 2,
                            midx + c.width / 2, midy + c.height / 2
                        ),
                        Rect(
                            0, 0,
                            c.width, c.height
                        ),
                        paint
                    )

                    totalBgDrawTime += System.currentTimeMillis() - startTime

                    for (i in animatedObjects.indices) {
                        animatedObjects[i].draw(c, tick)
                    }
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c)
                totalDrawTime += System.currentTimeMillis() - startTime
                if (++framesCycled == 120) {
                    /*
                    Log.i(
                        "drawFrame",
                        String.format("Avg draw bg time: %d", totalBgDrawTime / framesCycled)
                    )
                     */
                    Log.i("drawFrame", String.format("Avg drew frame time: %d", totalDrawTime / framesCycled))
                    totalBgDrawTime = 0
                    totalDrawTime = 0
                    framesCycled = 0
                }

            }
            if (visible) {
                scheduleRedrawLater(System.currentTimeMillis() - startTime)
            }
        }

        private suspend fun decodeSampledBitmapFromResource(
            resId: Int, scale: Float?, reqWidth: Int = 0, reqHeight: Int = 0
        ): Bitmap {
            Log.i(
                "decodeSampledBitmapFromResource", String.format(
                    "%s: sizing image to scale: %f w:%d h:%d",
                    Calendar.getInstance().time, scale, reqWidth, reqHeight
                )
            )

            var rw = reqWidth
            var rh = reqHeight
            val opts = BitmapFactory.Options()
            opts.inPreferredConfig
            val bitmap = BitmapFactory.decodeResource(resources, resId, opts)
            if (scale != null) {
                rw = (scale * opts.outWidth).toInt()
                rh = (scale * opts.outHeight).toInt()
            }

            return Bitmap.createScaledBitmap(bitmap, rw, rh, false)
        }

        private fun distanceFromMidnight(): Float {
            var now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE)
            if (fixTime) {
                now = fixedTime * 60
            }
            val noon = 12 * 60
            if (now > noon) {
                return Math.abs((now - 2 * noon).toFloat() / noon.toFloat())
            }
            return now.toFloat() / noon.toFloat()
        }

        private fun fader(ratio: Float) : (Float) -> Float = { tick: Float ->
            if (!dayNight) 1f else logCurve(distanceFromMidnight()) * ratio + 1f - ratio
        }

        private fun boringCurve(x: Float): Float {
            return x
        }

        private fun logCurve(x: Float): Float {
            return log10(x + 0.1f) + 0.95f
        }
    }

    companion object {
        val MAXRAD = Math.toRadians(360.0)
        const val MAX_TICKS = 1048576
        const val MS_BETWEEN_FRAMES = 16

        private const val SHIPX = 1120f
        private const val SHIPY = 40f

        private const val FLAGX = 1880f
        private const val FLAGY = 245f

        private const val FRONTCRESTX = 0f
        private const val FRONTCRESTY = 1300f
        private const val REARCRESTX = 2200f
        private const val REARCRESTY = 1100f

        private const val WAVE1X = -3270f
        private const val WAVE1Y = 1245f
        private const val WAVE2X = -3270f
        private const val WAVE2Y = 1245f
        private const val WAVE3X = -3270f
        private const val WAVE3Y = 1245f
        private const val WAVE4X = -3270f
        private const val WAVE4Y = 1245f

        private const val CLOUD1X = 0f
        private const val CLOUD1Y = 0f

        private const val BIRD1X = 40f
        private const val BIRD1Y = 900f
        private const val BIRD2X = 1200f
        private const val BIRD2Y = 800f
        private const val BIRD3X = 2700f
        private const val BIRD3Y = 1100f

        private const val DARK_RATIO = 0.96f
        private const val MEDIUM_RATIO = 0.8f
        private const val LIGHT_RATIO = 0.65f

        internal class AnimHandler(service: WallpaperService): Handler(Looper.myLooper()!!) {
            private val myService: WeakReference<WallpaperService> = WeakReference(service)

            override fun handleMessage(msg: Message) {
                val service = myService.get()
                service?: return
                super.handleMessage(msg)
            }
        }
    }
}

data class SurfaceLimits( val width: Int, val height: Int )

data class ViewRect( val width: Int, val height: Int, val xoff: Int, val yoff: Int)