# 🤝 09 — Contributing

> Biar PR nggak bolak-balik. Ikuti 5 aturan keras + checklist di bawah.

## 🌿 Branch & Commit

```bash
git checkout -b fitur-xyz          # CI otomatis jalan di main
# ... ngoding ...
./gradlew assembleDebug             # wajib hijau lokal
git push -u origin fitur-xyz
```

- 📝 Commit message: `feat(run): tambah auto-pause` / `fix(musik): fallback muxed` / `docs: ...`.
- 📦 Satu PR = satu tujuan. Jangan campur GPS + musik + UI dalam satu PR.

## 🚫 5 Aturan Keras Proyek

1. 🎵 **Playback rapuh** — baca `NEWPIPE-EXTRACTOR.md` dulu. Jangan hapus log `totalAudio/videoStreams`. Jangan `catch` menelan error tanpa `Log.e`.
2. 🗄️ **Room** — ubah skema = naik versi + Migration + daftar di `AppModule.kt`.
3. 📍 **GPS** — jangan ubah urutan pipeline (akurasi → Doppler → glitch → diam → smoothing). Pace dari `movingMillis`.
4. 🗝️ **Maps key** via placeholder; `app/debug.keystore` jangan dihapus.
5. 🗣️ **Indonesia kasual** + versi cuma di `libs.versions.toml`.

## ✅ Checklist PR

- [ ] 🔨 `assembleDebug` hijau (lokal / CI)
- [ ] 🗄️ Migrasi Room dites upgrade dari APK lama (jangan fresh install doang!)
- [ ] 📍 GPS dites Lockito 11 km/j + jalan beneran (kalau sentuh tracking)
- [ ] 🎵 Musik dites: search → play → skip → playlist → offline (kalau sentuh musik)
- [ ] 🗺️ Peta dites dengan key valid (atau tulis "peta blank expected" di PR)
- [ ] 📚 Docs di `docs/` diupdate kalau ubah perilaku
- [ ] 🧹 Tidak ada `TODO` / log debug nyasar / key asli ke-commit

## 🎨 Panduan UI

- Pakai komponen `ZR*` di `ZRunComponents.kt` — jangan bikin design system tandingan.
- Ikon dari `material-icons-extended`. Font Sora / Space Grotesk sudah ada.
- Screen baru? Ikuti [🎨 UI & Navigasi](04-ui-ux-navigation.md) resep 4 langkah.

## 🐞 Lapor Bug (template)

```md
**Gejala:** ...
**Langkah:** 1. ... 2. ... 3. ...
**HP/OS:** ... **APK:** debug/CI run #...
**Log:** `adb logcat -d | grep -iE "ZmusicService|extractorError|Tracking"`
**Harapan vs kenyataan:** ...
```

⏭️ Mentok? → [🆘 Troubleshooting](11-troubleshooting.md).
