# CLAUDE.md — Zmusic

Panduan untuk AI assistant (dan diri sendiri di masa depan) yang bekerja di codebase ini.
Baca ini dulu sebelum menyentuh kode.

---

## 1. Apa Ini

**Zmusic** — aplikasi pemutar musik Android (streaming dari YouTube via NewPipeExtractor).
Dibangun solo, di-build dari HP (AndroidIDE/Termux). Build itu lambat (5–15 menit per percobaan),
jadi **utamakan perubahan yang aman & terverifikasi** daripada eksperimen besar yang gampang gagal.

- Package: `com.zaaam.Zmusic`
- versionName: `2.2.2`
- minSdk 26 · targetSdk 35 · compileSdk 35

---

## 2. Stack

| Area | Teknologi |
|------|-----------|
| Bahasa | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| DI | Hilt 2.51 |
| Player | Media3 / ExoPlayer 1.4.1 |
| Sumber musik | NewPipeExtractor v0.26.3 (YouTube) — lihat `NEWPIPE-EXTRACTOR.md` |
| DB lokal | Room 2.6.1 |
| HTTP | OkHttp 4.12.0 (satu `@Singleton` di `di/AppModule.kt`) |
| Gambar | Coil 2.x (singleton di-set di `ZmusicApp`) |
| Warna dominan | androidx.palette |
| AGP | 8.12.0 |

> Versi tunggal sumber kebenaran: `gradle/libs.versions.toml`. Jangan hardcode versi berbeda di `app/build.gradle.kts`.

---

## 3. Peta Arah Cepat

```
ZmusicApp.kt            → Application; init NewPipe + Coil singleton + daftar poToken provider
service/
  MusicService.kt       → inti playback (ExoPlayer, MediaSession, notif, wake/wifi lock)
  FloatingPlayerService → mini player melayang
data/
  local/MusicRepository.kt → JANTUNG: search() & getStreamUrl() (ekstraksi YouTube)
  LyricsRepository.kt   → ambil lirik (synced & plain)
  NewPipeDownloader.kt  → downloader OkHttp untuk NewPipe
  ZmusicPoTokenProvider.kt + potoken/ → mesin poToken (NGANGGUR, lihat §5)
util/
  QueueManager.kt       → antrean + shuffle + moveItem (drag-reorder)
  AudioDownloadManager  → unduh offline
  EqualizerManager, SleepTimerManager, DailyMixGenerator, Extensions.kt
ui/
  player/PlayerScreen.kt + PlayerViewModel.kt  → layar terbesar & terpenting
  home, search, library, settings, stats, mood, artist, explore,
  trending, recent, wrapped, equalizer, smartplaylist, about
  components/  → AlbumArt, MiniPlayerBar, SongItem, GradientPlayButton,
                 EqualizerBars, StateDisplay, GlassCard
  theme/  → ZmusicTheme.kt (token), Type.kt (font), Accent.kt (aksen dinamis)
```

> **Catatan jebakan:** `data/local/MusicRepository.kt` punya deklarasi `package com.zaaam.Zmusic.data`
> (bukan `.data.local`). Kotlin tidak memaksa folder = package, jadi ini sengaja. Jangan "perbaiki".

---

## 4. ⚠️ PLAYBACK — BACA INI SEBELUM UTAK-ATIK STREAMING

Ini bagian paling rapuh & paling sering rusak. Sejarahnya: app **bisu total ~2–3 bulan**
karena perubahan sisi YouTube, akhirnya diperbaiki dengan fallback.

**Akar masalah:** YouTube memberlakukan integrity check (poToken) + transisi ke protokol SABR.
Tanpa poToken, banyak client dikembalikan response **tanpa audio stream** → `audioStreams` kosong
(`totalAudio=0`) → ExoPlayer tak pernah dapat URL → lagu "skip sendiri" → berhenti.

