package com.example.kycapp.portal

import android.content.Context
import android.net.Uri
import com.example.kycapp.BuildConfig
import com.example.kycapp.dfs.DfsApiException
import com.example.kycapp.dfs.DfsSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class EntityTypeOption(val code: String, val label: String)

data class RequiredDoc(
    val documentCode: String,
    val documentLabel: String,
    val mandatory: Boolean,
    val uploaded: Boolean
)

data class AssocPerson(
    val id: Long,
    val roleType: String,
    val fullName: String,
    val phone: String?,
    val email: String?
)

data class PartySnapshot(
    val status: String,
    val trackingId: String?,
    val canSubmit: Boolean,
    val onboardingStep: Int,
    val fullName: String?,
    val businessName: String?,
    val entityType: String?,
    val partnershipUnregistered: Boolean,
    val applicantIsPartner: Boolean,
    val phone: String?,
    val email: String?,
    val requiredDocuments: List<RequiredDoc>,
    val associatedPersons: List<AssocPerson>,
    val rejectionReason: String?
)

object PortalApiClient {

    private val baseUrl: String
        get() = BuildConfig.DFS_API_BASE.trimEnd('/')

    suspend fun signup(
        fullName: String,
        businessName: String,
        email: String,
        phone: String
    ): JSONObject = withContext(Dispatchers.IO) {
        requestJson(
            "POST",
            "/api/auth/signup",
            body = JSONObject()
                .put("partyType", "MERCHANT")
                .put("fullName", fullName.trim())
                .put("businessName", businessName.trim())
                .put("email", email.trim())
                .put("phone", phone.trim())
        )
    }

    suspend fun verifyOtp(email: String, code: String): PortalSession = withContext(Dispatchers.IO) {
        val o = requestJson(
            "POST",
            "/api/auth/verify-otp",
            body = JSONObject().put("email", email.trim()).put("code", code.trim())
        )
        PortalSession(
            token = o.getString("token"),
            email = email.trim().lowercase(),
            partyStatus = o.optString("partyStatus"),
            partyPublicId = o.optString("partyPublicId").takeIf { it.isNotBlank() },
            trackingId = o.optString("trackingId").takeIf { it.isNotBlank() },
            fullName = null,
            role = o.optString("role").takeIf { it.isNotBlank() }
        )
    }

    suspend fun portalLogin(email: String, password: String): PortalSession = withContext(Dispatchers.IO) {
        val o = requestJson(
            "POST",
            "/api/auth/login",
            body = JSONObject().put("email", email.trim()).put("password", password)
        )
        PortalSession(
            token = o.getString("token"),
            email = email.trim().lowercase(),
            partyStatus = o.optString("partyStatus"),
            partyPublicId = o.optString("partyPublicId").takeIf { it.isNotBlank() },
            trackingId = null,
            fullName = o.optString("fullName").takeIf { it.isNotBlank() },
            role = o.optString("role").takeIf { it.isNotBlank() }
        )
    }

