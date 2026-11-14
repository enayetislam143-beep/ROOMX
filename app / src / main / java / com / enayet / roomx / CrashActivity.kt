package com.enayet.roomx

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra("trace") ?: "No error details"
        val view = TextView(this).apply {
            text = "ROOMX crashed. Copy this text and send it to Claude:\n\n" + trace
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            textSize = 12f
            setPadding(24, 48, 24, 24)
            setTextIsSelectable(true)
        }
        setContentView(ScrollView(this).apply { addView(view) })
    }
}
