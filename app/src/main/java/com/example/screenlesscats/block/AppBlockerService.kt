package com.example.screenlesscats.block

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.screenlesscats.data.AppData

class AppBlockerService : AccessibilityService() {

    // private var blockedPackages: ArrayList<String> = ArrayList()
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
            Log.d("BLOCK SERVICE EV", "EVENT")
            if (isCheckedPackage(event.packageName?.toString()) && sharedPreferences.getBoolean("isLimitEnabled", false)) {
                // Prevent the blocked app from launching
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Show a dialog indicating the app is blocked
                showToast("This app is blocked.")
            } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.d("BLOCK SERVICE EV", isCheckedPackage(event.packageName?.toString()).toString() + " " + sharedPreferences.getBoolean("isLimitEnabled", false))
                // Check if the blocked app's window is focused
                val source: AccessibilityNodeInfo? = event.source
                val packageName = event.packageName?.toString()

                if (isCheckedPackage(packageName) && source != null && sharedPreferences.getBoolean("isLimitEnabled", false)) {
                    // Show a dialog indicating the app is blocked when the user tries to interact with it
                    showToast("This app is blocked.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
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
        //blockedPackages.contains(packageName)
    }

    private fun loadBlockedPackages() {
        Log.d("BLOCK SERVICE", "GETTING PACKAGES")
        val sharedPreferencesApps: SharedPreferences = applicationContext.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)
        val blockedPackagesMap: Map<String, *> = sharedPreferencesApps.all
        //blockedPackages.clear() // Clear the existing values in the ArrayList
        for ((packageName, value) in blockedPackagesMap) {
            if (value is Boolean && value) {
                //blockedPackages.add(packageName)
            }
        }
    }



}
