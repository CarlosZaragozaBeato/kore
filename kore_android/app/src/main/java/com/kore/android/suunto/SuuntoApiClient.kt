package com.kore.android.suunto

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.json.JSONObject

data class SuuntoSyncResult(
    val pages: List<String>,
    val workoutCount: Int,
)

class SuuntoApiClient(private val credentialStore: SecureSuuntoStore) {
    fun synchronize(credentials: SuuntoCredentials, days: Int = 30): SuuntoSyncResult {
        require(days in 1..3650) { "El rango de sincronización no es válido" }
        val accessToken = renew(credentials)
        val until = Instant.now().toEpochMilli()
        val since = until - days * 86_400_000L
        val pages = mutableListOf<String>()
        var offset = 0
        var total = 0
        var previousPageKeys: List<String>? = null
        while (true) {
            val page = fetchPage(accessToken, credentials.subscriptionKey, since, until, offset)
            val payload = try {
                JSONObject(page).getJSONArray("payload")
            } catch (error: Exception) {
                throw IllegalStateException("Suunto devolvió una página de entrenamientos inválida", error)
            }
            val count = payload.length()
            val pageKeys = List(count) { index -> payload.getJSONObject(index).optString("workoutKey") }
            check(count == 0 || pageKeys != previousPageKeys) { "Suunto repitió una página; la sincronización se detuvo" }
            previousPageKeys = pageKeys
            pages += page
            total += count
            if (count < PAGE_SIZE) break
            offset += count
        }
        return SuuntoSyncResult(pages, total)
    }

    private fun renew(credentials: SuuntoCredentials): String {
        val body = form(mapOf("grant_type" to "refresh_token", "refresh_token" to credentials.refreshToken))
        val basic = Base64.encodeToString(
            "${credentials.clientId}:${credentials.clientSecret}".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
        val response = request(
            url = OAUTH_URL,
            method = "POST",
            headers = mapOf(
                "Authorization" to "Basic $basic",
                "Content-Type" to "application/x-www-form-urlencoded",
            ),
            body = body,
        )
        val json = try { JSONObject(response) } catch (error: Exception) {
            throw IllegalStateException("Suunto devolvió una renovación inválida", error)
        }
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token")
        require(accessToken.isNotBlank() && refreshToken.isNotBlank()) { "Suunto no devolvió tokens válidos" }
        credentialStore.save(credentials.copy(refreshToken = refreshToken))
        return accessToken
    }

    private fun fetchPage(
        accessToken: String,
        subscriptionKey: String,
        since: Long,
        until: Long,
        offset: Int,
    ): String {
        val query = form(mapOf(
            "since" to since.toString(),
            "until" to until.toString(),
            "limit" to PAGE_SIZE.toString(),
            "offset" to offset.toString(),
            "filter-by-modification-time" to "true",
        ))
        return request(
            url = "$WORKOUTS_URL?$query",
            method = "GET",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
                "Ocp-Apim-Subscription-Key" to subscriptionKey,
                "Accept" to "application/json",
            ),
        )
    }

    private fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String? = null,
    ): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            headers.forEach(connection::setRequestProperty)
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                connection.errorStream?.close()
                throw IllegalStateException("Suunto respondió HTTP $status")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun form(values: Map<String, String>): String = values.entries.joinToString("&") { (key, value) ->
        "${encode(key)}=${encode(value)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val OAUTH_URL = "https://cloudapi-oauth.suunto.com/oauth/token"
        const val WORKOUTS_URL = "https://cloudapi.suunto.com/v3/workouts"
        const val PAGE_SIZE = 50
        const val TIMEOUT_MILLIS = 30_000
    }
}
