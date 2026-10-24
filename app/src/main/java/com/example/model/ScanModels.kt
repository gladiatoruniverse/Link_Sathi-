package com.example.model

import java.util.UUID

enum class AppScreen(val route: String, val title: String) {
    HOME("home", "Dashboard"),
    URL_CHECKER("url_checker", "Check Link"),
    MESSAGE_ANALYZER("message_analyzer", "Analyze Text"),
    SCAN_RESULTS("scan_results", "Scan Assessment"),
    HISTORY("history", "Scan History"),
    SAFETY_CENTER("safety_center", "Safety Center"),
    SETTINGS("settings", "Settings")
}

enum class ScanType(val displayName: String) {
    URL("URL Check"),
    MESSAGE("Message Analysis")
}

enum class RiskLevel(val label: String, val badgeText: String) {
    LOW("Low Risk", "LOW RISK"),
    MEDIUM("Medium Risk", "MEDIUM RISK"),
    HIGH("High Risk", "HIGH RISK")
}

enum class SignalSeverity(val label: String, val weight: Int) {
    INFO("Advisory Notice", 0),
    WARNING("Suspicious Indicator", 1),
    CRITICAL("High-Risk Warning", 2)
}

data class DetectedSignal(
    val id: String,
    val title: String,
    val category: String,
    val severity: SignalSeverity,
    val explanation: String,
    val matchedSnippet: String? = null
)

data class ScanResult(
    val id: String = UUID.randomUUID().toString(),
    val scanType: ScanType,
    val rawInput: String,
    val sanitizedDisplayInput: String,
    val redactedPreview: String,
    val riskLevel: RiskLevel,
    val summaryTitle: String,
    val summaryReason: String,
    val signals: List<DetectedSignal>,
    val recommendations: List<String>,
    val disclaimer: String = DEFAULT_HEURISTIC_DISCLAIMER,
    val timestamp: Long = System.currentTimeMillis(),
    val isSavedToHistory: Boolean = false
) {
    companion object {
        const val DEFAULT_HEURISTIC_DISCLAIMER =
            "On-Device Heuristic Analysis: LinkSaathi evaluates patterns locally on your device without opening links or transmitting your data. An HTTPS connection or a Low Risk result does NOT guarantee a website or sender is trustworthy. Always verify unexpected requests independently."
    }
}

data class SampleInputPreset(
    val id: String,
    val label: String,
    val badge: String,
    val content: String,
    val scanType: ScanType
)

data class SafetyTip(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    val takeaway: String
)

data class SafetyGuideSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val categoryTag: String,
    val keyPoints: List<String>,
    val warningExample: String,
    val safeHabit: String
)

data class EmergencyChecklistItem(
    val id: String,
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val urgencyTag: String
)

object LinkSaathiPresets {

    val urlSamples: List<SampleInputPreset> = listOf(
        SampleInputPreset(
            id = "url_normal_https",
            label = "Normal HTTPS Domain",
            badge = "Standard URL",
            content = "https://www.rbi.org.in/Scripts/NotificationUser.aspx",
            scanType = ScanType.URL
        ),
        SampleInputPreset(
            id = "url_ip_phish",
            label = "IP Address + Non-HTTPS",
            badge = "High Risk Pattern",
            content = "http://192.168.45.112:8080/sbi-kyc-update/login.html",
            scanType = ScanType.URL
        ),
        SampleInputPreset(
            id = "url_at_redirect",
            label = "@ Credential Trick + Shortener",
            badge = "Deceptive Structure",
            content = "https://hdfcbank.com@login-verify-account.top/auth?token=%40%2F9981",
            scanType = ScanType.URL
        ),
        SampleInputPreset(
            id = "url_punycode",
            label = "Punycode Lookalike Domain",
            badge = "Homograph Attack",
            content = "https://xn--pytm-4na.com/reward-claim-cashback",
            scanType = ScanType.URL
        ),
        SampleInputPreset(
            id = "url_shortener",
            label = "Shortened Link",
            badge = "Hidden Destination",
            content = "https://bit.ly/3xK9pLm",
            scanType = ScanType.URL
        ),
        SampleInputPreset(
            id = "url_malformed",
            label = "Malformed URL Syntax",
            badge = "Invalid Syntax",
            content = "htp:///bad..domain//@@?",
            scanType = ScanType.URL
        )
    )

