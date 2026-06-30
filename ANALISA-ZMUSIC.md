# Analisa Penuh Proyek Zmusic (v2.2.1)

> Hasil review menyeluruh terhadap codebase Zmusic — 81 file Kotlin, ±16.500 baris kode.
> Mencakup: jalur playback inti, lapisan data/Room, 16 layar UI Compose, util, widget,
> konfigurasi build, dan verifikasi klaim di CLAUDE.md.

---

## 1. Ringkasan Eksekutif

**Penilaian umum: kualitas di atas rata-rata untuk proyek solo yang di-build dari HP.**

Jalur playback inti (`MusicService` + `MusicRepository.getStreamUrl()`) adalah bagian
terbaik dari codebase: penanganan audio focus manual yang benar, gapless playback ala
Spotify dengan pre-queue, identifikasi lagu yang error secara presisi via `mediaId`
(bukan asumsi `currentSong`), snapshot atomik untuk record play history, dan diagnostik
logging yang disiplin. Riwayat bug 2–3 bulan "bisu total" jelas meninggalkan bekas
positif — bagian ini sekarang defensif dan terdokumentasi.

Masalah terbesar justru ada di **pinggiran**, bukan di inti:

| Prioritas | Temuan | Lokasi |
|---|---|---|
| 🔴 Kritis | Widget membuat instance Room `AppDatabase` BARU setiap update, tidak pernah di-close | `ZmusicWidgetProvider.kt:77-90` |
| 🔴 Kritis | `versionCode = 1` permanen — update APK di atas instalasi lama akan ditolak Android | `app/build.gradle.kts` |
| 🟠 Tinggi | `app/build.gradle.kts` mengabaikan `libs.versions.toml` sepenuhnya — versi drift (Coil 2.6.0 vs 2.7.0, BOM 2024.09.03 vs 2024.09.00) | build config |
| 🟠 Tinggi | 47 titik `catch (_: Exception)` tanpa log — melanggar "aturan emas" CLAUDE.md §4 sendiri | tersebar |
| 🟡 Sedang | PlayerScreen 1.398 baris & HomeScreen 1.113 baris — monolitik, prop drilling 13 callback | `ui/player`, `ui/home` |
| 🟡 Sedang | Token warna di-hardcode ulang di ≥7 layar alih-alih pakai `ZmusicTheme` | lapisan UI |
| 🟡 Sedang | poToken: API key WAA & request key hardcoded; tidak ada recovery kalau init gagal | `JavaScriptUtil.kt:37,42` |

---

## 2. Arsitektur — Gambaran Besar

```
UI (Compose, 16 layar)
   │  collectAsState
   ▼
ViewModel (per layar, Hilt)
   │
   ├── QueueManager (singleton, sumber kebenaran antrean) ──┐
   │                                                        │ playCommand (SharedFlow)
   ├── MusicRepository (search/stream/mood/history) ◄───────┤
   │        │                                               ▼
   │        ├─ NewPipeExtractor v0.26.2 (YouTube)      MusicService
   │        └─ Room (6 entity, migrasi 1→6)            (ExoPlayer + MediaSession)
   │
   └── Util: AudioDownloadManager, EqualizerManager, SleepTimer,
             DailyMixGenerator, NowPlayingCardGenerator
```

Pola arsitekturnya sehat: MVVM + repository + DI Hilt, komunikasi UI→Service lewat
`QueueManager.playCommand` (SharedFlow buffer 8), state reaktif via StateFlow.
Keputusan satu `OkHttpClient` `@Singleton` (AppModule) dan satu Coil `ImageLoader`
(ZmusicApp) sudah benar dan konsisten dipakai... kecuali oleh widget (lihat §4.1).

---

## 3. Yang Sudah Bagus (jangan dirusak)

1. **MusicService** — kualitas tertinggi di codebase:
   - Audio focus manual dengan flag `isIntendingToPlay` untuk kasus OPPO yang kirim
     `AUDIOFOCUS_LOSS` saat transisi lagu — fix yang spesifik dan tepat.
   - Gapless: `onMediaItemTransition(REASON_AUTO)` menggantikan `STATE_ENDED`,
     pre-queue lagu berikutnya dengan delay 8 detik agar tidak rebutan bandwidth.
   - `onPlayerError` membedakan "current song error" vs "pre-queued next song error"
     via `player.currentMediaItem?.mediaId` — mencegah lagu yang sedang enak diputar
     ikut di-restart gara-gara lagu berikutnya gagal buffer.
   - `recordCurrentPlay()` pakai snapshot atomik durasi sebelum reset — race dengan
     `onIsPlayingChanged` sudah ditutup.
   - `recordingScope` terpisah dari `serviceScope` agar play history tidak hilang
     saat service di-destroy. `onDestroy` pakai `runBlocking + withTimeout(3s)` —
     pragmatis dan aman dari ANR.
   - Video renderer dimatikan (`setTrackTypeDisabled(TRACK_TYPE_VIDEO)`) untuk
     hemat baterai di mode fallback muxed. ✔ sesuai klaim CLAUDE.md #3.

