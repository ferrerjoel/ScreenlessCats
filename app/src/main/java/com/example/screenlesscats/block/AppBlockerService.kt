package com.example.screenlesscats.block

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.screenlesscats.R
import java.util.Calendar

class AppBlockerService : AccessibilityService() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesApps: SharedPreferences

    private lateinit var timer: CountDownTimer
    private var limitTime: Long = 0
    private var remainingTimeToday: Long = 0
    private var startDate: String = ""

    private var isTimerRunning: Boolean = false

    val gamePackageApps = hashSetOf(
        "com.oplus.games",
        "com.sec.android.app.samsungapps",
        "com.xiaomi.gamecenter",
        "com.huawei.gameassistant",
        "com.oppo.games",
        "com.vivo.gamecenter",
        "com.sony.playstation.playstationapp",
        "com.lge.games",
        "com.google.android.play.games",
        "com.asus.gamecenter",
        "com.motorola.gametime",
        "com.htc.vr.games"
    )

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = applicationContext.getSharedPreferences("Options", Context.MODE_PRIVATE)
        sharedPreferencesApps = applicationContext.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)

        Log.d("BLOCK SERVICE", "SERVICE STARTED")
        LocalBroadcastManager.getInstance(this).registerReceiver(timeUpdateReceiver, IntentFilter("TIME_UPDATE"))

        // Load original timer value and start date
        limitTime = sharedPreferences.getLong("limitTime", 0)
        startDate = sharedPreferences.getString("startDate", "") ?: ""

        setupTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BLOCK SERVICE", "SERVICE ENDED")
        if (::timer.isInitialized) timer.cancel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Log.d("EVENT BLOCK", event.eventType.toString() + " " + event.packageName)
            if (sharedPreferences.getBoolean("isLimitEnabled", false)) {
                val packageName = event.packageName?.toString()
                if (isCheckedPackage(packageName)) {
                    checkTimeAndBlock()
                } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    //Log.d("BLOCK SERVICE EV", isCheckedPackage(event.packageName?.toString()).toString() + " " + sharedPreferences.getBoolean("isLimitEnabled", false))
                    // Check if the blocked app's window is focused
                    val source: AccessibilityNodeInfo? = event.source
                    Log.d("EVENT TYPE", event.eventType.toString() + " " + event.action + " " + event.contentChangeTypes + isCheckedPackage(packageName) + " " +packageName)
                    if (isCheckedPackage(packageName) && source != null) {
                        // Show a dialog indicating the app is blocked when the user tries to interact with it
                        checkTimeAndBlock()
                    } else if (!gamePackageApps.contains(event.packageName)){
                        stopTimer()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("TIMER BLOCK", "INTERRUPTED")
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun isCheckedPackage(packageName: String?): Boolean {
        return sharedPreferencesApps.getBoolean(packageName, false)
    }

    private fun checkTimeAndBlock() {
        if (remainingTimeToday > 0L) {
            startTimer()
        } else {
            // Prevent the blocked app from launching
            performGlobalAction(GLOBAL_ACTION_HOME)

            // Show a dialog indicating the app is blocked
            showToast(getString(R.string.toast_blocked_app))
        }
    }

    private fun setupTimer() {
        Log.d("TIMER BLOCK", "SETUP TIMER CALLED")
        val currentDate = getCurrentDate()
        val isNewDay = isDifferentDay(startDate, currentDate)

        if (isNewDay) {
            Log.d("TIMER BLOCK", "NEW DAY")
            // Reset the timer to the original value at the start of a new day
            remainingTimeToday = limitTime
            startDate = currentDate
            saveTimerData(true)
        } else {
            Log.d("TIMER BLOCK", "ELSE")
            // Restore the remaining time from SharedPreferences
            remainingTimeToday = sharedPreferences.getLong("remainingTimeToday", limitTime)
        }

        // Start or resume the timer
        // startTimer()
    }

    private fun startTimer() {
        if (!isTimerRunning){
            Log.d("TIMER BLOCK", "TIMER STARTED")
            isTimerRunning = true
            timer = object : CountDownTimer(remainingTimeToday, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTimeToday = millisUntilFinished
                    Log.d("TIMER BLOCK", remainingTimeToday.toString())
                    // Update the remaining time in SharedPreferences
                    saveTimerData(false)
                }

                override fun onFinish() {
                    // Timer finished, handle the desired action
                    Log.d("TIMER BLOCK", "TIMER ON FINISH CALLED")
                    remainingTimeToday = 0L
                    checkTimeAndBlock()
                }
            }.start()
        }
    }

    private fun stopTimer() {
        if (::timer.isInitialized && isTimerRunning) {
            timer.cancel()
            saveTimerData(false)
            Log.d("TIMER BLOCK", "TIMER CANCELED")
            isTimerRunning = false
        }
    }

    private fun saveTimerData(withDate : Boolean) {
        val editor = sharedPreferences.edit()
        editor.putLong("remainingTimeToday", remainingTimeToday)
        if (withDate) editor.putString("startDate", startDate)
        editor.apply()
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }

    private fun isDifferentDay(startDate: String, currentDate: String): Boolean {
        // Compare the start date with the current date
        return startDate != currentDate
    }

    fun updateTimeValues() {
        limitTime = sharedPreferences.getLong("limitTime", 0)
        remainingTimeToday = sharedPreferences.getLong("remainingTimeToday", limitTime)
        Log.d("TIMER BLOCK", "$remainingTimeToday $limitTime")
    }

    /**
     * Every time a broadcast called "TIME_UPDATE" the function is going to be executed
     */
    private val timeUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "TIME_UPDATE") {
                updateTimeValues()
            }
        }
    }

}
