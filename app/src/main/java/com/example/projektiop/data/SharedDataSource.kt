package com.example.projektiop.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import com.example.projektiop.domain.models.base64
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SharedDataSource(private val appContext: Context) {
    companion object {
        private const val PREFS_NAME = "HelloBeaconSharedPrefs"
        private const val BASE_URL_KEY: String = "BASE_URL"
        // private const val BASE_URL = "hellobeacon.onrender.com" // Ujednolicona baza – auth i user pod jednym URL
        private const val BASE_URL = "192.168.1.13:3000" // "10.0.2.2:3000" // is bound to lo of local machine
        private const val DB_KEY_ALIAS = "hellobeacon_database_key"
        private const val AES_KEY_ALIAS = "hellobeacon_encryption_key"
        private const val AES_KEY_SIZE = 256
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_SIZE = 12
    }

    private val prefs = getPreferences(appContext)

    init {
        this.set(BASE_URL_KEY, BASE_URL)
    }

    fun getAppContext(): Context {
        return appContext
    }

    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun <T> set(key: String, value: T, context: Context = this.getAppContext()) {
        with(prefs.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("Unsupported type: ${value!!::class.java}")
            }
            apply()
        }
    }

    inline fun <reified T> get(key: String, defaultValue: T, context: Context = this.getAppContext()): T {
        val prefs = getPreferences(context)
        return when (T::class) {
            String::class -> prefs.getString(key, defaultValue as? String ?: "") as T
            Int::class -> prefs.getInt(key, defaultValue as? Int ?: 0) as T
            Boolean::class -> prefs.getBoolean(key, defaultValue as? Boolean ?: false) as T
            Float::class -> prefs.getFloat(key, defaultValue as? Float ?: 0f) as T
            Long::class -> prefs.getLong(key, defaultValue as? Long ?: 0L) as T
            else -> throw IllegalArgumentException("Unsupported type: ${T::class.java}")
        }
    }

    fun remove(key: String, context: Context = this.getAppContext()) {
        getPreferences(context).edit() { remove(key) }
    }

    private fun getOrCreateAESKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(AES_KEY_ALIAS)) {
            return keyStore.getKey(AES_KEY_ALIAS, null) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            AES_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun getOrCreateDBKey(): base64 {
        val existing = getEncryptedBase64(DB_KEY_ALIAS)
        if (existing != null) {
            return existing
        } else {
            val keyBytes = ByteArray(64).also { SecureRandom().nextBytes(it) }
            val newDBKey =  Base64.getEncoder().encodeToString(keyBytes)
            setEncryptedBase64(DB_KEY_ALIAS, newDBKey)
            return newDBKey
        }
    }

    private fun encrypt(valueBase64: base64): base64 {
        val secretKey = getOrCreateAESKey()
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(valueBase64.toByteArray(Charsets.UTF_8))

        // Prepend IV to ciphertext for storage
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedBase64: base64): base64? {
        return try {
            val combined = Base64.getDecoder().decode(encryptedBase64)
            val iv = combined.copyOfRange(0, GCM_IV_SIZE)
            val ciphertext = combined.copyOfRange(GCM_IV_SIZE, combined.size)

            val secretKey = getOrCreateAESKey()
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decrypted = cipher.doFinal(ciphertext)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setEncryptedBase64(key: String, valueBase64: base64) {
        val encrypted = encrypt(valueBase64)
        prefs.edit() { putString(key, encrypted) }
    }

    fun getEncryptedBase64(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return decrypt(encrypted)
    }
}