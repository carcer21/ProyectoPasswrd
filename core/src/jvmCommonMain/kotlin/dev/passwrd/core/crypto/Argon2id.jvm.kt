package dev.passwrd.core.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

actual object Argon2id {
    actual suspend fun derive(
        password: ByteArray,
        salt: ByteArray,
        params: Argon2Params,
        secret: ByteArray,
        associatedData: ByteArray,
    ): ByteArray {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(params.memoryKib)
            .withIterations(params.iterations)
            .withParallelism(params.parallelism)
            .withSalt(salt)

        if (secret.isNotEmpty()) builder.withSecret(secret)
        if (associatedData.isNotEmpty()) builder.withAdditional(associatedData)

        val generator = Argon2BytesGenerator().apply { init(builder.build()) }
        val output = ByteArray(params.outputLength)
        generator.generateBytes(password, output)
        return output
    }
}
