package com.example.screenlesscats

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import java.util.Calendar


class Register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    companion object{
    private lateinit var username: TextInputEditText
    }
    private lateinit var email: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var toLoginBtn: Button
    private lateinit var signupBtn: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        username = findViewById(R.id.username)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)

        toLoginBtn = findViewById(R.id.toLoginBtn)
        signupBtn = findViewById(R.id.signupBtn)

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
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = getString(R.string.invalid_mail)
        } else if (password.text.toString().length < 6) {
            password.error = getString(R.string.invalid_pwd)
        } else {
            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Snackbar.make(findViewById<View>(android.R.id.content), "Registration Failed", Snackbar.LENGTH_LONG)
                            .show()
                    }

                }
        }
    }

    private fun updateUI(user: FirebaseUser?){
        if(user != null){
            val date = Calendar.getInstance().time
            val userID = user.uid

            val data: HashMap<String, Any> = HashMap()
            val cats: HashMap<String, Any> = HashMap()
            val cat: HashMap<String, Any> = HashMap()
            val userData: HashMap<String, Any> = HashMap()
            val limits: HashMap<String, Any> = HashMap()
            cats["common_0"] = cat
            cat["id"] = 0
            cat["name"] = "Lluis"
            cat["rarity"] = "common"

            data["id"] = userID
            data["creation_date"] = date
            data["username"] = username.text.toString()
            data["friend_code"] = 0
            data["cats"] = cats
            data["user_data"] = userData

            userData["defined_screen_time"] = -1
            userData["days_streaks"] = -1
            userData["dedication_value"] = 0
            userData["limits"] = limits

            // We create a cursor and we give it a name
            val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app")
            val reference: DatabaseReference = database.getReference("")

            // Create a child with the values of playerData
            reference.child(userID).setValue(data)
            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
            finish()
        }

    }
}