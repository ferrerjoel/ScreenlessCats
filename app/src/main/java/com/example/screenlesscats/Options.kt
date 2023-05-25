package com.example.screenlesscats

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth

class Options : AppCompatActivity() {

    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var auth: FirebaseAuth

    private lateinit var topBar : MaterialToolbar

    private lateinit var switchWeek : MaterialSwitch

    private lateinit var accessibilityPerms : Button
    private lateinit var usagePerms : Button

    private lateinit var signOut : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        auth = FirebaseAuth.getInstance()

        topBar = findViewById(R.id.top_app_bar)
        topBar.setNavigationOnClickListener {
            finish()
        }

        sharedPreferences = getSharedPreferences("Options", Context.MODE_PRIVATE)!!

        switchWeek = findViewById(R.id.switch_week)
        accessibilityPerms = findViewById(R.id.request_accessibility)
        usagePerms = findViewById(R.id.request_usage)
        signOut = findViewById(R.id.sign_out)

        switchWeek.setOnClickListener {
            weekStartOption()
        }

        accessibilityPerms.setOnClickListener {
            requestAppAccessibilitySettings(this)
        }

        usagePerms.setOnClickListener {
            requestAppUsageSettings(this)
        }

        signOut.setOnClickListener {
            signOut()
        }

        recoverSwitchPositions()

        if (isAccessServiceEnabled(this)) accessibilityPerms.isEnabled = false
        if (isUsagePermissionsEnabled(this)) usagePerms.isEnabled = false

    }

    private fun recoverSwitchPositions() {
        switchWeek.isChecked = sharedPreferences.getBoolean("weekStartsOnMonday", false)
    }

    private fun weekStartOption() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("weekStartsOnMonday", switchWeek.isChecked)
        editor.apply()
    }

    companion object {
        fun isAccessServiceEnabled(context: Context): Boolean {
            val prefString =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return prefString.contains("${context.packageName}/${context.packageName}.block.AppBlockerService")
        }

        fun isUsagePermissionsEnabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
                return mode == AppOpsManager.MODE_ALLOWED
            } else {
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
                return mode == AppOpsManager.MODE_ALLOWED
            }
        }

        fun requestAppAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }

        fun requestAppUsageSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use ACTION_APP_USAGE_SETTINGS for API 29 (Android 10) and above
                Intent(Settings.ACTION_APP_USAGE_SETTINGS)
            } else {
                // Use ACTION_USAGE_ACCESS_SETTINGS for API below 29
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            }
            // Open the settings page
            context.startActivity(intent)
        }
    }

    /**
     * Signs out the user
     */
    private fun signOut() {
        auth.signOut()
        // Starts main screen
        val intent= Intent(this, Login::class.java)
        // We clear the stack app activity so the user can't return to the home activity with back gestures
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}