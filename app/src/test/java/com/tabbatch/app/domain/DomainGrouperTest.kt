package com.tabbatch.app.domain

import com.tabbatch.app.domain.grouping.DomainGrouper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainGrouperTest {

    @Test
    fun `same host groups together`() {
        assertEquals("example.com", DomainGrouper.registrableDomainOf("example.com"))
    }

    @Test
    fun `subdomain resolves to registrable domain`() {
        assertEquals("example.com", DomainGrouper.registrableDomainOf("docs.example.com"))
        assertEquals("example.com", DomainGrouper.registrableDomainOf("a.b.c.example.com"))
    }

    @Test
    fun `known two-label public suffix is handled`() {
        assertEquals("bbc.co.uk", DomainGrouper.registrableDomainOf("www.bbc.co.uk"))
        assertEquals("example.com.au", DomainGrouper.registrableDomainOf("shop.example.com.au"))
    }

    @Test
    fun `github io pages subdomain groups under github io`() {
        assertEquals("myuser.github.io", DomainGrouper.registrableDomainOf("myuser.github.io"))
    }

    @Test
    fun `ipv4 address has no registrable domain`() {
        assertNull(DomainGrouper.registrableDomainOf("192.168.1.1"))
        assertNull(DomainGrouper.registrableDomainOf("8.8.8.8"))
    }

    @Test
    fun `ipv6 address has no registrable domain`() {
        assertNull(DomainGrouper.registrableDomainOf("::1"))
        assertNull(DomainGrouper.registrableDomainOf("2001:db8::1"))
    }

    @Test
    fun `localhost has no registrable domain`() {
        assertNull(DomainGrouper.registrableDomainOf("localhost"))
    }

    @Test
    fun `single label host has no registrable domain`() {
        assertNull(DomainGrouper.registrableDomainOf("myrouter"))
    }

    @Test
    fun `empty host has no registrable domain`() {
        assertNull(DomainGrouper.registrableDomainOf(""))
    }

    @Test
    fun `host is case-insensitive`() {
        assertEquals("example.com", DomainGrouper.registrableDomainOf("EXAMPLE.COM"))
    }

    @Test
    fun `trailing dot is tolerated`() {
        assertEquals("example.com", DomainGrouper.registrableDomainOf("example.com."))
    }
}
