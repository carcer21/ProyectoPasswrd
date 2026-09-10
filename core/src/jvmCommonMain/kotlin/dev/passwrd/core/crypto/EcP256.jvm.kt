package dev.passwrd.core.crypto

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec

private const val FIELD_SIZE = 32
private const val CURVE = "secp256r1"

actual object EcP256 {
    actual suspend fun generateKeyPair(): EcKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec(CURVE))
        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKey = keyPair.public as ECPublicKey
        return EcKeyPair(
            privateKeyPkcs8 = keyPair.private.encoded,
            publicKeyX = publicKey.w.affineX.toFixedBytes(),
            publicKeyY = publicKey.w.affineY.toFixedBytes(),
        )
    }

    actual suspend fun sign(privateKeyPkcs8: ByteArray, data: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyPkcs8))
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    actual suspend fun verify(publicKeyX: ByteArray, publicKeyY: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        val algorithmParameters = AlgorithmParameters.getInstance("EC")
        algorithmParameters.init(ECGenParameterSpec(CURVE))
        val curveParams = algorithmParameters.getParameterSpec(ECParameterSpec::class.java)

        val keyFactory = KeyFactory.getInstance("EC")
        val publicKey = keyFactory.generatePublic(
            ECPublicKeySpec(ECPoint(BigInteger(1, publicKeyX), BigInteger(1, publicKeyY)), curveParams),
        )

        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(data)
        return try {
            verifier.verify(signature)
        } catch (e: java.security.SignatureException) {
            false
        }
    }

    private fun BigInteger.toFixedBytes(): ByteArray {
        val raw = toByteArray()
        return when {
            raw.size == FIELD_SIZE -> raw
            raw.size == FIELD_SIZE + 1 && raw[0] == 0.toByte() -> raw.copyOfRange(1, raw.size)
            raw.size < FIELD_SIZE -> ByteArray(FIELD_SIZE - raw.size) + raw
            else -> raw.copyOfRange(raw.size - FIELD_SIZE, raw.size)
        }
    }
}
