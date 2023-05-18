package com.example.screenlesscats

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.screenlesscats.data.Cat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match

class HomeFragment:Fragment(R.layout.fragment_home) {

    private lateinit var catImage : ImageView
    private lateinit var sharedPreferences : SharedPreferences

    private lateinit var dailyProgressBar : LinearProgressIndicator
    private lateinit var weeklyProgressBar : LinearProgressIndicator

    private lateinit var dailyTimeLeft : TextView
    private lateinit var weeklyTimeLeft : TextView

    private var limitTime: Long = 0
    private var remainingTimeToday: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = context?.getSharedPreferences("Options", Context.MODE_PRIVATE)!!

        dailyProgressBar = view.findViewById(R.id.daily_progress)
        weeklyProgressBar = view.findViewById(R.id.weekly_progress)

        dailyTimeLeft = view.findViewById(R.id.daily_time_left_text)
        weeklyTimeLeft = view.findViewById(R.id.weekly_time_left_text)

        catImage = view.findViewById(R.id.cat_home)

        val animation = AnimationUtils.loadAnimation(context, R.anim.cat_animation)
        catImage.setOnClickListener() {
            catImage.startAnimation(animation)
        }

        loadCat()
        loadProgressBars()
    }

    private fun loadCat() {
        val auth = Firebase.auth
        val uid = auth.uid.toString()

        val database = FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app").getReference(uid).child("cats")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
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
                    val imageID = requireContext().resources.getIdentifier("drawable/${randomCat.catRarity}_${randomCat.catId}", null, requireContext().packageName)
                    catImage.setImageResource(imageID)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("BON DIA", "On Cancelled")
            }
        })



    }

    private fun loadProgressBars() {
        limitTime = sharedPreferences.getLong("limitTime", 0)
        remainingTimeToday = sharedPreferences.getLong("remainingTimeToday", limitTime)

        val dailyProgress = ((remainingTimeToday.toDouble() / limitTime.toDouble()) * 100).toInt()

        dailyProgressBar.progress = dailyProgress


        val dailyHoursAndMinutes = convertLongToHoursAndMinutes(remainingTimeToday)

        dailyTimeLeft.text = getString(R.string.daily_time_left, dailyHoursAndMinutes.first, dailyHoursAndMinutes.second)
    }

    private fun convertLongToHoursAndMinutes(millis: Long): Pair<Int, Int> {
        val totalSeconds = millis / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()

        return Pair(hours, minutes)
    }

}
