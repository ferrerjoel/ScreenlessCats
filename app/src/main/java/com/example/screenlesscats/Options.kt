package com.example.screenlesscats

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch

class Options : AppCompatActivity() {

    private lateinit var sharedPreferences : SharedPreferences

    private lateinit var topBar : MaterialToolbar

    private lateinit var switchWeek : MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        topBar = findViewById(R.id.top_app_bar)
        topBar.setNavigationOnClickListener {
            finish()
        }

        sharedPreferences = getSharedPreferences("Options", Context.MODE_PRIVATE)!!

        switchWeek = findViewById(R.id.switch_week)
        switchWeek.setOnClickListener {
            weekStartOption()
        }

        recoverSwitchPositions()

    }

    private fun recoverSwitchPositions() {
        switchWeek.isChecked = sharedPreferences.getBoolean("weekStartsOnMonday", false)
    }

    private fun weekStartOption() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("weekStartsOnMonday", switchWeek.isChecked)
        editor.apply()
    }
}