package com.carmodai.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.carmodai.app.api.PaymentService
import com.carmodai.app.db.AppDatabase
import com.carmodai.app.ui.SubscribeView
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaymentActivity : AppCompatActivity() {
    companion object {
        private const val ADMIN_PAYPAL_EMAIL = "buthelloalton@gmail.com"
    }

    // Compose State for Client Secret
    private val clientSecretState = mutableStateOf("")
    private lateinit var db: AppDatabase
    private lateinit var planName: String
    private lateinit var planPrice: String
    private lateinit var layoutCreditCard: android.view.View
    private lateinit var rgPaymentMethod: android.widget.RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Safety Check: Ensure user is logged in
        if (!UserManager.isLoggedIn(this)) {
            Toast.makeText(this, "Please sign in to continue payment", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        setContentView(R.layout.activity_payment)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "car-mods-db")
            .fallbackToDestructiveMigration()
            .build()

        planName = intent.getStringExtra("PLAN_NAME") ?: "Pro"
        planPrice = intent.getStringExtra("PLAN_PRICE") ?: "$3.00 / month"

        findViewById<TextView>(R.id.tvPlanName).text = planName
        findViewById<TextView>(R.id.tvPlanPrice).text = planPrice

        layoutCreditCard = findViewById(R.id.layoutCreditCard)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
        val btnPayNow = findViewById<Button>(R.id.btnPayNow)
        val composeView = findViewById<ComposeView>(R.id.composeView)

        // Set Compose Content
        composeView.setContent {
            if (clientSecretState.value.isNotEmpty()) {
                SubscribeView(
                    clientSecret = clientSecretState.value,
                    onPaymentResult = ::onPaymentSheetResult
                )
            } else {
                // Optional: Loading state
                androidx.compose.material3.Text("Loading payment options...")
            }
        }

        // Handle Payment Method Selection
        rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbPaypal) {
                layoutCreditCard.visibility = android.view.View.GONE
                btnPayNow.visibility = android.view.View.VISIBLE
                composeView.visibility = android.view.View.GONE
                btnPayNow.text = "Pay with PayPal"
            } else {
                // Stripe Native (Compose)
                layoutCreditCard.visibility = android.view.View.GONE 
                btnPayNow.visibility = android.view.View.GONE
                composeView.visibility = android.view.View.VISIBLE
                
                if (clientSecretState.value.isEmpty()) {
                    fetchPaymentSheetParams()
                }
            }
        }

        // Initial State
        if (rgPaymentMethod.checkedRadioButtonId == R.id.rbCreditCard) {
            layoutCreditCard.visibility = android.view.View.GONE
            btnPayNow.visibility = android.view.View.GONE
            composeView.visibility = android.view.View.VISIBLE
            fetchPaymentSheetParams()
        }

        btnPayNow.setOnClickListener {
            // Only for PayPal now
            if (rgPaymentMethod.checkedRadioButtonId == R.id.rbPaypal) {
                try {
                    val priceValue = planPrice.replace("$", "").split(" ")[0]
                    val encodedBusiness = java.net.URLEncoder.encode(ADMIN_PAYPAL_EMAIL, "UTF-8")
                    val encodedItemName = java.net.URLEncoder.encode("CarModsAI $planName Subscription", "UTF-8")
                    
                    val returnUrl = java.net.URLEncoder.encode("carmods://payment_success", "UTF-8")
                    val cancelUrl = java.net.URLEncoder.encode("carmods://payment_cancel", "UTF-8")

                    // Updated to _xclick-subscriptions for recurring monthly payments
                    // a3: amount, p3: duration (1), t3: unit (M = Month), src: 1 (recurring)
                    val paypalUrl = "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick-subscriptions&business=$encodedBusiness&currency_code=USD&a3=$priceValue&p3=1&t3=M&src=1&sra=1&item_name=$encodedItemName&return=$returnUrl&cancel_return=$cancelUrl"
                    
                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(paypalUrl))
                    startActivity(browserIntent)

                    Toast.makeText(this, "Opening PayPal Subscription...", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening PayPal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchPaymentSheetParams() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl(com.carmodai.app.api.ApiConfig.BASE_URL) 
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                
                val service = retrofit.create(com.carmodai.app.api.PaymentService::class.java)
                
                // 1. Get Current User Email
                val email = UserManager.getCurrentUserEmail(this@PaymentActivity) ?: return@launch
                
                // 2. Create or Get Customer
                val customerResponse = service.createCustomer(com.carmodai.app.api.CreateCustomerRequest(email))
                if (!customerResponse.isSuccessful || customerResponse.body() == null) {
                    withContext(Dispatchers.Main) {
                         Toast.makeText(this@PaymentActivity, "Failed to create customer", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val customerId = customerResponse.body()!!.customerId
                
                // 3. Create Subscription
                val priceId = when {
                    planName.contains("VIP") -> SubscriptionManager.PRICE_ID_VIP
                    planName.contains("Pro") -> SubscriptionManager.PRICE_ID_PRO
                    else -> SubscriptionManager.PRICE_ID_STARTER
                }

                val subResponse = service.createSubscription(com.carmodai.app.api.CreateSubscriptionRequest(customerId, priceId))
                
                if (subResponse.isSuccessful && subResponse.body() != null) {
                    val data = subResponse.body()!!
                    val clientSecret = data.clientSecret
                    
                    // Note: We are relying on the Application class (MyApp) to init PaymentConfiguration with the PK.
                    
                    withContext(Dispatchers.Main) {
                        if (clientSecret != null) {
                            clientSecretState.value = clientSecret
                        } else {
                            Toast.makeText(this@PaymentActivity, "Error: Missing client secret", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorBody = subResponse.errorBody()?.string()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PaymentActivity, "Failed: $errorBody", Toast.LENGTH_LONG).show()
                        android.util.Log.e("PaymentActivity", "Subscription Error: $errorBody")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PaymentActivity, "Error fetching payment params: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when(paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(this, "Payment Canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(this, "Payment Failed: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
            }
            is PaymentSheetResult.Completed -> {
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show()
                processPayment(planPrice)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent

        val data = intent?.data
        if (data != null) {
            if (data.scheme == "carmods" && data.host == "payment_success") {
                // User returned from PayPal after successful payment
                Toast.makeText(this, "Payment successful! Upgrading your plan...", Toast.LENGTH_SHORT).show()
                processPayment(planPrice)
            } else if (data.scheme == "carmods" && data.host == "payment_cancel") {
                Toast.makeText(this, "Payment cancelled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processPayment(planPrice: String) {
        val finalPlanName = when {
            planPrice.contains("50") -> SubscriptionManager.PLAN_VIP
            planPrice.contains("10") -> SubscriptionManager.PLAN_PRO
            planPrice.contains("3") -> SubscriptionManager.PLAN_STARTER
            else -> SubscriptionManager.PLAN_STARTER
        }
        
        val limits = SubscriptionManager.getPlanLimits(finalPlanName)

        // Update User Profile in DB
        val currentUserEmail = UserManager.getCurrentUserEmail(this)
        if (currentUserEmail != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val user = db.userDao().getUser(currentUserEmail)
                if (user != null) {
                    val updatedUser = user.copy(
                        planName = finalPlanName,
                        isUnlimited = limits.isUnlimited,
                        calculationsLeft = limits.calculations,
                        dynoRunsLeft = limits.dynoRuns,
                        subscriptionExpiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // 30 days
                    )
                    db.userDao().update(updatedUser)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PaymentActivity, "Payment processed! You are now on $finalPlanName Plan.", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        } else {
             Toast.makeText(this, "Error: User not found", Toast.LENGTH_LONG).show()
             finish()
        }
    }
}
