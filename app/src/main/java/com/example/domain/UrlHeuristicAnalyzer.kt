package com.example.domain

import com.example.model.DetectedSignal
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.SignalSeverity
import java.net.URI

/**
 * Pure on-device heuristic URL analyzer.
 * Never opens, fetches, or executes URLs. Evaluates structural, lexical, and domain indicators locally.
 */
object UrlHeuristicAnalyzer {

    private const val MAX_SAFE_URL_LENGTH = 2048
    private const val LONG_URL_THRESHOLD = 120

    // Common public URL shorteners frequently abused in SMS/WhatsApp phishing
    private val KNOWN_SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "rb.gy", "is.gd",
        "ow.ly", "tiny.cc", "shorturl.at", "v.gd", "rebrand.ly", "bl.ink",
        "buff.ly", "tr.im", "cli.re"
    )

    // High-abuse top-level domains frequently paired with banking/KYC lures
    private val SUSPICIOUS_TLDS = setOf(
        "top", "xyz", "click", "buzz", "loan", "work", "gq", "tk",
        "cf", "ml", "ga", "zip", "mov", "icu", "monster", "rest", "fit"
    )

    // High-value Indian & global brands commonly impersonated in subdomains or hyphenated domains
    private val TARGETED_BRANDS = mapOf(
        "sbi" to listOf("sbi.co.in", "onlinesbi.sbi"),
        "onlinesbi" to listOf("onlinesbi.sbi", "sbi.co.in"),
        "hdfc" to listOf("hdfcbank.com", "hdfc.com"),
        "hdfcbank" to listOf("hdfcbank.com"),
        "icici" to listOf("icicibank.com"),
        "icicibank" to listOf("icicibank.com"),
        "axisbank" to listOf("axisbank.com"),
        "kotak" to listOf("kotak.com"),
        "pnb" to listOf("pnbindia.in"),
        "paytm" to listOf("paytm.com"),
        "phonepe" to listOf("phonepe.com"),
        "googlepay" to listOf("pay.google.com", "google.com"),
        "gpay" to listOf("pay.google.com", "google.com"),
        "npci" to listOf("npci.org.in"),
        "uidai" to listOf("uidai.gov.in"),
        "aadhaar" to listOf("uidai.gov.in"),
        "incometax" to listOf("incometax.gov.in"),
        "indiapost" to listOf("indiapost.gov.in"),
        "parivahan" to listOf("parivahan.gov.in"),
        "echallan" to listOf("parivahan.gov.in"),
        "amazon" to listOf("amazon.in", "amazon.com"),
        "flipkart" to listOf("flipkart.com"),
        "whatsapp" to listOf("whatsapp.com"),
        "netflix" to listOf("netflix.com"),
        "microsoft" to listOf("microsoft.com")
    )

    // Common character-substitution typosquats (e.g., rn -> m, 0 -> o, 1 -> l)
    private val TYPOSQUAT_PATTERNS = listOf(
        Regex("rnicrosoft", RegexOption.IGNORE_CASE) to "microsoft.com ('rn' used to mimic 'm')",
        Regex("g00gle", RegexOption.IGNORE_CASE) to "google.com (zeros '00' used to mimic 'oo')",
        Regex("amaz0n", RegexOption.IGNORE_CASE) to "amazon ('0' used to mimic 'o')",
        Regex("paytrn", RegexOption.IGNORE_CASE) to "paytm ('rn' used to mimic 'm')",
        Regex("hdfc-?bank", RegexOption.IGNORE_CASE) to "hdfcbank.com",
        Regex("sbi-?kyc|sbi-?yono|yono-?sbi", RegexOption.IGNORE_CASE) to "sbi.co.in",
        Regex("icici-?bank", RegexOption.IGNORE_CASE) to "icicibank.com"
    )

    // Dangerous file download extensions in URL paths
    private val DANGEROUS_EXTENSIONS_REGEX =
        Regex("\\.(apk|exe|scr|bat|cmd|msi|vbs|jar|iso)(?:[?#]|$)", RegexOption.IGNORE_CASE)

    // IPv4 address pattern
    private val IPV4_HOST_REGEX = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

    // Phishing lure keywords in URL path or hostname
    private val PHISHING_LURE_KEYWORDS = listOf(
        "kyc", "verify", "update-pan", "pan-link", "reward", "cashback",
        "unblock", "suspend", "refund", "login-auth", "netbanking", "otp", "free-gift"
    )

    sealed class ValidationOutcome {
        data class InvalidEmpty(val message: String) : ValidationOutcome()
        data class Ready(val result: ScanResult) : ValidationOutcome()
    }

    /**
     * Validates and analyzes a user-supplied URL string entirely on-device.
     */
    fun analyze(rawUrlInput: String): ValidationOutcome {
        val trimmed = rawUrlInput.trim()
        if (trimmed.isEmpty()) {
            return ValidationOutcome.InvalidEmpty("Please paste or enter a URL to inspect.")
        }

        val signals = mutableListOf<DetectedSignal>()
        val isTruncated = trimmed.length > MAX_SAFE_URL_LENGTH
        val sanitizedInput = PiiRedactor.sanitizeUntrustedInput(trimmed, maxLength = MAX_SAFE_URL_LENGTH)

        // Rule 1: Extremely long / oversized input check
        if (isTruncated || sanitizedInput.length > LONG_URL_THRESHOLD) {
            signals.add(
                DetectedSignal(
                    id = "url_excessive_length",
                    title = if (isTruncated) "Extremely Long URL (Truncated for Safety)" else "Unusually Long URL",
                    category = "URL Obfuscation",
                    severity = if (sanitizedInput.length > 250) SignalSeverity.CRITICAL else SignalSeverity.WARNING,
                    explanation = "This link is ${trimmed.length} characters long. Attackers often pad URLs with long random parameters or tracking strings to hide the real domain name on mobile screens.",
                    matchedSnippet = "${sanitizedInput.take(65)}… (${trimmed.length} chars)"
                )
            )
        }

        // Rule 2: Dangerous executable schemes (javascript:, data:, vbscript:, file:)
        val lowerInput = sanitizedInput.lowercase()
        if (lowerInput.startsWith("javascript:") || lowerInput.startsWith("data:") || lowerInput.startsWith("vbscript:")) {
            signals.add(
                DetectedSignal(
                    id = "url_executable_scheme",
                    title = "Executable Script / Data URI Scheme",
                    category = "Dangerous Syntax",
                    severity = SignalSeverity.CRITICAL,
                    explanation = "This string starts with a browser script or data URI scheme rather than a web address (https://). Pasting it into a browser can execute malicious code.",
                    matchedSnippet = sanitizedInput.take(40)
                )
            )
            return ValidationOutcome.Ready(buildResult(sanitizedInput, signals))
        }

        // Check scheme presence or malformed scheme (e.g., htp://, https//)
        val hasExplicitScheme = sanitizedInput.contains("://")
        val schemePrefix = if (hasExplicitScheme) sanitizedInput.substringBefore("://").lowercase() else ""
        if (hasExplicitScheme && schemePrefix !in listOf("http", "https")) {
            signals.add(
                DetectedSignal(
                    id = "url_invalid_scheme",
                    title = "Invalid or Unusual URL Protocol ($schemePrefix://)",
                    category = "URL Syntax",
                    severity = SignalSeverity.WARNING,
                    explanation = "Standard web links start with 'https://' or 'http://'. A misspelled protocol like '$schemePrefix://' indicates a malformed or tampered link.",
                    matchedSnippet = "$schemePrefix://"
                )
            )
        } else if (!hasExplicitScheme && (sanitizedInput.startsWith("http:") || sanitizedInput.startsWith("https:"))) {
            signals.add(
                DetectedSignal(
                    id = "url_malformed_scheme_slashes",
                    title = "Malformed URL Protocol Syntax",
                    category = "URL Syntax",
                    severity = SignalSeverity.WARNING,
                    explanation = "The URL protocol is missing the standard '://' separator.",
                    matchedSnippet = sanitizedInput.take(20)
                )
            )
        }

        // Normalize with https:// if no scheme was provided so URI parser can inspect host/path
        val candidateForParsing = when {
            hasExplicitScheme && schemePrefix in listOf("http", "https") -> sanitizedInput
            hasExplicitScheme -> "https://" + sanitizedInput.substringAfter("://")
            else -> "https://$sanitizedInput"
        }

        // Rule 3: Suspicious '@' symbol in authority (Credential/Host confusion attack)
        val authorityAndPath = candidateForParsing.substringAfter("://")
        val authorityPart = authorityAndPath.substringBefore("/").substringBefore("?").substringBefore("#")
        if (authorityPart.contains("@")) {
            val beforeAt = authorityPart.substringBeforeLast("@")
            val actualHost = authorityPart.substringAfterLast("@")
            signals.add(
                DetectedSignal(
                    id = "url_at_symbol_trick",
                    title = "Deceptive '@' Symbol in Web Address",
                    category = "URL Structure Trick",
                    severity = SignalSeverity.CRITICAL,
                    explanation = "Everything before the '@' symbol ('$beforeAt') is ignored as a username, and the browser actually connects to '$actualHost'. Attackers use this to make a link look like it belongs to a trusted bank.",
                    matchedSnippet = authorityPart
                )
            )
        }

        // Extract host after any '@' symbol
        val rawHostWithPort = authorityPart.substringAfterLast("@").trim()
        val rawHost = if (rawHostWithPort.startsWith("[")) {
            // IPv6 literal
            rawHostWithPort.substringBefore("]") + "]"
        } else {
            rawHostWithPort.substringBefore(":")
        }.lowercase()

        // Rule 4: Validate host syntax
        val isSyntaxValid = isValidHostSyntax(rawHost) && !sanitizedInput.contains(" ")
        if (!isSyntaxValid) {
            signals.add(
                DetectedSignal(
                    id = "url_malformed_syntax",
                    title = "Invalid or Malformed URL Syntax",
                    category = "URL Syntax",
                    severity = SignalSeverity.WARNING,
                    explanation = "This address contains invalid characters, spaces, consecutive dots, or lacks a valid domain name structure. Do not attempt to visit or fix unknown broken links from messages.",
                    matchedSnippet = sanitizedInput.take(60)
                )
            )
        }

        // Rule 5: Non-HTTPS check vs HTTPS educational reminder
        if (hasExplicitScheme && schemePrefix == "http") {
            signals.add(
                DetectedSignal(
                    id = "url_non_https",
                    title = "Unencrypted Connection (HTTP instead of HTTPS)",
                    category = "Transport Security",
                    severity = SignalSeverity.WARNING,
                    explanation = "This link uses unencrypted 'http://'. Any information entered on an HTTP page can be intercepted on the network. Legitimate banks and payment portals always use HTTPS.",
                    matchedSnippet = "http://"
                )
            )
        } else if (!hasExplicitScheme) {
            signals.add(
                DetectedSignal(
                    id = "url_missing_scheme",
                    title = "Missing Protocol Prefix (http/https)",
                    category = "URL Syntax",
                    severity = SignalSeverity.INFO,
                    explanation = "No 'https://' prefix was included. LinkSaathi analyzed the domain structure assuming a web link, but remember that even when HTTPS is present, it does NOT guarantee a website is legitimate.",
                    matchedSnippet = rawHost
                )
            )
        }

        if (rawHost.isNotEmpty()) {
            // Rule 6: IP address used instead of recognizable domain name
            val isIpv4 = IPV4_HOST_REGEX.matches(rawHost)
            val isIpv6 = rawHost.startsWith("[") && rawHost.endsWith("]") && rawHost.contains(":")
            if (isIpv4 || isIpv6) {
                signals.add(
                    DetectedSignal(
                        id = "url_ip_address_host",
                        title = "Numeric IP Address Used Instead of a Domain Name",
                        category = "Host Identity",
                        severity = SignalSeverity.CRITICAL,
                        explanation = "This link points directly to a raw numeric server address ('$rawHost') instead of an official domain name. Legitimate banks, couriers, and government portals never send login links using raw IP addresses.",
                        matchedSnippet = rawHostWithPort
                    )
                )
            }

            // Rule 7: Non-standard network port (e.g., :8080, :8443)
            val portPart = rawHostWithPort.substringAfter(":", "")
            if (portPart.isNotEmpty() && !rawHost.startsWith("[") && portPart !in listOf("80", "443")) {
                signals.add(
                    DetectedSignal(
                        id = "url_unusual_port",
                        title = "Unusual Network Port (:$portPart)",
                        category = "URL Structure",
                        severity = SignalSeverity.WARNING,
                        explanation = "This link specifies custom server port ':$portPart'. Consumer websites almost always use standard web ports without displaying a port number.",
                        matchedSnippet = rawHostWithPort
                    )
                )
            }

            // Rule 8: URL Shortener check
            val normalizedDomain = rawHost.removePrefix("www.")
            if (normalizedDomain in KNOWN_SHORTENERS) {
                signals.add(
                    DetectedSignal(
                        id = "url_shortener_detected",
                        title = "Shortened URL Hides Final Destination ($normalizedDomain)",
                        category = "Destination Opacity",
                        severity = SignalSeverity.WARNING,
                        explanation = "This link uses a URL shortener ('$normalizedDomain'), which conceals the actual website you will land on. Scammers use shorteners to hide phishing domains inside SMS and WhatsApp messages.",
                        matchedSnippet = normalizedDomain
                    )
                )
            }

            // Rule 9: Punycode ('xn--') and non-ASCII Unicode Homograph check
            val hasNonAscii = rawHost.any { it.code > 127 }
            if (rawHost.contains("xn--") || hasNonAscii) {
                signals.add(
                    DetectedSignal(
                        id = "url_punycode_homograph",
                        title = "Punycode or Lookalike International Domain ('xn--')",
                        category = "Lookalike Domain",
                        severity = SignalSeverity.CRITICAL,
                        explanation = "This domain uses internationalized Punycode ('xn--') or non-standard script characters. Attackers use visually identical foreign letters to impersonate well-known brands (homograph attack).",
                        matchedSnippet = rawHost
                    )
                )
            }

            // Rule 10: Misleading subdomains, brand impersonation & typosquatting
            if (!isIpv4 && !isIpv6 && isSyntaxValid) {
                val labels = rawHost.split(".")
                val tld = labels.lastOrNull() ?: ""
                val registrableDomain = extractRegistrableDomain(labels)

                // Check excessive subdomain depth (e.g. login.secure.sbi.account.evil.com)
                if (labels.size >= 5) {
                    signals.add(
                        DetectedSignal(
                            id = "url_deep_subdomains",
                            title = "Deeply Nested Subdomains (${labels.size} levels)",
                            category = "Misleading Subdomain",
                            severity = SignalSeverity.WARNING,
                            explanation = "This address chains ${labels.size} domain segments together. Scammers place trusted words at the start of a long subdomain so mobile browsers cut off the real domain at the end ('$registrableDomain').",
                            matchedSnippet = rawHost
                        )
                    )
                }

                // Check targeted brand impersonation in subdomain or deceptive domain
                for ((brandKeyword, officialDomains) in TARGETED_BRANDS) {
                    val containsBrandInUrl = rawHost.contains(brandKeyword) || authorityAndPath.lowercase().contains(brandKeyword)
                    val isOfficialDomain = officialDomains.any { official ->
                        rawHost == official || rawHost.endsWith(".$official")
                    }
                    if (containsBrandInUrl && !isOfficialDomain && normalizedDomain !in KNOWN_SHORTENERS) {
                        val inHost = rawHost.contains(brandKeyword)
                        signals.add(
                            DetectedSignal(
                                id = "url_brand_impersonation_$brandKeyword",
                                title = if (inHost) {
                                    "Potential Lookalike / Misleading Domain Mentioning '${brandKeyword.uppercase()}'"
                                } else {
                                    "Brand Reference ('${brandKeyword.uppercase()}') on Unaffiliated Domain"
                                },
                                category = "Lookalike Domain",
                                severity = SignalSeverity.CRITICAL,
                                explanation = "This link references '${brandKeyword.uppercase()}', but the actual website domain is '$registrableDomain' (not the official ${officialDomains.first()}). Scammers register hyphenated or lookalike domains to steal credentials.",
                                matchedSnippet = rawHost
                            )
                        )
                        break
                    }
                }

                // Check explicit character-substitution typosquats
                for ((pattern, description) in TYPOSQUAT_PATTERNS) {
                    if (pattern.containsMatchIn(rawHost)) {
                        val alreadyFlagged = signals.any { it.id.startsWith("url_brand_impersonation_") }
                        if (!alreadyFlagged) {
                            signals.add(
                                DetectedSignal(
                                    id = "url_typosquat_pattern",
                                    title = "Lookalike Domain Spelling Pattern Detected",
                                    category = "Lookalike Domain",
                                    severity = SignalSeverity.CRITICAL,
                                    explanation = "The domain '$rawHost' closely mimics $description. Always verify the exact spelling of the root domain before signing in.",
                                    matchedSnippet = rawHost
                                )
                            )
                        }
                        break
                    }
                }

                // Rule 11: High-abuse TLD check
                if (tld in SUSPICIOUS_TLDS) {
                    signals.add(
                        DetectedSignal(
                            id = "url_suspicious_tld",
                            title = "Uncommon or High-Risk Top-Level Domain (.$tld)",
                            category = "Domain Extension",
                            severity = SignalSeverity.WARNING,
                            explanation = "The '.${tld}' domain extension is inexpensive and disproportionately used by disposable phishing sites. Major Indian banks and government bodies use '.in', '.co.in', '.gov.in', or '.com'.",
                            matchedSnippet = ".$tld"
                        )
                    )
                }
            }
        }

        // Rule 12: Dangerous file download extension (.apk, .exe, .scr, etc.)
        val dangerousExtMatch = DANGEROUS_EXTENSIONS_REGEX.find(authorityAndPath)
        if (dangerousExtMatch != null) {
            val ext = dangerousExtMatch.groupValues[1].lowercase()
            signals.add(
                DetectedSignal(
                    id = "url_dangerous_file_ext",
                    title = "Direct App or Executable Download (.$ext)",
                    category = "Malware Risk",
                    severity = SignalSeverity.CRITICAL,
                    explanation = "This link points directly to a '.$ext' installer file. Installing '.apk' or executable files from SMS/WhatsApp links can grant attackers access to your SMS OTPs and banking apps.",
                    matchedSnippet = dangerousExtMatch.value
                )
            )
        }

        // Rule 13: Heavy percent-encoding or double-slash path obfuscation
        val percentMatches = Regex("%[0-9a-fA-F]{2}").findAll(sanitizedInput).count()
        val pathPart = authorityAndPath.substringAfter("/", "")
        if (percentMatches >= 3 || pathPart.contains("//")) {
            signals.add(
                DetectedSignal(
                    id = "url_obfuscated_encoding",
                    title = "Obfuscated Characters or Redirect Path Encoding",
                    category = "URL Obfuscation",
                    severity = SignalSeverity.WARNING,
                    explanation = "This URL uses encoded hex characters ('%XX') or multiple slashes ('//') in the path, a technique often used to disguise open redirects or hide deceptive parameters.",
                    matchedSnippet = sanitizedInput.take(70)
                )
            )
        }

        // Rule 14: Phishing lure keywords in URL path/domain when not on an official gov/bank domain
        val matchedLures = PHISHING_LURE_KEYWORDS.filter { authorityAndPath.lowercase().contains(it) }
        val isTrustedGovOrRbi = rawHost.endsWith(".gov.in") || rawHost.endsWith(".nic.in") || rawHost.endsWith(".rbi.org.in")
        if (matchedLures.isNotEmpty() && !isTrustedGovOrRbi) {
            signals.add(
                DetectedSignal(
                    id = "url_phishing_lure_words",
                    title = "High-Risk Lure Keywords in Link (${matchedLures.take(2).joinToString(", ")})",
                    category = "Lexical Indicator",
                    severity = SignalSeverity.WARNING,
                    explanation = "The link contains terms commonly used in phishing campaigns (${matchedLures.joinToString(", ")}). Verify account or KYC status directly inside your official app rather than through external links.",
                    matchedSnippet = matchedLures.joinToString(", ")
                )
            )
        }

        return ValidationOutcome.Ready(buildResult(sanitizedInput, signals))
    }

    private fun isValidHostSyntax(host: String): Boolean {
        if (host.isBlank()) return false
        if (IPV4_HOST_REGEX.matches(host)) {
            return host.split(".").all { (it.toIntOrNull() ?: 999) in 0..255 }
        }
        if (host.startsWith("[") && host.endsWith("]")) return true
        if (!host.contains(".")) return false
        if (host.startsWith(".") || host.endsWith(".") || host.contains("..")) return false
        val validLabelRegex = Regex("^[a-z0-9-_\\u0080-\\uFFFF]+$", RegexOption.IGNORE_CASE)
        val labels = host.split(".")
        if (labels.last().length < 2) return false
        return labels.all { label ->
            label.isNotEmpty() && !label.startsWith("-") && !label.endsWith("-") && validLabelRegex.matches(label)
        }
    }

    private fun extractRegistrableDomain(labels: List<String>): String {
        if (labels.size <= 2) return labels.joinToString(".")
        val secondLast = labels[labels.size - 2]
        val last = labels.last()
        return if (secondLast in listOf("co", "gov", "org", "net", "ac", "nic") && last.length == 2) {
            labels.takeLast(3).joinToString(".")
        } else {
            labels.takeLast(2).joinToString(".")
        }
    }

    private fun buildResult(sanitizedInput: String, signals: List<DetectedSignal>): ScanResult {
        val criticalCount = signals.count { it.severity == SignalSeverity.CRITICAL }
        val warningCount = signals.count { it.severity == SignalSeverity.WARNING }

        val riskLevel = when {
            criticalCount >= 1 || warningCount >= 3 -> RiskLevel.HIGH
            warningCount >= 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val summaryTitle = when (riskLevel) {
            RiskLevel.HIGH -> "High-Risk URL Indicators Detected"
            RiskLevel.MEDIUM -> "Caution Advised — Suspicious Link Characteristics"
            RiskLevel.LOW -> "No Obvious Heuristic Red Flags Detected"
        }

        val summaryReason = when (riskLevel) {
            RiskLevel.HIGH ->
                "LinkSaathi detected ${signals.size} warning sign(s), including critical indicators often used in phishing or malicious downloads. Do NOT open this link or enter any personal details."
            RiskLevel.MEDIUM ->
                "LinkSaathi identified ${signals.size} structural or domain warning sign(s) that warrant caution before visiting. Verify the destination independently."
            RiskLevel.LOW ->
                "No structural or lexical red flags were triggered by local checks. IMPORTANT: Neither HTTPS nor an absence of heuristic warnings guarantees a website is trustworthy."
        }

        val recommendations = buildList {
            when (riskLevel) {
                RiskLevel.HIGH -> {
                    add("Do NOT open this link or enter any passwords, OTPs, UPI PINs, or card numbers.")
                    add("If this link claimed to be from your bank, utility provider, or government portal, open their official app or type their known website address manually.")
                    if (signals.any { it.id == "url_dangerous_file_ext" }) {
                        add("Never download or install '.apk' files from links. Only install apps from the official Google Play Store.")
                    }
                    add("Use the 'Share Safety Warning' button below to alert family members if this link is circulating in WhatsApp groups.")
                }
                RiskLevel.MEDIUM -> {
                    add("Avoid signing in or entering financial details through this link.")
                    if (signals.any { it.id == "url_shortener_detected" }) {
                        add("Because this is a shortened URL, you cannot see the real destination domain until it redirects. Contact the sender or visit the official site directly instead.")
                    }
                    add("Verify the true root domain name (the word immediately before the first single '/') and remember that an HTTPS padlock does not prove legitimacy.")
                }
                RiskLevel.LOW -> {
                    add("Remember: HTTPS only means the network connection is encrypted — phishing websites can also use HTTPS.")
                    add("Even when no heuristic warning signs are detected, never enter an OTP, UPI PIN, or password on a page you reached from an unexpected SMS or chat link.")
                    add("Double-check the exact spelling of the domain name before logging into any sensitive account.")
                }
            }
        }

        return ScanResult(
            scanType = ScanType.URL,
            rawInput = sanitizedInput,
            sanitizedDisplayInput = sanitizedInput,
            redactedPreview = PiiRedactor.createRedactedPreview(sanitizedInput, ScanType.URL),
            riskLevel = riskLevel,
            summaryTitle = summaryTitle,
            summaryReason = summaryReason,
            signals = signals,
            recommendations = recommendations
        )
    }
}
