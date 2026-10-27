package com.prom3x209.cornermask

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class GreetingWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        GreetingController.render(context, "")
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AnimTest.ACTION_TEST) {
            AnimTest.next(context)
        } else {
            super.onReceive(context, intent)
        }
    }
}
