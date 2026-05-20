# 🥙 Shawarma Billing Pro — Android APK

## APK Kaise Banayein? (Step by Step)

### Method 1: Android Studio se (Sabse Aasaan)

**Step 1 — Android Studio Download Karein**
- https://developer.android.com/studio download karein
- Install karein (free hai)

**Step 2 — Project Open Karein**
- Android Studio open karein
- "Open" ya "Open an Existing Project" click karein
- Is `ShawarmaAPK` folder ko select karein
- Wait karein jab tak Gradle sync ho jaye (2-5 minutes)

**Step 3 — APK Build Karein**
- Top menu: `Build → Build Bundle(s) / APK(s) → Build APK(s)`
- Ya shortcut: Ctrl+F9 (Windows/Linux) / Cmd+F9 (Mac)
- Wait for "Build Successful" message

**Step 4 — APK Milega Yahaan:**
```
ShawarmaAPK/app/build/outputs/apk/debug/app-debug.apk
```

**Step 5 — Android Phone mein Install Karein**
- Phone ko USB se connect karein
- Ya APK file phone mein transfer karein aur open karein
- "Unknown Sources" allow karein (Settings → Security)

---

### Method 2: Command Line se

```bash
cd ShawarmaAPK
./gradlew assembleDebug
```

APK milega: `app/build/outputs/apk/debug/app-debug.apk`

---

### Release APK (Google Play ke liye)

```bash
./gradlew assembleRelease
```

---

## App Features

| Feature | Description |
|---------|-------------|
| 📝 Order Input | Messy text paste karo, auto-parse hoga |
| 🧾 Bill | Item-wise bill with totals |
| 📊 History | Aaj + all time orders |
| 🍽️ Menu | 49 standard + custom items |
| 💾 Save | Orders locally save hote hain |
| 🖨️ Print | Direct print support |
| 📤 Share | WhatsApp/any app mein share |
| 🔍 Fuzzy Match | Typos bhi kaam karte hain |

## Requirements

- Android 7.0+ (API 24+)
- ~8 MB size
- Internet permission: NOT required (completely offline)

## Project Structure

```
ShawarmaAPK/
├── app/
│   ├── src/main/
│   │   ├── java/com/shawarma/billing/
│   │   │   └── MainActivity.kt       ← Sab kuch yahaan hai
│   │   ├── res/
│   │   │   ├── layout/               ← UI layouts
│   │   │   ├── values/               ← Colors, strings, themes
│   │   │   ├── drawable/             ← Icons, backgrounds
│   │   │   └── menu/                 ← Bottom nav menu
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```
