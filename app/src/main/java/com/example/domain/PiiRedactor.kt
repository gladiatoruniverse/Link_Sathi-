package com.example.domain

import com.example.model.ScanResult
import com.example.model.ScanType

/**
 * Security utility for sanitizing untrusted input, redacting sensitive PII/OTP/financial tokens
 * before local history persistence, and defanging URLs for safe sharing with family and friends.
 */
object PiiRedactor {

    private val HTML_SCRIPT_TAG_REGEX = Regex("<\\s*/?\\s*(script|iframe|object|embed|style|link|meta|svg|img|a|div|span)[^>]*>", RegexOption.IGNORE_CASE)
    private val GENERIC_HTML_TAG_REGEX = Regex("<[^>]{1,120}>")
    private val CONTROL_CHARS_REGEX = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")

    // Matches 12-16 digit card or Aadhaar-style sequences (with optional spaces or hyphens)
    private val LONG_NUMBER_REGEX = Regex("\\b(?:\\d[ -]?){11,15}\\d\\b")

    // Matches 10-digit Indian mobile numbers (optionally prefixed with +91 or 0)
    private val PHONE_NUMBER_REGEX = Regex("(?:\\+91[\\s-]?|0)?\\b[6-9]\\d{9}\\b")

    // Matches UPI IDs (e.g., merchant@okaxis, user@ybl) while ignoring standard email domains
    private val UPI_HANDLE_REGEX = Regex(
        "\\b[a-zA-Z0-9._-]{2,40}@(okaxis|okhdfcbank|okicici|oksbi|ybl|ibl|axl|paytm|apl|upi|sbi|hdfcbank|icici|axisbank|kotak|fbl|pnb)\\b",
        RegexOption.IGNORE_CASE
    )

    // Matches 4 to 8 digit standalone numeric OTPs or PINs (avoiding common 4-digit years like 2024-2029)
    private val OTP_OR_PIN_REGEX = Regex("\\b(?!202[0-9]\\b)(?!1930\\b)\\d{4,8}\\b")

    /**
     * Treats scanned content as untrusted input: strips HTML/script tags and control characters
     * so rendered strings cannot cause script or markup injection.
     */
    fun sanitizeUntrustedInput(raw: String, maxLength: Int = 4000): String {
        if (raw.isBlank()) return ""
        val capped = if (raw.length > maxLength) raw.take(maxLength) else raw
        return capped
            .replace(CONTROL_CHARS_REGEX, "")
            .replace(HTML_SCRIPT_TAG_REGEX, "[removed-tag]")
            .replace(GENERIC_HTML_TAG_REGEX, "")
            .trim()
    }

    /**
     * Redacts sensitive numbers, OTPs, UPI IDs, and phone numbers in scan history previews
     * so personal credentials or contact numbers are never persisted in cleartext.
     */
    fun createRedactedPreview(input: String, scanType: ScanType, maxPreviewLength: Int = 140): String {
        val sanitized = sanitizeUntrustedInput(input, maxLength = 1000)
            .replace(Regex("\\s+"), " ")
            .trim()

        val redacted = if (scanType == ScanType.MESSAGE) {
            sanitized
                .replace(UPI_HANDLE_REGEX, "[REDACTED-UPI]")
                .replace(LONG_NUMBER_REGEX, "[REDACTED-NUMBER]")
                .replace(PHONE_NUMBER_REGEX, "[REDACTED-PHONE]")
                .replace(OTP_OR_PIN_REGEX, "[REDACTED-CODE]")
        } else {
            // For URLs, redact embedded phone numbers, query token codes, or credentials before '@'
            sanitized
                .replace(Regex("(?<=://)[^/@\\s]+(?=\\@)"), "[REDACTED-USER]")
                .replace(PHONE_NUMBER_REGEX, "[REDACTED-PHONE]")
                .replace(Regex("(?<=[?&](token|otp|code|pin|key|pass)=)[^&\\s]+", RegexOption.IGNORE_CASE), "[REDACTED]")
        }

        return if (redacted.length > maxPreviewLength) {
            redacted.take(maxPreviewLength).trimEnd() + "…"
        } else {
            redacted
        }
    }

    /**
     * Defangs URLs (replaces http/https with hxxp/hxxps and dots with [.]) so shared safety warnings
     * cannot be accidentally tapped by family members in WhatsApp or SMS.
     */
    fun defangText(text: String): String {
        return text
            .replace(Regex("https://", RegexOption.IGNORE_CASE), "hxxps://")
            .replace(Regex("http://", RegexOption.IGNORE_CASE), "hxxp://")
            .replace(".", "[.]")
    }

    /**
     * Builds a clean, family-friendly safety warning card text for sharing via Android's share sheet.
     */
    fun buildShareableWarningText(result: ScanResult): String {
        val defangedPreview = defangText(result.redactedPreview)
        val topSignals = if (result.signals.isEmpty()) {
            "• No automated heuristic warning triggers found (still verify independently)."
        } else {
            result.signals.take(4).joinToString("\n") { "• ${it.title}: ${it.explanation}" }
        }
        val topAction = result.recommendations.firstOrNull()
            ?: "Never share OTPs, UPI PINs, or passwords, and verify requests via official apps."

        return buildString {
            appendLine("🛡️ LinkSaathi Digital Safety Advisory")
            appendLine("Assessment: ${result.riskLevel.label.uppercase()} (${result.scanType.displayName})")
            appendLine("Summary: ${result.summaryTitle}")
            appendLine()
            appendLine("Inspected Content (Defanged & Redacted):")
            appendLine("\"$defangedPreview\"")
            appendLine()
            appendLine("Key Findings:")
            appendLine(topSignals)
            appendLine()
            appendLine("Recommended Next Step:")
            appendLine("👉 $topAction")
            appendLine()
            append("Note: Generated by LinkSaathi on-device heuristic analysis. In India, report cyber fraud at 1930 or cybercrime.gov.in.")
        }
    }
}
