package com.enayet.roomx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RoomInfo(
    val id: String,
    val code: String,
    val name: String,
    val description: String,
    val role: String,
    val members: Int,
    val capacity: Int
)

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

    private fun request(path: String, body: JSONObject, withAuth: Boolean): String {
        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            val token = accessToken
            if (withAuth && token != null) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val json = try {
                    JSONObject(text)
                } catch (e: Exception) {
                    JSONObject()
                }
                var msg = json.optString("msg")
                    .ifBlank { json.optString("error_description") }
                    .ifBlank { json.optString("message") }
                    .ifBlank { json.optString("error") }
                    .ifBlank { "Error $code" }
                if (msg.contains("JWT", ignoreCase = true)) {
                    msg = "Session expired. Please log out and log in again."
                }
                throw Exception(msg)
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val text = request(path, body, false)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
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

    suspend fun createRoom(name: String, description: String): RoomInfo =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_name", name)
                .put("p_description", description)
            val json = JSONObject(request("/rest/v1/rpc/create_room", body, true))
            RoomInfo(
                id = json.getString("id"),
                code = json.getString("code"),
                name = json.getString("name"),
                description = description,
                role = "owner",
                members = 1,
                capacity = 50
            )
        }

    suspend fun joinRoom(code: String): RoomInfo =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_code", code)
            val json = JSONObject(request("/rest/v1/rpc/join_room", body, true))
            RoomInfo(
                id = json.getString("id"),
                code = json.getString("code"),
                name = json.getString("name"),
                description = "",
                role = "member",
                members = 0,
                capacity = 50
            )
        }

    suspend fun leaveRoom(roomId: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_room_id", roomId)
            request("/rest/v1/rpc/leave_room", body, true)
            Unit
        }

    suspend fun myRooms(): List<RoomInfo> =
        withContext(Dispatchers.IO) {
            val text = request("/rest/v1/rpc/my_rooms", JSONObject(), true)
            val arr = JSONArray(if (text.isBlank()) "[]" else text)
            val list = mutableListOf<RoomInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    RoomInfo(
                        id = o.getString("id"),
                        code = o.getString("code"),
                        name = o.getString("name"),
                        description = o.optString("description", ""),
                        role = o.optString("role", "member"),
                        members = o.optInt("members", 0),
                        capacity = o.optInt("capacity", 50)
                    )
                )
            }
            list
        }
}
