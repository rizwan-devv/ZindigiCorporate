package com.example.kycapp.portal

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Portal JWT session (entity signup / onboarding). Separate from personal KYC DfsSession. */
data class PortalSession(
    val token: String,
    val email: String,
    val partyStatus: String,
    val partyPublicId: String?,
    val trackingId: String?,
    val fullName: String?,
    val role: String?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("token", token)
        put("email", email)
        put("partyStatus", partyStatus)
        put("partyPublicId", partyPublicId ?: JSONObject.NULL)
        put("trackingId", trackingId ?: JSONObject.NULL)
        put("fullName", fullName ?: JSONObject.NULL)
        put("role", role ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): PortalSession = PortalSession(
            token = o.getString("token"),
            email = o.optString("email"),
            partyStatus = o.optString("partyStatus"),
            partyPublicId = o.optString("partyPublicId").takeIf { it.isNotBlank() && it != "null" },
            trackingId = o.optString("trackingId").takeIf { it.isNotBlank() && it != "null" },
            fullName = o.optString("fullName").takeIf { it.isNotBlank() && it != "null" },
            role = o.optString("role").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}

class PortalSessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zindigi_portal_session", Context.MODE_PRIVATE)

    fun save(session: PortalSession) {
        prefs.edit().putString(KEY, session.toJson().toString()).apply()
    }

    fun load(): PortalSession? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { PortalSession.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "portal"
    }
}
