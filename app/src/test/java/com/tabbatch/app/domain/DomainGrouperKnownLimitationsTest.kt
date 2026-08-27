package com.tabbatch.app.domain

import com.tabbatch.app.domain.grouping.DomainGrouper
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Documents, with assertions, the *known-wrong* behavior of [DomainGrouper]'s simplified
 * "curated suffix list, else last two labels" heuristic (see ADR-0002 in
 * docs/ARCHITECTURE_DECISIONS.md). These tests intentionally assert the CURRENT (imperfect)
 * output, not the theoretically-correct eTLD+1 — the point is to make the limitation visible
 * and regression-checked, so a future contributor sees it's a known, accepted tradeoff and not
 * an oversight.
 */
class DomainGrouperKnownLimitationsTest {

    @Test
    fun `curated suffixes are handled correctly (positive control)`() {
        assertEquals("example.co.uk", DomainGrouper.registrableDomainOf("shop.example.co.uk"))
        assertEquals("example.com.au", DomainGrouper.registrableDomainOf("shop.example.com.au"))
        assertEquals("user.github.io", DomainGrouper.registrableDomainOf("user.github.io"))
        assertEquals("example.blogspot.com", DomainGrouper.registrableDomainOf("example.blogspot.com"))
    }

    @Test
    fun `known-wrong- multi-part public suffix not in curated list is mis-split`() {
        // amazonaws.com regional/service subdomains (e.g. S3, EC2 compute) are NOT full Public
        // Suffix List entries the same way co.uk is, so unrelated AWS-hosted tenants/buckets
        // sharing the "amazonaws.com" tail get incorrectly grouped together under
        // "amazonaws.com" instead of being treated as distinct services. This is the documented
        // tradeoff of NOT depending on the full PSL (ADR-0002) — not a bug to silently fix here.
        assertEquals(
            "amazonaws.com",
            DomainGrouper.registrableDomainOf("my-bucket.s3.us-east-1.amazonaws.com"),
        )
        assertEquals(
            "amazonaws.com",
            DomainGrouper.registrableDomainOf("other-bucket.s3.eu-west-1.amazonaws.com"),
        )
        // Two unrelated tenants collapse into the same registrable domain / group, even though a
        // full PSL-aware resolver would keep S3 buckets under their own bucket-specific subtree.
    }

    @Test
    fun `known-wrong- unlisted two-label ccTLD suffix falls back to last-two-labels`() {
        // "com.ar" (Argentina) is a real two-label public suffix but is not in
        // DomainGrouper.KNOWN_MULTI_LABEL_SUFFIXES, so the heuristic incorrectly treats "ar" as
        // if it doesn't need special handling and returns the last two labels ("com.ar") as if
        // that were itself the registrable domain, rather than "example.com.ar".
        assertEquals("com.ar", DomainGrouper.registrableDomainOf("shop.example.com.ar"))
    }

    @Test
    fun `co uk without a registrable label collapses to the suffix itself`() {
        // A bare "co.uk" host (no label in front of the suffix) still returns null per the
        // labels.size >= 3 guard — documented here as the edge-of-curated-list behavior.
        assertEquals(null, DomainGrouper.registrableDomainOf("co.uk"))
    }
}
