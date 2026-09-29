# 📚 ZRun Docs Hub

> Pusat dokumentasi developer **ZRun** — Strava GPS 🏃 + Musik YouTube 🎵 — Kotlin + Jetpack Compose.

![Android](https://img.shields.io/badge/Android-minSdk26%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-BOM_2024.09-4285F4?logo=jetpackcompose&logoColor=white)
![Room v8](https://img.shields.io/badge/Room-v8-orange)
![CI](https://img.shields.io/badge/CI-Actions_hijau-success?logo=githubactions)

## 🗺️ Peta Dokumen

| # | Dokumen | Isi | Untuk siapa |
|---|---------|-----|--------------|
| 🏠 | [00 — Overview](01-overview.md) | Apa itu ZRun, fitur 30 detik, screenshot mental, tech stack | Semua orang |
| 🚀 | [01 — Quickstart](02-quickstart.md) | Clone → Maps key → `assembleDebug` → install dalam 10 menit | Dev baru |
| 🏗️ | [02 — Architecture](03-architecture.md) | MVVM + Hilt, peta file, aliran data GPS & musik | Dev fitur |
| 🎨 | [03 — UI / Navigasi / Ember DS](04-ui-ux-navigation.md) | Routes, bottom-bar + RUN FAB, komponen `ZR*` | Dev UI |
| ✨ | [04 — Features / Screens](05-features.md) | 8 screens: Dashboard, Run, Feed, Profile, Musik, Search, Playlist, Player | QA / PM / Dev |
| 📍 | [05 — Tracking GPS Engine](06-tracking-gps.md) | `TrackingService` pipeline, akurasi, pace, hemat baterai | Dev GPS |
| 🎵 | [06 — Music Engine](07-music-engine.md) | NewPipe + ExoPlayer, `getStreamUrl`, queue, offline | Dev musik |
| 🗄️ | [07 — Database (Room v8)](08-database.md) | 6 tabel, DAO, migrasi 1→8, aturan wajib | Dev data |
| 📦 | [08 — Build / Release / CI](09-build-release-ci.md) | Variants, signing, Maps key, Actions, versioning | Release manager |
| 🤝 | [09 — Contributing](10-contributing.md) | Branch, commit, checklist PR, aturan playback & Room | Kontributor |
| 🆘 | [10 — Troubleshooting & FAQ](11-troubleshooting.md) | Peta blank, musik gagal, GPS ngaco, build gagal | Semua (hemat waktu) |

## ⚡ Mulai 30 Detik

```bash
cp local.properties.example local.properties  # isi MAPS_API_KEY
./gradlew assembleDebug
./gradlew installDebug
```

> Tanpa `MAPS_API_KEY` app tetap jalan, cuma peta blank. Detail → [🚀 Quickstart](02-quickstart.md).

## 🧭 Konvensi Penting (jangan dilanggar)

- 📦 `applicationId = com.zaaam.zrun` ≠ 📁 package kode `com.zaaam.Zmusic` — **jangan rename massal**.
- 🔢 Versi: `1.0.0` → `100` (`X*100+Y*10+Z`) — **wajib naik tiap rilis**.
- 🎵 Playback rapuh — baca [🎵 Music Engine](07-music-engine.md) dulu, jangan hapus log diagnostik.
- 🗄️ Ubah Room → naik versi + Migration + daftar di `di/AppModule.kt`.
- 🗣️ Bahasa UI: **Indonesia kasual**.
- 📌 Versi dependency hanya di `gradle/libs.versions.toml`.

## 🔗 Referensi Luar

- [README root](../README.md) — landing ringkas
- [`CLAUDE.md`](../CLAUDE.md) — panduan AI (aturan keras)
- [`NEWPIPE-EXTRACTOR.md`](../NEWPIPE-EXTRACTOR.md) — referensi SABR/poToken 382 baris

---
💡 **Tips:** pakai `Ctrl+K` di GitHub VSCode web untuk loncat antar dokumen ini.