    suspend fun entityTypes(): List<EntityTypeOption> = withContext(Dispatchers.IO) {
        val arr = requestArray("GET", "/api/entity-types")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            EntityTypeOption(o.getString("code"), o.optString("label", o.getString("code")))
        }
    }

    suspend fun me(token: String): PartySnapshot = withContext(Dispatchers.IO) {
        parseParty(requestJson("GET", "/api/onboarding/me", token = token))
    }

    suspend fun saveProfile(token: String, profile: JSONObject): PartySnapshot = withContext(Dispatchers.IO) {
        parseParty(requestJson("PUT", "/api/onboarding/profile", token = token, body = profile))
    }

    suspend fun addPartner(
        token: String,
        fullName: String,
        phone: String,
        email: String
    ): PartySnapshot = withContext(Dispatchers.IO) {
        parseParty(
            requestJson(
                "POST",
                "/api/onboarding/associated-persons",
                token = token,
                body = JSONObject()
                    .put("roleType", "PARTNER")
                    .put("fullName", fullName.trim())
                    .put("phone", phone.trim())
                    .put("email", email.trim())
                    .put("authorizedToOperate", true)
            )
        )
    }

    suspend fun uploadDocument(
        context: Context,
        token: String,
        documentCode: String,
        uri: Uri
    ): PartySnapshot = withContext(Dispatchers.IO) {
        multipartDocument(context, token, documentCode, uri)
    }

    suspend fun submit(token: String): PartySnapshot = withContext(Dispatchers.IO) {
        parseParty(requestJson("POST", "/api/onboarding/submit", token = token))
    }

    suspend fun startPersonalKyc(token: String): DfsSession = withContext(Dispatchers.IO) {
        val o = requestJson("POST", "/api/onboarding/start-personal-kyc", token = token)
        DfsSession(
            sessionToken = o.getString("sessionToken"),
            appUserId = o.optLong("appUserId"),
            phone = o.optString("phone"),
            fullName = o.optString("fullName"),
            email = o.optString("email").takeIf { it.isNotBlank() },
            status = o.optString("status"),
            businessName = o.optString("businessName").takeIf { it.isNotBlank() },
            trackingId = o.optString("trackingId").takeIf { it.isNotBlank() },
            canSubmit = o.optBoolean("canSubmit", false)
        )
    }

    private fun parseParty(o: JSONObject): PartySnapshot {
        val docs = mutableListOf<RequiredDoc>()
        val req = o.optJSONArray("requiredDocuments")
        if (req != null) {
            for (i in 0 until req.length()) {
                val d = req.getJSONObject(i)
                docs.add(
                    RequiredDoc(
                        documentCode = d.getString("documentCode"),
                        documentLabel = d.optString("documentLabel", d.getString("documentCode")),
                        mandatory = d.optBoolean("mandatory", false),
                        uploaded = d.optBoolean("uploaded", false)
                    )
                )
            }
        }
        val persons = mutableListOf<AssocPerson>()
        val ap = o.optJSONArray("associatedPersons")
        if (ap != null) {
            for (i in 0 until ap.length()) {
                val p = ap.getJSONObject(i)
                persons.add(
                    AssocPerson(
                        id = p.optLong("id"),
                        roleType = p.optString("roleType"),
                        fullName = p.optString("fullName"),
                        phone = p.optString("phone").takeIf { it.isNotBlank() },
                        email = p.optString("email").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
        return PartySnapshot(
            status = o.optString("status"),
            trackingId = o.optString("trackingId").takeIf { it.isNotBlank() },
            canSubmit = o.optBoolean("canSubmit", false),
            onboardingStep = o.optInt("onboardingStep", 1),
            fullName = o.optString("fullName").takeIf { it.isNotBlank() },
            businessName = o.optString("businessName").takeIf { it.isNotBlank() },
            entityType = o.optString("entityType").takeIf { it.isNotBlank() },
            partnershipUnregistered = o.optBoolean("partnershipUnregistered", false),
            applicantIsPartner = o.optBoolean("applicantIsPartner", false),
            phone = o.optString("phone").takeIf { it.isNotBlank() },
            email = o.optString("email").takeIf { it.isNotBlank() },
            requiredDocuments = docs,
            associatedPersons = persons,
            rejectionReason = o.optString("rejectionReason").takeIf { it.isNotBlank() }
        )
    }

    private fun requestArray(method: String, path: String): JSONArray {
        val url = URL("$baseUrl$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            if (code !in 200..299) {
                val err = runCatching { JSONObject(text).optString("error") }.getOrDefault("")
                throw DfsApiException(err.ifBlank { "Request failed ($code)" })
            }
            return JSONArray(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun requestJson(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject {
        val url = URL("$baseUrl$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 60000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            val ct = conn.contentType.orEmpty()
            if (!ct.contains("json", ignoreCase = true) && text.trimStart().startsWith("<")) {
                throw DfsApiException(
                    "API returned HTML instead of JSON. Check DFS_API_BASE=$baseUrl"
                )
            }
            val json = runCatching { JSONObject(text) }.getOrElse {
                throw DfsApiException(if (text.isBlank()) "Empty response ($code)" else text.take(200))
            }
            if (code !in 200..299) {
                val err = json.optString("error").ifBlank { json.optString("message") }
                throw DfsApiException(err.ifBlank { "Request failed ($code)" })
            }
            return json
        } finally {
            conn.disconnect()
        }
    }

    private fun multipartDocument(
        context: Context,
        token: String,
        documentCode: String,
        uri: Uri
    ): PartySnapshot {
        val boundary = "----ZindigiOnb${System.currentTimeMillis()}"
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "upload.bin"
        val url = URL("$baseUrl/api/onboarding/documents")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30000
            readTimeout = 120000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            DataOutputStream(conn.outputStream).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"documentCode\"\r\n\r\n")
                out.writeBytes("$documentCode\r\n")
                out.writeBytes("--$boundary\r\n")
                out.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$name\"\r\n"
                )
                out.writeBytes("Content-Type: $mime\r\n\r\n")
                out.flush()
                resolver.openInputStream(uri)?.use { it.copyTo(out) }
                    ?: throw DfsApiException("Could not read selected file")
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse {
                throw DfsApiException(if (text.isBlank()) "Empty response ($code)" else text.take(200))
            }
            if (code !in 200..299) {
                val err = json.optString("error").ifBlank { json.optString("message") }
                throw DfsApiException(err.ifBlank { "Upload failed ($code)" })
            }
            return parseParty(json)
        } finally {
            conn.disconnect()
        }
    }
}
