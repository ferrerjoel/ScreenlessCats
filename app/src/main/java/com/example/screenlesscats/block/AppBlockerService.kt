package com.example.screenlesscats.block

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

private const val BLOCKED_PACKAGE = "com.example.verbumly"

class AppBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Log.d("BLOCK SERVICE", "SERVICE STARTED 2")
            if (event.packageName?.toString() == BLOCKED_PACKAGE) {
                // Prevent the blocked app from launching
                performGlobalAction(GLOBAL_ACTION_HOME)
                Log.d("BLOCK SERVICE", "YES")
                // Show a dialog indicating the app is blocked
                showToast("This app is blocked.")
            } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // Check if the blocked app's window is focused
                val source: AccessibilityNodeInfo? = event.source
                val packageName = event.packageName?.toString()
                Log.d("BLOCK SERVICE", "NO")
                if (packageName == BLOCKED_PACKAGE && source != null) {
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
}
