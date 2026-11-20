package com.kore.android.suunto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class SecureSuuntoStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = preferences.contains(ENCRYPTED_CREDENTIALS)

    fun save(credentials: SuuntoCredentials) {
        val value = credentials.validated()
        val clearText = JSONObject()
            .put("clientId", value.clientId)
            .put("clientSecret", value.clientSecret)
            .put("subscriptionKey", value.subscriptionKey)
            .put("refreshToken", value.refreshToken)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val encrypted = cipher.doFinal(clearText)
        val payload = ByteArray(cipher.iv.size + encrypted.size)
        cipher.iv.copyInto(payload)
        encrypted.copyInto(payload, cipher.iv.size)
        // Synchronous by design: a rotated refresh token must reach disk before the next API call.
        preferences.edit(commit = true) {
            putString(ENCRYPTED_CREDENTIALS, Base64.encodeToString(payload, Base64.NO_WRAP))
        }
    }

    fun load(): SuuntoCredentials {
        val encoded = preferences.getString(ENCRYPTED_CREDENTIALS, null)
            ?: throw IllegalStateException("Suunto no está configurado")
        try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > IV_SIZE_BYTES) { "Credenciales cifradas inválidas" }
            val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
            val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, iv))
            val json = JSONObject(cipher.doFinal(encrypted).toString(Charsets.UTF_8))
            return SuuntoCredentials(
                clientId = json.getString("clientId"),
                clientSecret = json.getString("clientSecret"),
                subscriptionKey = json.getString("subscriptionKey"),
                refreshToken = json.getString("refreshToken"),
            ).validated()
        } catch (error: Exception) {
            throw IllegalStateException("No se pudieron abrir las credenciales locales de Suunto", error)
        }
    }

    fun clear() {
        preferences.edit { remove(ENCRYPTED_CREDENTIALS) }
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "kore-suunto-secure"
        const val ENCRYPTED_CREDENTIALS = "credentials"
        const val KEY_ALIAS = "kore-suunto-credentials-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
    }
}
