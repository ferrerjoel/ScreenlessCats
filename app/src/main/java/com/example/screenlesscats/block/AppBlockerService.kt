package com.example.screenlesscats.block

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.screenlesscats.R
import java.util.Calendar
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Channel id to receive local broadcasts
 */
private const val CHANNEL_ID = "TimerNotificationChannel"

/**
 * Accessibility service to track the apps the user is using in order to subtract time from the daily and weekly time if the user is using a selected app, and then block these apps. The service makes use of global actions to not allow the user to use a blocked app.
 *
 */
class AppBlockerService : AccessibilityService() {

    private var oneMinuteNotificationSend: Boolean = false
    private var fiveMinuteNotificationSend: Boolean = false
    private var tenMinuteNotificationSend: Boolean = false

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesApps: SharedPreferences

    private lateinit var timer: CountDownTimer

    private var limitTime: Long = 0
    private var remainingTimeToday: Long = 0

    private var userHasActivatedWeeklyTime: Boolean = false
    private var limitTimeWeekly: Long = 0
    private var remainingTimeWeekly: Long = 0

    private var startDate: String = ""
    private var startDateWeekly: String = ""

    private var isTimerRunning: Boolean = false

    /**
     * Android systems have an app that shows above the games as an overlay, this ends the timer if we don't ignore it
     */
    private val gamePackageApps = hashSetOf(
        "com.oplus.games",
        "com.sec.android.app.samsungapps",
        "com.xiaomi.gamecenter",
        "com.huawei.gameassistant",
        "com.oppo.games",
        "com.vivo.gamecenter",
        "com.sony.playstation.playstationapp",
        "com.lge.games",
        "com.google.android.play.games",
        "com.asus.gamecenter",
        "com.motorola.gametime",
        "com.htc.vr.games"
    )

