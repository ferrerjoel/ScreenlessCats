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

class Info : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var auth : FirebaseAuth
    private lateinit var dedication : TextView
    private lateinit var topBar : MaterialToolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        dedication = findViewById(R.id.dedication_v)
        topBar = findViewById(R.id.top_app_bar)
        topBar.setNavigationOnClickListener {
            finish()
        }

        auth = Firebase.auth
        val uid = auth.uid.toString()

        var commonText = findViewById<TextView>(R.id.commonText)
        var rareText = findViewById<TextView>(R.id.rareText)
        var veryrareText = findViewById<TextView>(R.id.veryrareText)
        var epicText = findViewById<TextView>(R.id.epicText)
        var legendaryText = findViewById<TextView>(R.id.legendaryText)
        var mythicText = findViewById<TextView>(R.id.mythicText)

        database = FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app").getReference(uid).child("user_data").child("dedication_value")

        database.get().addOnSuccessListener {
            dedication.text = resources.getString(R.string.dedication_v, it.value.toString().toFloat())
            val commonV = 60 - (it.value.toString().toFloat()/100*(60-5))
            val rareV = 22 - (it.value.toString().toFloat()/100*(22-1))
            val veryrareV = 11.5 - (it.value.toString().toFloat()/100*(11.5-0.5))
            val epicV = 5 + (it.value.toString().toFloat()/100*(60-5))
            val legendaryV = 1 + (it.value.toString().toFloat()/100*(22-1))
            val mythicV = 0.5 + (it.value.toString().toFloat()/100*(11.5-0.5))

            Log.d("VALUES", mythicV.toString())
            commonText.text = resources.getString(R.string.common_60, commonV)
            rareText.text = resources.getString(R.string.rare_22, rareV)
            veryrareText.text = resources.getString(R.string.very_rare_11_5, veryrareV)
            epicText.text = resources.getString(R.string.epic_5, epicV)
            legendaryText.text = resources.getString(R.string.legendary_1, legendaryV)
            mythicText.text = resources.getString(R.string.mythic_0_5, mythicV)
        }






    }
}