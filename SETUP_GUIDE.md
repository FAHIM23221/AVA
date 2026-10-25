# AVA — AI Companion
## Complete Setup Guide
### Developed by Fahim Khan

---

## ⚡ QUICK START

### 1. Open in Android Studio
1. Extract the ZIP
2. Open Android Studio → **File → Open** → Select the `AVA` folder
3. Wait for Gradle sync to complete (first sync may take 3–5 minutes)

---

## 🔑 API KEY SETUP (Required before building)

Open `app/build.gradle` and set your API keys:

```groovy
buildConfigField "String", "GEMINI_API_KEY", '"YOUR_ACTUAL_GEMINI_KEY"'
buildConfigField "String", "ELEVENLABS_API_KEY", '"YOUR_ELEVENLABS_KEY"'
buildConfigField "String", "ELEVENLABS_VOICE_ID", '"YOUR_VOICE_ID"'
```

### Get Gemini API Key
1. Visit https://aistudio.google.com/app/apikey
2. Create a new API key
3. Paste into `GEMINI_API_KEY`

### Get ElevenLabs API Key (Optional — for premium voice)
1. Visit https://elevenlabs.io → Sign up
2. Go to Profile → API Key
3. Pick or clone a voice → copy Voice ID
4. Paste both into the config above

> **Without ElevenLabs**: AVA uses Android's built-in TTS — still works great!

---

## 🔐 ADMIN PANEL

**Access:** Tap the top header bar **5 times rapidly**

**Default password:** `ava2024`

Change it in AdminRepository.kt or via the admin panel UI.

---

## 📱 REQUIRED PERMISSIONS

Grant these when prompted:
- ✅ Microphone — for voice input
- ✅ Camera — for Vision mode  
- ✅ Notifications — for always-on status
- ✅ Overlay (optional) — for floating AVA
- ✅ Accessibility (optional) — for screen understanding

---

## 🏗️ PROJECT ARCHITECTURE

```
AVA/
├── app/src/main/java/com/fahimkhan/ava/
│   ├── AvaApplication.kt          ← App class (Hilt root)
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── MainActivity.kt    ← Navigation host
│   │   │   ├── SplashScreen.kt    ← Cinematic splash
│   │   │   ├── MainScreen.kt      ← Primary AI interface
│   │   │   ├── AdminScreen.kt     ← Hidden control center
│   │   │   ├── MemoryScreen.kt    ← Memory browser
│   │   │   ├── VisionScreen.kt    ← Camera + face register
│   │   │   ├── SecurityScreen.kt  ← Security logs & control
│   │   │   └── SettingsScreen.kt  ← Configuration
│   │   ├── components/
│   │   │   ├── AvaOrbComponent.kt ← Living AI orb
│   │   │   └── AvaComponents.kt   ← NeonText, CyberCard, etc.
│   │   └── theme/
│   │       └── AvaTheme.kt        ← Full cyberpunk theme
│   ├── ai/
│   │   ├── brain/
│   │   │   └── AiBrainRepository.kt   ← Gemini AI core
│   │   ├── personality/
│   │   │   └── PersonalityEngine.kt   ← Boss/GF/Pro modes
│   │   └── memory/
│   │       └── MemorySystem.kt        ← Room DB + all DAOs
│   ├── voice/
│   │   ├── engine/
│   │   │   └── VoiceEngine.kt         ← TTS + STT
│   │   └── wake/
│   │       └── WakeWordDetector.kt    ← "AVA" wake word
│   ├── security/
│   │   └── owner/
│   │       └── OwnerSecuritySystem.kt ← Trust & access control
│   ├── services/
│   │   ├── AvaForegroundService.kt    ← Always-on service
│   │   └── AvaServices.kt             ← Accessibility, Notif, Boot
│   ├── admin/
│   │   └── AdminRepository.kt         ← Admin data layer
│   ├── animations/
│   │   └── ParticleSystem.kt          ← Canvas particle engine
│   └── core/
│       ├── di/
│       │   └── AppModule.kt           ← Hilt DI wiring
│       ├── utils/
│       │   └── DeviceControlSystem.kt ← WiFi/BT/Volume/Torch
│       └── extensions/
│           └── AvaExtensions.kt       ← Kotlin utils
└── res/
    ├── values/
    │   ├── colors.xml
    │   ├── strings.xml
    │   └── themes.xml
    ├── drawable/
    │   ├── ic_ava_splash.xml
    │   └── ic_launcher_foreground.xml
    └── xml/
        ├── accessibility_service_config.xml
        ├── backup_rules.xml
        └── data_extraction_rules.xml
```

---

## 🧠 AVA PERSONALITY SYSTEM

| Situation | Call Style |
|-----------|------------|
| 75% of time | "Boss" |
| 20% of time | User's saved name |
| 5% of time | Emotional nickname (GF mode only) |

**Switch modes** in Settings or Admin Panel:
- **Assistant Mode** — Warm, helpful companion
- **GF Mode** — Intimate, emotionally expressive
- **Professional Mode** — Formal, precise, efficient

---

## 🗣️ IDENTITY RESPONSES

When asked "Who created you?" / "Who made you?" / "তোমাকে কে বানিয়েছে?":

> *"I was developed by Fahim Khan."*

This is hardcoded in `AiBrainRepository.kt` as offline fallback AND injected into every Gemini prompt.

---

## 🔒 SECURITY SYSTEM

