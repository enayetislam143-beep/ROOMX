package com.enayet.roomx

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this).apply {
            text = "ROOMX test OK"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            textSize = 28f
            setPadding(48, 200, 48, 48)
        }
        setContentView(view)
    }
}
