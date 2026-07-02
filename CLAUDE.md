# CLAUDE.md — ZRun

Panduan untuk AI assistant (dan diri sendiri) yang bekerja di codebase ini.

## Apa Ini

**ZRun** — app Android pelacak lari ala Strava **dengan fitur musik** (streaming
YouTube via NewPipeExtractor, mesin diambil dari proyek Zmusic). UI 100% baru
(design system "Ember"). Verifikasi build via CI GitHub Actions (`build-apk.yml`,
artifact `zrun-debug-apk`).

- applicationId: `com.zaaam.zrun` · package kode internal: `com.zaaam.Zmusic` (sengaja, JANGAN di-rename massal)
- versionName `1.0.0` / versionCode `100` — **WAJIB naik tiap rilis** (konvensi X.Y.Z → X*100+Y*10+Z)
- minSdk 26 · target/compileSdk 35 · Kotlin 2.0.21 · AGP 8.12 · Hilt 2.51 · Compose BOM 2024.09

## Peta Arah

```
ZmusicApp.kt            → Application: init NewPipe + Coil singleton + poToken
ui/zrun/                → SEMUA UI (theme ZR, komponen, ZRunApp shell, screens/)
tracking/               → TrackingService (FGS location) + TrackingStateHolder
ui/tracking/            → Record/Activities/ActivityDetail ViewModel (UI-nya di ui/zrun)
service/MusicService.kt → inti playback (ExoPlayer/MediaSession) — RAPUH, hati-hati
data/local/MusicRepository.kt → search & getStreamUrl YouTube (package .data, sengaja)
data/potoken/ + ZmusicPoTokenProvider → mesin poToken (standby)
ui/{home,search,library,player}/ *ViewModel.kt → dipakai UI zrun (screen lama sudah dihapus)
util/ QueueManager, AudioDownloadManager, EqualizerManager, SleepTimerManager,
      DailyMixGenerator, LocationUtils, AudioSessionHolder, Extensions
```

## Aturan Penting

1. **PLAYBACK RAPUH** — baca `NEWPIPE-EXTRACTOR.md` sebelum utak-atik streaming.
   Jangan hapus diagnostik `totalAudio/videoStreams` di `getStreamUrl()`. Jangan
   pakai `catch (_: Exception)` yang menelan error tanpa `Log.e`.
2. **Room** — `AppDatabase` v8. Tabel musik (songs/playlists/play_history/search_history)
   + `activities` (tracking). Tiap ubah skema: naikkan versi + tulis Migration + daftar di `di/AppModule.kt`.
3. **Pipeline akurasi GPS** (`TrackingService`): filter akurasi 15m → Doppler speed
   (fallback implied) → filter glitch >54 km/j (anchor SELALU maju!) → ambang diam
   2.5 km/j → smoothing 5 sampel. Pace = movingMillis (waktu bergerak), bukan durasi total.
4. **Maps API key** dari `local.properties`/`gradle.properties` (`MAPS_API_KEY`)
   → manifestPlaceholder. Debug keystore tetap (`app/debug.keystore`) agar SHA-1 konsisten.
5. **Bahasa UI: Indonesia kasual.** Versi dependensi hanya di `gradle/libs.versions.toml`.
6. Verifikasi: push ke `claude/**` → CI build → unduh artifact. Tes GPS tanpa keluar
   rumah: Lockito (mock location), kecepatan ~11 km/j.
