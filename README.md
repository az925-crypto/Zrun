# Zmusic + Strava

Aplikasi Android **pelacak olahraga mirip Strava** (GPS, jarak, pace, riwayat)
**dengan fitur pemutar musik** — Kotlin + Jetpack Compose. Musik streaming via
NewPipeExtractor (basis app Zmusic). Saat dibuka, app langsung ke layar **Rekam**;
musik ada di tab **Musik** dan mini-player tetap muncul saat tracking.

## Fitur Strava (modul tracking)

- 📍 **Rekam lari + peta** (Google Maps): rute live, jarak, waktu, pace,
  kecepatan. Berjalan sebagai foreground service (lanjut saat layar mati).
- 📜 **Riwayat aktivitas** (Room) + halaman detail dengan peta rute.
- 🎵 **Mini-player** nempel di atas bottom-nav → kontrol musik sambil lari.

### Setup peta
Salin `local.properties.example` → `local.properties`, isi `MAPS_API_KEY`
(aktifkan *Maps SDK for Android* di Google Cloud). Tanpa key, app tetap
build & jalan, hanya petanya blank.

> Kode musik Zmusic (equalizer, lyrics, stats, wrapped, floating player,
> widget) tetap ada di repo, hanya tidak ditaruh di bottom-nav agar app fokus
> ke olahraga. MVP olahraga: tipe **Lari (Run)**.

## Build otomatis (GitHub Actions)

Setiap push ke `main`/`claude/**` memicu workflow **Build APK Zmusic**
(`.github/workflows/build-apk.yml`):

1. Buka tab **Actions** → pilih run terbaru.
2. Tunggu hijau (±10–20 menit), lalu unduh dari bagian **Artifacts**:
   - `zmusic-debug-apk` — langsung bisa di-install untuk dites.
   - `zmusic-release-unsigned-apk` — hasil R8/minify (belum ditandatangani,
     perlu signing dulu kalau mau di-install).

> Build dari HP (AndroidIDE/Termux) tetap jalan — override aapt2 di
> `gradle.properties` dipertahankan dan hanya di-strip saat build CI.

## Isi repo

- `app/`, `gradle/`, `build.gradle.kts`, dst. — project Android lengkap
  - `app/.../tracking/` + `app/.../ui/tracking/` — modul Strava (service GPS,
    layar Rekam/Aktivitas/Detail)
- [`CLAUDE.md`](CLAUDE.md) — panduan codebase Zmusic (untuk AI assistant)
- [`ANALISA-ZMUSIC.md`](ANALISA-ZMUSIC.md) — laporan review Zmusic v2.2.1
