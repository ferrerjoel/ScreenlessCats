package com.example.screenlesscats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationBarView

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val homeFragment = HomeFragment()

        NavigationBarView.OnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.item_time -> {
                    Log.d("DEBUG", "HOME CLICKED")
                    true
                }
                R.id.item_home -> {
                    Log.d("DEBUG", "HOME CLICKED")
                    setCurrentFragment(homeFragment)
                    true
                }
                R.id.item_cats -> {
                    Log.d("DEBUG", "HOME CLICKED")
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