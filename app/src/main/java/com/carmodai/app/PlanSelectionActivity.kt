package com.carmodai.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlanSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_selection)

        val btnStarter = findViewById<Button>(R.id.btnStarter)
        val btnPro = findViewById<Button>(R.id.btnPro)
        val btnVip = findViewById<Button>(R.id.btnVip)

        btnStarter.setOnClickListener {
            launchPayment("Starter", "$1.00 / month")
        }

        btnPro.setOnClickListener {
            launchPayment("Pro", "$5.00 / month")
        }

        btnVip.setOnClickListener {
            launchPayment("VIP", "$50.00 / month")
        }
    }

    private fun launchPayment(planName: String, price: String) {
        if (UserManager.isLoggedIn(this)) {
            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra("PLAN_NAME", planName)
                putExtra("PLAN_PRICE", price)
            }
            startActivity(intent)
        } else {
            // Force Login/Signup first
            val intent = Intent(this, LoginActivity::class.java).apply {
                putExtra("RETURN_TO_PAYMENT", true)
                putExtra("PLAN_NAME", planName)
                putExtra("PLAN_PRICE", price)
            }
            startActivity(intent)
        }
        finish()
    }
}
