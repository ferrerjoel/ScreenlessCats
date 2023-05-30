package com.example.screenlesscats

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.screenlesscats.data.Cat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

/**
 * Fragment dedicates to show the remaining daily and weekly time, tips, a random cat of the user and the button to activate weekly time if the requirements are met
 *
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var catImage: ImageView
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var dailyProgressBar: LinearProgressIndicator
    private lateinit var weeklyProgressBar: LinearProgressIndicator

    private lateinit var dailyTimeLeft: TextView
    private lateinit var weeklyTimeLeft: TextView
    private lateinit var catText: TextView

    private lateinit var weeklyActivationButton: TextView

    private var limitTime: Long = 0
    private var limitTimeWeekly: Long = 0
    private var remainingTimeToday: Long = 0
    private var remainingTimeWeekly: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = context?.getSharedPreferences("Options", Context.MODE_PRIVATE)!!

        dailyProgressBar = view.findViewById(R.id.daily_progress)
        weeklyProgressBar = view.findViewById(R.id.weekly_progress)

        dailyTimeLeft = view.findViewById(R.id.daily_time_left_text)
        weeklyTimeLeft = view.findViewById(R.id.weekly_time_left_text)

        weeklyActivationButton = view.findViewById(R.id.weekly_activation_time_button)

        catImage = view.findViewById(R.id.cat_home)
        catText = view.findViewById(R.id.cat_text)

        val animation = AnimationUtils.loadAnimation(context, R.anim.cat_animation)
        catImage.setOnClickListener {
            catImage.startAnimation(animation)
        }

        loadCat()
        loadProgressBars()
    }

    override fun onResume() {
        super.onResume()
        loadProgressBars()
    }

    /**
     * Method called when the view is created to load a random cat which the user has
     *
     */
    private fun loadCat() {
        //The user
        val auth = Firebase.auth
        val uid = auth.uid.toString()

        //The cats he has in database
        val database =
            FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(uid).child("cats")

        //Gets cats from the database
        database.get().addOnSuccessListener { dataSnapshot ->
            //Get all cats
            val cats = dataSnapshot.children

            // Convert `cats` into a list
            val catList = ArrayList<Cat>()
            cats.forEach { s ->
                val cat = Cat(
                    Integer.parseInt(s.child("id").value.toString()),
                    s.child("rarity").value.toString(),
                    s.child("rarity").value.toString()
                )
                catList.add(cat)
            }
            if (catList.size > 0) {
                // Generate a random index within the list size
                val randomIndex = (0 until catList.size).random()

                // Access the random cat
                val randomCat = catList[randomIndex]
                val imageID = requireContext().resources.getIdentifier(
                    "drawable/${randomCat.catRarity}_${randomCat.catId}",
                    null,
                    requireContext().packageName
                )
                catImage.setImageResource(imageID)
            }
            catText.text = resources.getStringArray(R.array.cat_phrases).random()
        }.addOnCanceledListener {
            Log.d("BON DIA", "On Cancelled")
        }
    }

    /**
     * Sets the text and the progress of the bars according to the saved values on the shared preferences
     *
     */
    private fun loadProgressBars() {
        limitTime = sharedPreferences.getLong("limitTime", 0)
        limitTimeWeekly = sharedPreferences.getLong("limitTimeWeekly", 0)

        remainingTimeToday = sharedPreferences.getLong("remainingTimeToday", limitTime)
        remainingTimeWeekly = sharedPreferences.getLong("remainingTimeWeekly", limitTimeWeekly)

        val dailyProgress = ((remainingTimeToday.toDouble() / limitTime.toDouble()) * 100).toInt()
        val weeklyProgress =
            ((remainingTimeWeekly.toDouble() / limitTimeWeekly.toDouble()) * 100).toInt()

        dailyProgressBar.postDelayed({
            dailyProgressBar.progress = dailyProgress
        }, 100) // Adjust the delay as needed

        weeklyProgressBar.postDelayed({
            weeklyProgressBar.progress = weeklyProgress
        }, 100) // Adjust the delay as needed


        val dailyHoursAndMinutes = convertLongToHoursAndMinutes(remainingTimeToday)
        val weeklyHoursAndMinutes = convertLongToHoursAndMinutes(remainingTimeWeekly)

        dailyTimeLeft.text = getString(
            R.string.daily_time_left,
            dailyHoursAndMinutes.first,
            dailyHoursAndMinutes.second
        )
        weeklyTimeLeft.text = getString(
            R.string.weekly_time_left,
            weeklyHoursAndMinutes.first,
            weeklyHoursAndMinutes.second
        )

        showWeeklyButtonActivation()
    }

    /**
     * Converts the time to hours and minutes
     *
     * @param millis Time in milliseconds to convert
     * @return A pair containing in the first positions the hours and the second the minutes
     */
    private fun convertLongToHoursAndMinutes(millis: Long): Pair<Int, Int> {
        val totalSeconds = millis / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()

        return Pair(hours, minutes)
    }

    /**
     * Shows a button to the user to activate the weekly time if the user has a limit activated and it has run out of daily time
     *
     */
    private fun showWeeklyButtonActivation() {
        if (remainingTimeToday == 0L && sharedPreferences.getBoolean("isLimitEnabled", false)) {

            weeklyActivationButton.visibility = View.VISIBLE
            if (sharedPreferences.getBoolean("userHasActivatedWeeklyTime", false)) {
                weeklyActivationButton.text = getString(R.string.deactivate_weekly_time)
            }

            weeklyActivationButton.setOnClickListener {
                val editor = sharedPreferences.edit()
                if (!sharedPreferences.getBoolean("userHasActivatedWeeklyTime", false)) {
                    editor.putBoolean("userHasActivatedWeeklyTime", true)
                    weeklyActivationButton.text = getString(R.string.deactivate_weekly_time)
                } else {
                    editor.putBoolean("userHasActivatedWeeklyTime", false)
                    weeklyActivationButton.text = getString(R.string.activate_weekly_time_button)
                }
                editor.apply()
                val intent = Intent("USER_HAS_UPDATED_WEEKLY")
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
            }

        } else {
            weeklyActivationButton.visibility = View.GONE
        }
    }

}
