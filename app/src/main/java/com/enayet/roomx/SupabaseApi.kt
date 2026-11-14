package com.enayet.roomx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupabaseApi {
    private const val BASE_URL = "https://auvathuvfzfeqnwwkoyb.supabase.co"
    private const val API_KEY = "sb_publishable_MrS0c9eYluNL9DHB_5AALg_x0_ayu7T"

    var accessToken: String? = null
        private set
    var userId: String? = null
        private set
    var username: String? = null
        private set
    var email: String? = null
        private set

    val isLoggedIn: Boolean
        get() = accessToken != null

    private fun post(path: String, body: JSONObject): JSONObject {
        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = try {
                if (text.isNotBlank()) JSONObject(text) else JSONObject()
            } catch (e: Exception) {
                JSONObject()
            }

            if (code !in 200..299) {
                val msg = json.optString("msg")
                    .ifBlank { json.optString("error_description") }
                    .ifBlank { json.optString("message") }
                    .ifBlank { json.optString("error") }
                    .ifBlank { "Error $code" }
                throw Exception(msg)
            }
            return json
        } finally {
            conn.disconnect()
        }
    }

    private fun applySession(json: JSONObject) {
        val token = json.optString("access_token")
        if (token.isBlank()) {
            throw Exception("Please confirm your email, then log in.")
        }
        val user = json.getJSONObject("user")
        accessToken = token
        userId = user.optString("id")
        email = user.optString("email")
        username = user.optJSONObject("user_metadata")?.optString("username")
    }

    suspend fun signUp(email: String, password: String, username: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("data", JSONObject().put("username", username))
            applySession(post("/auth/v1/signup", body))
        }

    suspend fun signIn(email: String, password: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("email", email)
                .put("password", password)
            applySession(post("/auth/v1/token?grant_type=password", body))
        }

    fun signOut() {
        accessToken = null
        userId = null
        username = null
        email = null
    }
}
