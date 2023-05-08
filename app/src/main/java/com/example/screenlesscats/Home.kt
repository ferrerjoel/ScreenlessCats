package com.example.screenlesscats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home : AppCompatActivity() {
    private lateinit var bottomNavigationBar: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigationBar = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        topAppBar = findViewById(R.id.top_app_bar)

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
                    // Handle favorite icon press
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
}