    val messageSamples: List<SampleInputPreset> = listOf(
        SampleInputPreset(
            id = "msg_legit_notice",
            label = "Legitimate Bank Alert",
            badge = "Normal Message",
            content = "Dear Customer, your monthly e-statement for October has been sent to your registered email ID. Note: Bank officials will NEVER ask for your OTP, UPI PIN, or CVV over phone or SMS.",
            scanType = ScanType.MESSAGE
        ),
        SampleInputPreset(
            id = "msg_otp_request",
            label = "OTP & UPI PIN Harvesting",
            badge = "Credential Scam",
            content = "URGENT: Your SBI YONO account has been temporarily locked due to incomplete PAN KYC. Reply immediately with the 6-digit OTP sent to 9876543210 or enter your UPI PIN at http://sbi-pan-verify.xyz to avoid permanent suspension.",
            scanType = ScanType.MESSAGE
        ),
        SampleInputPreset(
            id = "msg_urgent_bill",
            label = "Urgent Electricity Disconnection",
            badge = "Urgency Coercion",
            content = "Dear Consumer, your electricity power connection will be disconnected tonight at 9:30 PM from main office because your previous month bill was not updated. Immediately contact our officer on WhatsApp 8899001122 and pay ₹10 verification fee.",
            scanType = ScanType.MESSAGE
        ),
        SampleInputPreset(
            id = "msg_apk_install",
            label = "Unknown APK + Screen Share",
            badge = "Malware / Remote Risk",
            content = "Congratulations! Your PM-Kisan ₹6,000 installment is approved. Download and install the official verification app PMKisan_Update_v4.apk or install AnyDesk and share the 9-digit code with our agent to credit your bank account.",
            scanType = ScanType.MESSAGE
        ),
        SampleInputPreset(
            id = "msg_customs_threat",
            label = "Customs Parcel / Legal Threat",
            badge = "Impersonation Threat",
            content = "FINAL NOTICE from Customs & Narcotics Cell: A parcel booked under your Aadhaar ID contains illegal items. An arrest warrant is issued. Transfer ₹45,000 security deposit via UPI within 30 minutes to stay digital arrest proceedings.",
            scanType = ScanType.MESSAGE
        )
    )

    val safetyTips: List<SafetyTip> = listOf(
        SafetyTip(
            id = "tip_upi_pin",
            category = "UPI & Payments Safety",
            title = "A UPI PIN is ONLY required to SEND money — never to receive it.",
            body = "Scammers often send 'Collect Requests' disguised as cashback, refunds, or OLX marketplace payments. If an app asks for your 4 or 6-digit UPI PIN, money is leaving your bank account.",
            takeaway = "Decline any payment screen asking for your PIN when you expect to receive funds."
        ),
        SafetyTip(
            id = "tip_https_myth",
            category = "Link Inspection",
            title = "The HTTPS padlock does NOT mean a website is legitimate.",
            body = "HTTPS only means the connection between your phone and the website is encrypted. Phishing websites routinely obtain free SSL certificates so browsers show a padlock icon.",
            takeaway = "Always inspect the actual root domain name (just before the first single slash '/')."
        ),
        SafetyTip(
            id = "tip_apk_danger",
            category = "Device Security",
            title = "Never install '.apk' files sent via WhatsApp, SMS, or Telegram.",
            body = "Fake banking apps, wedding invitations (.apk), and utility bill updaters installed outside the official app store can silently read your incoming SMS OTPs and drain accounts.",
            takeaway = "Keep 'Install Unknown Apps' disabled for browsers and messaging apps."
        ),
        SafetyTip(
            id = "tip_digital_arrest",
            category = "Impersonation Defense",
            title = "Police, CBI, RBI, and Customs NEVER conduct 'Digital Arrests' on video calls.",
            body = "Fraudsters wearing fake uniforms coerce victims into staying on Skype or WhatsApp video calls and transferring money to 'verify' bank accounts.",
            takeaway = "Hang up immediately and verify by dialing 1930 or visiting your local police station."
        ),
        SafetyTip(
            id = "tip_short_urls",
            category = "URL Hygiene",
            title = "Treat shortened links in SMS messages with extreme caution.",
            body = "Legitimate banks and government agencies rarely use public shorteners like bit.ly or tinyurl.com for critical KYC or account verification alerts.",
            takeaway = "Instead of tapping an SMS link, open your official banking app or type the bank's known address yourself."
        )
    )

