# 🆘 10 — Troubleshooting & FAQ

> 90% masalah = 6 baris di bawah. Baca ini dulu sebelum buka issue. ❤️

## 🗺️ Peta Blank (abu-abu + logo Google)

| Cek | Cara |
|-----|------|
| 🔑 Key kepasang? | `adb shell dumpsys package com.zaaam.zrun \| grep -i maps` / cek `local.properties` |
| 🌐 API enabled? | Cloud Console → *Maps SDK for Android* → Enabled |
| 📦 Package + SHA-1? | Daftarkan `com.zaaam.zrun` **dan** `com.zaaam.Zmusic` + SHA-1 `app/debug.keystore` |
| 📱 Play Services? | Emulator tanpa Play Services = blank selamanya. Pakai HP fisik / image `google_apis` |

GPS tetap ngerekam walau peta blank — jangan campur aduk.

<details>
<summary>🔍 Log peta</summary>

```bash
adb logcat -d | grep -iE "GoogleMaps|Authorization|API_KEY"
# "API key not authorized" = restriction salah
# "API key expired" = key dihapus di console
```
</details>

## 🎵 Musik Gagal / Muter Terus / Tiba-tiba Stop

```bash
adb logcat -d | grep -iE "totalAudio|FALLBACK|extractorError|onPlayerError"
```

| Log | Artinya | Obat |
|-----|---------|------|
| `totalAudio=0` | SABR / YouTube berubah | Lihat `NEWPIPE-EXTRACTOR.md` §2-3, coba update extractor |
| `FALLBACK muxed` | Audio-only hilang, pakai 360p | Normal sementara, pantau |
| `extractorError` | DOM YouTube berubah | Update `v0.26.3` → rilis baru |
| `onPlayerError` + `403` | URL kadaluarsa / region-lock | Retry / lagu lain |
| Semua lagu gagal | `NewPipe.init` gagal / network | Cek `ZmusicApp.init` + OkHttp + internet |

Jangan hapus log diagnostik di `getStreamUrl()` — itu stetoskopnya!

## 📍 GPS Ngaco (jarak loncat / pace 0)

- 🏠 Indoor? Akurasi >15 m dibuang — wajar. Tes di luar / Lockito.
- 🛰️ Mock location belum diizinkan? Aktifkan di Developer Options.
- 🐢 Pace 0 padahal jalan? Cek `movingMillis` — di bawah 2.5 km/j dianggap diam.
- 👾 Jarak loncat jauh? Glitch filter 15 m/s — kalau masih loncat, ambil log `TrackingService`.

Tes standar: **Lockito ~11 km/j** → jarak & pace harus wajar.

## 🔨 Build Gagal

| Error | Obat |
|-------|------|
| `SDK location not found` | Isi `sdk.dir` di `local.properties` |
| `Unsupported Java` / `JVM` | Pakai **JDK 17** |
| `aapt2` di Termux | Jangan hapus `aapt2FromMavenOverride` — CI yang strip sendiri |
| `Unresolved reference Hilt/Room` | `./gradlew clean` → rebuild (KSP kadang nyangkut) |
| `MAPS_API_KEY` error | Kosongkan saja (`""`) — build tetap jalan |
| Rilis crash tapi debug aman | ProGuard! Cek `proguard-rules.pro` (Rhino/NewPipe/Media3) |

## ❓ FAQ

**Q: `com.zaaam.Zmusic` vs `com.zaaam.zrun`?**
A: Sengaja beda. Kode = `Zmusic`, app di HP = `zrun`. Jangan rename massal. Detail → [🏠 Overview](01-overview.md).

**Q: Naikkan versi gimana?**
A: `versionName 1.0.0 → 1.0.1` + `versionCode 100 → 101` (`X*100+Y*10+Z`). Wajib naik tiap rilis!

**Q: Tambah tabel Room?**
A: Naik versi + Migration + daftar di `AppModule` → [🗄️ Database](08-database.md).

**Q: CI ignore `.md`?**
A: Ya — push docs doang tidak trigger build. Sentuh `.kt`/`.kts` biar CI jalan.

**Q: Butuh Firebase?**
A: Tidak. Sisa Firebase Zmusic sudah dihapus.

---
Masih mentok? Buka issue pakai template di [🤝 Contributing](10-contributing.md) + sertakan log. 🙏