2. **Fallback muxed di `getStreamUrl()`** — solusi yang masuk akal untuk masalah
   poToken/SABR, dengan diagnostik lengkap (`totalAudio`, `videoStreams`, `hlsUrl`,
   `dashMpd`, `streamInfo.errors`) yang memang JANGAN dihapus.

3. **QueueManager** — semua mutasi antrean dilindungi `synchronized(lock)`,
   shuffle Fisher-Yates dengan anti-clustering mood, race TOCTOU di `current()`
   sudah ditutup.

4. **Room** — rantai migrasi 1→6 utuh tanpa `fallbackToDestructiveMigration()`,
   index di `play_history(songId, playedAt)` dan `search_history(query)`,
   upsert atomik via `INSERT OR REPLACE`, query streak pakai `strftime(..., 'localtime')`
   yang timezone-aware. DAO bersih, tidak ada N+1 (sudah pakai JOIN).

5. **ProGuard rules** — keep rules untuk Rhino, timeago patterns, dan Room query-result
   class (TopSongResult dkk.) menunjukkan pemahaman nyata atas crash release-build.

6. **Resource hygiene** — response OkHttp konsisten pakai `.use {}`, download
   menghapus file parsial saat cancel, Visualizer dilepas di `DisposableEffect`.

7. **Design system** — `Accent.kt` (ekstraksi Palette di `Dispatchers.Default`,
   resize 128px, animasi 600ms) implementasinya bagus. Komponen reusable
   (`SongItem`, `GlassCard`, `StateDisplay`, `MiniPlayerBar`) layak pakai.

---

## 4. Temuan Kritis

### 4.1 🔴 Widget: instance database baru setiap update — `ZmusicWidgetProvider.kt:77-90`

`loadPlaylists()` memanggil `Room.databaseBuilder(...).build()` setiap kali
`onUpdate()` jalan (per widget ID!), dan **tidak pernah `close()`**. Tiap instance
membuka koneksi SQLite + WAL sendiri. Widget yang refresh berkala = kebocoran
handle database menumpuk + risiko `SQLiteDatabaseLockedException` saat app utama
juga menulis.

**Fix:** ambil database lewat Hilt `EntryPoint` (BroadcastReceiver tidak bisa
`@AndroidEntryPoint` field injection untuk ini, tapi `EntryPointAccessors.fromApplication()`
bisa), atau minimal `db.close()` di `finally`. Opsi EntryPoint:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint { fun playlistDao(): PlaylistDao }

// di loadPlaylists():
val dao = EntryPointAccessors
    .fromApplication(context, WidgetEntryPoint::class.java)
    .playlistDao()
