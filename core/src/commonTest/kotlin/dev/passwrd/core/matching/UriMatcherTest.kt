package dev.passwrd.core.matching

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UriMatcherTest {

    @Test
    fun exactHostMatches() {
        assertTrue(UriMatcher.matchesHost("https://github.com", "github.com"))
    }

    @Test
    fun subdomainOfStoredHostMatches() {
        assertTrue(UriMatcher.matchesHost("https://github.com", "gist.github.com"))
    }

    @Test
    fun storedSubdomainDoesNotMatchParentDomain() {
        assertFalse(UriMatcher.matchesHost("https://gist.github.com", "github.com"))
    }

    @Test
    fun unrelatedDomainDoesNotMatch() {
        assertFalse(UriMatcher.matchesHost("https://github.com", "github.com.evil.example"))
    }

    @Test
    fun matchIsCaseInsensitive() {
        assertTrue(UriMatcher.matchesHost("https://GitHub.com", "github.COM"))
    }

    @Test
    fun uriWithPathAndPortStillMatches() {
        assertTrue(UriMatcher.matchesHost("https://github.com:443/login", "github.com"))
    }
}