**Solusi yang sekarang dipakai (di `getStreamUrl()`):**
Saat `audioStreams` kosong, **fallback ke muxed video stream** (video+audio jadi satu, itag 18 / 360p,
resolusi terendah). ExoPlayer memutarnya; audio tetap keluar.

**Konsekuensi fallback:**
- **Kuota lebih boros** (ikut mengunduh track video 360p). Ini belum bisa dihindari di mode muxed.
- CPU/baterai: sudah dihemat — video renderer **dimatikan** di `MusicService` via
  `setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)`. (Tidak mengurangi kuota, hanya decode.)

**Diagnostik sudah terpasang** di `getStreamUrl()` — JANGAN dihapus. Log tag `ZmusicService`:
mencetak `totalAudio`, `videoStreams`, `hlsUrl`, `dashMpd`, dan `streamInfo.errors`.

**Kalau musik bisu lagi, cek logcat DULU:**
```
adb logcat -d | grep -iE "ZmusicService|totalAudio|videoStreams|extractorError"
```
- `totalAudio>0` → harusnya bunyi; masalah lain (cek `onPlayerError`).
- `totalAudio=0, videoStreams=1` → kondisi normal saat ini (fallback bekerja).
- `totalAudio=0, videoStreams=0` → **YouTube mencabut muxed juga.** Fallback mati total.
  Saatnya naikkan strategi: aktifkan poToken (§5) atau backend yt-dlp.

**Aturan emas debugging di sini:** JANGAN pakai `catch (_: Exception)` yang menelan error diam-diam.
Selalu `Log.e(TAG, ..., e)`. Bug 2–3 bulan itu tak terlihat justru karena exception ditelan tanpa log.

---

## 5. poToken — Mesin yang Nganggur

Ada implementasi poToken lengkap (WebView BotGuard) di:
`data/ZmusicPoTokenProvider.kt`, `data/potoken/PoTokenWebView.kt`, `JavaScriptUtil.kt`,
`assets/po_token.html`. Didaftarkan di `ZmusicApp` via `YoutubeStreamExtractor.setPoTokenProvider(...)`.

**Statusnya: terpasang tapi tidak terpakai.** Log membuktikan extractor hanya memanggil
`getAndroidClientPoToken` (return null), TIDAK pernah `getWebClientPoToken` (tempat WebView nyantol).
Jadi mesin ini standby — baru relevan kalau upgrade NewPipeExtractor ke versi yang mengaktifkan
jalur web client. `PoTokenResult` constructor = `(visitorData, playerToken, streamToken)`.

> **Update v0.26.3:** workaround SABR internal (pindah player client otomatis) bikin audio-only
> sering balik **tanpa** poToken. Mesin poToken tetap standby sebagai cadangan. Detail lengkap di
> `NEWPIPE-EXTRACTOR.md` (referensi tunggal untuk Zmusic & Ztube).

---

## 6. Design System (Dark Plum)

Tema gelap berlapis + aksen yang **menyerap warna dari cover lagu**.

- **Token** di `ui/theme/ZmusicTheme.kt`: `Bg0..Bg3` (gelap berlapis), `TextPrimary/Muted/Faint`,
  aksen `AccentAmber/Magenta/Violet`, `GradientAccent(Full)`. Token lama (Glass*, Gradient*) dipertahankan
  namanya demi kompatibilitas — ubah nilainya, jangan hapus namanya.
- **Font** (`Type.kt`): **Sora** (display/body) + **Space Grotesk** (utility: angka, durasi, eyebrow).
  File `.ttf` di `res/font/` (huruf kecil + underscore). label* di Typography = Grotesk.
- **Aksen dinamis** (`Accent.kt`): `rememberAccentColors(thumbnailUrl)` → ekstrak warna via Palette dari
  bitmap Coil (allowHardware false, di Dispatchers.Default, resize 128px) → animasi transisi 600ms.
  Dipakai di Player, MiniPlayer, dll. Mengembalikan `AccentColors(primary, secondary, glow)`.
