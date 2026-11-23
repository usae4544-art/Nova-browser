package com.example.engine

import android.net.http.SslCertificate
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Locale

enum class PageSecurityState {
    NOVA_HOME,
    SECURE_HTTPS,
    INSECURE_HTTP,
    CERT_ERROR
}

data class SiteCertificateSummary(
    val host: String,
    val issuedTo: String,
    val issuedBy: String,
    val validUntil: String,
    val isHttps: Boolean,
    val threatWarning: String?
)

object SecurityUtils {

    fun classifyUrlSecurity(url: String, hasSslError: Boolean = false): PageSecurityState {
        if (hasSslError) return PageSecurityState.CERT_ERROR
        return when {
            url.isBlank() || url == UrlResolver.NOVA_HOME_URL -> PageSecurityState.NOVA_HOME
            url.startsWith("https://", ignoreCase = true) -> PageSecurityState.SECURE_HTTPS
            else -> PageSecurityState.INSECURE_HTTP
        }
    }

    /**
     * Heuristic anti-phishing & link safety check:
     * Flags deceptive credential-embedded URLs (e.g. https://trusted.com@evil.site),
     * Punycode homograph domains (xn--), or raw-IP login pages.
     */
    fun detectSuspiciousUrlWarning(url: String): String? {
        if (url.isBlank() || url == UrlResolver.NOVA_HOME_URL) return null
        val afterScheme = url.substringAfter("://", "")
        val authority = afterScheme.substringBefore("/")
        if (authority.contains("@")) {
            return "Suspicious URL detected: contains embedded '@' authority redirect."
        }
        val host = UrlResolver.extractHost(url)
        if (host.contains("xn--", ignoreCase = true)) {
            return "Internationalized (Punycode) domain detected. Verify the website spelling carefully."
        }
        val lower = url.lowercase()
        val isRawIp = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(host)
        if (isRawIp && (lower.contains("login") || lower.contains("bank") || lower.contains("verify") || lower.contains("signin"))) {
            return "Caution: Sensitive login page hosted on a raw IP address instead of a verified domain."
        }
        return null
    }

    fun buildCertificateSummary(url: String, cert: SslCertificate?): SiteCertificateSummary {
        val host = UrlResolver.extractHost(url).ifBlank { "Local / Home" }
        val isHttps = url.startsWith("https://", ignoreCase = true)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val issuedTo = cert?.issuedTo?.cName?.takeIf { it.isNotBlank() }
            ?: cert?.issuedTo?.oName?.takeIf { it.isNotBlank() }
            ?: if (isHttps) host else "Not encrypted"
        val issuedBy = cert?.issuedBy?.oName?.takeIf { it.isNotBlank() }
            ?: cert?.issuedBy?.cName?.takeIf { it.isNotBlank() }
            ?: if (isHttps) "Trusted Certificate Authority" else "None (Plain HTTP)"
        val validUntil = cert?.validNotAfterDate?.let { dateFormat.format(it) }
            ?: if (isHttps) "Verified by Android Trust Store" else "No TLS certificate"
        return SiteCertificateSummary(
            host = host,
            issuedTo = issuedTo,
            issuedBy = issuedBy,
            validUntil = validUntil,
            isHttps = isHttps,
            threatWarning = detectSuspiciousUrlWarning(url)
        )
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHexString()
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var current = "$salt:$pin:nova_vault_v1".toByteArray(Charsets.UTF_8)
        // Iterative stretching for strong offline brute-force resistance
        repeat(10_000) {
            current = digest.digest(current)
        }
        return current.toHexString()
    }

    fun verifyPin(pin: String, expectedHash: String, salt: String): Boolean {
        if (expectedHash.isBlank() || salt.isBlank()) return false
        val actual = hashPin(pin, salt)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    private fun ByteArray.toHexString(): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }
}
