package com.carmodai.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun RegisterView() {
    var email by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(value = email,
            label = { Text(text = "Email") },
            onValueChange = { email = it })
        Button(onClick = {
            val body = JSONObject().put("email", email).toString()
                .toRequestBody("application/json".toMediaType())
            val request =
                Request.Builder().url("http://10.0.2.2:4242/create-customer").post(body).build()
            CoroutineScope(Dispatchers.IO).launch {
                OkHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println(JSONObject(response.body!!.string()).get("customer"))
                    }
                }
            }
        }) {
            Text(text = "Submit")
        }
    }
}
