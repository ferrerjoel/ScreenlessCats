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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var email: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var toRegisterBtn: Button
    private lateinit var loginBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        email = findViewById<TextInputEditText>(R.id.email)

        password = findViewById<TextInputEditText>(R.id.password)

        toRegisterBtn = findViewById<Button>(R.id.toRegisterBtn)

        loginBtn = findViewById<Button>(R.id.loginBtn)

        toRegisterBtn.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
        loginBtn.setOnClickListener {
            signIn()
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and go to home directly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun signIn() {
        //Check that the email camp is a mail and tries to log in
        if (!Patterns.EMAIL_ADDRESS.matcher(email.toString()).matches()) {
            Toast.makeText(baseContext, "Mail not valid", Toast.LENGTH_SHORT).show()
        } else {
            auth.signInWithEmailAndPassword(email.toString(), password.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val intent = Intent(this, Home::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }


    }
}