package com.tabbatch.app.domain.normalizer

import com.tabbatch.app.domain.model.ImportError
import java.net.IDN
import java.net.URI
import java.net.URISyntaxException

/** Result of normalizing a single raw input line. Never throws — always returns one side. */
sealed class NormalizeResult {
    data class Success(
        val normalizedUrl: String,
        val originalUrl: String,
        val host: String,
    ) : NormalizeResult()

    data class Failure(
        val originalUrl: String,
        val error: ImportError,
    ) : NormalizeResult()
}

/**
 * Pure, deterministic URL validation/normalization.
 *
 * Deliberately conservative per the project brief (section 7): this does NOT strip query
 * parameters, does NOT rewrite tracking parameters, and does NOT change fragments. It only:
 *  - trims surrounding whitespace,
 *  - strips one layer of accidental wrapping quotes (`"https://x"` / `'https://x'`),
 *  - validates that the result is a syntactically well-formed http/https URL with a host,
 *  - lower-cases the scheme and host (hosts are case-insensitive per RFC 3986),
 *  - converts internationalized hostnames to their ASCII (punycode) form for [host]/grouping
 *    purposes while leaving [NormalizeResult.Success.normalizedUrl] otherwise untouched.
 */
object UrlNormalizer {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    fun normalize(rawInput: String): NormalizeResult {
        val original = rawInput
        var candidate = rawInput.trim()

        if (candidate.isEmpty()) {
            return NormalizeResult.Failure(original, ImportError.InvalidUrl("empty line"))
        }

        candidate = stripWrappingQuotes(candidate)

        val uri = try {
            URI(candidate)
        } catch (e: URISyntaxException) {
            return NormalizeResult.Failure(original, ImportError.InvalidUrl(candidate))
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme == null) {
            return NormalizeResult.Failure(original, ImportError.InvalidUrl(candidate))
        }
        if (scheme !in ALLOWED_SCHEMES) {
            return NormalizeResult.Failure(original, ImportError.UnsupportedScheme(scheme))
        }

        // java.net.URI#getHost() returns null for internationalized (non-ASCII) hostnames
        // because they don't match the strict RFC 3986 "host" grammar it enforces — it falls
        // back to treating the whole authority as opaque. Recover the host from the raw
        // authority in that case (stripping any userinfo/port) before IDN-converting it.
        val rawHost = uri.host ?: extractHostFromAuthority(uri.rawAuthority)
        if (rawHost.isNullOrBlank()) {
            return NormalizeResult.Failure(original, ImportError.InvalidUrl(candidate))
        }

        val asciiHost = try {
            IDN.toASCII(rawHost).lowercase()
        } catch (e: IllegalArgumentException) {
            rawHost.lowercase()
        }

        // Rebuild with a lower-cased scheme; leave authority/path/query/fragment characters as-is
        // (we do not want to alter path casing or percent-encoding semantics).
        val schemeAndRest = candidate.indexOf("://").let { idx ->
            if (idx < 0) candidate else candidate.substring(idx)
        }
        val normalized = scheme + schemeAndRest

        return NormalizeResult.Success(
            normalizedUrl = normalized,
            originalUrl = original,
            host = asciiHost,
        )
    }

    /** Best-effort host extraction from a raw (possibly non-ASCII) authority component, used
     * only as a fallback when [URI.getHost] returns null for an internationalized hostname. */
    private fun extractHostFromAuthority(rawAuthority: String?): String? {
        if (rawAuthority.isNullOrBlank()) return null
        val withoutUserInfo = rawAuthority.substringAfterLast('@')
        return if (withoutUserInfo.startsWith("[")) {
            // IPv6 literal, e.g. "[::1]:8080" -> "[::1]"
            withoutUserInfo.substringBefore(']') + "]"
        } else {
            withoutUserInfo.substringBefore(':')
        }
    }

    private fun stripWrappingQuotes(input: String): String {
        if (input.length < 2) return input
        val first = input.first()
        val last = input.last()
        val isQuotePair = (first == '"' && last == '"') || (first == '\'' && last == '\'')
        return if (isQuotePair) input.substring(1, input.length - 1).trim() else input
    }
}
