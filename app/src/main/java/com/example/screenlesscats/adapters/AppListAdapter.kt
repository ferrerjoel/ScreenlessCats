package com.example.screenlesscats.adapters

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.R
import com.example.screenlesscats.data.AppData

class AppListAdapter(private val appList: List<AppData>): RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {

    private lateinit var sharedPreferences : SharedPreferences

    inner class AppListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.app_name)
        private val appIconImageView: ImageView = itemView.findViewById(R.id.app_icon)
        private val checkBox: CheckBox = itemView.findViewById(R.id.check_box)

        fun bind(app: AppData) {
            appNameTextView.text = app.appName
            appIconImageView.setImageDrawable(app.appIcon)
            checkBox.isChecked = app.checked // set checkbox state based on checked property
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                app.checked = isChecked // update checked property of AppData object
                updateAppCheckedState(app.packageName, isChecked) // update shared preferences
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.app_list_element, parent, false)
        sharedPreferences = parent.context.getSharedPreferences("LimitedApps", Context.MODE_PRIVATE)
        return AppListViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        val app = appList[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int {
        return appList.size
    }

    fun getCheckedApps(): List<AppData> {
        return appList.filter { it.checked }
    }

    private fun updateAppCheckedState(appPackage: String, isChecked: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(appPackage, isChecked)
        editor.apply()
    }

}

