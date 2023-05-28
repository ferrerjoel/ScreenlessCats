package com.example.screenlesscats

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity to show various settings to the user
 *
 */
class Options : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    private lateinit var topBar: MaterialToolbar

    private lateinit var switchWeek: MaterialSwitch

    private lateinit var accessibilityPerms: Button
    private lateinit var usagePerms: Button

    private lateinit var signOut: Button

    /**
     * Initializes the UI elements
     *
     * @param savedInstanceState
     */
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

        checkButtonPermissions()

    }

    /**
     * If the user returns from the accessibility settings we check if the user has granted permissions to disable buttons
     *
     */
    override fun onResume() {
        super.onResume()
        checkButtonPermissions()
    }

    /**
     * Recovers the shared preferences of the different switch options
     *
     */
    private fun recoverSwitchPositions() {
        switchWeek.isChecked = sharedPreferences.getBoolean("weekStartsOnMonday", false)
    }

    /**
     * Saves the state of the week starts on monday switch
     *
     */
    private fun weekStartOption() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("weekStartsOnMonday", switchWeek.isChecked)
        editor.apply()
    }

    /**
     * This companion object includes various functions to check for user permissions
     */
    companion object {
        /**
         * Checks if the user has given accessibility permissions
         *
         * @param context Context of the activity
         * @return True if the user has given out permissions
         */
        fun isAccessServiceEnabled(context: Context): Boolean {
            val prefString =
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
            return prefString.contains("${context.packageName}/${context.packageName}.block.AppBlockerService")
        }

        /**
         * Checks if the user has given out usage permissions
         *
         * @param context Context of the activity
         * @return True if the user has given out permissions
         */
        fun isUsagePermissionsEnabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val appOpsManager =
                    context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
                return mode == AppOpsManager.MODE_ALLOWED
            } else {
                val appOpsManager =
                    context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
                return mode == AppOpsManager.MODE_ALLOWED
            }
        }

        /**
         * Opens the accessibility permissions page of the system
         *
         * @param context Context of the activity
         */
        fun requestAppAccessibilitySettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        /**
         * Opens the usage permissions page of the system
         *
         * @param context Context of the activity
         */
        fun requestAppUsageSettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    /**
     * Signs out the user of the Firebase server
     *
     */
    private fun signOut() {
        auth.signOut()
        // Starts main screen
        val intent = Intent(this, Login::class.java)
        // We clear the stack app activity so the user can't return to the home activity with back gestures
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Checks if the buttons have to be enabled or not
     *
     */
    private fun checkButtonPermissions() {
        if (isAccessServiceEnabled(this)) accessibilityPerms.isEnabled = false
        if (isUsagePermissionsEnabled(this)) usagePerms.isEnabled = false
    }
}