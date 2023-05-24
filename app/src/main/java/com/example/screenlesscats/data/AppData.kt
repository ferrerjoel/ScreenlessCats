package com.example.screenlesscats.data

import android.graphics.drawable.Drawable

data class AppData(
    val appName: String,
    val hoursToday: Int,
    val minutesToday: Int,
    val packageName: String,
    val appIcon: Drawable,
    var checked: Boolean = false
)

