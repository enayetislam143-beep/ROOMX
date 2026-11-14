package com.enayet.roomx

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

class RoomXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val trace = Log.getStackTraceString(throwable)
                val intent = Intent(this, CrashActivity::class.java)
                intent.putExtra("trace", trace.take(6000))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                // ignore
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
