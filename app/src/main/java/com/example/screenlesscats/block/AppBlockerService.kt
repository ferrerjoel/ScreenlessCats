package com.example.screenlesscats.block

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.screenlesscats.R
import com.example.screenlesscats.data.AppData

class AppBlockerService : AccessibilityService() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesApps: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = applicationContext.getSharedPreferences("Options", Context.MODE_PRIVATE)
        sharedPreferencesApps = applicationContext.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)

        Log.d("BLOCK SERVICE", "SERVICE STARTED")

        //loadBlockedPackages()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BLOCK SERVICE", "SERVICE ENDED")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            //Log.d("BLOCK SERVICE EV", "EVENT")
            if (isCheckedPackage(event.packageName?.toString()) && sharedPreferences.getBoolean("isLimitEnabled", false)) {
                // Prevent the blocked app from launching
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Show a dialog indicating the app is blocked
                showToast(getString(R.string.toast_blocked_app))
            } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                //Log.d("BLOCK SERVICE EV", isCheckedPackage(event.packageName?.toString()).toString() + " " + sharedPreferences.getBoolean("isLimitEnabled", false))
                // Check if the blocked app's window is focused
                val source: AccessibilityNodeInfo? = event.source
                val packageName = event.packageName?.toString()

                if (isCheckedPackage(packageName) && source != null && sharedPreferences.getBoolean("isLimitEnabled", false)) {
                    // Show a dialog indicating the app is blocked when the user tries to interact with it
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    showToast(getString(R.string.toast_blocked_app))
                }

            }
        }
    }

    override fun onInterrupt() {
        // Handle accessibility service interruption
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun isCheckedPackage(packageName: String?): Boolean {
        return sharedPreferencesApps.getBoolean(packageName, false)
    }


}
