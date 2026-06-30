# NewPipeExtractor — Referensi Lengkap (Zmusic & Ztube)

Dokumen acuan untuk semua hal terkait NewPipeExtractor di project Zmusic (audio) dan Ztube (video).
Disusun dari pengalaman nyata debugging + riset rilis resmi. Simpan di root kedua project.

> **TL;DR:** YouTube memberlakukan SABR → stream audio terpisah hilang, sering cuma dapat 360p muxed.
> NewPipeExtractor **v0.26.3** (9 Jun 2026) bawa workaround. Naik versi = audio-only & resolusi tinggi
> berpotensi balik **otomatis tanpa ngoding poToken**. Tetap pertahankan fallback muxed (video anak-anak
> masih 360p). Selalu pasang logging diagnostik. Jangan pernah `catch` yang menelan error.

---

## 1. Apa Itu NewPipeExtractor

Library Java/Kotlin yang mengekstrak data (stream video/audio, search, playlist, channel) dari situs
streaming — terutama YouTube — dengan **menganalisis web interface**, bukan API resmi. Ini inti
dari aplikasi NewPipe, tapi bisa dipakai standalone.

- Repo: `github.com/TeamNewPipe/NewPipeExtractor`
- Dependency (JitPack): `com.github.TeamNewPipe:NewPipeExtractor:<versi>`
- Lisensi: GPLv3
- **Konsekuensi penting:** karena scraping web interface (bukan API resmi), library ini **rapuh by design**.
  YouTube berubah → ekstraksi bisa rusak sewaktu-waktu. Ini bukan bug app-mu, ini sifat pendekatannya.

---

## 2. Akar Masalah: SABR, poToken, dan 360p

Inti dari hampir semua drama streaming YouTube belakangan:

### SABR (Server-Side Adaptive Bitrate)
Protokol streaming baru YouTube yang menggantikan cara lama (URL progresif langsung). Saat YouTube
memaksa SABR, gejalanya:
- **Stream audio terpisah hilang** (`audioStreams` kosong / `totalAudio=0`)
- **Resolusi video tinggi hilang**
- Sering **cuma tersisa satu MP4 360p muxed** (video+audio jadi satu, itag 18)

### poToken (Proof of Origin Token)
Token integrity check yang diminta YouTube untuk membuktikan request datang dari client "sah".
Tanpa poToken, banyak player client ditolak / dikasih response miskin (tanpa audio stream).

### Hubungannya dengan gejala "musik mati"
```
YouTube paksa SABR + minta poToken
   → client yang dipakai NewPipe ditolak/dibatasi
   → audioStreams kosong (totalAudio=0)
   → ExoPlayer tak pernah dapat URL audio
   → lagu "skip sendiri" / app bisu
```
Ini persis yang bikin Zmusic bisu ~2–3 bulan.

---

## 3. Sejarah Versi (yang relevan ke kita)

Fokus pada PR yang menyangkut SABR/poToken/stream. Nomor PR lebih reliable daripada tanggal.

