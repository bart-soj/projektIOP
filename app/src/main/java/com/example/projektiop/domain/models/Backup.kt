package com.example.projektiop.domain.models


typealias base64 = String
typealias KB = Int


data class BackupInfo (
    val encryptedPrivateKey: base64,
    val encryptedBackupKey: base64,
    val passwordDerivationParams: AlgorithmParams.PasswordDerivationParams,
    val backupEncryptionParams: AlgorithmParams.EncryptionParams,
    val privateEncryptionParams: AlgorithmParams.EncryptionParams
)


sealed class AlgorithmParams {
    data class PasswordDerivationParams( // for Argon2id
        val algorithm: String,
        val salt: base64,
        val opsLimit: Int,
        val memLimit: KB,
        val parallelism: Int,
        val hashLength: Int
    )

    data class EncryptionParams( // for AES-256-GCM
        val algorithm: String,
        val iv: base64,
        val tagLength: Int
    )
}