    val safetyGuideSections: List<SafetyGuideSection> = listOf(
        SafetyGuideSection(
            id = "guide_phishing",
            title = "How to Recognize Phishing Links & Messages",
            subtitle = "Spot deceptive domains, manufactured urgency, and lookalike branding before you tap.",
            categoryTag = "PHISHING DEFENSE",
            keyPoints = listOf(
                "Inspect the true domain: In 'https://sbi.co.in.kyc-update.top/login', the real website owner is 'kyc-update.top', NOT 'sbi.co.in'.",
                "Watch for letter substitutions (homographs): Scammers replace 'm' with 'rn' (rnicrosoft), 'o' with '0', or use Punycode ('xn--') international characters.",
                "Question artificial countdowns: Messages threatening account freeze or power disconnection within 2 hours are designed to make you panic before thinking.",
                "Remember the HTTPS rule: A padlock icon only encrypts traffic; over 80% of modern phishing sites use HTTPS."
            ),
            warningExample = "Dear Customer, your PAN card is delinked. Update within 30 mins at https://hdfc-pan-portal.xyz or account will be blocked.",
            safeHabit = "Never tap links inside unexpected SMS alerts. Open the official app from your home screen or manually type the official domain."
        ),
        SafetyGuideSection(
            id = "guide_otp_upi",
            title = "Why OTPs & UPI PINs Must Never Be Shared",
            subtitle = "Your final cryptographic barrier against unauthorized transactions.",
            categoryTag = "CREDENTIAL SECURITY",
            keyPoints = listOf(
                "OTPs authorize actions: Sharing an OTP over a call—even to 'cancel' a transaction, 'stop' a SIM swap, or 'claim' a reward—authorizes the scammer's login or transfer.",
                "UPI PINs only push money out: Receiving money via UPI requires zero PIN entry. Entering your UPI PIN or scanning a QR code and entering a PIN always deducts money.",
                "Beware of call-forwarding codes: Never dial '*401*<number>' or similar USSD codes at a caller's request; this forwards your calls and voice OTPs to the attacker.",
                "No bank employee, RBI official, or support agent ever needs your OTP, ATM PIN, CVV, or UPI PIN."
            ),
            warningExample = "Sir, I am calling from Bank Fraud Dept. To block the unauthorized ₹18,000 debit, please tell me the 6-digit cancellation OTP just sent to your phone.",
            safeHabit = "Read the full SMS text accompanying every OTP—it clearly states the transaction amount or login action being authorized."
        ),
        SafetyGuideSection(
            id = "guide_verify_sender",
            title = "How to Verify a Sender Independently",
            subtitle = "Break the scammer's loop by using trusted, out-of-band communication channels.",
            categoryTag = "VERIFICATION PROTOCOL",
            keyPoints = listOf(
                "Never call the phone number printed inside a suspicious SMS or email—that number connects directly to the scam call center.",
                "Do not trust caller ID names blindly, and never search Google Maps or random blogs for 'customer care numbers', as scammers buy ads with fake helpline numbers.",
                "Look up the official helpline on the back of your physical debit/credit card or inside the official mobile banking app.",
                "If a friend or relative messages urgently asking for money on WhatsApp, call them on a regular voice call to confirm their account wasn't hijacked."
            ),
            warningExample = "Your courier parcel is held at warehouse. Call Customer Support at +91-7000XXXXXX immediately to reschedule delivery.",
            safeHabit = "Check order status directly inside the shopping app where you placed the order, never via third-party SMS links."
        ),
        SafetyGuideSection(
            id = "guide_apk_downloads",
            title = "Avoiding Unknown APK Files & Remote Access Apps",
            subtitle = "Prevent silent SMS interception and full device takeover.",
            categoryTag = "MALWARE PROTECTION",
            keyPoints = listOf(
                "APK files ('.apk') are raw Android app installers. Scammers send files named 'eChallan.apk', 'Electricity_Bill.apk', 'Wedding_Card.apk', or 'Bank_KYC.apk' on WhatsApp.",
                "Once installed and granted SMS permission, malicious APKs silently forward all your banking OTPs to the attacker in the background.",
                "Never install screen-sharing tools like AnyDesk, TeamViewer, QuickSupport, or RustDesk when asked by someone claiming to be bank, courier, or refund support.",
                "Also beware of WhatsApp screen-sharing during calls with unknown callers—they can read OTP notifications popping up on your screen."
            ),
            warningExample = "Download 'RTO_Traffic_Challan_View.apk' attached below to view your vehicle photo and pay fine online.",
            safeHabit = "Only install apps from the official Google Play Store and verify that Google Play Protect is enabled in your Play Store settings."
        ),
        SafetyGuideSection(
            id = "guide_after_scam",
            title = "What to Do Immediately After a Suspected Scam",
            subtitle = "The 'Golden Hour' response plan to freeze funds and secure your identity.",
            categoryTag = "INCIDENT RESPONSE",
            keyPoints = listOf(
                "First 15–60 minutes are critical: Reporting financial cyber fraud immediately gives law enforcement and banks the best chance to freeze the money before it is withdrawn.",
                "Disconnect from the device network (enable Airplane Mode) if you installed an unknown APK or screen-sharing app so the attacker loses live control.",
                "Contact your bank's official 24x7 fraud hotline to block UPI, netbanking, and debit/credit cards.",
                "Preserve all evidence: Take screenshots of the SMS/WhatsApp chat, caller phone number, URL, transaction Reference/UTR number, and bank statement—do not delete messages."
            ),
            warningExample = "Realizing you just shared an OTP or installed an unknown APK and seeing an unauthorized debit SMS.",
            safeHabit = "Use the interactive Emergency Recovery Checklist below and immediately report via 1930 or cybercrime.gov.in."
        )
    )

