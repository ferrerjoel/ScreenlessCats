package com.example.screenlesscats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
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

        resetPwdBtn.setOnClickListener() {
            auth = FirebaseAuth.getInstance()
            // Validate input
            val email: String = email.text.toString()
            // Mail validation
            // If it's not a mail
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                email.error = "Invalid Mail"
            } else {
                Log.d("DEBUG", email.text.toString())
                auth.sendPasswordResetEmail(email.text.toString())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("DEBUG", "Email sent.")
                            Toast.makeText(
                                this,
                                "Reset password mail sent to your email",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(this, Login::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Incorrect mail", Toast.LENGTH_LONG).show()
                        }
                    }
            }


    }
}