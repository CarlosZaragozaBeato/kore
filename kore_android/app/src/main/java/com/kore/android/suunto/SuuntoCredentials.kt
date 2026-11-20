package com.kore.android.suunto

data class SuuntoCredentials(
    val clientId: String,
    val clientSecret: String,
    val subscriptionKey: String,
    val refreshToken: String,
) {
    fun validated(): SuuntoCredentials = copy(
        clientId = clientId.trim(),
        clientSecret = clientSecret.trim(),
        subscriptionKey = subscriptionKey.trim(),
        refreshToken = refreshToken.trim(),
    ).also {
        require(it.clientId.isNotEmpty()) { "Falta Client ID" }
        require(it.clientSecret.isNotEmpty()) { "Falta Client Secret" }
        require(it.subscriptionKey.isNotEmpty()) { "Falta Subscription Key" }
        require(it.refreshToken.isNotEmpty()) { "Falta Refresh Token" }
    }
}
