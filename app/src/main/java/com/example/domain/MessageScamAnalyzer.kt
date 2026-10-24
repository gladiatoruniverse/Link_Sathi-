package com.example.domain

import com.example.model.DetectedSignal
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.SignalSeverity

/**
 * Pure on-device rule-based Message Scam Analyzer for SMS, WhatsApp, and email text.
 *
 * Key design principle:
 * Never labels a message definitively fraudulent solely because it contains a single keyword like "OTP".
 * Distinguishes between protective security reminders ("Never share your OTP") and active solicitation
 * ("Reply with the 6-digit OTP"), and explains uncertainty clearly so users can inspect the evidence.
 */
object MessageScamAnalyzer {

    private const val MAX_SAFE_MESSAGE_LENGTH = 4000

    // Protective disclaimer phrases used in genuine bank alerts (e.g., "Bank will NEVER ask for your OTP")
    private val PROTECTIVE_OTP_REMINDER_REGEX = Regex(
        "\\b(never\\s+(share|disclose|reveal|give|ask)|do\\s+not\\s+(share|disclose|reveal|give)|will\\s+never\\s+ask|does\\s+not\\s+ask)\\b[^.!?]{0,70}\\b(otp|pin|password|cvv)\\b",
        RegexOption.IGNORE_CASE
    )

    // Rule Category 1: Active requests for OTPs, passwords, UPI PINs, CVV, or banking credentials
    private val CREDENTIAL_SOLICITATION_REGEX = Regex(
        "\\b(share|send|reply(\\s+with)?|enter|provide|tell|confirm|submit|verify|input)\\b[^.!?]{0,60}\\b(otp|one[ -]?time[ -]?password|upi[ -]?pin|mpin|atm[ -]?pin|cvv|password|passcode|netbanking\\s+id|login\\s+credentials)\\b|" +
            "\\b(otp|upi[ -]?pin|cvv|atm[ -]?pin)\\b[^.!?]{0,50}\\b(to\\s+(receive|claim|cancel|stop|verify|unblock|activate)|is\\s+required\\s+to)\\b",
        RegexOption.IGNORE_CASE
    )

    // UPI Collect / QR receive money scam ("enter UPI PIN to receive money")
    private val UPI_RECEIVE_PIN_SCAM_REGEX = Regex(
        "\\b(enter|input|use)\\s+(your\\s+)?(upi\\s+)?pin\\b[^.!?]{0,50}\\b(receive|get|claim|credit|refund|cashback)\\b|" +
            "\\bscan\\s+(this\\s+)?qr\\b[^.!?]{0,40}\\b(receive|get|credit|cashback)\\b",
        RegexOption.IGNORE_CASE
    )

    // Rule Category 2: Urgent payment demands, bill disconnection, or artificial countdowns
    private val URGENT_PAYMENT_OR_DEADLINE_REGEX = Regex(
        "\\b(electricity|power\\s+supply|water\\s+supply|gas\\s+connection)\\b[^.!?]{0,80}\\b(disconnect(ed)?|cut\\s+off|suspended)\\s+(tonight|today|immediately|at\\s+\\d)|" +
            "\\b(immediately|urgent(ly)?|within\\s+\\d+\\s*(min(ute)?s?|hours?|hrs?)|tonight\\s+at)\\b[^.!?]{0,80}\\b(pay|transfer|deposit|update|verify|complete\\s+kyc|link\\s+pan)\\b|" +
            "\\b(pay|transfer|send)\\s*(rs\\.?|inr|₹)\\s*\\d+",
        RegexOption.IGNORE_CASE
    )

    // Rule Category 3: Unexpected prizes, lottery wins, rewards, or tax refunds
    private val PRIZE_OR_REFUND_LURE_REGEX = Regex(
        "\\b(congratulations|congrats|lucky\\s+winner|lucky\\s+draw|won\\s+a?\\s*(prize|lottery|cashback|iphone|car|reward)|" +
            "income\\s+tax\\s+refund|approved\\s+refund|pm[ -]?kisan\\s+installment|pre-?approved\\s+grant|claim\\s+your\\s+(reward|bonus|prize|refund))\\b",
        RegexOption.IGNORE_CASE
    )

