package com.tabbatch.app.domain.grouping

import java.util.Locale

/**
 * Best-effort, dependency-free registrable-domain ("eTLD+1") resolver.
 *
 * ## Why not a full Public Suffix List (PSL) dependency
 * A fully correct implementation of registrable-domain resolution requires Mozilla's Public
 * Suffix List (thousands of rules, including exceptions and wildcards), which changes over
 * time. Two realistic options were considered:
 *
 *  1. `com.google.guava:guava` `InternetDomainName` — correct, well-maintained, but pulls in
 *     all of Guava (~3 MB) for a single feature, and still needs periodic PSL updates bundled
 *     with the Guava version.
 *  2. A small hand-maintained heuristic covering the common cases.
 *
 * TabBatch ships option 2 to keep the dependency footprint minimal for an offline utility app,
 * and documents its limits explicitly here rather than silently getting rare cases wrong. This
 * is a deliberate, documented tradeoff (see docs/ARCHITECTURE_DECISIONS.md, ADR-0002) — NOT a
 * naive `host.split(".").takeLast(2)`.
 *
 * ## Heuristic
 * 1. IP addresses (v4/v6) and `localhost` are never treated as having a registrable domain —
 *    the group falls back to the literal host.
 * 2. A small curated list of common two-label public suffixes (e.g. `co.uk`, `com.au`,
 *    `github.io`) is checked first: if the host ends in one of these, the registrable domain is
 *    the label immediately preceding it plus the suffix.
 * 3. Otherwise, the registrable domain is the last two labels of the host (standard eTLD+1
 *    shape for the overwhelming majority of gTLD/ccTLD domains, e.g. `example.com`).
 * 4. Single-label hosts (e.g. a bare hostname on a LAN) have no registrable domain.
 *
 * ## Known limitations
 * This heuristic will misclassify registrable domains for public suffixes NOT in
 * [KNOWN_MULTI_LABEL_SUFFIXES] (e.g. some `*.compute.amazonaws.com`-style multi-part suffixes),
 * and does not implement PSL wildcard/exception rules. For an app whose job is descriptive
 * grouping (not security-sensitive cookie scoping), this tradeoff is acceptable and disclosed.
 */
object DomainGrouper {

    // Common multi-label public suffixes. Not exhaustive — see class doc.
    private val KNOWN_MULTI_LABEL_SUFFIXES = setOf(
        "co.uk", "org.uk", "ac.uk", "gov.uk", "me.uk", "ltd.uk", "plc.uk",
        "com.au", "net.au", "org.au", "edu.au", "gov.au",
        "co.nz", "co.jp", "co.in", "co.za", "co.kr", "co.id",
        "com.br", "com.cn", "com.mx", "com.tr", "com.sg", "com.hk", "com.tw",
        "github.io", "gitlab.io", "pages.dev", "netlify.app", "vercel.app", "web.app",
        "blogspot.com", "wordpress.com", "appspot.com",
    )

    private val IPV4_REGEX = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

    /** Returns the best-effort registrable domain for [host], or null if none applies
     * (IP address, localhost, single-label host, or empty host). */
    fun registrableDomainOf(host: String): String? {
        val normalized = host.trim().lowercase(Locale.ROOT).trimEnd('.')
        if (normalized.isEmpty()) return null
        if (isLikelyIpAddress(normalized)) return null
        if (normalized == "localhost" || normalized.endsWith(".localhost")) return null

        val labels = normalized.split(".")
        if (labels.size < 2) return null

        // Check known 2-label public suffixes (e.g. co.uk) using the last two labels.
        val lastTwo = labels.takeLast(2).joinToString(".")
        if (lastTwo in KNOWN_MULTI_LABEL_SUFFIXES) {
            return if (labels.size >= 3) labels.takeLast(3).joinToString(".") else null
        }

        return labels.takeLast(2).joinToString(".")
    }

    /** Human-friendly display name for a registrable domain / host, used as the group label.
     * Currently just the domain itself (e.g. "youtube.com"); kept as a separate function so a
     * future "friendly brand name" lookup (YouTube, GitHub, ...) can be added without touching
     * grouping logic. */
    fun displayNameFor(registrableDomainOrHost: String): String = registrableDomainOrHost

    private fun isLikelyIpAddress(host: String): Boolean {
        if (IPV4_REGEX.matches(host)) {
            return host.split(".").all { it.toIntOrNull()?.let { n -> n in 0..255 } == true }
        }
        // IPv6 literals in a URI host come with brackets, e.g. "[::1]"; java.net.URI#getHost()
        // sometimes retains them depending on JDK version, so check both bracketed and colon forms.
        val stripped = host.removePrefix("[").removeSuffix("]")
        return stripped.contains(":") && stripped.count { it == ':' } >= 2
    }
}
