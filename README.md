# 🔥 ZRun — Lari + Musik dalam Satu App

> Aplikasi Android **pelacak lari ala Strava 🏃** (GPS, jarak, pace, riwayat) **+ pemutar musik 🎵** — Kotlin + Jetpack Compose, design system **Ember**.

![Android](https://img.shields.io/badge/Android-minSdk26%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-BOM_2024.09-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-v8-orange)
![CI](https://img.shields.io/badge/CI-Actions-success?logo=githubactions)

Musik streaming via **NewPipeExtractor** (mesin dari proyek Zmusic; UI + fitur ditulis ulang khusus ZRun).

## ✨ Fitur

| 🏃 Strava | 🎵 Musik |
|-----------|----------|
| 📍 Rekam lari + peta live (Google Maps, foreground service) | 🔥 Trending, search (~50 lagu), playlist, queue |
| 📊 Jarak, durasi, pace (*moving time* ala Strava) | 🎛️ Mini-player nempel di atas bottom-nav |
| 📜 Riwayat Room + detail peta rute | ⬇️ Download offline, 😴 sleep timer, 🎚️ EQ |
| ▶️ Start / Pause / Resume / Stop + Save dialog | 🔀 Smart shuffle, 😌 MoodMix, 📤 export/import JSON |

Bottom-nav: `🏠 Home | 🎵 Musik | [🔥 RUN] | 📜 Feed | 🙋 You`

## 🚀 Mulai Cepat

```bash
cp local.properties.example local.properties  # isi MAPS_API_KEY
./gradlew assembleDebug
./gradlew installDebug
```

> 🗺️ Tanpa key app tetap build & jalan, cuma peta blank.
> 📖 Panduan 10 menit → [`docs/02-quickstart.md`](docs/02-quickstart.md)

### 🗝️ Setup Peta

Salin `local.properties.example` → `local.properties`, isi `MAPS_API_KEY` (aktifkan *Maps SDK for Android* di Google Cloud, batasi ke `com.zaaam.zrun` + `com.zaaam.Zmusic` + SHA-1 `app/debug.keystore`).

## 📚 Dokumentasi Developer

> 💡 Semua docs dipecah per topik di [`docs/`](docs/README.md) biar gampang dibaca & dikembangkan:

| Dok | Isi |
|-----|-----|
| 🏠 [`docs/01-overview.md`](docs/01-overview.md) | Apa itu ZRun + tech stack + struktur repo |
| 🚀 [`docs/02-quickstart.md`](docs/02-quickstart.md) | Clone → build → install 10 menit |
| 🏗️ [`docs/03-architecture.md`](docs/03-architecture.md) | MVVM + Hilt + aliran GPS & musik |
| 🎨 [`docs/04-ui-ux-navigation.md`](docs/04-ui-ux-navigation.md) | Routes, bottom-bar, Ember DS |
| ✨ [`docs/05-features.md`](docs/05-features.md) | Tour 8 layar |
| 📍 [`docs/06-tracking-gps.md`](docs/06-tracking-gps.md) | Pipeline akurasi GPS |
| 🎵 [`docs/07-music-engine.md`](docs/07-music-engine.md) | NewPipe + ExoPlayer (area rapuh!) |
| 🗄️ [`docs/08-database.md`](docs/08-database.md) | Room v8 + migrasi wajib |
| 📦 [`docs/09-build-release-ci.md`](docs/09-build-release-ci.md) | Signing, versioning, Actions |
| 🤝 [`docs/10-contributing.md`](docs/10-contributing.md) | Aturan PR |
| 🆘 [`docs/11-troubleshooting.md`](docs/11-troubleshooting.md) | Peta blank, musik gagal, GPS ngaco |

Referensi mesin: [`CLAUDE.md`](CLAUDE.md) (aturan keras AI) · [`NEWPIPE-EXTRACTOR.md`](NEWPIPE-EXTRACTOR.md) (SABR/poToken).

## 🤖 Build Otomatis (GitHub Actions)

Setiap push ke `main` / `claude/**` memicu **Build APK ZRun** (`.github/workflows/build-apk.yml`):

1. Buka tab **Actions** → pilih run terbaru.
2. Tunggu 🟢 (±10–20 mnt) → unduh **Artifacts** → `zrun-debug-apk` → install.

> 📱 Build dari HP (Termux) tetap jalan — override `aapt2` di `gradle.properties` dipertahankan, CI yang strip otomatis.

## 🗂️ Isi Repo

- `app/.../ui/zrun/` — 🎨 seluruh UI ZRun (theme Ember + screens)
- `app/.../tracking/` — 📍 mesin GPS (service + state)
- `app/.../service/MusicService.kt` — 🔊 inti playback (hati-hati, rapuh)
- `app/.../data/local/MusicRepository.kt` — 🕵️ search & stream YouTube
- `gradle/libs.versions.toml` — 📌 single source of truth versi (jangan hardcode!)

> 🧹 Sisa Zmusic yang tidak dipakai sudah dihapus (equalizer UI, lyrics UI, stats, wrapped, floating player, widget, Firebase). MVP olahraga: **Lari (Run)**.

## ⚠️ Aturan Singkat

- 📦 `applicationId com.zaaam.zrun` ≠ 📁 package `com.zaaam.Zmusic` — jangan rename massal.
- 🔢 Versi `1.0.0/100` (`X*100+Y*10+Z`) — wajib naik tiap rilis.
- 🗣️ Bahasa UI: Indonesia kasual.
