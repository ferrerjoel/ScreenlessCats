package com.example.screenlesscats.adapters

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.R
import com.example.screenlesscats.data.AppData

/**
 * Adapter to show all apps in a recycler view
 *
 * @property appList The list of apps to populate the recycler view
 */
class AppListAdapter(private val appList: List<AppData>) :
    RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesOptions: SharedPreferences

    /**
     * Holder of the adapter
     *
     * @param itemView
     */
    inner class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.app_name)
        private val usageTimeTextView: TextView = itemView.findViewById(R.id.daily_usage_tv)
        private val appIconImageView: ImageView = itemView.findViewById(R.id.app_icon)
        private val checkBox: CheckBox = itemView.findViewById(R.id.check_box)

        /**
         * Bind of the holder
         *
         * @param app App to show
         */
        fun bind(app: AppData) {
            appNameTextView.text = app.appName
            usageTimeTextView.text = itemView.resources.getString(
                R.string.daily_usage_app_list,
                app.hoursToday,
                app.minutesToday
            )
            appIconImageView.setImageDrawable(app.appIcon)
            checkBox.setOnCheckedChangeListener(null) // Remove previous listener to avoid conflicts
            checkBox.isChecked = app.checked // set checkbox state based on checked property
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                app.checked = isChecked // update checked property of AppData object
                updateAppCheckedState(app.packageName, isChecked) // update shared preferences
            }
            checkBox.isEnabled = !sharedPreferencesOptions.getBoolean("isLimitEnabled", false)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.app_list_element, parent, false)
        sharedPreferences = parent.context.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)
        sharedPreferencesOptions =
            parent.context.getSharedPreferences("Options", Context.MODE_PRIVATE)
        return AppListViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        val app = appList[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int {
        return appList.size
    }

    /**
     * Returns all the apps that have been checked by the user
     *
     * @return All the checked apps
     */
    fun getCheckedApps(): List<AppData> {
        return appList.filter { it.checked }
    }

    /**
     * Saves the check of the app in the shared preferences
     *
     * @param appPackage Package of the checked app
     * @param isChecked True if the app is checked
     */
    private fun updateAppCheckedState(appPackage: String, isChecked: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(appPackage, isChecked)
        editor.apply()
    }

}

