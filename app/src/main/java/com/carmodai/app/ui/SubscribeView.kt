package com.carmodai.app.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet

@Composable
fun SubscribeView(
    clientSecret: String,
    onPaymentResult: (PaymentSheetResult) -> Unit
) {
    val paymentSheet = rememberPaymentSheet(onPaymentResult)

    Button(onClick = {
        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "Car Mods AI",
                // Set `allowsDelayedPaymentMethods` to true if your business handles
                // delayed notification payment methods like US bank accounts.
                allowsDelayedPaymentMethods = true
            )
        )
    }) {
        Text(text = "Subscribe")
    }
}
