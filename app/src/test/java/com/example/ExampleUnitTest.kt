package com.example

import com.example.domain.MessageScamAnalyzer
import com.example.domain.PiiRedactor
import com.example.domain.UrlHeuristicAnalyzer
import com.example.model.RiskLevel
import com.example.model.ScanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `normal HTTPS URL is classified as LOW risk with explicit HTTPS disclaimer`() {
        val outcome = UrlHeuristicAnalyzer.analyze("https://www.rbi.org.in/Scripts/NotificationUser.aspx")
        assertTrue(outcome is UrlHeuristicAnalyzer.ValidationOutcome.Ready)
        val result = (outcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.LOW, result.riskLevel)
        assertTrue(result.recommendations.any { it.contains("HTTPS", ignoreCase = true) })
    }

    @Test
    fun `malformed URL is flagged with syntax warnings`() {
        val outcome = UrlHeuristicAnalyzer.analyze("htp:///bad..domain//@@?")
        assertTrue(outcome is UrlHeuristicAnalyzer.ValidationOutcome.Ready)
        val result = (outcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        assertTrue(result.riskLevel == RiskLevel.MEDIUM || result.riskLevel == RiskLevel.HIGH)
        assertTrue(result.signals.any { it.id == "url_invalid_scheme" || it.id == "url_malformed_syntax" })
    }

    @Test
    fun `URL containing suspicious patterns IP at-symbol punycode and shortener are detected`() {
        // 1. IP Address + HTTP + Port + Brand Lure
        val ipOutcome = UrlHeuristicAnalyzer.analyze("http://192.168.45.112:8080/sbi-kyc-update/login.html")
        val ipResult = (ipOutcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.HIGH, ipResult.riskLevel)
        assertTrue(ipResult.signals.any { it.id == "url_ip_address_host" })
        assertTrue(ipResult.signals.any { it.id == "url_non_https" })

        // 2. @ symbol trick + suspicious TLD
        val atOutcome = UrlHeuristicAnalyzer.analyze("https://hdfcbank.com@login-verify-account.top/auth")
        val atResult = (atOutcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.HIGH, atResult.riskLevel)
        assertTrue(atResult.signals.any { it.id == "url_at_symbol_trick" })

        // 3. Punycode domain
        val punyOutcome = UrlHeuristicAnalyzer.analyze("https://xn--pytm-4na.com/reward")
        val punyResult = (punyOutcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.HIGH, punyResult.riskLevel)
        assertTrue(punyResult.signals.any { it.id == "url_punycode_homograph" })

        // 4. Shortener
        val shortOutcome = UrlHeuristicAnalyzer.analyze("https://bit.ly/3xK9pLm")
        val shortResult = (shortOutcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.MEDIUM, shortResult.riskLevel)
        assertTrue(shortResult.signals.any { it.id == "url_shortener_detected" })
    }

    @Test
    fun `legitimate message with protective OTP reminder is classified as LOW risk`() {
        val msg = "Dear Customer, your monthly statement has been emailed. Bank officials will NEVER ask for your OTP, UPI PIN, or CVV."
        val outcome = MessageScamAnalyzer.analyze(msg)
        val result = (outcome as MessageScamAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.LOW, result.riskLevel)
        assertFalse(result.signals.any { it.id == "msg_credential_request" })
        assertTrue(result.signals.any { it.id == "msg_protective_otp_notice" })
    }

    @Test
    fun `message requesting an OTP and embedded phishing URL is classified as HIGH risk`() {
        val msg = "URGENT: Your SBI account is locked. Reply immediately with the 6-digit OTP 482910 sent to 9876543210 or verify at http://sbi-pan-verify.xyz"
        val outcome = MessageScamAnalyzer.analyze(msg)
        val result = (outcome as MessageScamAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(result.signals.any { it.id == "msg_credential_request" })
        // Verify automatic PII redaction in preview
        assertFalse(result.redactedPreview.contains("482910"))
        assertFalse(result.redactedPreview.contains("9876543210"))
        assertTrue(result.redactedPreview.contains("[REDACTED-CODE]"))
        assertTrue(result.redactedPreview.contains("[REDACTED-PHONE]"))
    }

    @Test
    fun `message containing urgent payment demand is classified as HIGH risk`() {
        val msg = "Dear Consumer, your electricity power connection will be disconnected tonight at 9:30 PM. Immediately pay Rs 10 verification fee and WhatsApp officer at 8899001122."
        val outcome = MessageScamAnalyzer.analyze(msg)
        val result = (outcome as MessageScamAnalyzer.ValidationOutcome.Ready).result
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(result.signals.any { it.id == "msg_urgent_payment_demand" })
    }

    @Test
    fun `empty input and extremely long input are handled gracefully`() {
        val emptyUrl = UrlHeuristicAnalyzer.analyze("   ")
        assertTrue(emptyUrl is UrlHeuristicAnalyzer.ValidationOutcome.InvalidEmpty)

        val emptyMsg = MessageScamAnalyzer.analyze("")
        assertTrue(emptyMsg is MessageScamAnalyzer.ValidationOutcome.InvalidEmpty)

        val veryLongUrl = "https://example.com/" + "a".repeat(3000)
        val longUrlOutcome = UrlHeuristicAnalyzer.analyze(veryLongUrl)
        assertTrue(longUrlOutcome is UrlHeuristicAnalyzer.ValidationOutcome.Ready)
        val longUrlResult = (longUrlOutcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        assertTrue(longUrlResult.signals.any { it.id == "url_excessive_length" })

        val veryLongMsg = "Hello ".repeat(1000)
        val longMsgOutcome = MessageScamAnalyzer.analyze(veryLongMsg)
        assertTrue(longMsgOutcome is MessageScamAnalyzer.ValidationOutcome.Ready)
    }

    @Test
    fun `untrusted HTML script tags are stripped and shareable summary defangs links`() {
        val maliciousInput = "<script>alert('xss')</script>Visit https://evil-phish.top/login"
        val sanitized = PiiRedactor.sanitizeUntrustedInput(maliciousInput)
        assertFalse(sanitized.contains("<script>"))

        val outcome = UrlHeuristicAnalyzer.analyze("https://evil-phish.top/login")
        val result = (outcome as UrlHeuristicAnalyzer.ValidationOutcome.Ready).result
        val shareText = PiiRedactor.buildShareableWarningText(result)
        assertTrue(shareText.contains("hxxps://evil-phish[.]top/login"))
    }
}
