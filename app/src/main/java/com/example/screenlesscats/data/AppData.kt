package com.example.screenlesscats.data

import android.graphics.drawable.Drawable

/**
 * Saves the data of an app
 *
 * @property appName Name of the app
 * @property hoursToday Hours the user has spent in the app today
 * @property minutesToday Minutes the user has spent in the app today
 * @property packageName Package name of the app
 * @property appIcon Icon of the app
 * @property checked True if the user has checked the app for the limit
 */
data class AppData(
    val appName: String,
    var hoursToday: Int,
    var minutesToday: Int,
    val packageName: String,
    val appIcon: Drawable,
    var checked: Boolean = false
)

