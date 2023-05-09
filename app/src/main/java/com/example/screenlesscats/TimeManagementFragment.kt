package com.example.screenlesscats

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = context?.getSharedPreferences("LimitTimeVariablesLocal", Context.MODE_PRIVATE)!!
        sharedPreferencesApps = context?.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)!!

        limitHours = sharedPreferences.getInt("limitHours", 0)
        limitMinutes = sharedPreferences.getInt("limitMinutes", 0)

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

        // Load apps in the background thread
        loadAppsInBackground()
    }

    private fun loadAppsInBackground() {
        // Using coroutines we can load the list while the user is in the fragment, and not make him wait until all apps are loaded
        CoroutineScope(Dispatchers.IO).launch {
            packetManager = requireContext().packageManager
            phoneApps = packetManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in phoneApps) {
                // Exclude system apps
                if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                    val appName = packetManager.getApplicationLabel(app).toString()
                    val isChecked = sharedPreferencesApps.getBoolean(appName, false)
                    if (isChecked) Log.d("DEBUG", appName)
                    apps.add(
                        AppData(
                            appName,
                            "yes",
                            packetManager.getApplicationIcon(app),
                            isChecked
                        )
                    )
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

    private fun createAppList(view : View) {
        recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        // Material divider
        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = AppListAdapter(apps)
    }
    
}