| Versi | Inti perubahan |
|-------|----------------|
| **v0.24.4** | Hotfix: update iOS client + tambah visitor data ke request (saat YouTube ubah sesuatu) |
| **v0.24.5** (pre-release) | **poToken support diperkenalkan** (#1272): refactor player clients, ekstrak visitor data. Masih perlu penyesuaian API, belum teruji penuh |
| **v0.24.6** | **Fix crash pada SABR-only player responses; berhenti pakai WEB client untuk stream URL** (#1297). Titik awal era SABR |
| **v0.25.0** | BREAKING: refactor date parsing (`DateWrapper(Calendar)` dihapus → pakai `Instant`/`LocalDateTime`). Tandai members-only videos. Error khusus "Sign in to confirm…" |
| **v0.26.0** | BREAKING: `Service.getMediaCapabilities()` kembalikan `Set` (bukan `List`). `AccountTerminatedException` |
| **v0.26.1** | Fix fetching duration for items |
| **v0.26.2** | Fallback playlist uploader; fix related videos duration/livestream; fix SoundCloud comments crash |
| **v0.26.3** (9 Jun 2026, **terbaru**) | **Workaround SABR enforcement dengan player client lain** (#1508); pakai formats **xTags** untuk tipe track audio (#1504); fix ekstraksi item playlist (#1503); fix Bandcamp search (#1493) |

### Apa arti v0.26.3 (paling penting)
Dari pengumuman NewPipe app yang ship v0.26.3:
> "Memperbaiki masalah akibat YouTube memberlakukan SABR, **kecuali video anak-anak** (limitasi
> workaround). Yang paling kentara: resolusi video hilang, tidak ada audio stream terpisah, dan
> hanya tersedia MP4 360p dengan audio."

**Mekanisme:** workaround **internal** — extractor diam-diam pindah ke **player client lain** yang
masih mengembalikan stream proper. Artinya **otomatis**, tidak butuh kamu setup poToken manual.

**Batas:** video anak-anak (made for kids) tetap mentok 360p muxed. SABR jangka panjang masih PR
TeamNewPipe (butuh implementasi penuh, belum selesai).

---

## 4. Setup & Dependency

### libs.versions.toml
```toml
[versions]
newpipe-extractor = "v0.26.3"

[libraries]
newpipe-extractor = { group = "com.github.TeamNewPipe", name = "NewPipeExtractor", version.ref = "newpipe-extractor" }
```

### app/build.gradle.kts
```kotlin
implementation(libs.newpipe.extractor)
// atau hardcode (pastikan SAMA dengan toml — jangan beda sumber kebenaran):
// implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.3")
```

### settings.gradle.kts — JitPack repo wajib ada
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

### ProGuard (wajib untuk release build — Rhino JS engine)
```proguard
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
```

### minSdk < 33
Butuh core library desugaring dengan artifact `desugar_jdk_libs_nio`:
```kotlin
android { compileOptions { isCoreLibraryDesugaringEnabled = true } }
dependencies { coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:<versi>") }
```
> Catatan: NewPipeExtractor v0.26.x pakai `java.time` yang di API 26–32 perlu desugaring.
> Bungkus `NewPipe.init(...)` dengan try-catch agar app tetap bisa launch + retry kalau desugar
> runtime belum siap.

---

## 5. Inisialisasi

```kotlin
NewPipe.init(
    downloader,                       // implementasi Downloader (OkHttp)
    Localization("id", "ID"),
    ContentCountry("ID")
)

// Opsional: daftarkan poToken provider (lihat §8). Aman dibiarkan walau workaround sudah cukup.
YoutubeStreamExtractor.setPoTokenProvider(poTokenProvider)
```

`Downloader` adalah kelas abstrak yang harus kamu implement (biasanya pakai OkHttp): handle
`execute(Request)`, header, dsb.

---

## 6. Ekstraksi Stream — API Inti

```kotlin
val youtube = NewPipe.getService(ServiceList.YouTube.serviceId)
val info = StreamInfo.getInfo(youtube, "https://www.youtube.com/watch?v=$videoId")
```

### Properti penting `StreamInfo`
| Properti | Isi |
|----------|-----|
| `audioStreams` | List `AudioStream` — audio-only (yang kita mau di Zmusic) |
| `videoStreams` | List `VideoStream` — **muxed** (video+audio), termasuk itag 18 / 360p |
| `videoOnlyStreams` | List `VideoStream` — video-only (perlu digabung audio untuk HD) |
| `dashMpdUrl` | URL manifest DASH (kalau ada) |
| `hlsUrl` | URL HLS (kalau ada) |
| `errors` | **List exception non-fatal per-client yang ditelan extractor** — emas untuk diagnosis |

### Properti penting `Stream` (Audio/Video)
| Method | Arti |
|--------|------|
| `getContent()` | URL **atau** isi manifest, tergantung `deliveryMethod` |
| `isUrl()` | true kalau `getContent()` adalah URL langsung (progresif) |
| `getDeliveryMethod()` | `PROGRESSIVE_HTTP` / `DASH` / `HLS` / `TORRENT` |
| `getFormat()` | MediaFormat (M4A, WEBMA, MPEG_4, dll) |
| `averageBitrate` | (audio) bitrate rata-rata |
| `getResolution()` / `height` | (video) resolusi |

> **Jebakan:** jangan langsung pakai `getContent()` tanpa cek `isUrl()`. Kalau `deliveryMethod`
> bukan `PROGRESSIVE_HTTP`, isinya bisa manifest, bukan URL siap putar.

---

## 7. Pola Pakai

### 7a. ZMUSIC (audio-first, muxed sebagai fallback)

Filosofi: **mau audio-only**; kalau kosong (SABR), **fallback ke muxed** supaya tetap bunyi.
Pola ini sudah terbukti dan **otomatis manfaatin audio yang balik** setelah upgrade ke v0.26.3.

```kotlin
suspend fun getStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
    try {
        val yt = NewPipe.getService(ServiceList.YouTube.serviceId)
        val info = StreamInfo.getInfo(yt, "https://www.youtube.com/watch?v=$videoId")

        // 1) Audio-only (jalur utama)
        val audio = info.audioStreams
            .filter { it.content != null }
            .filter { it.averageBitrate > 0 }
            .maxByOrNull { it.averageBitrate }
            ?: info.audioStreams.firstOrNull { it.content != null }

        // ── DIAGNOSTIK (JANGAN DIHAPUS) ──
        Log.d("ZmusicService",
            "getStreamUrl $videoId — totalAudio=${info.audioStreams.size}, " +
            "videoStreams=${info.videoStreams.size}, picked=${audio?.deliveryMethod}, " +
            "isUrl=${audio?.isUrl}, bitrate=${audio?.averageBitrate}")
        info.errors.forEachIndexed { i, e ->
            Log.w("ZmusicService", "extractorError[$i]: ${e.javaClass.simpleName}: ${e.message}")
        }

        audio?.content
            ?: run {
                // 2) FALLBACK: muxed (video+audio, biasanya itag 18 / 360p)
                val muxed = info.videoStreams
                    .filter { it.content != null }
                    .minByOrNull { it.height }   // resolusi terendah = hemat
                muxed?.let { Log.w("ZmusicService", "FALLBACK ke muxed ${it.resolution}") }
                muxed?.content
            }
            ?: throw Exception("Tidak ada stream audio tersedia untuk video ini")
    } catch (e: Exception) {
        Log.e("ZmusicService", "getStreamUrl GAGAL untuk $videoId", e)  // jangan ditelan!
        throw e
    }
}
```

**Di MusicService (ExoPlayer):** matikan track video supaya mode muxed tidak boros decode CPU/baterai:
```kotlin
player.trackSelectionParameters = player.trackSelectionParameters
    .buildUpon()
    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
    .build()
```
> Ini menghemat CPU/baterai, **bukan kuota** (muxed tetap satu file). Kuota baru hemat kalau
> audio-only kembali (mis. setelah v0.26.3).

### 7b. ZTUBE (video-first, muxed/360p sebagai fallback)

Kebalikan Zmusic: video adalah tujuan. Setelah v0.26.3, coba resolusi tinggi dulu.

```kotlin
data class VideoSource(val url: String, val label: String)

suspend fun getVideoSource(videoId: String): VideoSource = withContext(Dispatchers.IO) {
    val yt = NewPipe.getService(ServiceList.YouTube.serviceId)
    val info = StreamInfo.getInfo(yt, "https://www.youtube.com/watch?v=$videoId")

    Log.d("ZtubeService",
        "getVideoSource $videoId — muxed=${info.videoStreams.size}, " +
        "videoOnly=${info.videoOnlyStreams.size}, audio=${info.audioStreams.size}, " +
        "dash=${!info.dashMpdUrl.isNullOrEmpty()}, hls=${!info.hlsUrl.isNullOrEmpty()}")
    info.errors.forEach { Log.w("ZtubeService", "extractorError: ${it.message}") }

    // 1) Muxed resolusi tertinggi (paling simpel — 1 URL, langsung putar)
    val muxed = info.videoStreams
        .filter { it.content != null && it.isUrl }
        .maxByOrNull { it.height }
    if (muxed != null) return@withContext VideoSource(muxed.content, muxed.resolution)

    // 2) (Tahap lanjut) HD asli = videoOnly + audio digabung pakai MergingMediaSource di ExoPlayer.
    //    Butuh penanganan terpisah; sering perlu poToken/DASH. Lihat §8.

    throw Exception("Tidak ada video stream tersedia")
}
```

> **Untuk HD beneran (720p/1080p):** YouTube pisahkan video & audio. Ambil `videoOnlyStreams`
> (resolusi tinggi) + `audioStreams`, lalu gabung di ExoPlayer pakai `MergingMediaSource`
> (atau pakai `dashMpdUrl` bila tersedia). Ini lebih kompleks dan paling mungkin butuh poToken/SABR
> sepenuhnya beres. Untuk MVP, **muxed 360p sudah cukup**.

---

## 8. Sistem poToken (untuk kasus berat)

Walau v0.26.3 sering cukup tanpa poToken, ini referensi kalau perlu jalur web client.

### API
- Daftarkan: `YoutubeStreamExtractor.setPoTokenProvider(provider)` (static)
- Interface `PoTokenProvider` (4 method, semua `@Nullable PoTokenResult ...(String videoId)`):
  - `getWebClientPoToken(videoId)`  ← jalur web (yang butuh poToken)
  - `getWebEmbedClientPoToken(videoId)`
  - `getAndroidClientPoToken(videoId)`
  - `getIosClientPoToken(videoId)`
- `PoTokenResult(visitorData, playerRequestPoToken, streamingDataPoToken)` — **urutan 3 argumen ini penting**

### Realita
- poToken di-generate dengan menjalankan **BotGuard JS** YouTube, biasanya lewat **WebView tersembunyi**
  (NewPipe punya implementasi `PoTokenWebView`).
- Threading: provider dipanggil dari thread background extractor, tapi WebView wajib di main thread →
  perlu jembatan (mis. `runBlocking` + `Dispatchers.Main`).
- **Fragile:** format BotGuard berubah-ubah; bahkan token valid kadang playback masih gagal.
- Extractor versi tertentu mungkin **tidak memanggil** `getWebClientPoToken` (cuma Android/iOS client) —
  buktikan dengan log di tiap method sebelum berasumsi providernya kepakai.

---

## 9. Diagnostik & Troubleshooting

### Aturan emas
**JANGAN PERNAH `catch (_: Exception)` yang menelan error tanpa log.** Bug Zmusic tak terlihat
berbulan-bulan justru karena exception ditelan diam-diam. Selalu `Log.e(TAG, msg, e)`.

### Perintah logcat
```
adb logcat -c                                              # bersihkan dulu
# (buka app, putar, tunggu sampai gagal/skip)
adb logcat -d | grep -iE "ZmusicService|totalAudio|FALLBACK|extractorError|onPlayerError"
```
> Pakai `-d` (dump sekali) lebih reliable daripada live + grep (buffering). Pastikan momen "play"
> ada di dalam window capture; force-close app dulu agar log init muncul lagi.

### Pohon keputusan (decision tree)
```
Musik/video tidak jalan?
├─ totalAudio>0 (atau videoStreams berisi) tapi tetap gagal
│   └─ ada onPlayerError? → cek HTTP code
│       ├─ 403 → URL ditolak (throttle/region). Coba bump versi / cek nsig.
│       └─ SOURCE/format → cek isUrl & deliveryMethod (mungkin DASH/manifest, bukan URL)
├─ totalAudio=0, videoStreams=1 (muxed)
│   └─ NORMAL era SABR. Fallback muxed bekerja. Bunyi tapi 360p/boros kuota.
│       → Upgrade ke v0.26.3 untuk coba kembalikan audio-only/HD.
├─ totalAudio=0, videoStreams=0 (KOSONG TOTAL)
│   └─ Ekstraksi gagal total. Cek info.errors untuk alasan.
│       ├─ ParsingException/ExtractionException → versi extractor ketinggalan → BUMP versi
│       ├─ "Sign in to confirm…" → IP/bot check (umum di VPS/datacenter)
│       └─ IOException/timeout → masalah jaringan/Downloader
└─ Tidak ada log app sama sekali
    └─ APK lama / build belum masuk / momen play di luar window capture
```

### Tanda bahaya jangka panjang
`totalAudio=0` **dan** `videoStreams=0` di banyak video = YouTube mencabut muxed juga.
Fallback mati total → saatnya poToken (§8) atau pindah ekstraksi ke backend (yt-dlp).

---

## 10. Strategi Upgrade

1. **Cek breaking changes** di changelog antara versimu dan target. Yang sudah diketahui breaking:
   - v0.25.0: date parsing (`DateWrapper(Calendar)` dihapus)
   - v0.26.0: `getMediaCapabilities()` → `Set`
   - v0.26.2 → v0.26.3: **tidak ada breaking change** (aman).
2. Ubah versi di **dua tempat** (toml + build.gradle.kts) — jangan beda.
3. Build → test logcat **beberapa video berbeda** (jangan satu doang; hasil bisa beda per video/region).
4. **Jangan hapus** fallback muxed & poToken provider saat upgrade — keduanya tetap berguna
   (video anak-anak tetap 360p; provider standby).
5. Pantau rilis baru: `github.com/TeamNewPipe/NewPipeExtractor/releases`. Snapshot dev tersedia di
   Maven Central snapshot repo (berlaku 90 hari, basis short-hash commit) kalau butuh fix terbaru
   sebelum rilis stabil.

---

## 11. Batasan & Ranah di Luar NewPipe

Yang **tidak** bisa lewat NewPipeExtractor (butuh API YouTube resmi/OAuth):
- Login akun, like, subscribe, komentar (posting)
- Riwayat tontonan akun YouTube, rekomendasi personal

Yang bisa tapi nambah kompleksitas: live stream, Shorts, DASH/HLS adaptif (HD).

**Posisi legal/ToS:** scraping web interface YouTube ada di area abu-abu ToS. Sama posisinya untuk
Zmusic & Ztube — pakai untuk tools pribadi, sadari risikonya.

---

## 12. Ringkasan Cepat per Project

**Zmusic (audio)**
- Target versi: `v0.26.3`
- Pola: audio-first → muxed fallback (kode existing sudah benar, tinggal bump versi)
- Setelah upgrade: cek `totalAudio>0` balik = kuota hemat lagi
- Pertahankan: muxed fallback, `setTrackTypeDisabled(VIDEO)`, logging

**Ztube (video)**
- Target versi: `v0.26.3` dari awal
- Pola: video-first → ambil `videoStreams` resolusi tertinggi → muxed 360p fallback
- MVP: muxed 360p sudah cukup. HD (720p+) = videoOnly+audio merge / DASH, tahap lanjut
- Bawa logging diagnostik yang sama sejak hari pertama

---

*Terakhir diperbarui mengacu NewPipeExtractor v0.26.3 (9 Jun 2026). Saat YouTube berubah lagi,
update tabel §3 dan cek apakah pola fallback masih relevan.*
