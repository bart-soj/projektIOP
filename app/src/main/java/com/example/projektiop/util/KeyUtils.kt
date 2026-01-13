package com.example.projektiop.util

import android.util.Log
import com.example.projektiop.data.api.BackupApi
import com.example.projektiop.data.api.PublicKeyApi
import com.example.projektiop.data.api.PublishPublicKeyRequest
import com.example.projektiop.data.db.realm.RealmDBRepository
import com.example.projektiop.data.repositories.SharedDataSource
import com.example.projektiop.domain.models.AlgorithmParams.EncryptionParams
import com.example.projektiop.domain.models.AlgorithmParams.PasswordDerivationParams
import com.example.projektiop.domain.models.BackupInfo
import com.example.projektiop.domain.models.base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.KeyGenerationParameters
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESFastEngine
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import retrofit2.HttpException
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.KeyPair
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class KeyUtils(private val pubKeyApi: PublicKeyApi,
               private val backupApi: BackupApi,
               private val sharedDataSource: SharedDataSource,
               private val dbRepository: RealmDBRepository)
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

    /*
    fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA", "BC")
        generator.initialize(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
        return generator.generateKeyPair()
    }
    */

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
        val saltBytes: ByteArray
        val opsLimit: Int
        val memLimit: Int
        val parallelism: Int
        val hashLength: Int
        val secureRandom = SecureRandom()

        saltBytes = ByteArray(16).also { secureRandom.nextBytes(it) }
        opsLimit = 3
        memLimit = 65_536
        parallelism = 1
        hashLength = 32

        // ---- Argon2id ----
        val argonParams = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(saltBytes)
            .withIterations(opsLimit)
            .withMemoryAsKB(memLimit)
            .withParallelism(parallelism)
            .build()

        val masterKey = ByteArray(hashLength)
        val generator = Argon2BytesGenerator()
        generator.init(argonParams)
        generator.generateBytes(password.toByteArray(Charsets.UTF_8), masterKey)

        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(
            HKDFParameters(
                masterKey,
                null, // no salt; Argon2 already salted
                null
            )
        )

        val encryptionKey = ByteArray(32)
        val verificationKey = ByteArray(32)

        hkdf.generateBytes(encryptionKey, 0, encryptionKey.size)
        hkdf.generateBytes(verificationKey, 0, verificationKey.size)

        val digest = MessageDigest.getInstance("SHA-256")
        val verificatorBytes = digest.digest(verificationKey)

        val verificatorBase64 =
            Base64.getEncoder().encodeToString(verificatorBytes)

        val encryptionKeyBase64 =
            Base64.getEncoder().encodeToString(encryptionKey)

        val paramsOut = PasswordDerivationParams(
            algorithm = "Argon2id",
            salt = Base64.getEncoder().encodeToString(saltBytes),
            opsLimit = opsLimit,
            memLimit = memLimit,
            parallelism = parallelism,
            hashLength = hashLength,
            verificator = verificatorBase64
        )

        return Pair(encryptionKeyBase64, paramsOut)
    }

    fun recreateKeyFromPassword(
        password: String,
        params: PasswordDerivationParams
    ): Result<Pair<base64, PasswordDerivationParams>, BackupError>  {

        val saltBytes: ByteArray
        val opsLimit: Int
        val memLimit: Int
        val parallelism: Int
        val hashLength: Int
        val verificator: base64?

        saltBytes = Base64.getDecoder().decode(params.salt)
        opsLimit = params.opsLimit
        memLimit = params.memLimit
        parallelism = params.parallelism
        hashLength = params.hashLength
        verificator = params.verificator

        // ---- Argon2id ----
        val argonParams = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(saltBytes)
            .withIterations(opsLimit)
            .withMemoryAsKB(memLimit)
            .withParallelism(parallelism)
            .build()

        val masterKey = ByteArray(hashLength)
        val generator = Argon2BytesGenerator()
        generator.init(argonParams)
        generator.generateBytes(password.toByteArray(Charsets.UTF_8), masterKey)

        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(
            HKDFParameters(
                masterKey,
                null, // no salt; Argon2 already salted
                null
            )
        )

        val encryptionKey = ByteArray(32)
        val verificationKey = ByteArray(32)

        hkdf.generateBytes(encryptionKey, 0, encryptionKey.size)
        hkdf.generateBytes(verificationKey, 0, verificationKey.size)

        val digest = MessageDigest.getInstance("SHA-256")
        val verificatorBytes = digest.digest(verificationKey)

        val verificatorBase64 =
            Base64.getEncoder().encodeToString(verificatorBytes)

        // wrong password, failed verification
        if (verificatorBase64 != verificator) {
            return Result.Error(BackupError.WRONG_PASSWORD)
        }

        val encryptionKeyBase64 =
            Base64.getEncoder().encodeToString(encryptionKey)

        val paramsOut = PasswordDerivationParams(
            algorithm = "Argon2id",
            salt = Base64.getEncoder().encodeToString(saltBytes),
            opsLimit = opsLimit,
            memLimit = memLimit,
            parallelism = parallelism,
            hashLength = hashLength,
            verificator = verificatorBase64
        )

        return Result.Success(Pair(encryptionKeyBase64, paramsOut))
    }


    suspend fun createBackupInfo(password: String, userId: String): BackupInfo {
        val passwordKeyPair = createKeyFromPassword(password)

        val backupKey = createBackupKey()

        val privKeyStorageKey = "x25519_private_$userId"
        val pubKeyStorageKey = "x25519_public_$userId"

        val storedPrivBase64 = sharedDataSource.getEncryptedBase64(privKeyStorageKey)
        val storedPubBase64 = sharedDataSource.getEncryptedBase64(pubKeyStorageKey)

        // only create backup in the gotKeys state
        assert(storedPrivBase64 != null && storedPubBase64 != null)

        val encryptedBackupPair = encryptKeyAES256GCM(passwordKeyPair.first, backupKey)
        val encryptedPrivPair = encryptKeyAES256GCM(backupKey, storedPrivBase64!!)

        return BackupInfo(
            publicKey = storedPubBase64!!,
            encryptedPrivateKey = encryptedPrivPair.first,
            encryptedBackupKey = encryptedBackupPair.first,
            passwordDerivationParams = passwordKeyPair.second,
            backupEncryptionParams = encryptedBackupPair.second,
            privateEncryptionParams = encryptedPrivPair.second
        )
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

    fun decryptKeyAES256GCM(key: base64, keyToDecrypt: base64, encryptionParams: EncryptionParams): base64 {
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


    suspend fun getLocalKeyPair(
        userId: String,
    ): Result<Pair<base64, base64>, DataError.Local> {

        val privKeyStorageKey = "x25519_private_$userId"
        val pubKeyStorageKey = "x25519_public_$userId"

        val storedPrivBase64 = sharedDataSource.getEncryptedBase64(privKeyStorageKey)
        val storedPubBase64 = sharedDataSource.getEncryptedBase64(pubKeyStorageKey)

        if (storedPrivBase64 != null && storedPubBase64 != null) {
            return Result.Success(storedPubBase64 to storedPrivBase64)
        } else {
            return Result.Error(DataError.Local.NO_DATA)
        }
    }

    suspend fun getBackupInfo(): Result<BackupInfo, DataError> {
        // get backup from server
        var backupInfo: BackupInfo
        try {
            val result = backupApi.getBackup()
            if (!result.isSuccessful) throw HttpException(result)
            if (result.body() == null) return Result.Error(DataError.Network.UNKNOWN)
            backupInfo = result.body()!!
            return Result.Success(backupInfo)
        } catch (e: Exception) {
            return apiExceptionToDataError<BackupInfo>(e)
        }
    }

    suspend fun getDecryptedKeyPairFromBackup(password: String, backupInfo: BackupInfo, userId: String): Result<Pair<base64, base64>, BackupError> {
        val keyFromPasswordResult = recreateKeyFromPassword(password, backupInfo.passwordDerivationParams)
        when (keyFromPasswordResult) {
            is Result.Error -> return Result.Error(BackupError.WRONG_PASSWORD)
            is Result.Success ->  {
                val keyFromPassword = keyFromPasswordResult.data.first
                val decryptedBackupKey = decryptKeyAES256GCM(keyFromPassword, backupInfo.encryptedBackupKey, backupInfo.backupEncryptionParams)

                val decryptedPrivateKey = decryptKeyAES256GCM(decryptedBackupKey, backupInfo.encryptedPrivateKey, backupInfo.privateEncryptionParams)

                val publicKey = backupInfo.publicKey

                val privKeyStorageKey = "x25519_private_$userId"
                val pubKeyStorageKey = "x25519_public_$userId"

                sharedDataSource.setEncryptedBase64(privKeyStorageKey, decryptedPrivateKey)
                sharedDataSource.setEncryptedBase64(pubKeyStorageKey, publicKey)

                return Result.Success(publicKey to decryptedPrivateKey)
            }
        }
    }

    suspend fun createKeyPair(userId: String): Result<Pair<base64, base64>, DataError> {

        val privKeyStorageKey = "x25519_private_$userId"
        val pubKeyStorageKey = "x25519_public_$userId"

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
            val result = pubKeyApi.publishPublicKey(PublishPublicKeyRequest(pubBase64))
            if (!result.isSuccessful) throw HttpException(result)
        } catch (e: Exception) {
            return apiExceptionToDataError<Pair<base64, base64>>(e)
        }

        return Result.Success(pubBase64 to privBase64)
    }

    fun calculateChatKey(myUserId: String, theirPubBase64: String): String {

        val privKeyStorageKey = "x25519_private_$myUserId"
        val ourPrivBase64 = sharedDataSource.getEncryptedBase64(privKeyStorageKey)

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

    suspend fun encryptMessage(
        content: String,
        chatKey: base64
    ): base64 {

        val key = Base64.getDecoder().decode(chatKey)
        require(key.size == 32)

        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val plaintext = content.toByteArray(Charsets.UTF_8)

        val cipher = GCMBlockCipher(AESFastEngine())
        cipher.init(
            true,
            AEADParameters(KeyParameter(key), 128, iv)
        )

        val out = ByteArray(cipher.getOutputSize(plaintext.size))
        var len = cipher.processBytes(plaintext, 0, plaintext.size, out, 0)
        len += cipher.doFinal(out, len)

        val result = ByteArray(iv.size + out.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(out, 0, result, iv.size, out.size)

        return Base64.getEncoder().encodeToString(result)
    }


    suspend fun decryptMessage(
        content: base64,
        chatKey: base64
    ): String {

        val key = Base64.getDecoder().decode(chatKey)
        require(key.size == 32)

        val data = Base64.getDecoder().decode(content)
        require(data.size >= 12 + 16)

        val iv = data.copyOfRange(0, 12)
        val ciphertext = data.copyOfRange(12, data.size)

        val cipher = GCMBlockCipher(AESFastEngine())
        cipher.init(
            false,
            AEADParameters(KeyParameter(key), 128, iv)
        )

        val out = ByteArray(cipher.getOutputSize(ciphertext.size))
        var len = cipher.processBytes(ciphertext, 0, ciphertext.size, out, 0)
        len += cipher.doFinal(out, len)

        return String(out, 0, len, Charsets.UTF_8)
    }

    suspend fun getPubKey(userId: String): Result<base64, DataError> {
        // val localData = dbRepository.getUserById(userId)?.publicKey
        // if (localData != null) return Result.Success(localData)
        try {
            val result = pubKeyApi.getPublicKey(userId)
            if (!result.isSuccessful) throw HttpException(result)
            val body = result.body()
            val key = body?.publicKey
            require(key != null)
            dbRepository.savePubKey(userId, key)
            return Result.Success(key)
        } catch(e: Exception) {
            return apiExceptionToDataError<base64>(e)
        }
    }

    suspend fun postMyPubKey(myUserId: String, myPubKey: base64): Result<Unit, DataError> {
        try {
            val result = pubKeyApi.publishPublicKey(PublishPublicKeyRequest(myPubKey))
            if (!result.isSuccessful) throw HttpException(result)
            dbRepository.savePubKey(myUserId, myPubKey)
            return Result.Success(Unit)
        } catch(e: Exception) {
            return apiExceptionToDataError<Unit>(e)
        }
    }
}