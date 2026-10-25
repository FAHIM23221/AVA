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

| How the ZIP arrives | What happens |
|---|---|
| You push / upload a `.zip` into the repository | It is extracted next to itself (`app.zip` → `app/`) and the extracted files are committed back to the branch |
| You attach a `.zip` to a GitHub Release | It is extracted into `releases/<tag>/<name>/` and re-attached to the release as `<name>-extracted.zip` |
| Actions → **Auto Extract ZIP** → *Run workflow* | Runs manually; you can also paste a direct `.zip` URL |

**Where to find the result**
- **Repository** — the extracted folder is committed to the branch
- **Actions → run summary** — lists every extracted folder
- **Actions → Artifacts** — `extracted-files-<run number>` (kept 30 days)

**Good to know**
- Archives containing `../` or absolute paths are rejected (safety guard)
- The auto-commit ends with `[skip actions]`, so it never causes a rebuild loop
- The original `.zip` stays in the repository
- Files: `.github/workflows/auto-extract-zip.yml` and `.github/scripts/extract-zips.sh`

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
