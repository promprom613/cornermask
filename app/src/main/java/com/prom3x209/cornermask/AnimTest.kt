package com.prom3x209.cornermask

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import java.util.Locale
import java.util.Random

object AnimTest {
    const val ACTION_TEST = "com.prom3x209.cornermask.TEST_NEXT"

    private class Frame(
        val at: Long,
        val text: CharSequence,
        val tag: String = "",
        val color: Int = Color.WHITE,
        val alpha: Float = 1f
    )

    private class Stat {
        var n = 0
        var jSum = 0L
        var jMax = 0L
        var cSum = 0L
        var cMax = 0L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val token = Any()
    private var index = 0
    private var cachedPi: PendingIntent? = null

    private val tests: List<Pair<String, () -> List<Frame>>> = listOf(
        "rate sweep" to { rateSweep() },
        "kernel log" to { kernelLog() },
        "scramble" to { scramble() },
        "blink + fade" to { blinkFade() },
        "styles" to { styles() }
    )

    fun tapPi(ctx: Context): PendingIntent {
        cachedPi?.let { return it }
        val i = Intent(ctx, GreetingWidget::class.java).setAction(ACTION_TEST)
        val pi = PendingIntent.getBroadcast(
            ctx, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        cachedPi = pi
        return pi
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(token)
    }

    fun next(ctx: Context) {
        val app = ctx.applicationContext
        GreetingController.pause()
        cancel()
        val i = index % tests.size
        index++
        run(app, "T${i + 1} ${tests[i].first}", tests[i].second())
    }

    private fun run(ctx: Context, title: String, frames: List<Frame>) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, GreetingWidget::class.java))
        if (ids.isEmpty()) return
        val start = SystemClock.uptimeMillis()
        val stats = LinkedHashMap<String, Stat>()
        show(ctx, mgr, ids, "", Color.WHITE, 1f)
        for (fr in frames) {
            handler.postAtTime({
                val now = SystemClock.uptimeMillis()
                val c0 = System.nanoTime()
                show(ctx, mgr, ids, fr.text, fr.color, fr.alpha)
                val call = (System.nanoTime() - c0) / 1000
                if (fr.tag.isNotEmpty()) {
                    val s = stats.getOrPut(fr.tag) { Stat() }
                    val late = maxOf(0L, now - (start + fr.at))
                    s.n++
                    s.jSum += late
                    if (late > s.jMax) s.jMax = late
                    s.cSum += call
                    if (call > s.cMax) s.cMax = call
                }
            }, token, start + fr.at)
        }
        val end = frames.maxOf { it.at } + 900
        handler.postAtTime({
            show(ctx, mgr, ids, summary(title, stats), Color.WHITE, 1f)
        }, token, start + end)
    }

    private fun show(
        ctx: Context, mgr: AppWidgetManager, ids: IntArray,
        text: CharSequence, color: Int, alpha: Float
    ) {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_greeting)
        rv.setTextViewText(R.id.greeting_text, text)
        rv.setTextColor(R.id.greeting_text, color)
        rv.setFloat(R.id.greeting_text, "setAlpha", alpha)
        rv.setOnClickPendingIntent(R.id.root, tapPi(ctx))
        mgr.updateAppWidget(ids, rv)
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.1f", v)

    private fun summary(title: String, stats: Map<String, Stat>): String {
        val sb = StringBuilder(title).append("\nlate/call ms avg/max")
        for ((tag, s) in stats) {
            if (s.n == 0) continue
            sb.append('\n').append(tag.padEnd(7))
                .append(fmt(s.jSum.toDouble() / s.n)).append('/').append(fmt(s.jMax.toDouble()))
                .append("  ")
                .append(fmt(s.cSum / 1000.0 / s.n)).append('/').append(fmt(s.cMax / 1000.0))
        }
        return sb.toString()
    }

    private fun rateSweep(): List<Frame> {
        val f = mutableListOf<Frame>()
        val abc = "abcdefghijklmnopqrstuvwx"
        val done = StringBuilder()
        var t = 500L
        for (iv in listOf(250L, 120L, 80L, 50L, 33L, 20L)) {
            val label = iv.toString().padStart(3) + "ms "
            for (n in 1..abc.length) {
                t += iv
                f.add(Frame(t, done.toString() + label + abc.take(n), "${iv}ms"))
            }
            done.append(label).append(abc).append('\n')
        }
        return f
    }

    private fun kernelLog(): List<Frame> {
        val log = listOf(
            "[    0.000] Booting kernel",
            "[    0.041] mount /storage ... ok",
            "[    0.088] start display ... ok",
            "[    0.131] load launcher ... ok",
            "[    0.176] start corner mask ... ok",
            "[    0.220] net: wlan0 up",
            "[    0.265] audio: ok",
            "[    0.309] sensors: ok",
            "[    0.352] session: restored",
            "[    0.398] reached target home"
        )
        val f = mutableListOf<Frame>()
        var t = 500L
        for (iv in listOf(60L, 30L)) {
            val sb = StringBuilder()
            for (line in log) {
                t += iv
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(line)
                f.add(Frame(t, sb.toString(), "log$iv"))
            }
            t += 900
        }
        return f
    }

    private fun scramble(): List<Frame> {
        val target = "Good evening."
        val chars = "01#@%&*+=<>?/|"
        val rnd = Random(7)
        val f = mutableListOf<Frame>()
        val steps = 24
        var base = 500L
        for (iv in listOf(50L, 25L)) {
            for (s in 0..steps) {
                val solved = target.length * s / steps
                val sb = StringBuilder(target.substring(0, solved))
                for (i in solved until target.length) {
                    sb.append(if (target[i] == ' ') ' ' else chars[rnd.nextInt(chars.length)])
                }
                f.add(Frame(base + s * iv, sb.toString(), "scr$iv"))
            }
            base += steps * iv + 900
        }
        return f
    }

    private fun blinkFade(): List<Frame> {
        val f = mutableListOf<Frame>()
        val msg = "Good evening."
        var t = 500L
        for (i in 0 until 6) {
            f.add(Frame(t, msg + if (i % 2 == 0) "_" else " ", "blink"))
            t += 500
        }
        t += 300
        for (i in 0..10) {
            val g = 255 - i * 25
            f.add(Frame(t, msg, "gray", Color.rgb(g, g, g)))
            t += 60
        }
        t += 700
        for (i in 0..10) {
            f.add(Frame(t, msg, "alpha", Color.WHITE, 1f - i / 10f))
            t += 60
        }
        return f
    }

    private fun styles(): List<Frame> {
        val sb = SpannableStringBuilder("[  ")
        val s = sb.length
        sb.append("OK")
        sb.setSpan(ForegroundColorSpan(0xFF4CAF50.toInt()), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("  ] Session resumed")
        val sb2 = SpannableStringBuilder(sb).append("\n^ OK should be green")
        return listOf(
            Frame(500, sb2, "span"),
            Frame(3000, "[  OK  ] plain white", "plain")
        )
    }
}
