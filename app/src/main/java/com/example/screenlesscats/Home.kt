package com.example.screenlesscats

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.screenlesscats.block.AppBlockerService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class Home : AppCompatActivity() {

    private lateinit var bottomNavigationBar: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
                else -> false
            }
        }

        val serviceClass = AppBlockerService::class.java

        if (!isServiceRunning(serviceClass)) {
            val intent = Intent(applicationContext, serviceClass)
            startService(intent)
            Log.d("BLOCK SERVICE", "SERVICE STARTED")
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

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val runningServices = activityManager?.getRunningServices(Integer.MAX_VALUE)

        for (service in runningServices ?: emptyList()) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }

        return false
    }






}