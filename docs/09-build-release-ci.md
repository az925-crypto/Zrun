# 📦 08 — Build / Release / CI

> Debug gampang, release butuh disiplin versi + signing. CI cuma build debug biar hemat runner.

## 🧪 Build Types (tanpa flavors)

| Type | Signing | Minify | Kapan |
|------|---------|--------|-------|
| `debug` 🐞 | `debugFixed` (`app/debug.keystore`, committed, `android/androiddebugkey`) | ❌ | Harian, CI artifact |
| `release` 📦 | env `ZMUSIC_KEYSTORE_*` kalau ada, else **unsigned** | ✅ R8 + `proguard-rules.pro` | Play / share manual |

```bash
./gradlew assembleDebug     # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # perlu signing env kalau mau signed
./gradlew installDebug
```

`debug.keystore` **sengaja di-commit** → SHA-1 konsisten → Maps API restriction stabil.

## 🗝️ Maps Key (4 lapis prioritas)

`app/build.gradle.kts` → `manifestPlaceholders["MAPS_API_KEY"]`:

1. `local.properties` (`MAPS_API_KEY=...`, tak di-commit) 🥇
2. `gradle.properties` / `-P` (ter-commit, test key)
3. env `MAPS_API_KEY` (CI secret)
4. `""` (build jalan, peta blank)

## 🤖 CI (`.github/workflows/build-apk.yml`)

> Semua build resmi jalan di GitHub Actions — jangan build rilis di env lokal.

- Trigger: push `main` / tag `v*` (+ manual dispatch). Ignore `**.md`, `patches/**` (ignore tidak berlaku untuk tag).
- Runner: `ubuntu-latest`, JDK 17 Temurin, Gradle cache, timeout 45 mnt.
- Step kunci: `sed -i /aapt2FromMavenOverride/d gradle.properties` (hapus override Termux khusus CI).
- Artifact: **`zrun-debug-apk`** (14 hari). Step release **dimatikan** (hemat runner).
- Cara ambil: tab **Actions** → run hijau → **Artifacts** → install di HP.

## 🚀 Rilis ke GitHub Release

1. Naikkan `versionCode`/`versionName` ikut konvensi di bawah, commit + push ke `main`.
2. Buat tag yang sama dengan versionName:
   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```
3. CI otomatis: build debug → buat **Release** `v1.0.0` → lampirkan `zrun-v1.0.0-debug.apk` (langsung bisa di-install).
4. Kalau tag di-push ulang / release sudah ada: APK di-upload ulang (`--clobber`).

> APK rilis = varian **debug** (signed `debug.keystore`, langsung ter-install).
> Belum ada signing release-store; kalau butuh APK R8/signed Play, aktifkan
> kembali step `assembleRelease` + secrets `ZMUSIC_KEYSTORE_*`.

## 🔢 Versioning (Wajib Naik!)

```kotlin
// app/build.gradle.kts
versionCode = 100
versionName = "1.0.0"
// Konvensi: X.Y.Z → X*100 + Y*10 + Z
// 1.0.1 → 101, 1.1.0 → 110, 2.0.0 → 200
```

Android menolak update kalau `versionCode` tidak naik → user terpaksa uninstall → **data Room hilang**. `BuildConfig.VERSION_NAME` dipakai layar About/Settings (jangan hardcode).

## ✅ Checklist Rilis

- [ ] 🔢 Naik `versionCode/Name` ikut konvensi
- [ ] 🗄️ Kalau ubah Room: versi + Migration + daftar di `AppModule`
- [ ] 🗺️ Maps key valid untuk `com.zaaam.zrun` + SHA-1 release
- [ ] 🐞 `assembleDebug` hijau lokal
- [ ] 🚀 Push → CI hijau → unduh artifact → tes di HP fisik (GPS + musik + peta)
- [ ] 📦 `assembleRelease` (signed) → tes R8 (musik sering kena ProGuard!)

⏭️ Mau ikut ngoding? → [🤝 Contributing](10-contributing.md).
