package com.prom3x209.cornermask

import android.app.KeyguardManager
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
    private const val STEP_MS = 450L

    private val handler = Handler(Looper.getMainLooper())
    private var seq = 0
    private var inactive = false
    private var currentPkg: String? = null
    private var lastPlay = 0L
    private var receiver: BroadcastReceiver? = null

    fun attach(ctx: Context) {
        if (receiver != null) return
        val app = ctx.applicationContext
        inactive = !isActive(app)
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.action == Intent.ACTION_SCREEN_OFF) {
                    inactive = true
                    stop(app)
                } else {
                    refresh(app)
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

    private fun isActive(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return pm.isInteractive && !km.isKeyguardLocked
    }

    // Returns true if the phone is on and unlocked. Handles the moment it becomes active again.
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
            in 5..11 -> "Good morning."
            in 12..16 -> "Good afternoon."
            in 17..21 -> "Good evening."
            else -> "Welcome back, night owl."
        }
    }

    private fun play(ctx: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlay < 1200) return
        lastPlay = now
        val my = ++seq
        handler.removeCallbacksAndMessages(null)
        render(ctx, "")
        val lines = listOf(
            "[  OK  ] Session resumed",
            "[  OK  ] Reached target Home",
            "",
            greeting()
        )
        val shown = StringBuilder()
        var t = DELAY_MS
        lines.forEachIndexed { i, line ->
            if (i > 0) t += STEP_MS
            handler.postDelayed({
                if (my != seq) return@postDelayed
                if (i > 0) shown.append('\n')
                shown.append(line)
                render(ctx, shown.toString())
            }, t)
        }
    }

    private fun stop(ctx: Context) {
        seq++
        lastPlay = 0L
        handler.removeCallbacksAndMessages(null)
        render(ctx, "")
    }

    fun render(ctx: Context, text: String) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val cn = ComponentName(ctx, GreetingWidget::class.java)
        if (mgr.getAppWidgetIds(cn).isEmpty()) return
        val rv = RemoteViews(ctx.packageName, R.layout.widget_greeting)
        rv.setTextViewText(R.id.greeting_text, text)
        mgr.updateAppWidget(cn, rv)
    }
}
