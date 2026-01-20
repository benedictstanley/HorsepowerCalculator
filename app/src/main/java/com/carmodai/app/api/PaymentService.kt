package com.carmodai.app.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class CheckoutRequest(
    @SerializedName("amount") val amount: Int,
    @SerializedName("plan_name") val planName: String
)

data class CheckoutResponse(
    @SerializedName("url") val url: String,
    @SerializedName("error") val error: ErrorDetails? = null
)

data class ErrorDetails(
    @SerializedName("message") val message: String
)

data class PaymentSheetResponse(
    @SerializedName("paymentIntent") val paymentIntent: String,
    @SerializedName("ephemeralKey") val ephemeralKey: String,
    @SerializedName("customer") val customer: String,
    @SerializedName("publishableKey") val publishableKey: String
)

interface PaymentService {
    @POST("create-checkout-session")
    suspend fun createCheckoutSession(@Body request: CheckoutRequest): Response<CheckoutResponse>
    
    @POST("payment-sheet")
    suspend fun createPaymentSheet(@Body request: CheckoutRequest): Response<PaymentSheetResponse>

    @POST("create-customer")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): Response<CreateCustomerResponse>

    @POST("create-subscription")
    suspend fun createSubscription(@Body request: CreateSubscriptionRequest): Response<SubscriptionResponse>

    @POST("cancel-subscription")
    suspend fun cancelSubscription(@Body request: CancelSubscriptionRequest): Response<SubscriptionResponse>
}

data class CreateCustomerRequest(
    @SerializedName("email") val email: String
)

data class CreateCustomerResponse(
    @SerializedName("customer") val customerId: String,
    @SerializedName("message") val message: String?
)

data class CreateSubscriptionRequest(
    @SerializedName("customerId") val customerId: String,
    @SerializedName("priceId") val priceId: String
)

data class CancelSubscriptionRequest(
    @SerializedName("subscriptionId") val subscriptionId: String
)

data class SubscriptionResponse(
    @SerializedName("subscriptionId", alternate = ["id"]) val subscriptionId: String,
    @SerializedName("clientSecret") val clientSecret: String? = null,
    @SerializedName("status") val status: String? = null
)