```

### 4.2 🔴 `versionCode = 1` selamanya — `app/build.gradle.kts`

`versionName` sudah 2.2.1 tapi `versionCode` masih 1. Android menolak instalasi
APK dengan versionCode ≤ yang terpasang — artinya **user lama tidak bisa update
tanpa uninstall** (kehilangan database playlist & history!). Naikkan versionCode
setiap rilis (mis. 221 untuk 2.2.1).

### 4.3 🟠 `libs.versions.toml` ternyata tidak dipakai sama sekali

CLAUDE.md §2 menyebut toml sebagai "versi tunggal sumber kebenaran", dan hutang #7
ditandai "✅ Lunas". Kenyataannya `app/build.gradle.kts` hardcode SEMUA dependensi
sebagai string literal dan tidak ada satu pun referensi `libs.*`. Drift yang sudah
terjadi:

| Dependensi | build.gradle.kts | libs.versions.toml |
|---|---|---|
| Coil | 2.6.0 | 2.7.0 |
| Compose BOM | 2024.09.03 | 2024.09.00 |
| Lifecycle | 2.7.0 | 2.8.7 |
| Navigation | 2.7.7 | 2.8.5 |
| Activity Compose | 1.9.0 | 1.9.3 |

NewPipe memang seragam v0.26.2 (klaim #7 benar sebatas itu), tapi sumber kebenaran
sesungguhnya saat ini adalah **build.gradle.kts**, bukan toml. Pilih salah satu:
migrasi ke `libs.*` (sekali kerja, aman selamanya) atau perbarui CLAUDE.md agar
tidak menyesatkan AI assistant berikutnya.

### 4.4 🟠 47 `catch (_: Exception)` bisu — melanggar aturan emas sendiri

CLAUDE.md §4: *"JANGAN pakai `catch (_: Exception)` yang menelan error diam-diam"*
— tapi ada 47 kemunculan. Sebagian memang degradasi yang disengaja (discovery,
lirik, translasi), tapi beberapa berbahaya karena menyembunyikan kegagalan yang
user rasakan:

- `MusicService.kt:233` — gagal load playlist dari widget: hening, user tap widget
  tidak terjadi apa-apa.
- `FloatingPlayerService` (addView) — bubble gagal muncul tanpa jejak.
- `EqualizerManager` (4 titik) — EQ diam-diam mati.
- `LyricsRepository` (4 titik) — lirik/translasi gagal tanpa bisa didiagnosa.
- `PlayerViewModel:183-191` — cleanup MediaController.

Tidak perlu refactor besar: tambahkan `Log.w(TAG, "...", e)` satu baris di tiap
catch. Murah, dan persis pelajaran dari bug bisu 2–3 bulan itu.

---

## 5. Temuan per Lapisan

### 5.1 Lapisan data & poToken

- **`ZmusicPoTokenProvider`**: kalau `ensureReady()` gagal sekali (fetch challenge
  error), tidak ada retry — token null sampai app restart. Tambahkan re-init dengan
  backoff. (Dampak saat ini kecil karena mesin poToken memang nganggur — extractor
  v0.26.2 tidak memanggil `getWebClientPoToken`, sesuai catatan §5 CLAUDE.md.)
- **`JavaScriptUtil`**: WAA API key & BotGuard request key hardcoded. Catatan: ini
  key *publik* yang sama dengan yang dipakai NewPipe/yt-dlp, jadi bukan kebocoran
  rahasia — risikonya hanya patah diam-diam kalau Google merotasi. Regex ekstraksi
  visitorData dari HTML juga rapuh terhadap perubahan struktur halaman.
- **`PoTokenWebView`**: hidup di singleton — pastikan `destroy()` di jalur expiry
  selalu jalan (sudah ada, OK). `addJavascriptInterface` aman karena HTML dari
  `file:///android_asset/`, bukan jaringan.
- **`AudioDownloadManager`**: solid (connection pool share, `.use{}`, hapus file
  parsial saat cancel, cek kuota). Catatan non-teknis: unduh audio YouTube melanggar
  ToS YouTube — risiko ditanggung pemakai; jangan publikasikan ke Play Store dengan
  fitur ini aktif.
- **`MusicRepository`**: jantungnya sehat. `searchByArtist` menelan semua error dan
  mengembalikan list kosong — UI tidak bisa membedakan "artis tidak ditemukan" vs
  "jaringan mati".

### 5.2 Lapisan UI (16 layar)

- **Ukuran**: PlayerScreen 1.398 baris (lirik + queue sheet + sleep timer + speed/
  pitch + download dalam satu file), HomeScreen 1.113 baris (9+ seksi + infinite
  scroll), LibraryScreen 737 baris. Saran: pecah jadi sub-composable per seksi —
  juga mengurangi scope recomposition.
- **Prop drilling**: `HomeScreen` menerima **13 callback navigasi** lalu meneruskan
  semuanya ke `HomeContentLayout`. Bungkus jadi satu `data class HomeNavActions`
  atau pakai `CompositionLocal`.
- **Tema tidak ditegakkan**: ≥7 layar (Home, Search, Library, Artist, Trending,
  Settings, Equalizer) mendefinisikan ulang `ScreenBg`/`CardSurface`/`TextPrimary`
  secara lokal dengan nilai yang identik dengan token `ZmusicTheme` — kalau ganti
  tema nanti, harus edit 7+ file. Peta warna mood juga duplikat persis di
  `MoodScreen` dan `StatsScreen`.
- **Empty state hilang**: StatsScreen ("belum ada statistik") dan TrendingScreen
  ("trending kosong") — padahal `StateDisplay` sudah siap dipakai (sesuai catatan
  CLAUDE.md §8).
- **Navigasi** (`MainActivity`): struktur sealed-class rapi. Perhatikan highlight
  bottom-nav untuk route berparameter (`artist/{artistName}`) yang dicocokkan
  dengan string literal.

### 5.3 Util

