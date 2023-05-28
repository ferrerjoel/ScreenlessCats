package com.example.screenlesscats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

/**
 * Activity to show the probabilities if the cat you receive when you met the requirements to get one
 *
 */
class Info : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var dedication: TextView
    private lateinit var topBar: MaterialToolbar

    /**
     * Calculates and shows the probabilities to get every possible rarity of cats
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        //Get dedication value and topbar
        dedication = findViewById(R.id.dedication_v)
        topBar = findViewById(R.id.top_app_bar)
        //The topbar button goes back
        topBar.setNavigationOnClickListener {
            finish()
        }
        //Get user id
        auth = Firebase.auth
        val uid = auth.uid.toString()

        //Get all the texts
        val commonText = findViewById<TextView>(R.id.commonText)
        val rareText = findViewById<TextView>(R.id.rareText)
        val veryrareText = findViewById<TextView>(R.id.veryrareText)
        val epicText = findViewById<TextView>(R.id.epicText)
        val legendaryText = findViewById<TextView>(R.id.legendaryText)
        val mythicText = findViewById<TextView>(R.id.mythicText)

        //Db reference
        database =
            FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(uid).child("user_data").child("dedication_value")

        //Gets the current dedication value
        database.get().addOnSuccessListener {
            //Adds the calculations to the strings
            dedication.text =
                resources.getString(R.string.dedication_v, it.value.toString().toFloat())
            val commonV = 60 - (it.value.toString().toFloat() / 100 * (60 - 5))
            val rareV = 22 - (it.value.toString().toFloat() / 100 * (22 - 1))
            val veryrareV = 11.5 - (it.value.toString().toFloat() / 100 * (11.5 - 0.5))
            val epicV = 5 + (it.value.toString().toFloat() / 100 * (60 - 5))
            val legendaryV = 1 + (it.value.toString().toFloat() / 100 * (22 - 1))
            val mythicV = 0.5 + (it.value.toString().toFloat() / 100 * (11.5 - 0.5))

            commonText.text = resources.getString(R.string.common_60, commonV)
            rareText.text = resources.getString(R.string.rare_22, rareV)
            veryrareText.text = resources.getString(R.string.very_rare_11_5, veryrareV)
            epicText.text = resources.getString(R.string.epic_5, epicV)
            legendaryText.text = resources.getString(R.string.legendary_1, legendaryV)
            mythicText.text = resources.getString(R.string.mythic_0_5, mythicV)
        }
    }
}