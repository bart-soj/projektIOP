package com.example.projektiop.data

import android.util.Log
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.RSAKeyGenParameterSpec

object CertificateUtils
{
    init {
        val provider = BouncyCastleProvider()
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(provider)
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
        val provider = BouncyCastleProvider()
        if (Security.getProvider(provider.name) == null) {
            Security.insertProviderAt(provider, 1)
        }

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
}