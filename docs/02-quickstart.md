# 🚀 01 — Quickstart (10 Menit ke APK Jalan)

> Dari clone kosong → app kepasang di HP. Checklist style biar nggak nyasar.

## ✅ Prasyarat

- [ ] ☕ **JDK 17** (Temurin di CI, apa aja lokal asal 17)
- [ ] 📦 **Android SDK** API 35 + build-tools + platform-tools (`adb`)
- [ ] 📱 HP / emulator **dengan Play Services** (Maps + Location butuh ini)
- [ ] 🗝️ **Google Cloud project** dengan *Maps SDK for Android* enabled (untuk peta, bisa menyusul)

Cek cepat:

```bash
java -version      # harus 17.x
adb --version
```

## 📥 1. Clone & Buka

```bash
git clone https://github.com/az925-crypto/Strava-app
cd Strava-app
```

Buka di Android Studio (atau code editor + terminal).

## 🗝️ 2. Pasang Maps Key (2 menit)

```bash
cp local.properties.example local.properties
```

Isi `local.properties`:

```properties
sdk.dir=/path/ke/Android/Sdk
MAPS_API_KEY=AIzaSy...kamu
```

Cara dapat key:
1. 🌐 [Google Cloud Console](https://console.cloud.google.com/) → project → *APIs & Services* → enable **Maps SDK for Android**.
2. *Credentials* → Create API key → batasi:
   - 📦 Package: `com.zaaam.zrun` **dan** `com.zaaam.Zmusic`
   - 🔑 SHA-1: dari `app/debug.keystore` (password `android`, alias `androiddebugkey`):
     ```bash
     keytool -list -v -keystore app/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
     ```
3. Tempel ke `local.properties`.

> 💡 **Tanpa key app tetap build & jalan, cuma peta blank.** Jadi bisa coding dulu, key menyusul. Prioritas baca key: `local.properties` → `gradle.properties` → env `MAPS_API_KEY` → `""`.

## 🔨 3. Build & Install (3 menit)

```bash
./gradlew assembleDebug
./gradlew installDebug
# APK ada di: app/build/outputs/apk/debug/app-debug.apk
```

HP Termux? Tetap jalan — `android.aapt2FromMavenOverride=/usr/bin/aapt2` di `gradle.properties` sengaja dipertahankan (CI yang strip otomatis via `sed`).

## 🧪 4. Verifikasi Cepat

```bash
# Log musik + GPS
adb logcat -c; adb logcat -d | grep -iE "ZmusicService|totalAudio|FALLBACK|extractorError|onPlayerError"
```

| Tes | Cara | Harapan |
|-----|------|---------|
| 🏠 Dashboard kebuka | Buka app | Greeting + target 25km + CTA lari |
| 📍 GPS | Tap **RUN** → izinkan lokasi → Start | Titik + polyline gerak |
| 🎵 Musik | Tab Musik → play lagu | Mini-player muncul + bunyi |
| 🗺️ Peta | Run / Dashboard | Peta tampil (kalau key benar) |

Tes GPS tanpa keluar rumah: **Lockito** (mock location) kecepatan ~11 km/j. Verifikasi resmi: push ke `claude/**` → Actions hijau → unduh artifact `zrun-debug-apk`.

## 🩹 Gagal? (90% kasus)

| Gejala | Obat |
|--------|------|
| `SDK location not found` | Isi `sdk.dir` di `local.properties` |
| `JAVA_HOME` / versi salah | Pakai JDK 17 |
| Peta blank abu-abu | Key salah / API belum enabled / SHA-1 belum didaftarkan → [🆘 Troubleshooting](11-troubleshooting.md) |
| `aapt2` error di CI | Jangan hapus manual — CI sudah `sed` sendiri |

⏭️ Selanjutnya: [🏗️ Architecture](03-architecture.md) buat paham aliran data sebelum ngoding.
