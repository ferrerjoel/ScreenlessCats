package com.example.screenlesscats

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.adapters.AppListAdapter
import com.example.screenlesscats.data.AppData
import com.example.screenlesscats.data.FilterState
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fragment dedicated to set the desired limit time and the apps that are going to be affected by this limit
 *
 */

private const val TIME_TO_PENALTY = 172800

class TimeManagementFragment : Fragment(R.layout.fragment_time_management) {

    private lateinit var setTimeButton: Button
    private lateinit var activateLimitButton: Button
    private lateinit var limitTimeTv: TextView

    private lateinit var timePicker: MaterialTimePicker
    private var limitHours: Int = 0
    private var limitMinutes: Int = 0
    private var totalMilliseconds: Long = 0
    private var totalMillisecondsWeekly: Long = 0

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesApps: SharedPreferences

    private lateinit var packetManager: PackageManager
    private lateinit var phoneApps: List<ApplicationInfo>

    private val apps = ArrayList<AppData>()
    private var appsHaveBeenFetched: Boolean = false
    private lateinit var recyclerView: RecyclerView

    private var filterState: FilterState = FilterState.ALL
    private lateinit var toggleButtonGroup: MaterialButtonToggleGroup

    private lateinit var searchBar: TextInputEditText
    private var searchQuery: String = ""

    private lateinit var spinner: CircularProgressIndicator

    private var loadAppsJob: Job? =
        null // Declare a nullable Job variable to keep track of the coroutine job
    private var loadAppsUsageJob: Job? = null

    /**
     * These apps are considered system apps since come with the android system and can't be uninstalled, then we have to filter them
     */
    private val preInstalledApps = hashSetOf(
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.maps",
        "com.android.chrome",
        "com.google.android.youtube",
        "com.android.vending",
        "com.google.android.music",
        "com.google.android.apps.photos",
        "com.google.android.gm",
        "com.google.android.apps.docs",
        "com.google.android.calendar",
        "com.google.android.apps.tachyon",
        "com.google.android.apps.magazines",
        "com.google.android.play.games",
        "com.google.android.talk",
        "com.google.android.keep",
        "com.google.android.contacts"
    )

