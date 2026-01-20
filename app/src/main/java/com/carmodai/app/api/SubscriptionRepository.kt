package com.carmodai.app.api

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SubscriptionRepository {
    fun createSubscription(priceId: String, customerId: String): SubscriptionResponse? {
        val body = JSONObject()
            .put("priceId", priceId)
            .put("customerId", customerId).toString()
            .toRequestBody("application/json".toMediaType())
        
        // Updated URL to match the server port 4242
        val request = Request.Builder()
            .url("${ApiConfig.BASE_URL}create-subscription")
            .post(body)
            .build()
            
        try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (response.isSuccessful && response.body != null) {
                    return Gson().fromJson(response.body!!.string(), SubscriptionResponse::class.java)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