| Trust Level | Access |
|-------------|--------|
| OWNER (Level 2) | Full access to all features |
| TRUSTED (Level 1) | Basic features, no admin |
| UNKNOWN (Level 0) | Restricted — informational only |
| INTRUDER | Denied + owner alert |

**Register owner face** → Vision screen → "SET AS OWNER"

**Grant temporary trust** → Security screen → "GRANT TEMP TRUST"

---

## 🌐 LANGUAGE SUPPORT

| Language | Status |
|----------|--------|
| English | ✅ Full support |
| Bengali (বাংলা) | ✅ Voice + Gemini |

AVA auto-detects language from user input and responds accordingly.

---

## 📋 FEATURE CHECKLIST

| Feature | Status |
|---------|--------|
| Animated AI Orb | ✅ Complete |
| Cyberpunk theme | ✅ Complete |
| Splash screen | ✅ Complete |
| Voice recognition | ✅ Complete |
| Wake word "AVA" | ✅ Complete |
| Gemini AI integration | ✅ Complete |
| ElevenLabs voice | ✅ Structure ready |
| Android TTS fallback | ✅ Complete |
| Memory system (Room DB) | ✅ Complete |
| Admin panel | ✅ Complete |
| Security system | ✅ Complete |
| Vision/Camera | ✅ Complete |
| Face registration | ✅ Complete |
| Always-on service | ✅ Complete |
| Accessibility service | ✅ Structure ready |
| Device control | ✅ Complete |
| Personality engine | ✅ Complete |
| Bengali support | ✅ Complete |
| Particle animations | ✅ Complete |
| Voice wave visualizer | ✅ Complete |
| Hilt DI | ✅ Complete |
| MVVM architecture | ✅ Complete |

---

## 📦 AUTO ZIP EXTRACT (GitHub Actions)

A GitHub Actions workflow unpacks any `.zip` automatically — no manual unzipping needed.

**What happens when a `.zip` arrives**

| | |
|---|---|
| 📂 **যেখানে বসবে** | zip-এর ভেতরের ফাইলগুলো সরাসরি repo root-এ বসে (আলাদা `zip-নাম/` ফোল্ডার থাকে না) |
| ✂️ **একটাই ফোল্ডার থাকলে** | zip-এর ভেতরের একমাত্র ফোল্ডারটা (যেমন `AVA/`) খুলে ফেলা হয় — ফাইলগুলো flat হয়ে root-এ বসে |
| 🔄 **একই নামের ফাইল** | নতুন zip-এর ভার্সন দিয়ে replace হয় |
| 🗑️ **পুরোনো ফাইল** | আগের extraction-এ আসা কিন্তু নতুন zip-এ নেই এমন ফাইল নিজে থেকেই মুছে যায় (খালি ফোল্ডারও সরে যায়) |
| 📦 **zip ফাইলটি** | extract সফল হওয়ার পর নিজে থেকেই মুছে যায় |

**কখন কী ঘটে**

| ZIP কোথায় আসে | ফলাফল |
|---|---|
| রিপোজিটরিতে push / upload | repo root-এ extract হয়, ফাইলগুলো ব্রাঞ্চে commit হয়, zip মুছে যায় |
| Release-এ attach | `releases/<tag>/<name>/` এ extract হয় এবং `<name>-extracted.zip` হিসেবে release-এ আবার attach হয় |
| Actions → **Auto Extract ZIP** → *Run workflow* | ম্যানুয়ালি চালানো যায় (ঐচ্ছিক: সরাসরি `.zip` URL, আর zip রেখে দেওয়ার অপশন) |

**ফলাফল কোথায় পাবেন**
- **Repository** — extract করা ফাইলগুলো ব্রাঞ্চে commit হয়ে যায়
- **Actions → run summary** — কোন ফাইল added / modified / deleted হলো তার তালিকা
- **Actions → Artifacts** — `extracted-files-<run number>` (kept 30 days)

**সুরক্ষা**
- `../` বা absolute path থাকা archive বাদ দেওয়া হয় (zip-slip guard)
- repo-র নিজস্ব ফাইলগুলো কখনো মুছে ফেলা হয় না
- অটোমেটেশনের নিজস্ব ফাইল (`auto-extract-zip.yml`, `extract-zips.sh`) সুরক্ষিত
- auto-commit-এর মেসেজে `[skip actions]` থাকে, তাই কোনো rebuild লুপ হয় না
- ফাইল: `.github/workflows/auto-extract-zip.yml`, `.github/scripts/extract-zips.sh`
- ব্যবহারের হিসাব রাখা হয় `.github/auto-extract-manifest.tsv` ও `.github/auto-extract-baseline.txt`-এ

---

## 🚀 BUILD & RUN

```bash
# Clean build
./gradlew clean assembleDebug

# Install on device
./gradlew installDebug

# Release APK (requires keystore)
./gradlew assembleRelease
```

---

## ⚠️ TROUBLESHOOTING

**Gradle sync fails?**
- Ensure internet connection for dependency download
- Check `compileSdk = 34` in app/build.gradle

**Voice not working?**
- Grant microphone permission
- Ensure Google app is installed (for SpeechRecognizer)

**Gemini not responding?**
- Verify API key in build.gradle
- Check internet connection

**Admin panel not opening?**
- Tap top bar **exactly 5 times** within 3 seconds each tap

---

*AVA — AI Companion | Developed by Fahim Khan*
