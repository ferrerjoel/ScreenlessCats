package com.example.screenlesscats

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var changePasswordBtn: Button

    /*
        Sign in method
     */
    private fun signIn() {
        //Check mail pattern
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = getString(R.string.invalid_mail)
        //Check password pattern
        } else if (password.text.toString().length < 6) {
            password.error = getString(R.string.invalid_pwd)
        } else {
            //Sign in
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
                        Snackbar.make(findViewById<View>(android.R.id.content), "Authentication Failed", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if(!Options.isAccessServiceEnabled(this)){
            showPermissionsAccessibilityWarning()
        }

        if(!Options.isUsagePermissionsEnabled(this)){
            showPermissionsUsageWarning()
        }

        auth = Firebase.auth

        email = findViewById(R.id.email)

        password = findViewById(R.id.password)

        toRegisterBtn = findViewById(R.id.toRegisterBtn)

        loginBtn = findViewById(R.id.loginBtn)

        changePasswordBtn = findViewById(R.id.changePasswordBtn)

        toRegisterBtn.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
        loginBtn.setOnClickListener {
            signIn()
        }
        changePasswordBtn.setOnClickListener {
            val intent = Intent(this, ResetPassword::class.java)
            startActivity(intent)
            finish()
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

    private fun showPermissionsAccessibilityWarning() {
        this.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.we_need_accessibility_title))
                .setMessage(resources.getString(R.string.we_need_accessibility))
                .setNeutralButton(resources.getString(R.string.i_understand)) { dialog, which ->
                    Options.requestAppAccessibilitySettings(this)
                }
                .setOnDismissListener {
                    Options.requestAppAccessibilitySettings(this)
                }
                .show()
        }
    }

    private fun showPermissionsUsageWarning() {
        this.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.we_need_usage_title))
                .setMessage(resources.getString(R.string.we_need_usage))
                .setNeutralButton(resources.getString(R.string.i_understand)) { dialog, which ->
                    Options.requestAppUsageSettings(this)
                }
                .setOnDismissListener {
                    Options.requestAppUsageSettings(this)
                }
                .show()
        }
    }
}