    val emergencyChecklist: List<EmergencyChecklistItem> = listOf(
        EmergencyChecklistItem(
            id = "step_disconnect",
            stepNumber = 1,
            title = "Sever Remote Access & Malicious Apps",
            instruction = "If you installed an unknown .apk file or screen-sharing app (AnyDesk, QuickSupport, etc.), turn on Airplane Mode immediately, then uninstall the suspicious app from Settings > Apps.",
            urgencyTag = "IMMEDIATE (MINUTE 1)"
        ),
        EmergencyChecklistItem(
            id = "step_freeze_bank",
            stepNumber = 2,
            title = "Freeze Bank Accounts, Cards & UPI ID",
            instruction = "Call the official number on the back of your bank card to block your Debit/Credit cards, freeze NetBanking, and deactivate UPI temporarily.",
            urgencyTag = "IMMEDIATE (MINUTE 5)"
        ),
        EmergencyChecklistItem(
            id = "step_call_1930",
            stepNumber = 3,
            title = "Dial 1930 — National Cybercrime Helpline (India)",
            instruction = "Call 1930 immediately if money was deducted. Keep your 12-digit UPI UTR / transaction reference number, bank account details, and timestamp ready so authorities can initiate a lien/freeze.",
            urgencyTag = "GOLDEN HOUR"
        ),
        EmergencyChecklistItem(
            id = "step_preserve_evidence",
            stepNumber = 4,
            title = "Save Screenshots, UTR Numbers & Links",
            instruction = "Screenshot the scammer's phone number, WhatsApp profile, SMS message, suspicious URL, and bank debit SMS. Do NOT delete the chat thread.",
            urgencyTag = "EVIDENCE"
        ),
        EmergencyChecklistItem(
            id = "step_file_portal",
            stepNumber = 5,
            title = "File Formal Complaint on cybercrime.gov.in",
            instruction = "Submit a detailed written complaint with your saved screenshots and transaction proof on the National Cyber Crime Reporting Portal (https://cybercrime.gov.in).",
            urgencyTag = "OFFICIAL REPORT"
        ),
        EmergencyChecklistItem(
            id = "step_rotate_passwords",
            stepNumber = 6,
            title = "Change Passwords from a Clean Device",
            instruction = "From a trusted device, update passwords for your primary email, banking portals, and social accounts, and check for unauthorized call-forwarding (dial ##002# to cancel all forwarding).",
            urgencyTag = "ACCOUNT HARDENING"
        )
    )
}
