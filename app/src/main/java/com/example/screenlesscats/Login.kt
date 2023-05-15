package com.example.screenlesscats

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.screenlesscats.block.AppBlockerService
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

        if(!isAccessServiceEnabled(this)){
            requestAppAccessibilitySettings()
        }


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
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = getString(R.string.invalid_mail)
        } else if (password.text.toString().length < 6) {
            password.error = getString(R.string.invalid_pwd)
        } else {
            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
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

    private fun requestAppAccessibilitySettings() {

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)

    }
    private fun isAccessServiceEnabled(context: Context): Boolean {
        val prefString =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString.contains("${context.packageName}/${context.packageName}.block.AppBlockerService")
    }
}