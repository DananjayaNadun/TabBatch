# Security Policy

TabBatch is a local-first Android application with no backend server, no account system, and
no required network access for core functionality. Its main sensitive-data surface is the
tab/URL data it processes and exports, all of which stays on-device unless the user explicitly
shares an exported file.

## Reporting a vulnerability

If you believe you've found a security issue (for example: a way for exported files to leak
data unexpectedly, a Storage Access Framework misuse that exposes files outside the intended
scope, or an issue in how imported/untrusted CSV/JSON is parsed), please report it responsibly:

- **Preferred:** open a [GitHub Security Advisory](../../security/advisories/new) on this
  repository (private to maintainers until resolved).
- **Alternative:** email `security@tabbatch.example` (placeholder — replace with a maintained
  address before publishing this repository) with a description of the issue, steps to
  reproduce, and any relevant logs/screenshots.

Please do not open a public GitHub issue for a security vulnerability.

## What to include

- Affected version/commit.
- Steps to reproduce.
- Expected vs. actual behavior.
- Impact assessment, if known (e.g. what data could be exposed, and to whom).

## Response

This is a portfolio/community project maintained on a best-effort basis. We aim to acknowledge
reports within a reasonable time and will credit reporters (unless you prefer to remain
anonymous) once a fix ships.

## Scope

In scope:

- The Android application code in this repository.
- The CI/build configuration.

Out of scope:

- Third-party libraries' own vulnerabilities (report those upstream; we'll still take a
  dependency bump PR).
- Social engineering, physical device access, or anything requiring the attacker to already
  control the user's device.