    // Rule Category 4: Unknown APK installation or Remote Access / Screen-Sharing apps
    private val UNKNOWN_APP_INSTALL_REGEX = Regex(
        "\\b([a-zA-Z0-9_-]+\\.apk)\\b|" +
            "\\b(install|download)\\b[^.!?]{0,60}\\b(apk|anydesk|teamviewer|quicksupport|rustdesk|airdroid|screen[ -]?shar(e|ing)|verification\\s+app|support\\s+app)\\b|" +
            "\\b(anydesk|teamviewer|quicksupport|rustdesk)\\b",
        RegexOption.IGNORE_CASE
    )

    // Rule Category 5: Account suspension, KYC block, or legal / police / customs ("Digital Arrest") threats
    private val SUSPENSION_OR_LEGAL_THREAT_REGEX = Regex(
        "\\b(account|sim\\s+card|pan\\s+card|aadhaar|netbanking|yono|debit\\s+card)\\b[^.!?]{0,60}\\b(blocked|suspended|frozen|deactivated|closed|delinked|locked)\\b|" +
            "\\b(arrest\\s+warrant|digital\\s+arrest|cbi|narcotics|customs\\s+parcel|police\\s+case|fir\\s+registered|legal\\s+action|court\\s+notice|seized\\s+parcel|money\\s+laundering)\\b",
        RegexOption.IGNORE_CASE
    )

    // Rule Category 6: Off-channel contact coercion (calling/messaging a random personal mobile number for "official" support)
    private val UNOFFICIAL_CONTACT_REGEX = Regex(
        "\\b(call|contact|whatsapp)\\s+(our\\s+)?(officer|manager|agent|helpline|support|customs|head\\s+office)?[^.!?]{0,35}\\b(\\+91[\\s-]?)?[6-9]\\d{9}\\b",
        RegexOption.IGNORE_CASE
    )

    // Extracts embedded URLs (with http/https or common domain patterns) inside messages
    private val EMBEDDED_URL_REGEX = Regex(
        "(?:https?://|www\\.)[^\\s<>\"')\\]]+|\\b[a-zA-Z0-9.-]+\\.(?:com|in|co\\.in|org|net|top|xyz|click|buzz|loan|work|ly|gd|cc|at|apk)(?:/[^\\s<>\"')\\]]*)?",
        RegexOption.IGNORE_CASE
    )

    sealed class ValidationOutcome {
        data class InvalidEmpty(val message: String) : ValidationOutcome()
        data class Ready(val result: ScanResult) : ValidationOutcome()
    }

