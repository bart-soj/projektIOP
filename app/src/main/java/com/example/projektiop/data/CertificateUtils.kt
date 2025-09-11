package com.example.projektiop.data

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64

object CertificateUtils {
    fun generateKeyPair(algorithm: String = "RSA", keySize: Int = 2048): KeyPair {
        val gen = KeyPairGenerator.getInstance(algorithm)
        gen.initialize(keySize)
        return gen.generateKeyPair()
    }

    // Placeholder CSR generator. Replace with real CSR creation (e.g., BouncyCastle) when backend is ready.
    fun generateCSR(email: String, keyPair: KeyPair): String {
        val pub = keyPair.public.encoded
        val b64 = Base64.getEncoder().encodeToString(pub)
        return "-----BEGIN CERTIFICATE REQUEST-----\n" +
                "CN=$email\n" +
                b64.chunked(64).joinToString("\n") +
                "\n-----END CERTIFICATE REQUEST-----\n"
    }
}
