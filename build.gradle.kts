// Top-level build file where you can add configuration options common to all sub-projects/modules.

// StringFog: obfuscate string literal di bytecode (proteksi umum saat R8/release).
// Diterapkan via buildscript classpath karena tidak tersedia di Gradle Plugin Portal.
buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("com.github.megatronking.stringfog:gradle-plugin:5.2.0")
        classpath("com.github.megatronking.stringfog:xor:5.0.0")
    }
}

plugins {
    id("com.android.application") version "8.12.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    // apply false = hanya taruh di classpath, TIDAK dijalankan. Aman walau
    // google-services.json belum ada; app/build.gradle.kts yang meng-apply
    // kondisional (lihat di sana). Verifikasi versi terbaru kalau build gagal.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
