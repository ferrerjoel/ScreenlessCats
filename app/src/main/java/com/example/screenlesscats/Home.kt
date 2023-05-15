package com.example.screenlesscats

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.screenlesscats.block.AppBlockerService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class Home : AppCompatActivity() {

    private lateinit var bottomNavigationBar: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if(!isAccessServiceEnabled(this)){
         Snackbar.make(findViewById<View>(android.R.id.content), "Accessibility perms needed. Some functionalities will now work otherwise", Snackbar.LENGTH_LONG)
             .setAction("Settings"){
                 requestAppAccessibilitySettings()
             }
             .show()
        }

        bottomNavigationBar = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        topAppBar = findViewById(R.id.top_app_bar)

        auth = FirebaseAuth.getInstance()

        val homeFragment = HomeFragment()
        val timeFragment = TimeManagementFragment()
        val catsFragment = CatsFragment()

        setCurrentFragment(homeFragment)

        bottomNavigationBar.selectedItemId = R.id.item_home

        bottomNavigationBar.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.item_time -> {
                    setCurrentFragment(timeFragment)
                    topAppBar.title = "Time management"
                    true
                }
                R.id.item_home -> {
                    setCurrentFragment(homeFragment)
                    topAppBar.title = "Home"
                    true
                }
                R.id.item_cats -> {
                    setCurrentFragment(catsFragment)
                    topAppBar.title = "Cats"
                    true
                }
                else -> false
            }
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about_us_option -> {
                    // Handle edit text press
                    true
                }
                R.id.sign_out_option -> {
                    signOut()
                    true
                }
                R.id.give_accessibility_permissions -> {
                    requestAppAccessibilitySettings()
                    true
                }
                else -> false
            }
        }

    }

    private fun setCurrentFragment(fragment: Fragment)=
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment,fragment)
            commit()
        }
    /**
     * Signs out the user
     */
    private fun signOut() {
        auth.signOut()
        // Starts main screen
        val intent= Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

    fun startBlockService() {
        val serviceClass = AppBlockerService::class.java

        if (!isServiceRunning(serviceClass)) {
            Log.d("BLOCK SERVICE", "TRYING TO START SERVICE")
            val intent = Intent(applicationContext, serviceClass)
            startService(intent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val runningServices = activityManager?.getRunningServices(Integer.MAX_VALUE)

        for (service in runningServices ?: emptyList()) {
            if (serviceClass.name == service.service.className) {
                Log.d("BLOCK SERVICE", "SERVICE ALREADY RUNNING")
                return true
            }
        }

        return false
    }

    private fun requestAppAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    private fun isAccessServiceEnabled(context: Context): Boolean {
        val prefString =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString.contains("${context.packageName}/${context.packageName}.block.AppBlockerService")
    }
}