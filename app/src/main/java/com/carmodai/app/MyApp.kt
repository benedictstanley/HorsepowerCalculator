package com.carmodai.app

import androidx.multidex.MultiDexApplication
import com.stripe.android.PaymentConfiguration

class MyApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        
        // Initializing Stripe with the LIVE Publishable Key.
        PaymentConfiguration.init(
            applicationContext,
            BuildConfig.STRIPE_PUBLISHABLE_KEY
        )
    }
}
