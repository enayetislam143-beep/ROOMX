package com.enayet.roomx

import android.content.Context
import android.content.SharedPreferences
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
    val capacity: Int,
    val status: String = "open"
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val username: String,
    val content: String,
    val createdAt: String
)

data class MemberInfo(
    val userId: String,
    val username: String,
    val role: String
)

data class TournamentInfo(
    val id: String,
    val code: String,
    val name: String,
    val game: String,
    val format: String,
    val participation: String,
    val status: String,
    val isPublic: Boolean,
    val maxParticipants: Int,
    val startAt: String,
    val participants: Int,
    val myRole: String
)

data class TournamentPerson(
    val userId: String,
    val username: String
)

data class TournamentDetail(
    val id: String,
    val code: String,
    val name: String,
    val game: String,
    val description: String,
    val format: String,
    val participation: String,
    val maxParticipants: Int,
    val startAt: String,
    val rules: String,
    val prize: String,
    val contact: String,
    val isPublic: Boolean,
    val status: String,
    val myRole: String,
    val organizer: String,
    val participants: List<TournamentPerson>
)

data class CreatedTournament(
    val id: String,
    val code: String,
    val name: String
)

data class MatchInfo(
    val id: String,
    val round: Int,
    val slot: Int,
    val playerA: String,
    val playerB: String,
    val nameA: String,
    val nameB: String,
    val scoreA: Int?,
    val scoreB: Int?,
    val winner: String,
    val status: String
)

object SupabaseApi {
    private const val BASE_URL = "https://auvathuvfzfeqnwwkoyb.supabase.co"
    private const val API_KEY = "sb_publishable_MrS0c9eYluNL9DHB_5AALg_x0_ayu7T"