    /**
     * Initializes UI elements and gets saved data on shared preferences
     *
     * @param view View of the fragment
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinner = view.findViewById(R.id.spinner)

        sharedPreferences = context?.getSharedPreferences("Options", Context.MODE_PRIVATE)!!
        sharedPreferencesApps = context?.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)!!

        limitHours = sharedPreferences.getInt("limitHours", 0)
        limitMinutes = sharedPreferences.getInt("limitMinutes", 0)
        totalMilliseconds = sharedPreferences.getLong("limitTime", 0)

        toggleButtonGroup = view.findViewById(R.id.toggleButton)

        toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button1 -> filterState = FilterState.ALL
                    R.id.button2 -> filterState = FilterState.CHECKED
                    R.id.button3 -> filterState = FilterState.UNCHECKED
                }
                createAppList(view)
            }
        }

        limitTimeTv = view.findViewById(R.id.timeLimitTv)
        setTextViewTime()

        timePicker =
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(1)
                .setMinute(0)
                .setTitleText(getString(R.string.select_daily_screen_time))
                .build()

        timePicker.addOnPositiveButtonClickListener {
            setLimitTime()
            Log.d("DEBUG", (recyclerView.adapter as AppListAdapter).getCheckedApps().toString())
        }

        setTimeButton = view.findViewById(R.id.setTimeButton)

        setTimeButton.setOnClickListener {
            if (sharedPreferences.getBoolean("isLimitEnabled", false)) {
                showWarningChangeTime()
            } else {
                timePicker.show(parentFragmentManager, "tag")
            }
        }

        activateLimitButton = view.findViewById(R.id.activateLimitButton)

        if (sharedPreferences.getBoolean("isLimitEnabled", false))
            activateLimitButton.setText(R.string.activate_limit_button_off)

        activateLimitButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            if (sharedPreferences.getBoolean("isLimitEnabled", false)) {
                showWarningEndLimit(editor, view)
            } else {
                setLimitTime() // If the user ends the time and it starts it again we restart the timers even if the user haven't changed the time
                editor?.putBoolean("isLimitEnabled", true)
                activateLimitButton.setText(R.string.activate_limit_button_off)
                editor?.apply()
                createAppList(view)

                (requireActivity() as Home).startBlockService()
                uploadLimit(totalMilliseconds)
            }
        }

        searchBar = view.findViewById(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Update the search query
                if (appsHaveBeenFetched) {
                    searchQuery = s.toString().trim()
                    // Refresh the app list with the new search query
                    createAppList(view)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used
            }
        })

        // Load apps in the background thread
        // TODO: this works but can be optimized, if the user leaves the fragment while fetching usage data it breaks, so we start over
        if (apps.isEmpty()) {
            loadAppsInBackground()
        } else if (!appsHaveBeenFetched && !(loadAppsJob?.isActive!! || loadAppsUsageJob?.isActive!!)) {
            apps.clear()
            loadAppsInBackground()
        }

    }

    /**
     * Fetches and saves all apps on the user phone, excluding the majority of system apps and this app itself
     *
     */
    private fun loadAppsInBackground() {
        loadAppsJob = CoroutineScope(Dispatchers.IO).launch {
            packetManager = requireContext().packageManager
            phoneApps = packetManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in phoneApps) {
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM == 0) || preInstalledApps.contains(
                        app.packageName
                    )
                ) {
                    val appName = packetManager.getApplicationLabel(app).toString()
                    val packageName = app.packageName
                    // We are going to exclude this app
                    if (packageName != "com.example.screenlesscats") {
                        val isChecked = sharedPreferencesApps.getBoolean(packageName, false)
                        if (isChecked) Log.d("DEBUG", packageName)
                        apps.add(
                            AppData(
                                appName,
                                0,
                                0,
                                packageName,
                                packetManager.getApplicationIcon(app),
                                isChecked
                            )
                        )
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (isActive && isAdded && view != null) {
                    updateAppUsageTimes(requireContext(), apps)
                }
            }
        }
    }

    /**
     * Saves the chosen limit time using SharedPreferences
     */
    private fun setLimitTime() {
        limitHours = timePicker.hour
        limitMinutes = timePicker.minute
        totalMilliseconds = ((limitHours * 60 + limitMinutes) * 60 * 1000).toLong()
        totalMillisecondsWeekly = (600000 + (totalMilliseconds * 0.2) * 7).toLong()

        val editor = sharedPreferences.edit()
        editor?.putInt("limitHours", limitHours)
        editor?.putInt("limitMinutes", limitMinutes)

        editor?.putLong("limitTime", totalMilliseconds)
        editor?.putLong("remainingTimeToday", totalMilliseconds)
        editor?.putLong("limitTimeWeekly", totalMillisecondsWeekly)
        editor?.putLong("remainingTimeWeekly", totalMillisecondsWeekly)
        editor?.apply()

        setTextViewTime()
        // We update the time values of the blocker service
        val intent = Intent("TIME_UPDATE")
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    /**
     * Uploads the created limit into the firebase for history purposes
     *
     * @param totalMilliseconds Total set time of the created limit in milliseconds
     */
    private fun uploadLimit(totalMilliseconds: Long) {
        val auth = Firebase.auth
        val uid = auth.uid.toString()

        val ref =
            FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(uid).child("user_data")

        val limit: HashMap<String, Any> = HashMap()

        val calendar = Calendar.getInstance()

        limit["Date_limit_defined"] =
            "" + calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(
                Calendar.DAY_OF_MONTH
            ) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE)
        limit["Defined_screen_time"] = totalMilliseconds
        limit["Date_limit_ended"] = ""
        limit["Cats_earned"] = 0

        var maxId: Long
        ref.child("limits").get().addOnSuccessListener {
            maxId = it.childrenCount
            Log.d("BON", maxId.toString())
            ref.child("limits").child((maxId + 1).toString()).setValue(limit)
            ref.child("Defined_screen_time").setValue(totalMilliseconds)
        }.addOnCanceledListener {
            Log.d("BON DIA", "On Cancelled")
        }

    }

    /**
     * Changes the text of the text view that shows the time set
     */
    private fun setTextViewTime() {
        if (limitHours or limitMinutes != 0) {
            limitTimeTv.text = getString(R.string.TimeTemplate, limitHours, limitMinutes)
        } else {
            limitTimeTv.text = getString(R.string.zero_time_limit_msg)
        }
    }

    /**
     * Creates an app list, filtering using FilterState flags and calling the adapter
     *
     * @param view View of the fragment
     */
    private fun createAppList(view: View) {
        recyclerView = view.findViewById(R.id.app_list)

        val filteredApps = when (filterState) {
            FilterState.ALL -> filterApps(apps, searchQuery)
            FilterState.CHECKED -> filterApps(apps.filter { it.checked }, searchQuery)
            FilterState.UNCHECKED -> filterApps(apps.filter { !it.checked }, searchQuery)
        }

        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        if (recyclerView.itemDecorationCount < 1) {
            recyclerView.addItemDecoration(dividerItemDecoration)
        }

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = AppListAdapter(filteredApps)
        spinner.visibility = View.GONE
    }

    /**
     * Filters the app list by usage time and if there is by search query, receives the app list to filter and a query from the search bar
     *
     * @param appList List of apps to filter
     * @param query Filters the apps by if they contain this query string
     * @return The filtered list
     */
    private fun filterApps(appList: List<AppData>, query: String): List<AppData> {
        return if (query.isBlank()) {
            appList.sortedWith(compareByDescending<AppData> { it.hoursToday }
                .thenByDescending { it.minutesToday })
        } else {
            appList.filter { app ->
                app.appName.contains(query, ignoreCase = true)
            }.sortedWith(compareByDescending<AppData> { it.hoursToday }
                .thenByDescending { it.minutesToday })
        }
    }

    /**
     * Warning shown when trying to change the time when you have the limit set
     */
    private fun showWarningChangeTime() {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.warning_title_end_limit_time))
                .setMessage(resources.getString(R.string.warning_end_limit_time))
                .setNeutralButton(resources.getString(R.string.i_understand)) { dialog, which ->
                    // Respond to neutral button press
                }
                .show()
        }
    }

    /**
     * Warning shown when trying to end the limit
     *
     * @param editor Shared preferences options editor
     * @param view View of the fragment
     */
    private fun showWarningEndLimit(editor: SharedPreferences.Editor, view: View) {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.warning_title_end_limit))
                .setMessage(resources.getString(R.string.warning_end_limit))
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                    // Respond to neutral button press
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    editor.putBoolean("isLimitEnabled", false)
                    editor.apply()
                    activateLimitButton.setText(R.string.activate_limit_button_on)
                    createAppList(view)
                    endLimitFirebase()
                    //(requireActivity() as Home).endService(requireContext())
                }
                .show()

        }
    }

    /**
     * Ends the limit on Firebase, saving the correspondent data into the server
     *
     */
    private fun endLimitFirebase() {
        val auth = Firebase.auth
        val uid = auth.uid.toString()

        val ref =
            FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(uid).child("user_data")

        val calendar = Calendar.getInstance()
        val toUpdate: HashMap<String, Any> = HashMap()
        val dateEnded =
            "" + calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(
                Calendar.DAY_OF_MONTH
            ) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE)
        toUpdate["Date_limit_ended"] = dateEnded


        var maxId: Long = -1
        ref.get().addOnSuccessListener {
            maxId = it.child("limits").childrenCount
            ref.child("limits").child(maxId.toString()).updateChildren(toUpdate)

            val newDefinedTime: HashMap<String, Any> = HashMap()
            newDefinedTime["Defined_screen_time"] = 0
            ref.updateChildren(newDefinedTime)
            ref.child("days_streaks").setValue(0)
        }.addOnCanceledListener {
            Log.d("BON DIA", "On Cancelled")
        }
        ref.get().addOnSuccessListener {
            val limitStarted = it.child("limits").child(maxId.toString())
                .child("Date_limit_defined").value.toString()

            val dateFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
                ) // Format of the dates
            val dateStart = dateFormat.parse(limitStarted)
            val dateEnd = dateFormat.parse(dateEnded)

            val dif = dateEnd.time - dateStart.time
            val currentDedicationValue = it.child("dedication_value").value.toString().toFloat()
            if ((dif / 1000 >= TIME_TO_PENALTY) && (currentDedicationValue > 0)) {
                ref.child("dedication_value").setValue(currentDedicationValue - 0.0135)
            }
        }

    }

    /**
     * Fetches all usage app time of each app in the list and saves it as hours and minutes format
     *
     * @param context Context of the activity
     * @param appDataList List of apps to fetch data
     */
    private fun updateAppUsageTimes(context: Context, appDataList: ArrayList<AppData>) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Run the function in a background coroutine
        loadAppsUsageJob = CoroutineScope(Dispatchers.Default).launch {
            for (appData in appDataList) {
                val packageName = appData.packageName

                val queryEvents = usageStatsManager.queryEvents(
                    calendar.timeInMillis,
                    Calendar.getInstance().timeInMillis
                )

                var totalUsageTime = 0L
                var startTime: Long? = null

                while (queryEvents.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    queryEvents.getNextEvent(event)

                    if ((event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) &&
                        event.packageName == packageName
                    ) {
                        startTime = event.timeStamp // Set the start time for the app
                    } else if ((event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) &&
                        startTime != null && event.packageName == packageName
                    ) {
                        val usageTime = event.timeStamp - startTime
                        totalUsageTime += usageTime
                        startTime = null // Reset the start time for the next iteration
                    }
                }

                val hours = (totalUsageTime / (1000 * 60 * 60)).toInt()
                val minutes = ((totalUsageTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()

                // Update the usage time values in the AppData object
                withContext(Dispatchers.Main) {
                    appData.hoursToday = hours
                    appData.minutesToday = minutes
                }
            }

            appsHaveBeenFetched = true

            // Create the app list after updating all the app usage times
            withContext(Dispatchers.Main) {
                if (isActive && isAdded && view != null) {
                    createAppList(requireView())
                }
            }
        }
    }
}
