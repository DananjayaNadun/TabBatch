package com.tabbatch.app.domain

import com.tabbatch.app.domain.normalizer.NormalizeResult
import com.tabbatch.app.domain.normalizer.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun `valid https url is accepted`() {
        val result = UrlNormalizer.normalize("https://example.com/page")
        assertTrue(result is NormalizeResult.Success)
        result as NormalizeResult.Success
        assertEquals("https://example.com/page", result.normalizedUrl)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `valid http url is accepted`() {
        val result = UrlNormalizer.normalize("http://example.com")
        assertTrue(result is NormalizeResult.Success)
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        val result = UrlNormalizer.normalize("   https://example.com/page   ")
        assertTrue(result is NormalizeResult.Success)
        result as NormalizeResult.Success
        assertEquals("https://example.com/page", result.normalizedUrl)
    }

    @Test
    fun `wrapping double quotes are stripped`() {
        val result = UrlNormalizer.normalize("\"https://example.com/page\"")
        assertTrue(result is NormalizeResult.Success)
        result as NormalizeResult.Success
        assertEquals("https://example.com/page", result.normalizedUrl)
    }

    @Test
    fun `wrapping single quotes are stripped`() {
        val result = UrlNormalizer.normalize("'https://example.com/page'")
        assertTrue(result is NormalizeResult.Success)
    }

    @Test
    fun `malformed url is rejected`() {
        val result = UrlNormalizer.normalize("not a url at all ::")
        assertTrue(result is NormalizeResult.Failure)
    }

    @Test
    fun `unsupported scheme is rejected with typed error`() {
        val result = UrlNormalizer.normalize("ftp://example.com/file")
        assertTrue(result is NormalizeResult.Failure)
        result as NormalizeResult.Failure
        assertTrue(result.error is com.tabbatch.app.domain.model.ImportError.UnsupportedScheme)
    }

    @Test
    fun `javascript scheme is rejected`() {
        val result = UrlNormalizer.normalize("javascript:alert(1)")
        assertTrue(result is NormalizeResult.Failure)
    }

    @Test
    fun `query strings are preserved`() {
        val result = UrlNormalizer.normalize("https://example.com/search?q=hello+world&x=1")
        assertTrue(result is NormalizeResult.Success)
        result as NormalizeResult.Success
        assertEquals("https://example.com/search?q=hello+world&x=1", result.normalizedUrl)
    }

    @Test
    fun `fragments are preserved`() {
        val result = UrlNormalizer.normalize("https://example.com/page#section-2")
        assertTrue(result is NormalizeResult.Success)
        result as NormalizeResult.Success
        assertEquals("https://example.com/page#section-2", result.normalizedUrl)
    }

    @Test
    fun `empty string is rejected`() {
        val result = UrlNormalizer.normalize("")
        assertTrue(result is NormalizeResult.Failure)
    }

    @Test
    fun `host without scheme is rejected`() {
        val result = UrlNormalizer.normalize("example.com/page")
        assertTrue(result is NormalizeResult.Failure)
    }

    @Test
    fun `scheme is lower-cased`() {
        val result = UrlNormalizer.normalize("HTTPS://example.com")
        assertTrue(result is NormalizeResult.Success)
        result as NormalizeResult.Success
        assertTrue(result.normalizedUrl.startsWith("https://"))
    }

    @Test
    fun `internationalized host is converted to punycode for grouping`() {
        val result = UrlNormalizer.normalize("https://münchen.example/")
        assertTrue(result is NormalizeResult.Success)
        result as NormalizeResult.Success
        assertTrue(result.host.startsWith("xn--"))
    }

    @Test
    fun `url with no path is accepted`() {
        val result = UrlNormalizer.normalize("https://example.com")
        assertTrue(result is NormalizeResult.Success)
    }
}
