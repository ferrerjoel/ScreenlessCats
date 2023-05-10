package com.example.screenlesscats

import android.content.Context
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.adapters.AppListAdapter
import com.example.screenlesscats.data.AppData
import com.example.screenlesscats.data.FilterState
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class TimeManagementFragment:Fragment(R.layout.fragment_time_management) {

    private lateinit var setTimeButton: Button
    private lateinit var limitTimeTv: TextView

    private lateinit var timePicker: MaterialTimePicker
    private var limitHours: Int = 0
    private var limitMinutes: Int = 0

    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var sharedPreferencesApps : SharedPreferences

    private lateinit var packetManager: PackageManager
    private lateinit var phoneApps : List<ApplicationInfo>

    private val apps = ArrayList<AppData>()
    private lateinit var recyclerView : RecyclerView

    private var filterState: FilterState = FilterState.ALL
    private lateinit var toggleButtonGroup : MaterialButtonToggleGroup

    private lateinit var searchBar: TextInputEditText
    private var searchQuery: String = ""


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = context?.getSharedPreferences("LimitTimeVariablesLocal", Context.MODE_PRIVATE)!!
        sharedPreferencesApps = context?.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)!!

        limitHours = sharedPreferences.getInt("limitHours", 0)
        limitMinutes = sharedPreferences.getInt("limitMinutes", 0)

        toggleButtonGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleButton)

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
                .setTitleText("Select daily screen time")
                .build()

        timePicker.addOnPositiveButtonClickListener {
            setLimitTime()
            Log.d("DEBUG", (recyclerView.adapter as AppListAdapter).getCheckedApps().toString())
        }

        setTimeButton = view.findViewById(R.id.setTimeButton)

        setTimeButton.setOnClickListener {
            timePicker.show(parentFragmentManager, "tag")
        }

        searchBar = view.findViewById(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Update the search query
                searchQuery = s.toString().trim()
                // Refresh the app list with the new search query
                createAppList(view)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used
            }
        })

        // Load apps in the background thread
        loadAppsInBackground()
    }

    private fun loadAppsInBackground() {
        // Using coroutines we can load the list while the user is in the fragment, and not make him wait until all apps are loaded
        CoroutineScope(Dispatchers.IO).launch {
            packetManager = requireContext().packageManager
            phoneApps = packetManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in phoneApps) {
                if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                    val appName = packetManager.getApplicationLabel(app).toString()
                    val packageName = app.packageName
                    if (packageName != "com.example.screenlesscats") {
                        val isChecked = sharedPreferencesApps.getBoolean(packageName, false)
                        if (isChecked) Log.d("DEBUG", packageName)
                        when (filterState) {
                            FilterState.ALL ->
                                apps.add(
                                    AppData(
                                        appName,
                                        packageName,
                                        packetManager.getApplicationIcon(app),
                                        isChecked
                                    )
                                )

                            FilterState.CHECKED -> {
                                if (isChecked) {
                                    AppData(
                                        appName,
                                        packageName,
                                        packetManager.getApplicationIcon(app),
                                        true
                                    )
                                }
                            }

                            FilterState.UNCHECKED -> {
                                if (!isChecked) {
                                    AppData(
                                        appName,
                                        packageName,
                                        packetManager.getApplicationIcon(app),
                                        false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                createAppList(requireView())
            }
        }
    }


    private fun setLimitTime() {
        limitHours = timePicker.hour
        limitMinutes = timePicker.minute
        val editor = sharedPreferences.edit()
        editor?.putInt("limitHours", limitHours)
        editor?.putInt("limitMinutes", limitMinutes)
        editor?.apply()
        setTextViewTime()
    }

    private fun setTextViewTime() {
        if (limitHours and limitHours != 0) {
            limitTimeTv.text = getString(R.string.TimeTemplate, limitHours, limitMinutes)
        } else {
            limitTimeTv.text = getString(R.string.zero_time_limit_msg)
        }
    }

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
    }

    private fun filterApps(appList: List<AppData>, query: String): List<AppData> {
        return if (query.isBlank()) {
            appList
        } else {
            appList.filter { app ->
                app.appName.contains(query, ignoreCase = true)
            }
        }
    }


}