- `QueueManager`, `SleepTimerManager`, `AudioSessionHolder`: bersih.
- `DailyMixGenerator`: cache per-tanggal bagus; 4 catch bisu.
- `NowPlayingCardGenerator`: alokasi bitmap 1080×1080 tiap panggilan + PNG di
  `cacheDir` tidak pernah dibersihkan — tambahkan pembersihan file > 24 jam.
- `EqualizerManager`: fallback graceful, tapi semua error ditelan.

### 5.4 Konfigurasi build

- `android.enableJetifier=true` di `gradle.properties` kemungkinan besar tidak
  diperlukan lagi (tidak ada dependensi support-library) — **menonaktifkannya
  mempercepat build**, relevan banget untuk build 5–15 menit di HP.
- `gradle.properties` belum berisi `kotlin.compiler.execution.strategy=in-process`
  yang direkomendasikan CLAUDE.md §9 sendiri untuk kondisi low-RAM.
- `android.aapt2FromMavenOverride=/usr/bin/aapt2` — spesifik mesin (Termux);
  wajar, tapi patah kalau project dipindah ke PC. Idealnya di `local.properties`
  semacamnya / di-comment dengan jelas.
- `local.properties` ikut ter-zip — jangan ikut version control.
- Typo komentar kecil: `// Hilton` → `// Hilt` 🙂

### 5.5 Manifest & izin

Bersih. Semua izin punya alasan (FOREGROUND_SERVICE_MEDIA_PLAYBACK, SPECIAL_USE
untuk bubble, RECORD_AUDIO untuk visualizer, SYSTEM_ALERT_WINDOW dengan grant
manual). Komponen exported semuanya memang harus exported (launcher, MediaSession
service, widget). FileProvider tidak exported. Tidak ada temuan keamanan.

---

## 6. Verifikasi Tabel Hutang Teknis CLAUDE.md §8

| # | Klaim | Hasil verifikasi |
|---|---|---|
| 1 | Fallback muxed boros kuota — diterima | ✔ Benar, fallback + diagnostik terpasang |
| 2 | poToken nganggur, standby | ✔ Benar; tambah catatan: tanpa recovery saat init gagal |
| 3 | Video renderer dimatikan — lunas | ✔ Terverifikasi di `MusicService.kt:325-328` |
| 4 | Kode mati / import unused — lunas | ✔ Relatif bersih |
| 5 | Queue drag-reorder terpasang | ✔ `moveItem` ada di QueueManager (uji manual tetap perlu) |
| 6 | Chips filter Search di-skip | ✔ Sesuai |
| 7 | Versi NewPipe toml vs gradle seragam — lunas | ⚠ **Setengah benar**: NewPipe seragam, tapi toml secara keseluruhan TIDAK dipakai oleh build.gradle.kts dan 5 dependensi lain drift (lihat §4.3) |

---

## 7. Rekomendasi — Urutan Kerja

Disusun sebagai batch kecil sesuai workflow build-dari-HP (§9 CLAUDE.md):

**Batch 1 — kritis, kecil, aman (1 sesi build):**
1. Naikkan `versionCode` (dan jadikan kebiasaan tiap rilis).
2. Fix widget: Hilt EntryPoint atau `db.close()` di `finally`.
3. Tambah `Log.w` di catch bisu yang menyentuh fitur user (MusicService widget
   action, FloatingPlayerService addView, EqualizerManager).

**Batch 2 — kebersihan build:**
4. Putuskan nasib `libs.versions.toml`: migrasi `build.gradle.kts` ke `libs.*`
   ATAU update CLAUDE.md. Sekalian samakan versi Coil/BOM/lifecycle.
5. Matikan `enableJetifier`, tambah `kotlin.compiler.execution.strategy=in-process`.

**Batch 3 — kualitas UI (bertahap, per layar):**
6. Sentralisasi token warna yang diduplikat 7 layar → pakai `ZmusicTheme`.
7. Satukan peta warna mood (MoodScreen ↔ StatsScreen).
8. Tambah empty state Stats & Trending pakai `StateDisplay`.
9. (Opsional) Pecah PlayerScreen & HomeScreen jadi sub-composable; bungkus 13
   callback HomeScreen jadi satu objek navigasi.

**Pantau terus:** log `totalAudio=0, videoStreams=0` = fallback muxed mati total →
saatnya upgrade NewPipeExtractor (yang sekaligus bisa menghidupkan mesin poToken
yang sudah nganggur) atau backend alternatif.

---

*Laporan dibuat otomatis dari analisa statis kode; tidak ada build/test yang dijalankan
(proyek Android, environment ini tanpa Android SDK).*
