package com.aibuilders.lockscreen

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class CountdownWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = CountdownEngine()

    inner class CountdownEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 1080
        private var height = 2400

        private val rainChars = "0123456789ABCDEF01アイウエオカキクケコ"
        private val fontSize = 34f
        private var drops = IntArray(0)

        private val bgPaint = Paint().apply { color = Color.parseColor("#040814") }
        private val rainTrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSize
            typeface = Typeface.MONOSPACE
        }
        private val rainHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSize
            typeface = Typeface.MONOSPACE
        }
        private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 10, 18, 36)
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val digitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    handler.postDelayed(this, 50L) // ~20 FPS smooth battery-friendly rain
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = max(1, w)
            height = max(1, h)
            val cols = max(1, (width / fontSize).toInt())
            drops = IntArray(cols) { Random.nextInt(-30, (height / fontSize).toInt().coerceAtLeast(1)) }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.post(drawRunnable)
            }
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawRunnable)
            super.onDestroy()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas == null) return

                val prefs = applicationContext.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val rainEnabled = prefs.getBoolean(MainActivity.KEY_RAIN, true)
                val theme = prefs.getString(MainActivity.KEY_THEME, "cyber") ?: "cyber"
                val targetIso = prefs.getString(MainActivity.KEY_DEADLINE, MainActivity.DEADLINE_DEC2) ?: MainActivity.DEADLINE_DEC2
                val nextMilestone = prefs.getString(MainActivity.KEY_MILESTONE, "Nov 17 — Complete 15-min IBM SkillsBuild intro course") ?: ""

                val accentColor = when (theme) {
                    "glass" -> Color.parseColor("#A78BFA")
                    "retro" -> Color.parseColor("#34D399")
                    else -> Color.parseColor("#38BDF8") // Cyber Cyan
                }
                val headColor = when (theme) {
                    "glass" -> Color.parseColor("#EDE9FE")
                    "retro" -> Color.parseColor("#FEF08A")
                    else -> Color.parseColor("#E0F2FE")
                }

                // 1. Background
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // 2. Digital Rain
                if (rainEnabled && drops.isNotEmpty()) {
                    rainTrailPaint.color = accentColor
                    rainTrailPaint.alpha = 140
                    rainHeadPaint.color = headColor
                    rainHeadPaint.alpha = 230

                    for (i in drops.indices) {
                        val x = i * fontSize
                        val y = drops[i] * fontSize
                        val ch = rainChars[Random.nextInt(rainChars.length)].toString()
                        canvas.drawText(ch, x, y, if (Random.nextFloat() > 0.85f) rainHeadPaint else rainTrailPaint)

                        // Draw subtle trail
                        for (t in 1..4) {
                            val ty = y - t * fontSize
                            if (ty > 0) {
                                rainTrailPaint.alpha = max(20, 120 - t * 25)
                                val tch = rainChars[Random.nextInt(rainChars.length)].toString()
                                canvas.drawText(tch, x, ty, rainTrailPaint)
                            }
                        }

                        if (y > height && Random.nextFloat() > 0.965f) {
                            drops[i] = 0
                        }
                        drops[i]++
                    }
                }

                // 3. Adaptive Countdown Card (fits Z Fold 7 cover & unfolded screens)
                val isWide = width > height * 0.85f
                val cardWidth = min(width * 0.90f, if (isWide) 1100f else 940f)
                val cardHeight = if (isWide) 460f else 520f
                val left = (width - cardWidth) / 2f
                val top = height * (if (isWide) 0.34f else 0.38f)
                val rect = RectF(left, top, left + cardWidth, top + cardHeight)

                borderPaint.color = accentColor
                borderPaint.alpha = 120
                canvas.drawRoundRect(rect, 36f, 36f, cardPaint)
                canvas.drawRoundRect(rect, 36f, 36f, borderPaint)

                // Header
                titlePaint.color = Color.WHITE
                titlePaint.textSize = if (isWide) 40f else 34f
                canvas.drawText("AI BUILDERS CHALLENGE NA", width / 2f, top + 78f, titlePaint)

                labelPaint.color = accentColor
                labelPaint.textSize = 24f
                canvas.drawText("IBM SKILLSBUILD × BEMYAPP", width / 2f, top + 116f, labelPaint)

                // Live Countdown Math
                val targetMs = try {
                    ZonedDateTime.parse(targetIso).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    Instant.now().toEpochMilli()
                }
                val diff = max(0L, targetMs - System.currentTimeMillis())
                val days = (diff / 86_400_000L).toInt()
                val hours = ((diff / 3_600_000L) % 24).toInt()
                val mins = ((diff / 60_000L) % 60).toInt()
                val secs = ((diff / 1_000L) % 60).toInt()

                val units = listOf(
                    "%02d".format(days) to "DAYS",
                    "%02d".format(hours) to "HRS",
                    "%02d".format(mins) to "MIN",
                    "%02d".format(secs) to "SEC"
                )

                val boxGap = 18f
                val boxW = (cardWidth - 80f - boxGap * 3f) / 4f
                val boxTop = top + 150f
                val boxH = 170f
                digitPaint.textSize = if (isWide) 68f else 56f

                units.forEachIndexed { idx, (num, lbl) ->
                    val bx = left + 40f + idx * (boxW + boxGap)
                    val bRect = RectF(bx, boxTop, bx + boxW, boxTop + boxH)
                    canvas.drawRoundRect(bRect, 22f, 22f, bgPaint)
                    canvas.drawRoundRect(bRect, 22f, 22f, borderPaint)

                    digitPaint.color = if (idx == 0 || idx == 3) accentColor else Color.WHITE
                    canvas.drawText(num, bx + boxW / 2f, boxTop + 98f, digitPaint)

                    labelPaint.color = Color.parseColor("#94A3B8")
                    labelPaint.textSize = 22f
                    canvas.drawText(lbl, bx + boxW / 2f, boxTop + 142f, labelPaint)
                }

                // Next Milestone Footer
                labelPaint.color = Color.parseColor("#E2E8F0")
                labelPaint.textSize = 25f
                val clipped = if (nextMilestone.length > 46) nextMilestone.take(45) + "…" else nextMilestone
                canvas.drawText("NEXT: $clipped", width / 2f, top + cardHeight - 54f, labelPaint)

            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
