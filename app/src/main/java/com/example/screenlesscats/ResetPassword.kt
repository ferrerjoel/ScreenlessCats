package com.example.screenlesscats

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import androidx.compose.material3.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ResetPassword : AppCompatActivity() {
    lateinit var auth: FirebaseAuth //FIREBASE AUTH

    private lateinit var email : TextInputEditText
    private lateinit var resetPwdBtn : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        email = findViewById(R.id.email)
        resetPwdBtn = findViewById(R.id.resetPwdBtn)

        resetPwdBtn.setOnClickListener {
            auth = FirebaseAuth.getInstance()
            // Mail validation
            // If it's not a mail
            if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
                email.error = getString(R.string.invalid_email)
            } else {
                Log.d("DEBUG", email.text.toString())
                auth.sendPasswordResetEmail(email.text.toString())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            com.google.android.material.snackbar.Snackbar.make(
                                findViewById<View>(
                                    android.R.id.content
                                ),
                                getString(R.string.email_sent),
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            )
                                .show()

                            val intent = Intent(this, Login::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            com.google.android.material.snackbar.Snackbar.make(
                                findViewById<View>(
                                    android.R.id.content
                                ),
                                getString(R.string.couldn_t_send_email),
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            )
                                .show()
                        }
                    }
            }
        }

    }
}