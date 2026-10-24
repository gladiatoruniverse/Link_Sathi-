# LinkSaathi — Your Personal Digital Safety Assistant

LinkSaathi is an on-device digital safety and scam-detection companion that helps users inspect suspicious URLs, SMS texts, WhatsApp messages, and emails before interacting with them. It explains detected warning signs in plain language, recommends actionable precautions, and educates users on preventing and reporting cyber fraud—all entirely offline without transmitting personal messages to external servers.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Product & Privacy Decisions**
> All three preferences from our initial review have been locked into the design below, alongside native Android platform execution.

- **Confirmed Decision 1 — Privacy-Preserving History Redaction**: When saving message or URL scans to local history, LinkSaathi automatically masks phone numbers, OTPs, UPI IDs, and numeric codes (`[REDACTED-CODE]`, `[REDACTED-PHONE]`) in stored previews so sensitive data never lingers on disk.
- **Confirmed Decision 2 — Clipboard, Family Sharing & Emergency Checklist**: Input screens include a one-tap **Paste from Clipboard** button, the Scan Results screen generates a **Shareable Safety Summary** via the system share sheet to warn family or friends, and the Safety Center includes an interactive **Post-Scam Emergency Recovery Checklist**.
- **Confirmed Decision 3 — Default Cybersecurity Dark Theme**: LinkSaathi launches in its signature **Deep Navy & Electric Teal** dark theme by default, with an instant toggle for a high-contrast Light theme in Settings and the top header.
- **Platform Architecture Choice — Native Android Application**: Built directly as a native Android application (Kotlin + Jetpack Compose + Room + DataStore) rather than requiring a Capacitor wrapper, delivering native clipboard integration, system share sheets, dialer/browser intents for India's `1930` helpline and `cybercrime.gov.in`, and responsive mobile-to-tablet adaptive layouts.

---

## 1. Overview & Core Concept

- **What It Does**: Provides instant, transparent, rule-based heuristic analysis for suspicious web links and text messages, categorizing risk as **Low**, **Medium**, or **High** while explaining *why* each signal was flagged and what steps the user should take next.
- **Target Audience / Persona**: Everyday smartphone and desktop/tablet users—especially in India—who receive banking alerts, UPI payment requests, delivery links, lottery/reward claims, or KYC suspension warnings over SMS, WhatsApp, and email.
- **Key Value**: Bridges the gap between technical security indicators (Punycode homographs, `@` credential tricks, IP hosts, urgency coercion) and plain-language human understanding, without ever claiming false certainty or silently uploading private communications.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Home Dashboard Flow**: Users see the custom shield-marked LinkSaathi header, a security posture banner, two prominent primary action cards (**Check a Link** and **Analyze a Message**), a rotating **Daily Digital Safety Tip** carousel, and their most recent scan records with one-tap detail inspection.
2. **Suspicious URL Checker Flow**: Users type, paste via one-tap clipboard button, or select a realistic sample URL (e.g., normal HTTPS domain, malformed URL, IP-based phishing URL, Punycode lookalike, or `@`-redirect URL). Tapping **Analyze Link** validates syntax and transitions smoothly to the **Scan Results** screen without ever fetching or opening the target link.
3. **Message Scam Analyzer Flow**: Users paste an SMS/WhatsApp message or pick from realistic test presets (legitimate notification, OTP harvesting request, urgent UPI/KYC suspension threat, APK installation lure, lottery prize scam). Tapping **Analyze Message** evaluates multiple linguistic and structural rule categories and opens **Scan Results**.
4. **Dedicated Scan Results Flow**: Displays an accessible risk badge (**Low / Medium / High** with distinct iconography and semantic color tokens), an explicit heuristic disclaimer, a breakdown of every triggered signal with plain-language explanations, tailored recommended precautions, a **Save to History** action (with automatic PII redaction), a **Share Warning** action for family/friends, and a **Check Another** button.
5. **Scan History Flow**: Lists saved scans with timestamp, scan type badge (URL vs. Message), risk level pill, and redacted content preview. Users can filter by type or risk level, inspect full saved assessment details in a bottom sheet, delete individual records, or clear all history with confirmation.
6. **Safety Center & Emergency Checklist Flow**: Organized into expandable educational guides (Recognizing Phishing, OTP & UPI PIN Safety, Independent Sender Verification, APK & Download Hazards) plus an interactive **Post-Scam Emergency Recovery Checklist** and direct action cards for India's **1930 Cybercrime Helpline** and **cybercrime.gov.in** portal.
7. **Settings & Privacy Control Flow**: Allows toggling between Deep Navy Dark and Crisp Light themes, enabling/disabling scan history persistence (with immediate prompt to purge existing data), one-tap **Clear All History**, and inspecting transparent on-device privacy guarantees and app version details.