- **Komponen reusable**: `GradientPlayButton`, `EqualizerBars`, `StateDisplay` (empty/error: violet=empty,
  magenta=error), `MiniPlayerBar`, `SongItem` (punya param `isPlaying`+`accent` opsional).
- Mockup acuan = file HTML `zmusic_redesign.html` & `_2.html` (10 layar). Semua sudah diterjemahkan ke Compose.

---

## 7. Konvensi

- **Bahasa UI: Indonesia.** Semua teks yang dilihat user pakai bahasa Indonesia kasual.
- **Edit bertarget** lebih disukai daripada tulis ulang file besar (PlayerScreen ~1100+ baris).
  Verifikasi keseimbangan kurung `{}` `()` setelah edit terprogram.
- **Jangan sentuh wiring ViewModel** saat redesign visual — fungsi harus tetap jalan.
- **Hati-hati hapus import "tak terpakai":** `getValue`/`setValue` dipakai diam-diam oleh sintaks
  delegasi `by` (collectAsState). Jangan dibuang.
- Setelah refactor file penting (terutama MusicService), **konfirmasi musik masih bunyi** sebelum lanjut.

---

## 8. Hutang Teknis (status terkini)

| # | Item | Status |
|---|------|--------|
| 1 | Fallback muxed = boros kuota | Diterima (tradeoff). Pantau `videoStreams=0` |
| 2 | Mesin poToken nganggur | Standby; tunggu NewPipe versi web-client |
| 3 | Video renderer decode sia-sia | ✅ Lunas (track video dimatikan) |
| 4 | Kode mati / import unused | ✅ Lunas (sweep) |
| 5 | Queue drag-reorder | Terpasang (grip ⠿ + `moveItem`); perlu uji manual |
| 6 | Chips filter Search (Artis/Album) | Sengaja di-skip — tak ada sumber data; jadi tombol kosong |
| 7 | Versi toml vs gradle beda | ✅ Lunas (build.gradle.kts sekarang pakai `libs.*`; SEMUA versi dari toml) |
| 8 | Widget bikin AppDatabase baru tiap update (bocor) | ✅ Lunas (Hilt EntryPoint → singleton DAO) |
| 9 | versionCode macet di 1 | ✅ Lunas (221; WAJIB naik tiap rilis, konvensi X.Y.Z → XYZ) |
| 10 | Warna mood duplikat Mood/Stats | ✅ Lunas (terpusat di `ui/theme/MoodColors.kt`) |

**Sisa redesign:** ✅ SELESAI (Batch 5) — semua palet biru-navy tema lama sudah diganti token
Dark Plum: Artist, Equalizer, SmartPlaylist, Trending, Explore, Recent, Home (gradient+kartu),
PlaylistDetail (2 teks inline), Stats (streak chart), Player (DefaultBg). Gradient halaman
terpusat di token `PageBgPlum` (ZmusicTheme.kt). Empty state Stats/Trending pakai `StateDisplay`.

---

## 9. Workflow Build

- Build dari HP, lambat. Kelompokkan perubahan jadi batch kecil yang saling terkait.
- Patch dikirim sebagai zip dengan struktur folder asli; di-extract di root project (timpa).
- Setelah extract, **cek file beneran ke-replace** (insiden lama: patch ke-apply sebagian → error
  berulang di baris yang sama).
- pip di lingkungan build: -- (N/A, ini Gradle). Untuk Kotlin daemon yang mati saat low-RAM:
  `kotlin.compiler.execution.strategy=in-process` + `org.gradle.jvmargs=-Xmx2048m` — ✅ sudah diset
  di `gradle.properties`. `enableJetifier` juga sudah dimatikan (tak ada dependensi support-lib lama;
  kalau build error menyebut `android.support.*`, nyalakan lagi jadi true).
- **Naikkan `versionCode` setiap rilis** (konvensi: versionName X.Y.Z → versionCode XYZ).
  Tanpa ini user tidak bisa update APK tanpa uninstall (data Room ikut hilang).
