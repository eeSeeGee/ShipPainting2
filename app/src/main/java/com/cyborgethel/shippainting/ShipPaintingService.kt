package com.cyborgethel.shippainting

import android.content.res.Resources
import android.graphics.*
import android.os.Handler
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import com.cyborgethel.shippainting.AnimatedObject.AnimOffset
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log10

class ShipPaintingService : WallpaperService() {
    private val handler = Handler()
    private var animatedObjects: ArrayList<AnimatedObject> = ArrayList()
    var shipBackground: Bitmap? = null
    private var offset = 0f
    private var vx = 0f
    private var vy = 0f
    private var startx = 0
    private var starty = 0
    private var visible = false
    private var dayNight = true
    private var fixTime = false
    private var fixedTime = 0

    override fun onCreateEngine(): Engine {
        return ShipEngine()
    }

    internal inner class ShipEngine : Engine() {
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawShipRunnable)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this@ShipPaintingService.visible = visible

            val prefs = PreferenceManager.getDefaultSharedPreferences(this@ShipPaintingService)
            dayNight = prefs.getBoolean("day_night", true)
            fixTime = prefs.getBoolean("fix_time", false)
            fixedTime = prefs.getInt("time_of_day", 0)

            if (visible) {
                drawFrame()
            } else {
                handler.removeCallbacks(drawShipRunnable)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder, format: Int,
            width: Int, height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.i("onSurfaceChanged", String.format("size: w:%d h:%d", width, height))
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(
                resources,
                R.drawable.shipbg, options
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
                resources,
                R.drawable.shipbg,
                (scale * imageWidth.toFloat()).toInt(),
                (scale * imageHeight.toFloat()).toInt()
            )
            Log.i(
                "onSurfaceChanged", String.format(
                    "bg size: w:%d h:%d",
                    shipBackground!!.width, shipBackground!!.height
                )
            )
            startx = (width - shipBackground!!.width) / 2
            starty = (height - shipBackground!!.height) / 2
            BitmapFactory.decodeResource(
                resources,
                R.drawable.ship, options
            )
            val ship = decodeSampledBitmapFromResource(
                resources,
                R.drawable.ship,
                (scale * options.outWidth).toInt(),
                (scale * options.outHeight).toInt()
            )
            BitmapFactory.decodeResource(
                resources,
                R.drawable.wave, options
            )
            val wave = decodeSampledBitmapFromResource(
                resources,
                R.drawable.wave,
                (scale * options.outWidth).toInt(),
                (scale * options.outHeight).toInt()
            )
            BitmapFactory.decodeResource(
                resources,
                R.drawable.wavetop, options
            )
            val wavetop = decodeSampledBitmapFromResource(
                resources,
                R.drawable.wavetop,
                (scale * options.outWidth).toInt(),
                (scale * options.outHeight).toInt()
            )
            BitmapFactory.decodeResource(
                resources,
                R.drawable.cloud1, options
            )
            val cloud1 = decodeSampledBitmapFromResource(
                resources,
                R.drawable.cloud1,
                (scale * options.outWidth).toInt(),
                (scale * options.outHeight).toInt()
            )
            BitmapFactory.decodeResource(
                resources,
                R.drawable.cloud2, options
            )
            val cloud2 = decodeSampledBitmapFromResource(
                resources,
                R.drawable.cloud2,
                (scale * options.outWidth).toInt(),
                (scale * options.outHeight).toInt()
            )
            vx = shipBackground!!.width.toFloat()
            vy = shipBackground!!.height.toFloat()
            animatedObjects = ArrayList()
            val cl1 = ArrayList<Bitmap>()
            cl1.add(cloud1)
            animatedObjects.add(
                AnimatedObject(
                    cl1, CLOUD1X, CLOUD1Y, vx, vy,
                    CloudAnimator(cloud1.width, cloud1.height), fader(MEDIUM_RATIO)
                )
            )
            val cl2 = ArrayList<Bitmap>()
            cl2.add(cloud1)
            animatedObjects.add(
                AnimatedObject(
                    cl2, CLOUD2X, CLOUD2Y, vx, vy,
                    CloudAnimator(cloud2.width, cloud2.height), fader(MEDIUM_RATIO)
                )
            )
            val w2 = ArrayList<Bitmap>()
            w2.add(wavetop)
            animatedObjects.add(
                AnimatedObject(
                    w2, WAVETOPX, WAVETOPY, vx, vy,
                    WaveAnimator(wavetop.width, wavetop.height), fader(MEDIUM_RATIO)
                )
            )
            val sl = ArrayList<Bitmap>()
            sl.add(ship)
            animatedObjects.add(
                AnimatedObject(
                    sl, SHIPX, SHIPY, vx, vy,
                    ShipAnimator(ship.width, ship.height), fader(LIGHT_RATIO)
                )
            )
            val wl = ArrayList<Bitmap>()
            wl.add(wave)
            animatedObjects.add(
                AnimatedObject(
                    wl, WAVEX, WAVEY, vx, vy,
                    WaveAnimator(wave.width, wave.height), fader(MEDIUM_RATIO)
                )
            )
            drawFrame()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawShipRunnable)
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xStep: Float,
            yStep: Float, xPixels: Int, yPixels: Int
        ) {
            // Log.i("Ship", "Offset:" + xOffset);
            offset = xOffset
            drawFrame()
        }

        fun drawFrame() {
            val holder = surfaceHolder
            var c: Canvas? = null
            try {
                c = holder.lockCanvas()
                if (c != null) {
                    val tick = (SystemClock.elapsedRealtime() % 5000).toFloat() / 5000f

                    val colorMatrix = ColorMatrix()
                    val satVal = fader(DARK_RATIO)(tick)
                    colorMatrix.setScale(satVal, satVal, satVal, 1f)

                    val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
                    val paint = Paint()
                    paint.colorFilter = colorMatrixColorFilter

                    c.drawBitmap(
                        shipBackground!!,
                        Rect(
                            startx, starty,
                            shipBackground!!.width, shipBackground!!.height
                        ),
                        Rect(
                            startx, starty,
                            shipBackground!!.width, shipBackground!!.height
                        ),
                        paint
                    )

                    for (i in animatedObjects.indices) {
                        animatedObjects[i].draw(c, startx, starty, tick)
                    }
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c)
            }
            handler.removeCallbacks(drawShipRunnable)
            if (visible) {
                handler.postDelayed(drawShipRunnable, (1000 / 25).toLong())
            }
        }

        private val drawShipRunnable = Runnable { drawFrame() }

        private inner class ShipAnimator     // _w = w;
            (
            w: Int, // int _w;
            var _h: Int
        ) : AnimOffset {
            override fun getX(tick: Float): Int {
                return 0
            }

            override fun getY(tick: Float): Int {
                val highness = 0.025f * _h
                return (2 * highness).toInt() + (Math.sin(tick * MAXRAD) * highness / 3f).toInt()
            }
        }

        private inner class WaveAnimator(var _w: Int, var _h: Int) : AnimOffset {
            override fun getX(tick: Float): Int {
                val wideness = 0.01f * _w
                return (Math.sin(tick * MAXRAD) * wideness).toInt()
            }

            override fun getY(tick: Float): Int {
                val highness = 0.015f * _h
                return (highness / 2f).toInt() + (Math.sin(2 * tick * MAXRAD) * highness / 3f).toInt()
            }
        }

        private inner class CloudAnimator(
            var w: Int, h: Int
        ) : AnimOffset {
            override fun getX(tick: Float): Int {
                return ((offset - 0.5f) * w).toInt()
            }

            override fun getY(tick: Float): Int {
                return 0
            }
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int, reqHeight: Int
        ): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2
                // and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
                ) {
                    inSampleSize *= 2
                }
            } else if (height < reqHeight || width < reqWidth) {
                return -1
            }
            return inSampleSize
        }

        private fun decodeSampledBitmapFromResource(
            res: Resources, resId: Int, reqWidth: Int, reqHeight: Int
        ): Bitmap {
            Log.i(
                "decodeSampledBitmapFromResource", String.format(
                    "sizing image to w:%d h:%d",
                    reqWidth, reqHeight
                )
            )

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(
                options, reqWidth,
                reqHeight
            )
            if (options.inSampleSize == -1) {
                return Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(resources, resId),
                    reqWidth, reqHeight, false
                )
            }

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeResource(res, resId, options)
        }
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

    companion object {
        private const val SHIPX = 0.37f
        private const val SHIPY = 0.055f
        private const val WAVETOPX = -0.2f
        private const val WAVETOPY = 0.64f
        private const val WAVEX = -0.5f
        private const val WAVEY = 0.627f
        private const val CLOUD1X = 0.131f
        private const val CLOUD1Y = 0.136f
        private const val CLOUD2X = 0.666f
        private const val CLOUD2Y = 0.254f
        private val MAXRAD = Math.toRadians(360.0)

        private const val DARK_RATIO = 0.96f
        private const val MEDIUM_RATIO = 0.8f
        private const val LIGHT_RATIO = 0.65f
    }
}