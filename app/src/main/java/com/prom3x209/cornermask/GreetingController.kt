package com.prom3x209.cornermask

import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.inputmethod.InputMethodManager
import android.widget.RemoteViews
import java.util.Calendar

object GreetingController {
    private const val HOME = "fr.neamar.kiss"
    private const val DELAY_MS = 2500L
    private const val CHAR_MS = 38L
    private const val ENTER_MS = 250L
    private const val BLINK_MS = 500L
    private const val BLINK_STEPS = 60

    private val handler = Handler(Looper.getMainLooper())
    private var seq = 0
    private var inactive = false
    private var currentPkg: String? = null
    private var lastPlay = 0L
    private var receiver: BroadcastReceiver? = null
    private var termuxPi: PendingIntent? = null

    fun attach(ctx: Context) {
        if (receiver != null) return
        val app = ctx.applicationContext
        inactive = !isActive(app)
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                when (i.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        inactive = true
                        stop(app)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (inactive) stop(app)
                        refresh(app)
                    }
                    else -> refresh(app)
                }
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            app.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(r, f)
        }
        receiver = r
    }

    fun detach(ctx: Context) {
        val app = ctx.applicationContext
        receiver?.let {
            try { app.unregisterReceiver(it) } catch (e: Exception) { }
        }
        receiver = null
        stop(app)
    }

    fun pause() {
        seq++
        handler.removeCallbacksAndMessages(null)
    }

    private fun isActive(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return pm.isInteractive && !km.isKeyguardLocked
    }

    private fun refresh(ctx: Context): Boolean {
        if (!isActive(ctx)) {
            if (!inactive) {
                inactive = true
                stop(ctx)
            }
            return false
        }
        if (inactive) {
            inactive = false
            if (currentPkg == HOME) play(ctx)
        }
        return true
    }

    fun onWindowChanged(ctx: Context, pkg: String?) {
        val app = ctx.applicationContext
        if (!refresh(app) || pkg == null) return
        if (pkg != HOME && pkg in ignored(app)) return
        val wasHome = currentPkg == HOME
        currentPkg = pkg
        if (pkg == HOME) {
            if (!wasHome) play(app)
        } else if (wasHome) {
            stop(app)
        }
    }

    private fun ignored(ctx: Context): Set<String> {
        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.map { it.packageName }.toSet() +
            setOf("com.android.systemui", "android")
    }

    private fun greeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "good morning."
            in 12..16 -> "good afternoon."
            in 17..21 -> "good evening."
            else -> "still up, user?"
        }
    }

    private fun at(my: Int, delayMs: Long, block: () -> Unit) {
        handler.postDelayed({ if (my == seq) block() }, delayMs)
    }

    private fun play(ctx: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlay < 1200) return
        lastPlay = now
        val my = ++seq
        handler.removeCallbacksAndMessages(null)
        render(ctx, "")

        val cmds = listOf(
            "> const user = login();",
            "> mount(\"/storage\");",
            "> launcher.start();",
            "> console.log(greet());"
        )
        val outs = listOf("> welcome back, user.", "> " + greeting())

        // blank warm-up update just before typing starts
        at(my, DELAY_MS - 150) { render(ctx, "") }

        var t = DELAY_MS
        val done = StringBuilder()
        for (cmd in cmds) {
            for (k in 1..cmd.length) {
                val text = done.toString() + cmd.substring(0, k) + "_"
                at(my, t) { render(ctx, text) }
                t += CHAR_MS
            }
            done.append(cmd).append('\n')
            t += ENTER_MS
        }

        // both output lines appear at once, like real output
        val body = (cmds + outs).joinToString("\n")
        at(my, t) { render(ctx, body) }
        t += ENTER_MS

        // final prompt, then the cursor blinks for 30 s and stays solid
        for (n in 0..BLINK_STEPS) {
            val on = n % 2 == 0
            val text = body + "\n> " + (if (on) "_" else " ")
            at(my, t + n * BLINK_MS) { render(ctx, text) }
        }
    }

    private fun stop(ctx: Context) {
        seq++
        lastPlay = 0L
        handler.removeCallbacksAndMessages(null)
        render(ctx, "")
    }

    private fun termuxIntent(ctx: Context): PendingIntent {
        termuxPi?.let { return it }
        val i = Intent(Intent.ACTION_MAIN)
            .setClassName("com.termux", "com.termux.app.TermuxActivity")
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            ctx, 7, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        termuxPi = pi
        return pi
    }

    fun render(ctx: Context, text: String) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, GreetingWidget::class.java))
        if (ids.isEmpty()) return
        val rv = RemoteViews(ctx.packageName, R.layout.widget_greeting)
        rv.setTextViewText(R.id.greeting_text, text)
        rv.setOnClickPendingIntent(R.id.greeting_text, termuxIntent(ctx))
        mgr.updateAppWidget(ids, rv)
    }
}
