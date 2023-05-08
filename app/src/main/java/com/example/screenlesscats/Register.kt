package com.example.screenlesscats

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class Register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var username: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var toLoginBtn: Button
    private lateinit var signupBtn: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        username = findViewById<TextInputEditText>(R.id.username)
        email = findViewById<TextInputEditText>(R.id.email)
        password = findViewById<TextInputEditText>(R.id.password)

        toLoginBtn = findViewById<Button>(R.id.toLoginBtn)
        signupBtn = findViewById<Button>(R.id.signupBtn)

        toLoginBtn.setOnClickListener{
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        signupBtn.setOnClickListener{
            signUp()
        }


    }

    private fun signUp(){
        auth.createUserWithEmailAndPassword(email.toString(), password.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun updateUI(user: FirebaseUser?){
        if(user != null){
            val date = Calendar.getInstance().time
            val userID = user.uid.toString()

            val data: HashMap<String, Any> = HashMap<String, Any>()
            val cats: HashMap<String, Any> = HashMap<String, Any>()
            val userData: HashMap<String, Any> = HashMap<String, Any>()
            val limits: HashMap<String, Any> = HashMap<String, Any>()

            data["id"] = userID
            data["creation_date"] = date
            data["username"] = username
            data["friend_code"] = 0
            data["cats"] = cats
            data["user_data"] = userData

            userData["defined_screen_time"] = -1
            userData["days_streaks"] = limits
            userData["dedication_value"] = 0
            userData["limits"] = limits

            // We create a cursor and we give it a name
            val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://verbumly-default-rtdb.firebaseio.com/")
            val reference: DatabaseReference = database.getReference("")

            if(reference != null) {
                // Create a child with the values of playerData
                reference.child(userID).setValue(data)
                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Data base error", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

    }
}