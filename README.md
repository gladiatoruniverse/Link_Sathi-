# 🛡️ LinkSaathi — Your Personal Digital Safety Assistant

**LinkSaathi** helps users identify potential scams in suspicious URLs, SMS messages, WhatsApp forwards, and emails. It explains detected warning signs in plain language, recommends safe next steps, and provides educational resources and cybercrime reporting guidance—all **100% on-device** without requiring API keys, paid services, or external servers.

---

## ✨ Key Features

### 1. Home Dashboard
- **LinkSaathi Shield Branding** with Deep Navy & Electric Teal cybersecurity posture.
- Prominent **Check a Link** and **Analyze a Message** action cards.
- Interactive **Digital Safety Tip** carousel with actionable security habits.
- **Recent Scan History** with instant inspection of saved assessments.

### 2. Suspicious URL Checker
Evaluates URLs strictly on-device without ever opening, fetching, or pinging the remote address:
- **Syntax & Protocol Validation**: Detects malformed URLs, missing schemes, unusual server ports (`:8080`), and dangerous executable schemes (`javascript:`, `data:`).
- **Transport Security Context**: Flags unencrypted `http://` links while explicitly reminding users that **HTTPS encryption alone does NOT guarantee a website is legitimate**.
- **Host & Domain Heuristics**:
  - Raw IPv4 / IPv6 addresses used instead of domain names.
  - Public URL shorteners (`bit.ly`, `tinyurl.com`, `cutt.ly`, `rb.gy`, `t.co`, `is.gd`, etc.) hiding the final destination.
  - `@` credential/domain confusion tricks (`https://hdfcbank.com@evil.top`).
  - Punycode (`xn--`) and Unicode homograph lookalike domains.
  - Brand impersonation in subdomains or hyphenated domains (e.g., SBI, HDFC, ICICI, Paytm, PhonePe, UIDAI, IncomeTax, IndiaPost) and character-substitution typosquats (`rnicrosoft`, `g00gle`, `paytrn`).
  - High-abuse TLDs (`.top`, `.xyz`, `.click`, `.buzz`, `.loan`, etc.) and direct executable/app downloads (`.apk`, `.exe`, `.scr`).

### 3. Message Scam Analyzer
Context-aware local rule engine for SMS, WhatsApp, and email text:
- **OTP, UPI PIN & Credential Solicitation**: Flags requests to share OTPs, ATM PINs, CVV, passwords, or to *"enter your UPI PIN to receive money"*—while distinguishing genuine protective reminders (*"Bank will NEVER ask for your OTP"*).
- **Urgency & Disconnection Coercion**: Detects tonight's electricity/utility disconnection threats and artificial countdowns.
- **Prize, Lottery & Refund Lures**: Flags unsolicited lucky draws, cashback claims, and tax/subsidy lures.
- **Embedded Link Inspection**: Automatically extracts URLs inside messages and runs them through the `UrlHeuristicAnalyzer`.
- **Malware & Remote Access Traps**: Flags instructions to install unknown `.apk` files or remote screen-sharing tools (`AnyDesk`, `TeamViewer`, `QuickSupport`, `RustDesk`).
- **Account Suspension & "Digital Arrest" Intimidation**: Flags fake customs parcel seizures, CBI/police arrest threats, and KYC freeze warnings.

### 4. Dedicated Scan Results & Defanged Family Sharing
- Accessible **Low / Medium / High Risk** visual badge (avoids misleading arbitrary percentage scores).
- Plain-language breakdown of every triggered signal and matched snippet.
- **Save to History** with automatic PII/OTP/phone redaction.
- **Share Defanged Safety Summary** (`hxxps://`, `[.]`) via the system share sheet to warn family and friends safely.

### 5. Privacy-Preserving Scan History
- Stored locally using Room SQLite.
- Automatically masks 4–8 digit OTPs/PINs (`[REDACTED-CODE]`), 10-digit mobile numbers (`[REDACTED-PHONE]`), 12–16 digit card/Aadhaar numbers (`[REDACTED-NUMBER]`), and UPI IDs (`[REDACTED-UPI]`).
- Filter by Scan Type or Risk Level, delete individual items, clear all history, or disable history persistence altogether.

### 6. Safety Center & India Cybercrime Reporting
- Expandable guides on Phishing Defense, OTP & UPI PIN Security, Independent Sender Verification, APK Malware Protection, and Golden-Hour Incident Response.
- **Interactive Post-Scam Emergency Recovery Checklist** with persistent progress tracking.
- Direct actions for India's **1930 National Cybercrime Helpline** and **https://cybercrime.gov.in** official portal (with a reminder to verify current official guidance).

---

## 🔒 Privacy, Security & Limitations

1. **100% On-Device Processing**: Scanned URLs and messages never leave your device.
2. **Untrusted Input Sanitization**: All inputs are stripped of HTML/script tags and control characters before display or storage.
3. **Heuristic Limitations**: Local heuristic and rule-based checks identify structural and linguistic warning signs, but **cannot guarantee that a link or sender is safe**. New phishing domains over HTTPS may show zero structural warnings. Always verify sensitive requests through official apps or verified helplines.

---

## 🛠️ Running & Testing the Project

### Build & Compile
```bash
gradle assembleDebug
```

### Run Unit & Robolectric End-to-End Tests
```bash
gradle :app:testDebugUnitTest
```