    /**
     * System packages that the user can trigger when using an app
     */
    private val systemUIPackages = hashSetOf(
        "com.android.systemui", // When checking notifications the timer stops and doesn't start again
        "com.android.launcher"
    )

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = applicationContext.getSharedPreferences("Options", Context.MODE_PRIVATE)
        sharedPreferencesApps =
            applicationContext.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)

        Log.d("BLOCK SERVICE", "SERVICE STARTED")
        val intentFilter = IntentFilter().apply {
            addAction("TIME_UPDATE")
            addAction("USER_HAS_UPDATED_WEEKLY")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(timeUpdateReceiver, intentFilter)

        // Load original timer value and start date
        limitTime = sharedPreferences.getLong("limitTime", 0)
        limitTimeWeekly = sharedPreferences.getLong("limitTimeWeekly", 0)

        startDate = sharedPreferences.getString("startDate", "") ?: ""
        startDateWeekly = sharedPreferences.getString("startDateWeekly", "") ?: ""

        // We don't need to send a notification if the set time is not more than the notification time
        if (remainingTimeToday > 600000 || (userHasActivatedWeeklyTime && remainingTimeWeekly > 600000)) {
            tenMinuteNotificationSend = true
        } else if (remainingTimeToday > 300000 || (userHasActivatedWeeklyTime && remainingTimeWeekly > 300000)) {
            fiveMinuteNotificationSend = true
        } else if (remainingTimeToday > 60000 || (userHasActivatedWeeklyTime && remainingTimeWeekly > 60000)) {
            oneMinuteNotificationSend = true
        }

        setupTimer()

        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BLOCK SERVICE", "SERVICE ENDED")
        if (::timer.isInitialized) timer.cancel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            // Log.d("EVENT BLOCK", event.eventType.toString() + " " + event.packageName)
            if (sharedPreferences.getBoolean("isLimitEnabled", false)) {
                val packageName = event.packageName?.toString()
                if (isCheckedPackage(packageName)) {
                    checkTimeAndBlock()
                } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    //Log.d("BLOCK SERVICE EV", isCheckedPackage(event.packageName?.toString()).toString() + " " + sharedPreferences.getBoolean("isLimitEnabled", false))
                    // Check if the blocked app's window is focused
                    val source: AccessibilityNodeInfo? = event.source
                    // Log.d("EVENT TYPE", event.eventType.toString() + " " + event.action + " " + event.contentChangeTypes + isCheckedPackage(packageName) + " " +packageName)
                    if (isCheckedPackage(packageName) && source != null) {
                        // Show a dialog indicating the app is blocked when the user tries to interact with it
                        checkTimeAndBlock()
                    } else if (!gamePackageApps.contains(event.packageName) && !systemUIPackages.contains(
                            event.packageName
                        )
                    ) {
                        stopTimer()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("TIMER BLOCK", "INTERRUPTED")
    }

    /**
     * Shows a toast
     *
     * @param message Message to show
     */
    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Checks if a package is checked by the user
     *
     * @param packageName Package of the app to check
     * @return True if the app has been checked
     */
    private fun isCheckedPackage(packageName: String?): Boolean {
        return sharedPreferencesApps.getBoolean(packageName, false)
    }

    /**
     * Checks if the user has more time today, if not it doesn't allow the user to use the app. This function uses general actions to leave the app when the user tries to access them
     *
     */
    private fun checkTimeAndBlock() {

        if (remainingTimeToday == 0L) checkNewDayAndWeek()

        if (remainingTimeToday > 0L) {
            startTimer(false)
        } else if (userHasActivatedWeeklyTime && remainingTimeWeekly > 0L) {
            startTimer(true)
        } else {
            // Prevent the blocked app from launching
            performGlobalAction(GLOBAL_ACTION_HOME)

            // Show a dialog indicating the app is blocked
            showToast(getString(R.string.toast_blocked_app))
        }
    }

    /**
     * Checks if it's a different day besides the one the last daily time was used, same as for weekly time, checks if a week has passed since the weekly time was established. If so it sets a new time respectively.
     *
     */
    private fun checkNewDayAndWeek() {
        startDate = sharedPreferences.getString("startDate", "") ?: ""
        startDateWeekly = sharedPreferences.getString("startDateWeekly", "") ?: ""

        val currentDate = getCurrentDate()
        val isNewDay = isDifferentDay(startDate, currentDate)
        val isNewWeek = isDifferentWeek(startDateWeekly, currentDate)

        Log.d(
            "TIMER BLOCK",
            sharedPreferences.getString("startDate", "") ?: (" $currentDate")
        )
        if (isNewDay) {
            Log.d("TIMER BLOCK", "NEW DAY")
            // Reset the timer to the original value at the start of a new day
            remainingTimeToday = limitTime
            resetNotificationFlags()
            startDate = currentDate
            saveTimerData(true)

            restartUserHasActivatedWeeklyTime()
        }

        if (isNewWeek) {
            remainingTimeWeekly = limitTimeWeekly
            resetNotificationFlags()
            startDateWeekly = currentDate
            saveTimerData(true)

            restartUserHasActivatedWeeklyTime()
        }
    }

    /**
     * Prepares the app timer to be used, checks if it needs to set new daily or weekly times. And sets the timer needed for the circumstances (user is using weekly time, user doesn't have more time...)
     *
     */
    private fun setupTimer() {
        Log.d("TIMER BLOCK", "SETUP TIMER CALLED")
        val currentDate = getCurrentDate()
        val isNewDay = isDifferentDay(startDate, currentDate)
        val isNewWeek = isDifferentWeek(startDateWeekly, currentDate)

        if (isNewDay) {
            Log.d("TIMER BLOCK", "NEW DAY")
            // Reset the timer to the original value at the start of a new day
            remainingTimeToday = limitTime
            resetNotificationFlags()
            startDate = currentDate
            saveTimerData(true)

            restartUserHasActivatedWeeklyTime()

        } else {
            Log.d("TIMER BLOCK", "ELSE")
            // Restore the remaining time from SharedPreferences
            remainingTimeToday = sharedPreferences.getLong("remainingTimeToday", limitTime)
        }

        if (isNewWeek) {
            Log.d("TIMER BLOCK", "NEW WEEK")
            // Reset the weekly timer to the original value at the start of a new week
            remainingTimeWeekly = limitTimeWeekly
            //resetNotificationFlags()
            startDateWeekly = currentDate
            saveTimerData(false)
            saveWeeklyStartDate()
        } else {
            Log.d("TIMER BLOCK", "ELSE")
            // Restore the remaining time from SharedPreferences
            remainingTimeWeekly = sharedPreferences.getLong("remainingTimeWeekly", limitTimeWeekly)
        }

        // Start or resume the timer
        // startTimer()
    }

    /**
     * Starts the instance of the timer created by setupTimer(). Checks if the timer is already running, sets the time the user has left on the timer.
     *
     * @param isWeeklyTimer If set to true it starts a timer for weekly time instead of daily time
     */
    private fun startTimer(isWeeklyTimer: Boolean) {
        if (!isTimerRunning) {
            isTimerRunning = true
            if (!isWeeklyTimer) {
                Log.d("TIMER BLOCK", "TIMER STARTED DAILY")
                timer = object : CountDownTimer(remainingTimeToday, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        remainingTimeToday = millisUntilFinished
                        sendTimeNotification()
                        Log.d("TIMER BLOCK", remainingTimeToday.toString())
                        // Update the remaining time in SharedPreferences
                        saveTimerData(false)
                    }

                    override fun onFinish() {
                        // Timer finished, handle the desired action
                        Log.d("TIMER BLOCK", "TIMER ON FINISH CALLED")
                        remainingTimeToday = 0L
                        checkTimeAndBlock()
                    }
                }.start()
            } else {
                Log.d("TIMER BLOCK", "TIMER STARTED WEEKLY")
                sendNotification(
                    getString(R.string.weekly_time_activated),
                    getString(R.string.remember_that_this_time_is_not_for_common_use)
                )
                timer = object : CountDownTimer(remainingTimeWeekly, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        remainingTimeWeekly = millisUntilFinished
                        sendTimeNotification()
                        Log.d("TIMER BLOCK", remainingTimeWeekly.toString())
                        // Update the remaining time in SharedPreferences
                        saveTimerData(false)
                    }

                    override fun onFinish() {
                        // Timer finished, handle the desired action
                        Log.d("TIMER BLOCK", "TIMER ON FINISH CALLED")
                        remainingTimeWeekly = 0L
                        checkTimeAndBlock()
                    }
                }.start()
            }
        }
    }

    /**
     * Fully stops the timer
     *
     */
    private fun stopTimer() {
        if (::timer.isInitialized && isTimerRunning) {
            timer.cancel()
            saveTimerData(false)
            Log.d("TIMER BLOCK", "TIMER CANCELED")
            isTimerRunning = false
        }
    }

    /**
     * Saves the remaining time the user has on daily and weekly basis.
     *
     * @param withDate True to also save the startDate of the daily timer
     */
    private fun saveTimerData(withDate: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putLong("remainingTimeToday", remainingTimeToday)
        editor.putLong("remainingTimeWeekly", remainingTimeWeekly)
        if (withDate) editor.putString("startDate", startDate)
        editor.apply()
    }

    /**
     * Saves the day the weekly time was started
     *
     */
    private fun saveWeeklyStartDate() {
        val editor = sharedPreferences.edit()
        editor.putString("startDateWeekly", startDateWeekly)
        editor.apply()
    }

    /**
     * Returns the current date as a string formatted in "year-month-day"
     *
     * @return The date as a string
     */
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }

    /**
     * Checks if two dates are different, and thus if it's a different date
     *
     * @param startDate First date to compare
     * @param currentDate Second date to compare
     * @return True if the dates are different
     */
    private fun isDifferentDay(startDate: String, currentDate: String): Boolean {
        // Compare the start date with the current date
        return startDate != currentDate
    }

    /**
     * Checks if two weeks are different, not if 7 days have elapsed between dates. It checks if two dates pertain into different weeks of a calendar
     *
     * @param startDate First week to compare
     * @param currentDate Second week to compare
     * @return True if the weeks are different
     */
    private fun isDifferentWeek(startDate: String, currentDate: String): Boolean {
        if (startDate == "") return true
        val startCalendar = Calendar.getInstance()
        val currentCalendar = Calendar.getInstance()
        startCalendar.time = getDateFromString(startDate)
        currentCalendar.time = getDateFromString(currentDate)

        // Set the start day of the week, default is sunday
        if (sharedPreferences.getBoolean("weekStartsOnMonday", false)) {
            val startDayOfWeek = Calendar.MONDAY
            startCalendar.firstDayOfWeek = startDayOfWeek
            currentCalendar.firstDayOfWeek = startDayOfWeek
        }

        // Check if the week of the start date is different from the current week
        return startCalendar.get(Calendar.WEEK_OF_YEAR) != currentCalendar.get(Calendar.WEEK_OF_YEAR)
    }

    /**
     * Formats a string into a date
     *
     * @param dateString String date to format
     * @return The formatted date as yyyy-MM-dd
     */
    private fun getDateFromString(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }

    /**
     * Updates the time values (remaining time and set time of daily and weekly) into the shared preferences
     *
     */
    fun updateTimeValues() {
        limitTime = sharedPreferences.getLong("limitTime", 0)
        remainingTimeToday = sharedPreferences.getLong("remainingTimeToday", limitTime)
        limitTimeWeekly = sharedPreferences.getLong("limitTimeWeekly", 0)
        remainingTimeWeekly = sharedPreferences.getLong("remainingTimeWeekly", limitTimeWeekly)
        Log.d("TIMER BLOCK", "$remainingTimeToday $limitTime")
    }

    /**
     * Every time a broadcast called "TIME_UPDATE" or "USER_HAS_UPDATED_WEEKLY" the function is going to be called respectively
     */
    private val timeUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "TIME_UPDATE") {
                updateTimeValues()
            }
            if (intent?.action == "USER_HAS_UPDATED_WEEKLY") {
                userHasActivatedWeeklyTime =
                    sharedPreferences.getBoolean("userHasActivatedWeeklyTime", false)
                resetNotificationFlags()
            }
        }
    }

    /**
     * Creates a notification channel to send notifications to the user
     *
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Channel"
            val descriptionText = "Notification Channel for Timer"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Sens a notification to the user via system notifications
     *
     * @param title Title of the notification
     * @param customContent Content of the notification
     */
    private fun sendNotification(title: String, customContent: String = "") {
        val contentText =
            customContent.ifEmpty { getString(R.string.all_your_selected_apps_are_going_to_be_blocked) }
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.cat)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    /**
     * Resets all notification flags, this means that if the conditions are met the program is able to send again notifications for 10, 5 and 1 minute left
     *
     */
    private fun resetNotificationFlags() {
        oneMinuteNotificationSend = false
        fiveMinuteNotificationSend = false
        tenMinuteNotificationSend = false
    }

    /**
     * Sends a time notification if the conditions are met, for daily and weekly time
     *
     */
    private fun sendTimeNotification() {
        val remainingTime =
            if (userHasActivatedWeeklyTime) remainingTimeWeekly else remainingTimeToday
        if (!tenMinuteNotificationSend && remainingTime <= 600000) {
            sendNotification(
                getString(R.string._10_minutes_left) + if (userHasActivatedWeeklyTime) getString(
                    R.string.of_weekly_time_watch_out
                ) else ""
            )
            tenMinuteNotificationSend = true
        } else if (!fiveMinuteNotificationSend && remainingTime <= 300000) {
            sendNotification(
                getString(R.string._5_minutes_left) + if (userHasActivatedWeeklyTime) getString(
                    R.string.of_weekly_time_watch_out
                ) else ""
            )
            fiveMinuteNotificationSend = true
        } else if (!oneMinuteNotificationSend && remainingTime <= 60000) {
            sendNotification(
                getString(R.string._1_minute_left) + if (userHasActivatedWeeklyTime) getString(
                    R.string.of_weekly_time_watch_out
                ) else ""
            )
            oneMinuteNotificationSend = true
        }
    }

    /**
     * Deactivates the saved shared preference that indicates if the user has activated weekly time, thus deactivating weekly time
     *
     */
    private fun restartUserHasActivatedWeeklyTime() {
        userHasActivatedWeeklyTime = false
        val editor = sharedPreferences.edit()
        editor.putBoolean("userHasActivatedWeeklyTime", userHasActivatedWeeklyTime)
        editor.apply()
    }

}
