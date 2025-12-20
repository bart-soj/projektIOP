package com.example.projektiop.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.example.projektiop.data.api.PublicKeyApi
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.models.AlgorithmParams
import com.example.projektiop.domain.models.AlgorithmParams.EncryptionParams
import com.example.projektiop.domain.models.AlgorithmParams.PasswordDerivationParams
import com.example.projektiop.domain.models.base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.KeyGenerationParameters
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.RSAKeyGenParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CertificateUtils(private val pubKeyApi: PublicKeyApi,
                       private val sharedDataSource: SharedDataSource)
{
    val provider = BouncyCastleProvider()
    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(provider, 1)
            // Security.addProvider(provider)
            Log.d("MyApplication", "Bouncy Castle provider added.")
        } else {
            Log.d("MyApplication", "Bouncy Castle provider already present.")
        }
    }


    fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA", "BC")
        generator.initialize(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
        return generator.generateKeyPair()
    }


    fun generateCSR(email: String, keyPair: KeyPair): String {

        val subjectDN = X500Name("CN=$email")
        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        val builder = PKCS10CertificationRequestBuilder(subjectDN, publicKeyInfo)

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider(provider)
            .build(keyPair.private)

        val csr = builder.build(signer)

        return StringWriter().use { sw ->
            JcaPEMWriter(sw).use { pemWriter ->
                pemWriter.writeObject(csr)
            }
            sw.toString()
        }
    }


    fun loadX509Certificate(pem: String): X509Certificate {
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(ByteArrayInputStream(pem.toByteArray())) as X509Certificate
    }


    fun isCertificateValid(certPem: String, caCertPem: String): Boolean {
        return try {
            val cert = loadX509Certificate(certPem)
            val caCert = loadX509Certificate(caCertPem)

            cert.checkValidity()
            cert.verify(caCert.publicKey)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createBackupKey(): base64 {
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        return Base64.getEncoder().encodeToString(keyBytes)
    }

    fun createKeyFromPassword(password: String): Pair<base64, PasswordDerivationParams> {
        val secureRandom = SecureRandom()

        val saltBytes = ByteArray(16).also { secureRandom.nextBytes(it) }
        val saltBase64 = Base64.getEncoder().encodeToString(saltBytes)

        val opsLimit = 3
        val memLimit = 65536
        val parallelism = 1
        val hashLength = 32

        val argonParams = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(saltBytes)
            .withIterations(opsLimit)
            .withMemoryAsKB(memLimit)
            .withParallelism(parallelism)
            .build()

        val keyBytes = ByteArray(hashLength)
        val generator = Argon2BytesGenerator()
        generator.init(argonParams)
        generator.generateBytes(password.toByteArray(Charsets.UTF_8), keyBytes, 0, keyBytes.size)

        val keyBase64 = Base64.getEncoder().encodeToString(keyBytes)

        val params = PasswordDerivationParams(
            algorithm = "Argon2id",
            salt = saltBase64,
            opsLimit = opsLimit,
            memLimit = memLimit,
            parallelism = parallelism,
            hashLength = hashLength
        )

        return Pair(keyBase64, params)
    }

    fun encryptKeyAES256GCM(
        encryptingKey: base64,
        keyToEncrypt: base64
    ): Pair<base64, EncryptionParams> {

        val keyBytes = Base64.getDecoder().decode(encryptingKey)
        require(keyBytes.size == 32) { "AES-256 requires 32-byte key" }

        val plaintext = Base64.getDecoder().decode(keyToEncrypt)

        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertextWithTag = cipher.doFinal(plaintext)

        val encryptionParams = EncryptionParams(
            algorithm = "AES-256-GCM",
            iv = Base64.getEncoder().encodeToString(iv),
            tagLength = 128
        )

        val ciphertext: base64 = Base64.getEncoder().encodeToString(ciphertextWithTag)

        return ciphertext to encryptionParams
    }

    fun decryptKeyAES256GCM(key: base64, keyToDecrypt: base64, encryptionParams: AlgorithmParams.EncryptionParams): base64 {
        require(encryptionParams.algorithm == "AES-256-GCM")

        val iv = Base64.getDecoder().decode(encryptionParams.iv)
        val ciphertext = Base64.getDecoder().decode(keyToDecrypt)
        val keyBytes = Base64.getDecoder().decode(key)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(encryptionParams.tagLength, iv)
        )

        val plaintext = cipher.doFinal(ciphertext)
        val out: base64 = Base64.getEncoder().encodeToString(plaintext)
        return out
    }


    suspend fun ensureKeyPair(
        userId: String,
    ): Result<Pair<String, String>, DataError> {

        val privKeyStorageKey = "x25519_private_$userId"
        val pubKeyStorageKey = "x25519_public_$userId"

        val storedPrivBase64 = sharedDataSource.getEncryptedBase64(privKeyStorageKey)
        val storedPubBase64 = sharedDataSource.getEncryptedBase64(pubKeyStorageKey)

        if (storedPrivBase64 != null && storedPubBase64 != null) {
            return Result.Success(storedPubBase64 to storedPrivBase64)
        }

        val keyGen = X25519KeyPairGenerator()
        keyGen.init(KeyGenerationParameters(SecureRandom(), 256))
        val keyPair: AsymmetricCipherKeyPair = keyGen.generateKeyPair()

        val privParams = keyPair.private as X25519PrivateKeyParameters
        val pubParams = keyPair.public as X25519PublicKeyParameters

        val privBase64 = Base64.getEncoder().encodeToString(privParams.encoded)
        val pubBase64 = Base64.getEncoder().encodeToString(pubParams.encoded)

        sharedDataSource.setEncryptedBase64(privKeyStorageKey, privBase64)
        sharedDataSource.setEncryptedBase64(pubKeyStorageKey, pubBase64)

        try {
            pubKeyApi.publishPublicKey(pubBase64)
        } catch (e: Exception) {
            return exceptionToDataError(e)
        }

        return Result.Success(pubBase64 to privBase64)
    }

    fun calculateChatKey(ourPrivBase64: String, theirPubBase64: String): String {
        // Decode Base64 keys
        val ourPrivBytes = Base64.getDecoder().decode(ourPrivBase64)
        val theirPubBytes = Base64.getDecoder().decode(theirPubBase64)

        // Load X25519 keys
        val ourPriv = X25519PrivateKeyParameters(ourPrivBytes, 0)
        val theirPub = X25519PublicKeyParameters(theirPubBytes, 0)

        // Compute DH shared secret
        val sharedSecret = ByteArray(32)
        val agreement = X25519Agreement()
        agreement.init(ourPriv)
        agreement.calculateAgreement(theirPub, sharedSecret, 0)

        // Derive chat key using HKDF-SHA256
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        val salt = ByteArray(32) { 0 }  // optional fixed salt
        val info = "chat key".toByteArray(Charsets.UTF_8)
        hkdf.init(HKDFParameters(sharedSecret, salt, info))
        val chatKey = ByteArray(32)
        hkdf.generateBytes(chatKey, 0, chatKey.size)

        return Base64.getEncoder().encodeToString(chatKey)
    }
}