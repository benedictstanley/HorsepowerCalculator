package com.carmodai.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.carmodai.app.api.Message
import com.carmodai.app.api.OpenAIRequest
import com.carmodai.app.api.RetrofitClient
import com.carmodai.app.api.ChatGptResponse
import com.carmodai.app.api.DynoDataPoint
import com.carmodai.app.db.AppDatabase
import com.carmodai.app.db.CarBuild
import com.carmodai.app.db.User
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.EOFException

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var currentUser: User? = null
    private var currentCalculatedHp: Int = 0
    private var currentDynoData: ArrayList<DynoDataPoint>? = null

    private val garageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val build = result.data?.getSerializableExtra("selected_build") as? CarBuild
            build?.let {
                findViewById<TextInputEditText>(R.id.etYear).setText(it.year)
                findViewById<TextInputEditText>(R.id.etMake).setText(it.make)
                findViewById<TextInputEditText>(R.id.etModel).setText(it.model)
                findViewById<TextInputEditText>(R.id.etBaseHp).setText(it.baseHp.toString())
                findViewById<TextInputEditText>(R.id.etMods).setText(it.mods)
                findViewById<TextView>(R.id.tvResult).text = "Estimated HP: ${it.estimatedHp}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "car-mods-db")
            .fallbackToDestructiveMigration()
            .build()

        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)
        val btnSubscribe = findViewById<Button>(R.id.btnSubscribe)
        val etYear = findViewById<TextInputEditText>(R.id.etYear)
        val etMake = findViewById<TextInputEditText>(R.id.etMake)
        val etModel = findViewById<TextInputEditText>(R.id.etModel)
        val etBaseHp = findViewById<TextInputEditText>(R.id.etBaseHp)
        val etMods = findViewById<TextInputEditText>(R.id.etMods)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val tvResult = findViewById<TextView>(R.id.tvResult)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnGarage = findViewById<Button>(R.id.btnViewGarage)
        val btnDyno = findViewById<Button>(R.id.btnDynoChart)
        val btnSignOut = findViewById<Button>(R.id.btnSignOut)

        // Refresh user data on resume/start
        refreshUser()

        tvSignIn.setOnClickListener {
            if (currentUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        btnSignOut.setOnClickListener {
            UserManager.logout(this)
            currentUser = null
            updateUIForUser(null)
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }

        btnSubscribe.setOnClickListener {
            startActivity(Intent(this, PlanSelectionActivity::class.java))
        }

        btnCalculate.setOnClickListener {
            val user = currentUser
            if (user == null) {
                Toast.makeText(this, "Please Sign In First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!user.isUnlimited && user.calculationsLeft <= 0) {
                Toast.makeText(this, "No calculations left! Please upgrade.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, PlanSelectionActivity::class.java))
                return@setOnClickListener
            }

            val make = etMake.text.toString()
            val baseHpStr = etBaseHp.text.toString()
            val mods = etMods.text.toString()

            if (baseHpStr.isEmpty() || make.isEmpty()) {
                Toast.makeText(this, "Enter car details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            calculateHp(baseHpStr.toInt(), mods, tvResult)
        }

        btnSave.setOnClickListener {
            // Save logic
            if (currentCalculatedHp == 0) return@setOnClickListener
            
            lifecycleScope.launch(Dispatchers.IO) {
                val build = CarBuild(
                    year = etYear.text.toString(),
                    make = etMake.text.toString(),
                    model = etModel.text.toString(),
                    baseHp = etBaseHp.text.toString().toIntOrNull() ?: 0,
                    mods = etMods.text.toString(),
                    estimatedHp = currentCalculatedHp
                )
                db.carBuildDao().insert(build)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved to Garage!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnGarage.setOnClickListener {
            garageLauncher.launch(Intent(this, GarageActivity::class.java))
        }

        btnDyno.setOnClickListener {
            val user = currentUser
            if (user == null) {
                 Toast.makeText(this, "Please Sign In", Toast.LENGTH_SHORT).show()
                 return@setOnClickListener
            }

            if (!user.isUnlimited && user.dynoRunsLeft <= 0) {
                Toast.makeText(this, "No Dyno runs left! Please upgrade.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, PlanSelectionActivity::class.java))
            } else {
                // Show Dialog to ask for RPM
                val input = android.widget.EditText(this)
                input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                input.hint = "Enter Max RPM (e.g. 8000)"

                android.app.AlertDialog.Builder(this)
                    .setTitle("Dyno Chart Settings")
                    .setMessage("Enter the maximum RPM for the chart:")
                    .setView(input)
                    .setPositiveButton("Generate") { _, _ ->
                        val maxRpmStr = input.text.toString()
                        val maxRpm = maxRpmStr.toIntOrNull() ?: 15000

                        // Deduct run if not unlimited
                        if (!user.isUnlimited) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val updatedUser = user.copy(dynoRunsLeft = user.dynoRunsLeft - 1)
                                db.userDao().update(updatedUser)
                                refreshUser()
                            }
                        }

                        startActivity(Intent(this, DynoChartActivity::class.java).apply {
                            if (currentDynoData != null) {
                                val filteredData = ArrayList(currentDynoData!!.filter { it.rpm <= maxRpm.toDouble() })
                                putExtra("DYNO_DATA", filteredData)
                            }
                            putExtra("MAX_RPM", maxRpm)
                        })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUser()
    }

    private fun refreshUser() {
        val email = UserManager.getCurrentUserEmail(this) ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            currentUser = db.userDao().getUser(email)
            withContext(Dispatchers.Main) {
                updateUIForUser(currentUser)
            }
        }
    }

    private fun updateUIForUser(user: User?) {
        val btnDyno = findViewById<Button>(R.id.btnDynoChart)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)
        val btnSignOut = findViewById<Button>(R.id.btnSignOut)
        
        if (user != null) {
            val statusText = if (user.isUnlimited) {
                "User: ${user.email} (Unlimited)"
            } else {
                "User: ${user.email} (Free: ${user.calculationsLeft} calcs left)"
            }
            tvSignIn.text = statusText
            tvSignIn.isClickable = false // Disable clicking sign in text when logged in
            btnSignOut.visibility = View.VISIBLE
            
            // Show Dyno button ONLY if calculation is done (data exists) AND user has access
            if (currentDynoData != null && (user.isUnlimited || user.dynoRunsLeft > 0)) {
                btnDyno.visibility = View.VISIBLE
            } else if (currentDynoData != null) {
                // Keep visible but it will prompt upgrade on click if they have data but no runs left
                btnDyno.visibility = View.VISIBLE 
            } else {
                // Hide if no calculation has been performed yet
                btnDyno.visibility = View.GONE
            }
        } else {
            tvSignIn.text = "Sign In / Register"
            tvSignIn.isClickable = true
            btnSignOut.visibility = View.GONE
            btnDyno.visibility = View.GONE
        }
    }

    private fun calculateHp(baseHp: Int, modsStr: String, tvResult: TextView) {
        tvResult.text = "Calculating with AI..."
        tvResult.gravity = android.view.Gravity.CENTER
        currentDynoData = null
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    I have a car with $baseHp HP (base) and these mods: $modsStr.
                    Estimate the new horsepower and provide a detailed explanation of how each mod affects power, drivability, and the overall powerband.
                    Then define a realistic dyno curve from 2000 RPM to 8000 RPM considering turbo lag, peak torque, and peak horsepower.
                    
                    Output STRICTLY the following JSON object (no markdown, no code fences, no extra text):
                    {
                      "estimated_hp": 123,
                      "explanation": "detailed explanation text",
                      "dyno_data": [
                        {"rpm": 2000, "hp": 100, "torque": 262.6},
                        {"rpm": 2500, "hp": 110, "torque": 231.1},
                        {"rpm": 3000, "hp": 120, "torque": 210.1}
                      ]
                    }
                    The dyno_data array must:
                    - Contain objects with numeric rpm, hp, and torque fields.
                    - Use RPM values between 2000 and 8000.
                    - Keep HP and Torque consistent: torque ≈ (hp * 5252) / rpm.
                """.trimIndent()

                val request = OpenAIRequest(
                    messages = listOf(
                        Message(role = "system", content = "You are an expert automotive tuner. You output only raw JSON. Do not use Markdown code blocks."),
                        Message(role = "user", content = prompt)
                    )
                )

                var lastError: Exception? = null
                var response: retrofit2.Response<com.carmodai.app.api.OpenAIResponse>? = null

                repeat(3) {
                    try {
                        val r = RetrofitClient.instance.getCompletion(RetrofitClient.API_KEY, request)
                        if (r.isSuccessful) {
                            response = r
                            return@repeat
                        } else {
                            lastError = Exception("API Error: ${r.code()} ${r.message()}")
                        }
                    } catch (e: EOFException) {
                        lastError = e
                    } catch (e: java.io.IOException) {
                        lastError = e
                    }

                    if (response == null) {
                        kotlinx.coroutines.delay(1500)
                    }
                }

                val finalResponse = response ?: throw (lastError ?: Exception("AI request failed"))
                
                val content = finalResponse.body()?.choices?.firstOrNull()?.message?.content ?: "{}"
                val startIndex = content.indexOf("{")
                val endIndex = content.lastIndexOf("}")
                
                if (startIndex == -1 || endIndex == -1) {
                     throw Exception("No JSON found in response")
                }
                
                var jsonString = content.substring(startIndex, endIndex + 1)
                jsonString = removeTrailingCommas(jsonString)

                val gson = com.google.gson.GsonBuilder().setLenient().create()
                val root: JsonObject = try {
                    gson.fromJson(jsonString, JsonObject::class.java)
                } catch (e: Exception) {
                    // One last try: remove all newlines/tabs to fix some parsing issues
                    try {
                         val flatJson = jsonString.replace("\n", "").replace("\r", "").replace("\t", "")
                         gson.fromJson(flatJson, JsonObject::class.java)
                    } catch (e2: Exception) {
                         throw Exception("Invalid AI JSON format: ${e.message}", e)
                    }
                }

                val estimatedHp = root.get("estimated_hp")?.let { el ->
                    when {
                        el.isJsonNull -> 0.0
                        el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asDouble
                        el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                } ?: 0.0

                val explanation = root.get("explanation")?.let { el ->
                    when {
                        el.isJsonNull -> ""
                        el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString
                        else -> el.toString()
                    }
                } ?: ""

                val dynoList = mutableListOf<DynoDataPoint>()
                val dynoEl = root.get("dyno_data")
                if (dynoEl != null && dynoEl.isJsonArray) {
                    val arr = dynoEl.asJsonArray
                    for (item in arr) {
                        if (!item.isJsonObject) continue
                        val obj = item.asJsonObject

                        val rpmVal = obj.get("rpm")
                        val hpVal = obj.get("hp")
                        val tqVal = obj.get("torque")

                        if (rpmVal == null || hpVal == null || tqVal == null) continue

                        val rpm = when {
                            rpmVal.isJsonNull -> 0.0
                            rpmVal.isJsonPrimitive && rpmVal.asJsonPrimitive.isNumber -> rpmVal.asDouble
                            rpmVal.isJsonPrimitive && rpmVal.asJsonPrimitive.isString -> rpmVal.asString.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val hp = when {
                            hpVal.isJsonNull -> 0.0
                            hpVal.isJsonPrimitive && hpVal.asJsonPrimitive.isNumber -> hpVal.asDouble
                            hpVal.isJsonPrimitive && hpVal.asJsonPrimitive.isString -> hpVal.asString.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val torque = when {
                            tqVal.isJsonNull -> 0.0
                            tqVal.isJsonPrimitive && tqVal.asJsonPrimitive.isNumber -> tqVal.asDouble
                            tqVal.isJsonPrimitive && tqVal.asJsonPrimitive.isString -> tqVal.asString.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        dynoList.add(DynoDataPoint(rpm, hp, torque))
                    }
                }

                val finalDynoData = ArrayList<DynoDataPoint>().apply {
                    if (dynoList.size >= 2) {
                        val sorted = dynoList.sortedBy { it.rpm }
                        val minRpm = sorted.first().rpm.toInt()
                        val maxRpm = sorted.last().rpm.toInt()
                        var index = 0
                        var rpm = minRpm
                        while (rpm <= maxRpm) {
                            while (index < sorted.size - 2 && sorted[index + 1].rpm < rpm) {
                                index++
                            }
                            val p1 = sorted[index]
                            val p2 = sorted[if (index + 1 < sorted.size) index + 1 else sorted.size - 1]
                            val x1 = p1.rpm
                            val x2 = p2.rpm
                            val t = if (x2 - x1 == 0.0) 0.0 else (rpm.toDouble() - x1) / (x2 - x1)
                            val torque = p1.torque + (p2.torque - p1.torque) * t
                            val hp = (torque * rpm) / 5252.0
                            add(DynoDataPoint(rpm.toDouble(), hp, torque))
                            rpm += 250
                        }
                    } else {
                        val maxRpm = 15000
                        val peakTorqueRpm = 6500
                        val baseForTorque = if (estimatedHp > 0.0) estimatedHp else baseHp.toDouble()
                        val estimatedPeakTorque = (baseForTorque * 5252) / (maxRpm * 0.85)
                        for (rpm in 2000..maxRpm step 250) {
                            val dist = (rpm - peakTorqueRpm) / 6000.0
                            val dropOff = dist * dist * 0.5
                            var torque = estimatedPeakTorque * (1.0 - dropOff)
                            if (torque < estimatedPeakTorque * 0.4) torque = estimatedPeakTorque * 0.4
                            val hp = (torque * rpm) / 5252
                            add(DynoDataPoint(rpm.toDouble(), hp, torque))
                        }
                    }
                }

                currentCalculatedHp = estimatedHp.toInt()
                currentDynoData = finalDynoData

                // Update Usage
                val user = currentUser
                if (user != null && !user.isUnlimited) {
                    val updatedUser = user.copy(calculationsLeft = user.calculationsLeft - 1)
                    db.userDao().update(updatedUser)
                    currentUser = db.userDao().getUser(user.email) // Reload
                }

                withContext(Dispatchers.Main) {
                    val display = StringBuilder()
                    display.append("**Estimated HP:** ${currentCalculatedHp} hp\n\n")
                    display.append("**Explanation:**\n$explanation\n\n")
                    display.append("**Dyno Chart Ready:** Tap 'View Dyno Chart' to see the curve generated by AI.")
                    
                    tvResult.text = display.toString()
                    updateUIForUser(currentUser)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = when {
                        e.message?.contains("Invalid AI JSON format") == true ->
                            "AI response was not readable. Using offline estimation."
                        e is EOFException || e.cause is EOFException ->
                            "AI connection was interrupted. Using offline estimation."
                        e.message?.contains("End of input") == true ->
                            "AI response was not readable. Using offline estimation."
                        else ->
                            e.message ?: "Unknown error"
                    }
                    Toast.makeText(this@MainActivity, "AI Error: $errorMsg", Toast.LENGTH_LONG).show()
                    tvResult.text = "AI connection failed ($errorMsg). Using offline estimation..."
                    calculateHpOffline(baseHp, modsStr, tvResult)
                }
            }
        }
    }

    private fun removeTrailingCommas(json: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < json.length) {
            val c = json[i]
            if (c == ',') {
                var j = i + 1
                while (j < json.length && json[j].isWhitespace()) {
                    j++
                }
                if (j < json.length && (json[j] == '}' || json[j] == ']')) {
                    i++
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun calculateHpOffline(baseHp: Int, modsStr: String, tvResult: TextView) {
        lifecycleScope.launch(Dispatchers.Default) {
            val modList = modsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val breakdown = StringBuilder()
            val explanations = StringBuilder()
            var currentHp = baseHp
            var totalGain = 0
            
            // Helper to get mod details
            fun getModDetails(mod: String): Pair<Int, String> {
                val lowerMod = mod.lowercase()
                return when {
                    lowerMod.contains("turbo") || lowerMod.contains("boost") -> 100 to "The turbocharger increases the amount of air entering the engine, allowing more fuel to be added for combustion, resulting in a significant increase in power output."
                    lowerMod.contains("supercharger") -> 80 to "A supercharger forces more air into the engine intake, boosting power instantly without lag."
                    lowerMod.contains("exhaust") || lowerMod.contains("header") || lowerMod.contains("pipe") -> 20 to "The exhaust system helps the engine 'breathe' better by reducing back pressure, improving scavenging, and enhancing engine efficiency."
                    lowerMod.contains("intake") || lowerMod.contains("filter") -> 15 to "A cold air intake allows denser, oxygen-rich air to enter the engine, improving combustion efficiency."
                    lowerMod.contains("tune") || lowerMod.contains("ecu") || lowerMod.contains("chip") -> 35 to "ECU tuning optimizes fuel maps, ignition timing, and boost pressure to extract maximum performance from the engine."
                    lowerMod.contains("cam") -> 40 to "Performance camshafts increase valve lift and duration, allowing more air/fuel mixture into the cylinders."
                    lowerMod.contains("injector") || lowerMod.contains("fuel") -> 10 to "Upgraded fuel injectors ensure the engine receives enough fuel to match the increased airflow."
                    else -> 5 to "Minor performance improvement from $mod."
                }
            }

            breakdown.append("**Modifications and Horsepower Estimates (Offline Mode):**\n\n")

            modList.forEachIndexed { index, mod ->
                val (gain, explanation) = getModDetails(mod)
                totalGain += gain
                breakdown.append("${index + 1}. $mod:\n- Estimated horsepower gain: $gain hp\n\n")
                
                if (!explanations.contains(explanation)) {
                    explanations.append("$explanation ")
                }
            }
            
            currentHp += totalGain
            currentCalculatedHp = currentHp

            breakdown.append("**Total Estimated New Horsepower:**\n")
            breakdown.append("$baseHp hp (base)")
            modList.forEachIndexed { _, mod ->
                val (gain, _) = getModDetails(mod)
                breakdown.append(" + $gain hp ($mod)")
            }
            breakdown.append(" = $currentHp hp\n\n")

            breakdown.append("**Explanation:**\n")
            breakdown.append(explanations.toString().trim())
            
            // Generate dummy dyno data for offline mode
            currentDynoData = ArrayList()
            val peakTorqueRpm = 6500
            val maxRpm = 15000
            
            // Estimate peak torque from final HP (approximate)
            // HP = Torque * RPM / 5252  => Torque = HP * 5252 / RPM
            // Let's assume peak power is near max RPM, so peak torque is roughly derived
            val estimatedPeakTorque = (currentHp * 5252) / (maxRpm * 0.85) // rough estimate
            
            for (rpm in 2000..15000 step 250) {
                 // Simulate a torque curve: roughly parabolic peaking at peakTorqueRpm
                 // Torque drop-off factor away from peak
                 val dist = (rpm - peakTorqueRpm) / 6000.0 // Normalize distance
                 val dropOff = dist * dist * 0.5 // Parabolic drop
                 
                 var torque = estimatedPeakTorque * (1.0 - dropOff)
                 if (torque < estimatedPeakTorque * 0.4) torque = estimatedPeakTorque * 0.4 // Min floor
                 
                 val hp = (torque * rpm) / 5252
                 
                 currentDynoData?.add(DynoDataPoint(rpm.toDouble(), hp, torque))
            }

            // Update Usage
            val user = currentUser
            if (user != null && !user.isUnlimited) {
                val updatedUser = user.copy(calculationsLeft = user.calculationsLeft - 1)
                db.userDao().update(updatedUser)
                // Reload user
                currentUser = db.userDao().getUser(user.email)
            }

            withContext(Dispatchers.Main) {
                tvResult.text = breakdown.toString()
                tvResult.gravity = android.view.Gravity.CENTER
                updateUIForUser(currentUser)
            }
        }
    }
}
