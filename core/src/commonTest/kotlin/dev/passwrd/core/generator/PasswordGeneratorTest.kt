package dev.passwrd.core.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PasswordGeneratorTest {

    @Test
    fun respectsRequestedLength() {
        val policy = PasswordPolicy(length = 24)
        assertEquals(24, PasswordGenerator.generate(policy).length)
    }

    @Test
    fun onlyUsesEnabledCharacterClasses() {
        val policy = PasswordPolicy(length = 200, useLower = false, useUpper = true, useDigits = false, useSymbols = false)
        val password = PasswordGenerator.generate(policy)
        assertTrue(password.all { it.isUpperCase() })
    }

    @Test
    fun rejectsEmptyCharset() {
        val policy = PasswordPolicy(useLower = false, useUpper = false, useDigits = false, useSymbols = false)
        assertFailsWith<IllegalArgumentException> { PasswordGenerator.generate(policy) }
    }

    @Test
    fun consecutiveGenerationsDiffer() {
        val policy = PasswordPolicy(length = 20)
        val a = PasswordGenerator.generate(policy)
        val b = PasswordGenerator.generate(policy)
        assertTrue(a != b)
    }

    @Test
    fun passphraseHasRequestedWordCount() {
        val phrase = PasswordGenerator.generatePassphrase(wordCount = 6, separator = "-")
        assertEquals(6, phrase.split("-").size)
    }

    @Test
    fun entropyIncreasesWithLength() {
        val short = PasswordGenerator.estimateEntropyBits(PasswordPolicy(length = 8))
        val long = PasswordGenerator.estimateEntropyBits(PasswordPolicy(length = 32))
        assertTrue(long > short)
    }
}