### Visual Identity & Theme
- **Aesthetic Direction**: Authoritative, calm, modern cybersecurity utility—designed to reassure anxious users with clarity, structured telemetry cards, and zero visual clutter.
- **Color Palette & Mood**:
  - **Dark Theme (Default)**: Deep Navy canvas (`#070D19`), Elevated Slate-Navy surface cards (`#0F172A` / `#162238`), Electric Teal primary accent (`#14B8A6` / `#2DD4BF`), Crisp Ice-White typography (`#F8FAFC`), Muted Steel secondary text (`#94A3B8`).
  - **Light Theme**: Cool Alabaster background (`#F1F5F9`), Pure White elevated cards (`#FFFFFF`), Deep Cyber Teal primary (`#0D9488`), Deep Navy ink (`#0F172A`).
  - **Semantic Risk Tokens**: High Risk Crimson (`#EF4444` / container `#451A1A`), Medium Risk Amber (`#F59E0B` / container `#45270A`), Low / No-Signal Emerald-Teal (`#10B981` / container `#06392A`).
- **Typography & Hierarchy**:
  - **Display & Section Headers**: *Space Grotesk* (bold, technical geometric precision for risk badges, headers, and telemetry counters).
  - **Body & Explanations**: *Plus Jakarta Sans* (clean, highly legible humanist sans-serif for plain-language explanations and educational guides).
  - **Monospace URL Inspection**: *JetBrains Mono* for inspecting raw URL segments, domains, and highlighted suspicious tokens.
- **Component Styling & Layout**:
  - Custom vector **LinkSaathi Shield Logo** rendered with layered canvas gradients.
  - Adaptive navigation: Mobile bottom `NavigationBar` (Home, Check Link, Analyze Text, History, Safety) with top bar Settings access; switches automatically to a side `NavigationRail` and two-column supporting pane layout on wide/tablet screens.
  - All interactive controls enforce a minimum `48dp` touch target and accessible semantic descriptions.

### Interactive Feedback & Motion
- Smooth animated state transitions between input, analysis verification, and results screens.
- Clear empty states with quick-try sample chips when history or inputs are empty.
- Inline validation banners for empty inputs, malformed URLs, or excessively long payloads (gracefully capped and analyzed safely without UI freezes).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Transparent Rule-Based Heuristic Engine vs. Arbitrary "Fraud Percentage Score"**
  - *Chosen Approach*: Classify scans into **Low**, **Medium**, or **High** risk based on explicit, inspectable signal rules (severity weights + multi-category combinations) and display every triggered rule alongside a clear disclaimer that heuristics cannot guarantee safety.
  - *Why*: Arbitrary numbers like "78% Safe" create dangerous false confidence. Explaining specific indicators (e.g., "Uses an IP address instead of a domain name" or "Combines an urgent account suspension threat with an OTP request") empowers users to verify independently.
  - *Alternatives Considered*: Single numerical gauge or opaque keyword blocking—rejected because legitimate bank messages also contain words like "OTP" (e.g., "Never share your OTP"), which our analyzer distinguishes using context-aware phrasing checks.
- **Decision 2: Strict On-Device Storage with Automatic PII Redaction**
  - *Chosen Approach*: Store scan history locally via Room SQLite and user preferences via DataStore, passing all message/URL previews through a redaction sanitizer before writing to disk, and honoring a master **Enable Scan History** toggle.
  - *Why*: Users often scan messages containing private banking alerts or personal phone numbers. Local-only redacted storage guarantees complete privacy and zero network leakage.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (Jetpack Compose)                          │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Adaptive App Shell (Bottom NavigationBar / Wide-Screen NavRail)      │  │
│  └───┬──────────────┬───────────────┬──────────────┬────────────────┬───┘  │
│      ▼              ▼               ▼              ▼                ▼      │
│ ┌──────────┐  ┌───────────┐  ┌────────────┐  ┌───────────┐  ┌────────────┐ │
│ │   Home   │  │    URL    │  │  Message   │  │   Scan    │  │   Safety   │ │
│ │Dashboard │  │  Checker  │  │  Analyzer  │  │  History  │  │  Center &  │ │
│ │  Screen  │  │  Screen   │  │   Screen   │  │  Screen   │  │  Settings  │ │
│ └────┬─────┘  └─────┬─────┘  └──────┬─────┘  └─────┬─────┘  └─────┬──────┘ │
│      │              └───────┬───────┘              │              │        │
│      │                      ▼                      │              │        │
│      │            ┌───────────────────┐            │              │        │
│      └───────────►│ Scan Results View │◄───────────┘              │        │
│                   │ & Family Share    │                           │        │
│                   └─────────┬─────────┘                           │        │
└─────────────────────────────┼─────────────────────────────────────┼────────┘
                              ▼                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   STATE & DOMAIN LAYER (MVVM ViewModel)                    │