    /**
     * Analyzes an SMS, WhatsApp, or email message entirely on-device.
     */
    fun analyze(rawMessageInput: String): ValidationOutcome {
        val trimmed = rawMessageInput.trim()
        if (trimmed.isEmpty()) {
            return ValidationOutcome.InvalidEmpty("Please paste or enter a message to analyze.")
        }

        val isTruncated = trimmed.length > MAX_SAFE_MESSAGE_LENGTH
        val sanitized = PiiRedactor.sanitizeUntrustedInput(trimmed, maxLength = MAX_SAFE_MESSAGE_LENGTH)
        val signals = mutableListOf<DetectedSignal>()

        if (isTruncated) {
            signals.add(
                DetectedSignal(
                    id = "msg_truncated_length",
                    title = "Extremely Long Message Input (${trimmed.length} chars)",
                    category = "Input Length",
                    severity = SignalSeverity.INFO,
                    explanation = "LinkSaathi safely analyzed the first $MAX_SAFE_MESSAGE_LENGTH characters on-device to prevent memory exhaustion.",
                    matchedSnippet = "${sanitized.take(60)}…"
                )
            )
        }

        // Remove protective reminder clauses when checking for active credential solicitation
        // so genuine bank reminders ("We NEVER ask for your OTP") are not misclassified as scams.
        val hasProtectiveReminder = PROTECTIVE_OTP_REMINDER_REGEX.containsMatchIn(sanitized)
        val textWithoutProtectiveClauses = sanitized.replace(PROTECTIVE_OTP_REMINDER_REGEX, " ")

        // Check 1A: UPI PIN to receive money scam
        val upiReceiveMatch = UPI_RECEIVE_PIN_SCAM_REGEX.find(textWithoutProtectiveClauses)
        if (upiReceiveMatch != null) {
            signals.add(
                DetectedSignal(
                    id = "msg_upi_receive_pin_scam",
                    title = "Request to Enter UPI PIN to 'Receive' Money",
                    category = "Credential & UPI Fraud",
                    severity = SignalSeverity.CRITICAL,
                    explanation = "A UPI PIN is ONLY needed when money is leaving your bank account. You never need to enter a PIN or scan a QR code with a PIN to receive a refund, cashback, or payment.",
                    matchedSnippet = upiReceiveMatch.value.trim()
                )
            )
        }

        // Check 1B: Active OTP / Password / PIN / CVV solicitation
        val credentialMatch = CREDENTIAL_SOLICITATION_REGEX.find(textWithoutProtectiveClauses)
        if (credentialMatch != null) {
            signals.add(
                DetectedSignal(
                    id = "msg_credential_request",
                    title = "Solicitation of OTP, PIN, or Banking Credentials",
                    category = "Credential Harvesting",
                    severity = SignalSeverity.CRITICAL,
                    explanation = "This message asks you to share, enter, or reply with sensitive credentials (such as an OTP, UPI PIN, ATM PIN, or password). Legitimate banks and government agencies never ask you to share these secrets.",
                    matchedSnippet = credentialMatch.value.trim()
                )
            )
        } else if (hasProtectiveReminder) {
            signals.add(
                DetectedSignal(
                    id = "msg_protective_otp_notice",
                    title = "Contains Standard Protective Security Reminder",
                    category = "Contextual Check",
                    severity = SignalSeverity.INFO,
                    explanation = "This message mentions OTP/PIN in a protective context (reminding you NOT to share it). LinkSaathi distinguishes protective advice from scam requests, though you should still verify the rest of the message.",
                    matchedSnippet = PROTECTIVE_OTP_REMINDER_REGEX.find(sanitized)?.value?.trim()
                )
            )
        }

        // Check 2: Urgent payment demands or utility disconnection countdowns
        val urgencyMatch = URGENT_PAYMENT_OR_DEADLINE_REGEX.find(sanitized)
        if (urgencyMatch != null) {
            signals.add(
                DetectedSignal(
                    id = "msg_urgent_payment_demand",
                    title = "High-Urgency Payment Demand or Disconnection Deadline",
                    category = "Urgency & Coercion",
                    severity = SignalSeverity.CRITICAL,
                    explanation = "Scammers manufacture artificial panic (such as threatening tonight's electricity disconnection or immediate payment deadlines) so you act before verifying your actual bill or account status.",
                    matchedSnippet = urgencyMatch.value.trim()
                )
            )
        }

        // Check 3: Unexpected prizes, rewards, lottery wins, or tax refunds
        val prizeMatch = PRIZE_OR_REFUND_LURE_REGEX.find(sanitized)
        if (prizeMatch != null) {
            signals.add(
                DetectedSignal(
                    id = "msg_prize_or_refund_lure",
                    title = "Unexpected Prize, Reward, or Refund Lure",
                    category = "Financial Lure",
                    severity = SignalSeverity.WARNING,
                    explanation = "Unsolicited messages promising lottery winnings, lucky draw gifts, or instant tax/subsidy disbursements often lead to phishing forms or 'processing fee' advance-fee fraud.",
                    matchedSnippet = prizeMatch.value.trim()
                )
            )
        }

        // Check 4: Unknown APK installation or Remote Access software
        val appInstallMatch = UNKNOWN_APP_INSTALL_REGEX.find(sanitized)
        if (appInstallMatch != null) {
            signals.add(
                DetectedSignal(
                    id = "msg_unknown_app_install",
                    title = "Request to Install Unknown '.apk' or Screen-Sharing App",
                    category = "Malware / Remote Takeover",
                    severity = SignalSeverity.CRITICAL,
                    explanation = "This message instructs you to download an '.apk' file or use a remote screen-sharing tool (such as AnyDesk, TeamViewer, or QuickSupport). Fraudsters use these apps to silently read your SMS OTPs and control your phone.",
                    matchedSnippet = appInstallMatch.value.trim()
                )
            )
        }

        // Check 5: Account suspension, KYC block, or legal / police / customs threats
        val threatMatch = SUSPENSION_OR_LEGAL_THREAT_REGEX.find(sanitized)
        if (threatMatch != null) {
            val isLegalOrCustoms = threatMatch.value.contains(
                Regex("arrest|cbi|narcotics|customs|police|fir|court|seized|laundering", RegexOption.IGNORE_CASE)
            )
            signals.add(
                DetectedSignal(
                    id = "msg_suspension_or_legal_threat",
                    title = if (isLegalOrCustoms) {
                        "Law Enforcement / Customs Parcel Threat ('Digital Arrest' Pattern)"
                    } else {
                        "Account Suspension or KYC Freeze Threat"
                    },
                    category = "Impersonation & Intimidation",
                    severity = if (isLegalOrCustoms) SignalSeverity.CRITICAL else SignalSeverity.WARNING,
                    explanation = if (isLegalOrCustoms) {
                        "Indian law enforcement, CBI, Customs, and RBI never threaten arrest via SMS/WhatsApp or demand money transfers to clear a parcel or investigation."
                    } else {
                        "Messages claiming your bank account, SIM, or PAN/KYC is blocked are the #1 lure used to trick users into clicking phishing links."
                    },
                    matchedSnippet = threatMatch.value.trim()
                )
            )
        }

        // Check 6: Coercion to call/WhatsApp an unverified personal mobile number
        val contactMatch = UNOFFICIAL_CONTACT_REGEX.find(sanitized)
        if (contactMatch != null) {
            signals.add(
                DetectedSignal(
                    id = "msg_unofficial_mobile_contact",
                    title = "Directs You to Call/WhatsApp a Personal Mobile Number",
                    category = "Impersonation Indicator",
                    severity = SignalSeverity.WARNING,
                    explanation = "Electricity boards, banks, and government departments do not ask consumers to contact individual 10-digit WhatsApp or mobile numbers for bill updates or KYC.",
                    matchedSnippet = contactMatch.value.trim()
                )
            )
        }

        // Check 7: Embedded URLs inside the message
        val rawCandidateLinks = EMBEDDED_URL_REGEX.findAll(sanitized)
            .map { it.value.trimEnd('.', ',', ';', ')', ']', '!', '?') }
            .filter { !it.endsWith(".apk", ignoreCase = true) || it.contains("/") || it.contains("://") }
            .distinct()
            .take(3)
            .toList()

        for (embeddedUrl in rawCandidateLinks) {
            val urlOutcome = UrlHeuristicAnalyzer.analyze(embeddedUrl)
            if (urlOutcome is UrlHeuristicAnalyzer.ValidationOutcome.Ready) {
                val urlResult = urlOutcome.result
                val nonInfoUrlSignals = urlResult.signals.filter { it.severity != SignalSeverity.INFO }
                if (nonInfoUrlSignals.isNotEmpty()) {
                    val highestSeverity = if (nonInfoUrlSignals.any { it.severity == SignalSeverity.CRITICAL }) {
                        SignalSeverity.CRITICAL
                    } else {
                        SignalSeverity.WARNING
                    }
                    signals.add(
                        DetectedSignal(
                            id = "msg_embedded_suspicious_url_${embeddedUrl.hashCode()}",
                            title = "Contains Suspicious Link ($embeddedUrl)",
                            category = "Embedded Link Analysis",
                            severity = highestSeverity,
                            explanation = "LinkSaathi inspected the link inside this message and found: ${nonInfoUrlSignals.joinToString("; ") { it.title }}.",
                            matchedSnippet = embeddedUrl
                        )
                    )
                } else {
                    signals.add(
                        DetectedSignal(
                            id = "msg_embedded_url_notice_${embeddedUrl.hashCode()}",
                            title = "Contains External Web Link ($embeddedUrl)",
                            category = "Embedded Link Analysis",
                            severity = SignalSeverity.WARNING,
                            explanation = "This message asks you to visit an external link. Even if the link syntax looks normal, avoid clicking links in unexpected SMS/WhatsApp messages—open the official app directly instead.",
                            matchedSnippet = embeddedUrl
                        )
                    )
                }
            }
        }

        return ValidationOutcome.Ready(buildResult(sanitized, signals))
    }

