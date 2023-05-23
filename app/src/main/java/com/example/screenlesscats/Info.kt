package com.example.screenlesscats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

        database = FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app").getReference(uid).child("user_data").child("dedication_value")

        database.get().addOnSuccessListener {
            dedication.text = resources.getString(R.string.dedication_v, it.value)
        }

    }
}