│  ┌──────────────────────────┐  ┌────────────────────────┐  ┌────────────┐  │
│  │   UrlHeuristicAnalyzer   │  │  MessageScamAnalyzer   │  │    PII     │  │
│  │ • Syntax & Scheme Check  │  │ • OTP/PIN Request Rule │  │ Redaction  │  │
│  │ • IP Host & @ Symbol     │  │ • Urgency & Coercion   │  │ Sanitizer  │  │
│  │ • Shortener & Punycode   │  │ • Prize/Refund Lures   │  │ (Phones,   │  │
│  │ • Brand Lookalike/Subdom │  │ • APK/App Install Risk │  │  OTPs,     │  │
│  │ • Length & Obfuscation   │  │ • Embedded URL Scan    │  │  UPI IDs)  │  │
│  └──────────────────────────┘  └────────────────────────┘  └────────────┘  │
└─────────────────────────────┬─────────────────────────────────────┬────────┘
                              ▼                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     LOCAL PERSISTENCE LAYER (On-Device)                    │
│  ┌────────────────────────────────────┐  ┌──────────────────────────────┐  │
│  │ Room Database (scan_history table) │  │ Preferences DataStore        │  │
│  │ • Redacted Preview & Risk Level    │  │ • Dark/Light Theme Mode      │  │
│  │ • Triggered Signals JSON           │  │ • History Storage Enabled    │  │
│  │ • Timestamp & Scan Type            │  │ • Emergency Checklist State  │  │
│  └────────────────────────────────────┘  └──────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State
- **`ScanResult`**: Holds `id`, `scanType` (`URL` or `MESSAGE`), `rawInput` (kept in transient memory only during active view, sanitized for display), `redactedPreview` (persisted when saved), `riskLevel` (`LOW`, `MEDIUM`, `HIGH`), `signals` (list of `DetectedSignal` with `id`, `title`, `severity`, `category`, `plainExplanation`, `matchedSnippet`), `recommendations` (list of actionable steps), `disclaimer`, and `timestamp`.
- **`ScanHistoryEntity` (Room)**: Persists `id`, `timestamp`, `scanType`, `riskLevel`, `redactedPreview`, `signalsSummary`, and `recommendationsSummary`.
- **`UserPreferences` (DataStore)**: Tracks `isDarkTheme` (defaults to `true`), `isHistoryEnabled` (defaults to `true`), and `completedChecklistIds` (set of completed post-scam recovery steps).

### Interactive Component & State Mapping
- **URL Heuristic Engine (`UrlHeuristicAnalyzer`)**:
  - Handles empty, whitespace-only, missing-scheme, or malformed inputs gracefully without throwing exceptions.
  - Detects non-HTTPS (`http://`) schemes while explicitly explaining in results that **HTTPS alone never guarantees a site is legitimate**.
  - Flags raw IPv4/IPv6 hosts, URL shorteners (`bit.ly`, `tinyurl.com`, `t.co`, `cutt.ly`, `rb.gy`, `is.gd`, etc.), `@` credential-confusion characters in the authority segment, Punycode (`xn--`) and mixed-script homographs, deceptive brand subdomains (e.g., `sbi-kyc-update.login-verify.top`), risky TLDs paired with banking keywords, excessive length (`>150` chars) or heavy percent-encoding obfuscation, and dangerous file extensions (`.apk`, `.exe`, `.scr`).
- **Message Scam Engine (`MessageScamAnalyzer`)**:
  - Evaluates 6 distinct rule families:
    1. *Credential & Secret Harvesting* (OTP, UPI PIN, CVV, ATM PIN, netbanking password requests—while distinguishing protective warnings like "Do not share your OTP" from solicitation).
    2. *Urgent Payment & Coercion* ("electricity disconnected tonight", "PAN/KYC blocked in 24 hours", "immediate fine").
    3. *Unexpected Rewards, Lotteries & Refunds* ("won lucky draw", "income tax refund approved—click to claim").
    4. *Suspicious Links & Impersonation* (extracts any embedded URLs and runs them through `UrlHeuristicAnalyzer` automatically).
    5. *Unknown App / Remote Access Installation* (requests to download `.apk` files or screen-sharing apps like AnyDesk/TeamViewer/QuickSupport for "KYC verification").
    6. *Account Suspension & Legal Threats* ("CBI/customs parcel seized", "police case registered", "bank account frozen").
- **History & Privacy Controls**:
  - When `isHistoryEnabled` is `true`, users can tap **Save to History** on any scan result (with visual confirmation state) or auto-view recent saved items.
  - Toggling **Enable Scan History** off in Settings stops new saves and offers a one-tap purge of existing records.
  - **Share Safety Summary** formats a clean, link-defanged (`hxxp://` / `[dot]`) warning text block and launches `Intent.ACTION_SEND` so users can warn family members on WhatsApp or SMS without accidentally making the malicious link clickable.