    private fun buildResult(sanitized: String, signals: List<DetectedSignal>): ScanResult {
        val criticalCount = signals.count { it.severity == SignalSeverity.CRITICAL }
        val warningCount = signals.count { it.severity == SignalSeverity.WARNING }

        val riskLevel = when {
            criticalCount >= 1 || warningCount >= 3 -> RiskLevel.HIGH
            warningCount >= 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val summaryTitle = when (riskLevel) {
            RiskLevel.HIGH -> "High Scam Risk — Multiple Fraud Indicators Found"
            RiskLevel.MEDIUM -> "Medium Risk — Caution & Independent Verification Needed"
            RiskLevel.LOW -> "Low Risk — No High-Risk Scam Patterns Triggered"
        }

        val summaryReason = when (riskLevel) {
            RiskLevel.HIGH ->
                "This message exhibits high-risk patterns commonly used in financial, KYC, or impersonation fraud. Review the specific evidence below before taking any action."
            RiskLevel.MEDIUM ->
                "This message contains pressure tactics, lures, or external links that require independent verification. A single keyword does not prove fraud, so verify directly with the official organization."
            RiskLevel.LOW ->
                "Local rule-based checks did not detect active credential solicitation, coercion, or malicious links. However, automated rules cannot guarantee a sender is genuine."
        }

        val recommendations = buildList {
            when (riskLevel) {
                RiskLevel.HIGH -> {
                    add("Do NOT reply to this message, call the phone number mentioned in it, or tap any embedded links.")
                    add("Never share an OTP, UPI PIN, ATM PIN, or password — no genuine bank, utility, or government official will ever ask for them.")
                    if (signals.any { it.id == "msg_unknown_app_install" }) {
                        add("Do NOT download '.apk' files or open AnyDesk/TeamViewer. If you already installed one, turn on Airplane Mode and uninstall it immediately.")
                    }
                    add("Verify your account or bill status by opening the official app on your phone or calling the official helpline printed on your bank card.")
                    add("If you lost money or shared credentials, call India's National Cybercrime Helpline at 1930 or report at cybercrime.gov.in immediately.")
                }
                RiskLevel.MEDIUM -> {
                    add("Do not rely on phone numbers or links inside this message. Log in through the official app or website that you open yourself.")
                    add("Take a breath and ignore artificial deadlines—legitimate institutions provide verifiable notices inside your official account dashboard.")
                    add("Never enter your UPI PIN when someone claims they are sending you a refund or reward.")
                }
                RiskLevel.LOW -> {
                    add("Even when a message appears routine, never share OTPs, UPI PINs, or passwords if someone follows up with a phone call.")
                    add("Understand the limitation: Rule-based analysis looks for known scam patterns and cannot verify the real-world identity of the sender.")
                }
            }
        }

        return ScanResult(
            scanType = ScanType.MESSAGE,
            rawInput = sanitized,
            sanitizedDisplayInput = sanitized,
            redactedPreview = PiiRedactor.createRedactedPreview(sanitized, ScanType.MESSAGE),
            riskLevel = riskLevel,
            summaryTitle = summaryTitle,
            summaryReason = summaryReason,
            signals = signals,
            recommendations = recommendations
        )
    }
}
