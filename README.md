# ZRun

Aplikasi Android **pelacak lari ala Strava** (GPS, jarak, pace, riwayat)
**dengan fitur pemutar musik** — Kotlin + Jetpack Compose, UI design system
"Ember". Musik streaming via NewPipeExtractor (mesin dari proyek Zmusic; UI
dan fitur ditulis ulang khusus ZRun).

## Fitur Strava (modul tracking)

- 📍 **Rekam lari + peta** (Google Maps): rute live, jarak, waktu, pace,
  kecepatan. Berjalan sebagai foreground service (lanjut saat layar mati).
- 📜 **Riwayat aktivitas** (Room) + halaman detail dengan peta rute.
- 🎵 **Mini-player** nempel di atas bottom-nav → kontrol musik sambil lari.

### Setup peta
Salin `local.properties.example` → `local.properties`, isi `MAPS_API_KEY`
(aktifkan *Maps SDK for Android* di Google Cloud). Tanpa key, app tetap
build & jalan, hanya petanya blank.

> Sisa fitur/UI Zmusic yang tidak dipakai ZRun sudah dihapus dari source
> (equalizer UI, lyrics UI, stats musik, wrapped, floating player, widget,
> Firebase). Yang dipertahankan hanya mesin: streaming NewPipe + ExoPlayer,
> queue/playlist, download, MusicService. MVP olahraga: tipe **Lari (Run)**.

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
  - `app/.../ui/zrun/` — seluruh UI ZRun (theme Ember + screens)
  - `app/.../tracking/` — mesin GPS (service + state)
- [`CLAUDE.md`](CLAUDE.md) — panduan codebase (untuk AI assistant)
- [`NEWPIPE-EXTRACTOR.md`](NEWPIPE-EXTRACTOR.md) — referensi mesin streaming
