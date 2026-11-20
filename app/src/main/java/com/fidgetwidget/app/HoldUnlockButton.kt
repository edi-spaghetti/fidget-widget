package com.fidgetwidget.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil

/**
 * Visible padlock button. Press and hold for [holdMs]: a ring fills around it, a countdown shows
 * in the middle and the phone ticks once per second. Letting go early drains the ring back down.
 */
class HoldUnlockButton(
    context: Context,
    private val onHoldChanged: (holding: Boolean) -> Unit,
    private val onComplete: () -> Unit
) : View(context) {

    private val holdMs = 5000L
    private val d = resources.displayMetrics.density
    private val haptics = Haptics(context)

    private var holding = false
    private var holdStart = 0L
    private var progress = 0f
    private var lastSecondShown = -1
    private var lastFrame = 0L

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * d; color = 0x88FFFFFF.toInt()
    }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5 * d; color = 0x33FFFFFF
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5 * d; strokeCap = Paint.Cap.ROUND; color = 0xFFFFD23D.toInt()
    }
    private val lockFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFFFFF.toInt() }
    private val lockStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3 * d; color = 0xEEFFFFFF.toInt(); strokeCap = Paint.Cap.ROUND
    }
    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD23D.toInt(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val rect = RectF()

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> startHold()
            MotionEvent.ACTION_MOVE -> {
                val slop = 24 * d
                if (e.x < -slop || e.y < -slop || e.x > width + slop || e.y > height + slop) endHold()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endHold()
        }
        return true
    }

    private fun startHold() {
        if (holding) return
        holding = true
        // Resume from wherever the draining ring currently is.
        holdStart = SystemClock.uptimeMillis() - (progress * holdMs).toLong()
        lastSecondShown = -1
        haptics.tick()
        onHoldChanged(true)
        invalidate()
    }

    private fun endHold() {
        if (!holding) return
        holding = false
        lastFrame = 0L
        onHoldChanged(false)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        if (holding) {
            progress = ((now - holdStart).toFloat() / holdMs).coerceAtMost(1f)
            val secondsLeft = ceil((1f - progress) * holdMs / 1000f).toInt()
            if (secondsLeft != lastSecondShown) {
                if (lastSecondShown != -1) haptics.tick()
                lastSecondShown = secondsLeft
            }
            if (progress >= 1f) {
                holding = false
                progress = 0f
                haptics.success()
                onHoldChanged(false)
                onComplete()
                invalidate()
                return
            }
        } else if (progress > 0f) {
            val dt = if (lastFrame == 0L) 16f else (now - lastFrame).toFloat()
            progress = (progress - dt / 600f).coerceAtLeast(0f)  // drain in ~0.6 s
        }
        lastFrame = now

        val cx = width / 2f
        val cy = height / 2f
        val r = width / 2f - 4 * d
        bg.color = if (holding) 0xAA000000.toInt() else 0x66000000
        canvas.drawCircle(cx, cy, r, bg)
        canvas.drawCircle(cx, cy, r, border)

        rect.set(cx - r + 6 * d, cy - r + 6 * d, cx + r - 6 * d, cy + r - 6 * d)
        if (progress > 0f) {
            canvas.drawOval(rect, track)
            canvas.drawArc(rect, -90f, 360f * progress, false, arc)
        }

        if (holding) {
            numPaint.textSize = r * 0.9f
            val n = ceil((1f - progress) * holdMs / 1000f).toInt().coerceAtLeast(1)
            canvas.drawText(n.toString(), cx, cy - (numPaint.descent() + numPaint.ascent()) / 2, numPaint)
        } else {
            drawPadlock(canvas, cx, cy, r * 0.42f)
        }

        if (holding || progress > 0f) postInvalidateOnAnimation()
    }

    private fun drawPadlock(c: Canvas, cx: Float, cy: Float, s: Float) {
        // Shackle
        rect.set(cx - s * 0.55f, cy - s * 1.05f, cx + s * 0.55f, cy + s * 0.05f)
        c.drawArc(rect, 180f, 180f, false, lockStroke)
        c.drawLine(cx - s * 0.55f, cy - s * 0.5f, cx - s * 0.55f, cy - s * 0.1f, lockStroke)
        c.drawLine(cx + s * 0.55f, cy - s * 0.5f, cx + s * 0.55f, cy - s * 0.1f, lockStroke)
        // Body
        rect.set(cx - s * 0.85f, cy - s * 0.15f, cx + s * 0.85f, cy + s * 0.95f)
        c.drawRoundRect(rect, s * 0.2f, s * 0.2f, lockFill)
        // Keyhole
        bg.color = 0xFF241B5C.toInt()
        c.drawCircle(cx, cy + s * 0.32f, s * 0.14f, bg)
    }
}
