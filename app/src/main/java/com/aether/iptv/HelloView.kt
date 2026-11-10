package com.aether.iptv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup

/**
 * Apple-style "hello" splash: a greeting is written in with a soft left-to-right sweep,
 * held, faded, then the next language. Malayalam (നമസ്കാരം) always comes first.
 * Shown only during the very first channel load; call dismiss() as soon as channels are ready.
 */
class HelloView(ctx: Context) : View(ctx) {
    private class Greet(val text: String, val face: Typeface)

    private val greets = ArrayList<Greet>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mask = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val soft = 260f
    private val maskShader = LinearGradient(0f, 0f, soft, 0f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
    private val matrix = Matrix()
    private val colors = intArrayOf(c(0xFFFF9F0A), c(0xFFFF375F), c(0xFFBF5AF2), c(0xFF0A84FF))
    private val t0 = SystemClock.uptimeMillis()
    private var running = true
    private var shownIdx = -1
    private var textX = 0f
    private var textW = 0f
    private var baseline = 0f

    init {
        setBackgroundColor(Color.BLACK)
        var assetOk = true
        val bundled = try { Typeface.createFromAsset(ctx.assets, "hello.ttf") } catch (_: Exception) { assetOk = false; Typeface.DEFAULT }
        val script = Typeface.create("cursive", Typeface.NORMAL)

        fun system(t: String): Boolean {            // CJK etc: only if the device font can draw it
            if (Build.VERSION.SDK_INT < 23) return false
            paint.typeface = Typeface.DEFAULT
            var i = 0
            while (i < t.length) {
                val cp = t.codePointAt(i)
                if (!paint.hasGlyph(String(Character.toChars(cp)))) return false
                i += Character.charCount(cp)
            }
            return true
        }
        fun add(t: String, f: Typeface, ok: Boolean = true) { if (ok) greets.add(Greet(t, f)) }

        add("നമസ്കാരം", bundled, assetOk)           // Malayalam – always first
        add("hello", script)
        add("नमस्ते", bundled, assetOk)
        add("bonjour", script)
        add("வணக்கம்", bundled, assetOk)
        add("hola", script)
        add("こんにちは", Typeface.DEFAULT, system("こんにちは"))
        add("ciao", script)
        add("你好", Typeface.DEFAULT, system("你好"))
        add("hallo", script)
        add("안녕하세요", Typeface.DEFAULT, system("안녕하세요"))
        add("olá", script)
        if (greets.isEmpty()) greets.add(Greet("hello", script))
    }

    private fun prepare(g: Greet, w: Float, h: Float) {
        paint.typeface = g.face
        paint.textSize = h * 0.18f
        var tw = paint.measureText(g.text)
        if (tw > w * 0.8f) { paint.textSize *= (w * 0.8f) / tw; tw = paint.measureText(g.text) }
        textW = tw
        textX = (w - tw) / 2f
        baseline = h / 2f - (paint.ascent() + paint.descent()) / 2f
        paint.shader = LinearGradient(textX, 0f, textX + tw, 0f, colors, null, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val el = SystemClock.uptimeMillis() - t0
        val cycle = 3000L
        val idx = ((el / cycle) % greets.size).toInt()
        val lt = (el % cycle).toFloat()
        if (idx != shownIdx) { prepare(greets[idx], w, h); shownIdx = idx }

        val p = (lt / 1700f).coerceIn(0f, 1f)
        val eased = p * p * (3f - 2f * p)
        val edge = textX + ((textW + soft) * eased)
        val fade = if (lt > 2650f) 1f - (lt - 2650f) / 350f else 1f
        paint.alpha = (255 * fade.coerceIn(0f, 1f)).toInt()

        val sc = canvas.saveLayer(0f, 0f, w, h, null)
        canvas.drawText(greets[idx].text, textX, baseline, paint)
        matrix.setTranslate(edge - soft, 0f)
        maskShader.setLocalMatrix(matrix)
        mask.shader = maskShader
        canvas.drawRect(0f, 0f, w, h, mask)
        canvas.restoreToCount(sc)

        if (running) postInvalidateOnAnimation()
    }

    fun dismiss() {
        running = false
        animate().alpha(0f).setDuration(220).withEndAction { (parent as? ViewGroup)?.removeView(this) }.start()
    }
}
