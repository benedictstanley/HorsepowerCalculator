package com.carmodai.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.carmodai.app.db.AppDatabase
import com.carmodai.app.db.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "car-mods-db")
            .fallbackToDestructiveMigration()
            .build()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val layoutPhoneNumber = findViewById<android.view.View>(R.id.layoutPhoneNumber)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvCreateAccount = findViewById<TextView>(R.id.tvCreateAccount)

        var isLoginMode = true

        tvCreateAccount.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                layoutPhoneNumber.visibility = android.view.View.GONE
                btnLogin.text = "Sign In"
                tvCreateAccount.text = "Create Account"
            } else {
                layoutPhoneNumber.visibility = android.view.View.VISIBLE
                btnLogin.text = "Create Account"
                tvCreateAccount.text = "Already have an account? Sign In"
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val phoneNumber = etPhoneNumber.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isLoginMode && phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                // Admin backdoor
                if (email == "buthelloalton@gmail.com" && password == "1234567890") {
                    // Create admin user if not exists or update
                    var admin = db.userDao().getUser(email)
                    if (admin == null) {
                        admin = User(
                            email = email, 
                            password = password,
                            phoneNumber = phoneNumber,
                            planName = SubscriptionManager.PLAN_VIP,
                            isUnlimited = true,
                            calculationsLeft = 999999,
                            dynoRunsLeft = 999999
                        )
                        db.userDao().insert(admin)
                    } else {
                        val updatedAdmin = admin.copy(
                            planName = SubscriptionManager.PLAN_VIP,
                            isUnlimited = true,
                            calculationsLeft = 999999,
                            dynoRunsLeft = 999999
                        )
                        db.userDao().update(updatedAdmin)
                    }

                    UserManager.login(this@LoginActivity, email)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Admin Login: Unlimited Access Granted", Toast.LENGTH_SHORT).show()
                        navigateToNextScreen()
                    }
                    return@launch
                }

                // Normal user flow
                var user = db.userDao().getUser(email)
                
                if (isLoginMode) {
                    if (user == null) {
                         withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "User not found. Please create an account.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    
                    // Verify password
                    if (user.password != password) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Invalid Password", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                } else {
                    // Registration Mode
                    if (user != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Account already exists for this email.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Register new user
                    user = User(email = email, password = password, phoneNumber = phoneNumber)
                    db.userDao().insert(user)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                    }
                }

                // Login success
                UserManager.login(this@LoginActivity, email)
                
                withContext(Dispatchers.Main) {
                    navigateToNextScreen()
                }
            }
        }
    }

    private fun navigateToNextScreen() {
        if (intent.getBooleanExtra("RETURN_TO_PAYMENT", false)) {
            val paymentIntent = Intent(this, PaymentActivity::class.java)
            paymentIntent.putExtra("PLAN_NAME", intent.getStringExtra("PLAN_NAME"))
            paymentIntent.putExtra("PLAN_PRICE", intent.getStringExtra("PLAN_PRICE"))
            startActivity(paymentIntent)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
