# 🏠 00 — Overview ZRun

> **ZRun = Strava mini 🏃 + Zmusic 🎵.** Rekam lari pakai GPS + peta, dengerin musik YouTube sambil lari, semua offline-ready.

## 🎯 Pitch 30 Detik

| 🏃 Olahraga | 🎵 Musik |
|-------------|----------|
| Rekam lari (GPS foreground service) | Streaming YouTube via NewPipeExtractor |
| Rute live di Google Maps + polyline | Trending, search, playlist, queue |
| Pace pakai *moving time* ala Strava | Mini-player nempel di atas bottom-nav |
| Riwayat Room + detail peta | Download offline + sleep timer + EQ |

Single-activity, 100% Jetpack Compose, design system **Ember** 🔥 (gelap + aksen ember orange).

## 🧩 Tech Stack

| Layer | Pilihan | Versi |
|-------|---------|-------|
| 💬 Bahasa | Kotlin | 2.0.21 |
| 🎨 UI | Compose BOM + Material3 + Navigation | BOM 2024.09.03 / Nav 2.7.7 |
| 💉 DI | Hilt | 2.51 |
| 🗄️ DB | Room + KSP | 2.6.1 |
| 🔊 Playback | Media3 ExoPlayer + Session | 1.4.1 |
| 🌐 Streaming | NewPipeExtractor (JitPack) + OkHttp | v0.26.3 / OkHttp 4.12.0 |
| 🖼️ Image | Coil + Palette | 2.6.0 / 1.0.0 |
| 📍 GPS/Peta | play-services-location + maps + maps-compose | 21.3.0 / 19.0.0 / 4.4.1 |
| ⚙️ Build | AGP + Gradle + JDK | 8.12.0 / 8.13 / 17 |

> 📌 Semua versi dikunci di `gradle/libs.versions.toml` — **jangan hardcode di `build.gradle.kts`**.

## 🗂️ Struktur Repo (ringkas)

```
Zrun/
├── 📖 README.md / LICENSE / NEWPIPE-EXTRACTOR.md
├── 📚 docs/                  ← kamu di sini
├── ⚙️ gradle/libs.versions.toml  ← single source of truth versi
├── 🤖 .github/workflows/build-apk.yml
└── 📱 app/
    ├── src/main/kotlin/com/zaaam/Zmusic/
    │   ├── 💡 ZmusicApp.kt       → init NewPipe + Coil + poToken
    │   ├── 🎨 ui/zrun/           → SEMUA UI baru (ZRunApp, theme, screens/)
    │   ├── 📍 tracking/          → TrackingService + StateHolder
    │   ├── 🧠 ui/tracking|home|search|library|player → ViewModels
    │   ├── 🔊 service/MusicService.kt (962 baris!)
    │   ├── 🎵 data/local/MusicRepository.kt (~780 baris)
    │   ├── 🗄️ data/local/ AppDatabase + 5 DAO
    │   └── 🧰 util/ Queue, Download, EQ, SleepTimer, DailyMix, LocationUtils
    └── src/main/AndroidManifest.xml
```

Total ~50 file Kotlin, single-module (`:app` saja).

## 🖥️ 8 Layar Utama

```
🏠 Dashboard → ▶️ Run → 📜 Feed → 🙋 Profile
🎵 MusicHome → 🔍 Search → 📃 Playlist → ⏯️ Player
```

Bottom-bar: `Home | Musik | [RUN 🔥] | Feed | You` + mini-player 🎵 di atasnya.

Detail tiap layar → [✨ Features](05-features.md) · Alur pindah layar → [🎨 UI & Navigasi](04-ui-ux-navigation.md).

## 🔑 Identitas App

| Item | Nilai |
|------|-------|
| 📦 applicationId (di HP/Play) | `com.zaaam.zrun` |
| 📁 namespace kode | `com.zaaam.Zmusic` (sengaja beda!) |
| 🔢 version | `1.0.0` / `100` |
| 📱 minSdk / target / compile | 26 / 35 / 35 |
| 🗝️ Maps key | `local.properties` → `manifestPlaceholders` |

## ⏭️ Mau ke mana?

- Baru clone? → [🚀 Quickstart](02-quickstart.md)
- Mau tambah fitur? → [🏗️ Architecture](03-architecture.md)
- Peta blank / musik error? → [🆘 Troubleshooting](11-troubleshooting.md)
