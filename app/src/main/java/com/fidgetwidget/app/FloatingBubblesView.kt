package com.fidgetwidget.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Soap bubbles drift up the screen; tapping one pops it with a sound, a buzz and a burst of droplets. */
class FloatingBubblesView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private fun rand(a: Float, b: Float) = a + Random.nextFloat() * (b - a)

    private val minR = dp(26f)
    private val maxR = dp(62f)

    private class Bubble(
        var baseX: Float, var y: Float, val r: Float, val color: Int,
        val speed: Float, val drift: Float,
        val wobAmp: Float, val wobFreq: Float, val phase: Float,
        val shader: Shader, var t: Float = 0f
    ) {
        val x get() = baseX + wobAmp * sin(wobFreq * t + phase)
    }

    private class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        val size: Float, val color: Int, val life: Float, var age: Float = 0f
    )

    private class Ring(val x: Float, val y: Float, val r: Float, val color: Int, var age: Float = 0f)

    private val bubbles = ArrayList<Bubble>()
    private val particles = ArrayList<Particle>()
    private val rings = ArrayList<Ring>()

    private val palette = intArrayOf(
        0xFFFF5C7A.toInt(), 0xFFFF9F45.toInt(), 0xFFFFD23D.toInt(),
        0xFF4FD1A5.toInt(), 0xFF5CA8FF.toInt(), 0xFFA77BFF.toInt(), 0xFF5FE3F0.toInt()
    )

    private val bgPaint = Paint()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()

    private var targetCount = 12
    private var spawnTimer = 0f
    private var lastFrame = 0L

    private val haptics = Haptics(context)
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val popSound = soundPool.load(context, R.raw.pop, 1)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        bgPaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
            0xFF241B5C.toInt(), 0xFF0F0B2E.toInt(), Shader.TileMode.CLAMP)
        targetCount = ((w.toFloat() * h) / (dp(110f) * dp(110f)) * 0.55f).toInt().coerceIn(8, 18)
        bubbles.clear()
        // Start with bubbles already scattered over the screen.
        repeat(targetCount) { bubbles.add(newBubble(rand(0f, h.toFloat()))) }
    }

    private fun newBubble(y: Float): Bubble {
        val r = rand(minR, maxR)
        val color = palette[Random.nextInt(palette.size)]
        val shader = RadialGradient(
            0f, 0f, r,
            intArrayOf(withAlpha(color, 0x12), withAlpha(color, 0x38), withAlpha(color, 0xB0)),
            floatArrayOf(0f, 0.72f, 1f), Shader.TileMode.CLAMP
        )
        // Bigger bubbles rise more slowly.
        val speed = dp(rand(45f, 75f)) * (1.6f - (r - minR) / (maxR - minR) * 0.7f)
        return Bubble(
            baseX = rand(r, width - r), y = y, r = r, color = color,
            speed = speed, drift = dp(rand(-12f, 12f)),
            wobAmp = dp(rand(8f, 26f)), wobFreq = rand(0.8f, 1.8f), phase = rand(0f, 6.28f),
            shader = shader
        )
    }

    private fun withAlpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a shl 24)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                tryPop(e.getX(e.actionIndex), e.getY(e.actionIndex))
            MotionEvent.ACTION_MOVE ->
                for (i in 0 until e.pointerCount) tryPop(e.getX(i), e.getY(i))
        }
        return true
    }

    private fun tryPop(x: Float, y: Float) {
        // Topmost (last drawn) first.
        for (i in bubbles.indices.reversed()) {
            val b = bubbles[i]
            if (b.t < 0.15f) continue
            val bx = b.x
            if (hypot(x - bx, y - b.y) <= b.r * 1.12f) {
                bubbles.removeAt(i)
                pop(bx, b.y, b)
                return
            }
        }
    }

    private fun pop(x: Float, y: Float, b: Bubble) {
        val sizeT = (b.r - minR) / (maxR - minR)           // 0 = small, 1 = big
        val rate = (1.55f - sizeT * 0.8f) * rand(0.94f, 1.06f)
        soundPool.play(popSound, 1f, 1f, 1, 0, rate.coerceIn(0.5f, 2f))
        if (sizeT > 0.6f) haptics.click() else haptics.tick()

        val n = (14 + b.r / density / 3).toInt()
        repeat(n) {
            val a = rand(0f, (2 * PI).toFloat())
            val s = dp(rand(140f, 420f)) * (0.7f + sizeT * 0.5f)
            val sx = x + cos(a) * b.r * 0.85f
            val sy = y + sin(a) * b.r * 0.85f
            val c = if (Random.nextFloat() < 0.3f) Color.WHITE else b.color
            particles.add(Particle(sx, sy, cos(a) * s, sin(a) * s - dp(60f),
                dp(rand(2.5f, 5.5f)), c, rand(0.45f, 0.85f)))
        }
        rings.add(Ring(x, y, b.r, b.color))
        invalidate()
    }

    private fun update(dt: Float) {
        val h = height.toFloat()
        val w = width.toFloat()

        val it = bubbles.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.t += dt
            b.y -= b.speed * dt
            b.baseX += b.drift * dt
            if (b.y + b.r < -dp(10f) || b.baseX < -b.r * 2 || b.baseX > w + b.r * 2) it.remove()
        }

        spawnTimer -= dt
        if (bubbles.size < targetCount && spawnTimer <= 0f) {
            val nb = newBubble(0f)
            nb.y = h + nb.r + dp(4f)
            nb.t = 1f // already "grown in" since it enters from off-screen
            bubbles.add(nb)
            spawnTimer = rand(0.25f, 0.7f)
        }

        val gravity = dp(900f)
        val pit = particles.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            p.age += dt
            if (p.age >= p.life) { pit.remove(); continue }
            p.vx *= (1f - 1.8f * dt)
            p.vy = p.vy * (1f - 1.8f * dt) + gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }

        val rit = rings.iterator()
        while (rit.hasNext()) {
            val r = rit.next()
            r.age += dt
            if (r.age > 0.35f) rit.remove()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 0.016f else ((now - lastFrame) / 1e9f).coerceIn(0f, 0.05f)
        lastFrame = now
        update(dt)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        for (b in bubbles) {
            val grow = (b.t / 0.35f).coerceAtMost(1f)
            val s = if (grow < 1f) easeOutBack(grow) else 1f
            canvas.save()
            canvas.translate(b.x, b.y)
            canvas.scale(s, s)
            fillPaint.shader = b.shader
            canvas.drawCircle(0f, 0f, b.r, fillPaint)
            rimPaint.color = withAlpha(b.color, 0xE0)
            rimPaint.strokeWidth = dp(2f)
            canvas.drawCircle(0f, 0f, b.r, rimPaint)
            // Iridescent inner sheen on the lower-right edge.
            rimPaint.color = 0x55FFFFFF
            rimPaint.strokeWidth = dp(3f)
            rect.set(-b.r * 0.86f, -b.r * 0.86f, b.r * 0.86f, b.r * 0.86f)
            canvas.drawArc(rect, 10f, 80f, false, rimPaint)
            // Highlights.
            shinePaint.alpha = 0xB0
            rect.set(-b.r * 0.62f, -b.r * 0.66f, -b.r * 0.18f, -b.r * 0.36f)
            canvas.save()
            canvas.rotate(-30f, -b.r * 0.4f, -b.r * 0.5f)
            canvas.drawOval(rect, shinePaint)
            canvas.restore()
            shinePaint.alpha = 0x90
            canvas.drawCircle(-b.r * 0.12f, -b.r * 0.62f, b.r * 0.07f, shinePaint)
            canvas.restore()
        }

        for (r in rings) {
            val t = r.age / 0.35f
            // Quick flash, then an expanding ring.
            if (t < 0.35f) {
                dotPaint.color = withAlpha(Color.WHITE, ((1f - t / 0.35f) * 110).toInt())
                canvas.drawCircle(r.x, r.y, r.r * (1f + 0.2f * t), dotPaint)
            }
            ringPaint.color = withAlpha(r.color, ((1f - t) * 230).toInt())
            ringPaint.strokeWidth = dp(5f) * (1f - t) + 1f
            canvas.drawCircle(r.x, r.y, r.r * (1f + 0.6f * t), ringPaint)
        }

        for (p in particles) {
            val f = 1f - p.age / p.life
            dotPaint.color = withAlpha(p.color, (f * 255).toInt())
            canvas.drawCircle(p.x, p.y, p.size * (0.4f + 0.6f * f), dotPaint)
        }

        postInvalidateOnAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        lastFrame = 0L
    }

    private fun easeOutBack(t: Float): Float {
        val s = 1.9f
        val u = t - 1f
        return u * u * ((s + 1) * u + s) + 1f
    }

    fun release() = soundPool.release()
}