    private var prefs: SharedPreferences? = null
    private var refreshToken: String? = null

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

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext
            .getSharedPreferences("roomx_session", Context.MODE_PRIVATE)
        prefs = p
        accessToken = p.getString("access", null)
        refreshToken = p.getString("refresh", null)
        userId = p.getString("user_id", null)
        username = p.getString("username", null)
        email = p.getString("email", null)
    }

    private fun saveSession() {
        prefs?.edit()
            ?.putString("access", accessToken)
            ?.putString("refresh", refreshToken)
            ?.putString("user_id", userId)
            ?.putString("username", username)
            ?.putString("email", email)
            ?.apply()
    }

    private fun clearSession() {
        accessToken = null
        refreshToken = null
        userId = null
        username = null
        email = null
        prefs?.edit()?.clear()?.apply()
    }

    private fun refreshBlocking(): Boolean {
        val rt = refreshToken ?: return false
        val conn = URL("$BASE_URL/auth/v1/token?grant_type=refresh_token")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().put("refresh_token", rt).toString()
            conn.outputStream.use { it.write(payload.toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) {
                applySession(JSONObject(text))
                return true
            }
            if (code in 400..499) clearSession()
            return false
        } catch (e: Exception) {
            return false
        } finally {
            conn.disconnect()
        }
    }

    private fun request(
        path: String,
        body: JSONObject,
        withAuth: Boolean,
        canRetry: Boolean = true
    ): String {
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

            if (code == 401 && withAuth && canRetry && refreshBlocking()) {
                return request(path, body, withAuth, false)
            }

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
        val newRefresh = json.optString("refresh_token")
        if (newRefresh.isNotBlank()) refreshToken = newRefresh
        userId = user.optString("id")
        email = user.optString("email")
        username = user.optJSONObject("user_metadata")?.optString("username")
        saveSession()
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
        clearSession()
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
                        capacity = o.optInt("capacity", 50),
                        status = o.optString("status", "open")
                    )
                )
            }
            list
        }

    suspend fun sendMessage(roomId: String, content: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_room_id", roomId)
                .put("p_content", content)
            request("/rest/v1/rpc/send_message", body, true)
            Unit
        }

    suspend fun getMessages(roomId: String, limit: Int = 50): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_room_id", roomId)
                .put("p_limit", limit)
            val text = request("/rest/v1/rpc/get_messages", body, true)
            val arr = JSONArray(if (text.isBlank()) "[]" else text)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = o.getString("id"),
                        senderId = o.optString("sender_id", ""),
                        username = o.optString("username", "user"),
                        content = o.optString("content", ""),
                        createdAt = o.optString("created_at", "")
                    )
                )
            }
            list
        }

    suspend fun deleteMessage(messageId: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_message_id", messageId)
            request("/rest/v1/rpc/delete_message", body, true)
            Unit
        }

    suspend fun getMembers(roomId: String): List<MemberInfo> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_room_id", roomId)
            val text = request("/rest/v1/rpc/get_room_members", body, true)
            val arr = JSONArray(if (text.isBlank()) "[]" else text)
            val list = mutableListOf<MemberInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MemberInfo(
                        userId = o.getString("user_id"),
                        username = o.optString("username", "user"),
                        role = o.optString("role", "member")
                    )
                )
            }
            list
        }

    suspend fun removeMember(roomId: String, memberId: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_room_id", roomId)
                .put("p_user_id", memberId)
            request("/rest/v1/rpc/remove_member", body, true)
            Unit
        }

    suspend fun setModerator(roomId: String, memberId: String, make: Boolean) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_room_id", roomId)
                .put("p_user_id", memberId)
                .put("p_make", make)
            request("/rest/v1/rpc/set_moderator", body, true)
            Unit
        }

    suspend fun closeRoom(roomId: String, closed: Boolean) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_room_id", roomId)
                .put("p_closed", closed)
            request("/rest/v1/rpc/close_room", body, true)
            Unit
        }

    suspend fun deleteRoom(roomId: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_room_id", roomId)
            request("/rest/v1/rpc/delete_room", body, true)
            Unit
        }

    private fun JSONObject.str(key: String): String {
        return if (isNull(key)) "" else optString(key, "")
    }

    suspend fun createTournament(
        name: String,
        game: String,
        description: String,
        format: String,
        participation: String,
        maxParticipants: Int,
        startAtIso: String?,
        rules: String,
        prize: String,
        contact: String,
        isPublic: Boolean
    ): CreatedTournament =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_name", name)
                .put("p_game", game)
                .put("p_description", description)
                .put("p_format", format)
                .put("p_participation", participation)
                .put("p_max", maxParticipants)
                .put("p_start", startAtIso ?: JSONObject.NULL)
                .put("p_rules", rules)
                .put("p_prize", prize)
                .put("p_contact", contact)
                .put("p_public", isPublic)
            val json = JSONObject(request("/rest/v1/rpc/create_tournament", body, true))
            CreatedTournament(
                id = json.getString("id"),
                code = json.getString("code"),
                name = json.getString("name")
            )
        }

    suspend fun listTournaments(scope: String): List<TournamentInfo> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_scope", scope)
            val text = request("/rest/v1/rpc/list_tournaments", body, true)
            val arr = JSONArray(if (text.isBlank()) "[]" else text)
            val list = mutableListOf<TournamentInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TournamentInfo(
                        id = o.getString("id"),
                        code = o.getString("code"),
                        name = o.getString("name"),
                        game = o.optString("game", ""),
                        format = o.optString("format", "single_elimination"),
                        participation = o.optString("participation", "individual"),
                        status = o.optString("status", "registration"),
                        isPublic = o.optBoolean("is_public", true),
                        maxParticipants = o.optInt("max_participants", 16),
                        startAt = o.str("start_at"),
                        participants = o.optInt("participants", 0),
                        myRole = o.optString("my_role", "none")
                    )
                )
            }
            list
        }

    suspend fun getTournament(id: String): TournamentDetail =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_id", id)
            val o = JSONObject(request("/rest/v1/rpc/get_tournament", body, true))
            val people = mutableListOf<TournamentPerson>()
            val arr = o.optJSONArray("participants") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                people.add(
                    TournamentPerson(
                        userId = p.optString("user_id", ""),
                        username = p.optString("username", "user")
                    )
                )
            }
            TournamentDetail(
                id = o.getString("id"),
                code = o.getString("code"),
                name = o.getString("name"),
                game = o.optString("game", ""),
                description = o.optString("description", ""),
                format = o.optString("format", "single_elimination"),
                participation = o.optString("participation", "individual"),
                maxParticipants = o.optInt("max_participants", 16),
                startAt = o.str("start_at"),
                rules = o.optString("rules", ""),
                prize = o.optString("prize", ""),
                contact = o.optString("contact", ""),
                isPublic = o.optBoolean("is_public", true),
                status = o.optString("status", "registration"),
                myRole = o.optString("my_role", "none"),
                organizer = o.str("organizer").ifBlank { "user" },
                participants = people
            )
        }

    suspend fun registerTournament(code: String): CreatedTournament =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_code", code)
            val json = JSONObject(request("/rest/v1/rpc/register_tournament", body, true))
            CreatedTournament(
                id = json.getString("id"),
                code = json.getString("code"),
                name = json.getString("name")
            )
        }

    suspend fun unregisterTournament(id: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_id", id)
            request("/rest/v1/rpc/unregister_tournament", body, true)
            Unit
        }

    suspend fun setTournamentStatus(id: String, status: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_id", id)
                .put("p_status", status)
            request("/rest/v1/rpc/set_tournament_status", body, true)
            Unit
        }

    suspend fun deleteTournament(id: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_id", id)
            request("/rest/v1/rpc/delete_tournament", body, true)
            Unit
        }

    suspend fun getBracket(tournamentId: String): List<MatchInfo> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("p_tid", tournamentId)
            val text = request("/rest/v1/rpc/get_bracket", body, true)
            val arr = JSONArray(if (text.isBlank()) "[]" else text)
            val list = mutableListOf<MatchInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MatchInfo(
                        id = o.getString("id"),
                        round = o.optInt("round", 1),
                        slot = o.optInt("slot", 0),
                        playerA = o.str("player_a"),
                        playerB = o.str("player_b"),
                        nameA = o.str("name_a"),
                        nameB = o.str("name_b"),
                        scoreA = if (o.isNull("score_a")) null else o.optInt("score_a"),
                        scoreB = if (o.isNull("score_b")) null else o.optInt("score_b"),
                        winner = o.str("winner"),
                        status = o.optString("status", "pending")
                    )
                )
            }
            list
        }

    suspend fun setMatchResult(matchId: String, scoreA: Int, scoreB: Int) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("p_match_id", matchId)
                .put("p_score_a", scoreA)
                .put("p_score_b", scoreB)
            request("/rest/v1/rpc/set_match_result", body, true)
            Unit
        }
}
