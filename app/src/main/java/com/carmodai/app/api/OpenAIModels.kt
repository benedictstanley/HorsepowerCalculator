package com.carmodai.app.api

data class OpenAIRequest(
    val model: String = "gpt-4o",
    val messages: List<Message>,
    val max_tokens: Int = 900,
    val temperature: Double = 0.6,
    val response_format: ResponseFormat = ResponseFormat()
)

data class Message(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class DynoDataPoint(
    val rpm: Double,
    val hp: Double,
    val torque: Double
) : java.io.Serializable

data class ChatGptResponse(
    val estimated_hp: Double,
    val explanation: String,
    val dyno_data: List<DynoDataPoint>
)

data class ResponseFormat(
    val type: String = "json_object"
)
