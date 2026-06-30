import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// FITUR STRAVA: Maps API key. Prioritas baca:
//   1) local.properties (lokal, tak di-commit)  2) gradle.properties / -P (ter-commit)
//   3) env MAPS_API_KEY (CI secret)             4) "" (build tetap jalan, peta blank)
val mapsApiKey: String = run {
    val local = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    local.getProperty("MAPS_API_KEY")
        ?: (project.findProperty("MAPS_API_KEY") as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv("MAPS_API_KEY")
        ?: ""
}

// Firebase: plugin google-services HANYA di-apply kalau google-services.json ADA.
// Plugin ini menggagalkan build kalau di-apply tanpa file itu — jadi JANGAN ditaruh
// di blok plugins{} di atas. CI/debug (tanpa file) tetap build; Firebase jadi inert
// di runtime (lihat AnalyticsManager yang nge-guard FirebaseApp.getApps()).
// File ditaruh user di app/google-services.json (di-generate dari Firebase Console).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// StringFog — obfuscate string literal di package app kita (bukan NewPipe/lib).
// Catatan: TIDAK melindungi Maps API key (key dibaca dari AndroidManifest, bukan
// string di kode). Proteksi key tetap via restriction di Google Cloud.
apply(plugin = "com.github.megatronking.stringfog")
extensions.configure<com.github.megatronking.stringfog.plugin.StringFogExtension>("stringfog") {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    fogPackages = arrayOf("com.zaaam.Zmusic")
}

android {
    namespace = "com.zaaam.Zmusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zaaam.Zmusic"
        minSdk = 26
        targetSdk = 35
        // versionCode WAJIB naik tiap rilis — Android menolak update APK dengan
        // versionCode <= yang terpasang (user terpaksa uninstall = data Room hilang).
        // Konvensi: versionName X.Y.Z -> versionCode XYZ (2.2.1 -> 221)
        versionCode = 225
        versionName = "2.2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Disuntik ke AndroidManifest sebagai ${MAPS_API_KEY}
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    // Debug keystore TETAP (di-commit) → SHA-1 konsisten di semua build CI,
    // sehingga Maps API key bisa dibatasi ke package + SHA-1 ini di Google Cloud.
    signingConfigs {
        create("debugFixed") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // ── Signing release via env (untuk CI) ──────────────────────────────────
    // Hanya aktif kalau env ZMUSIC_KEYSTORE_PATH tersedia (di GitHub Actions,
    // di-set dari Secrets). Build lokal/HP TANPA env ini berperilaku persis
    // seperti sebelumnya: release unsigned.
    val ciKeystorePath: String? = System.getenv("ZMUSIC_KEYSTORE_PATH")
    if (ciKeystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(ciKeystorePath)
                storePassword = System.getenv("ZMUSIC_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ZMUSIC_KEY_ALIAS")
                keyPassword = System.getenv("ZMUSIC_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugFixed")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        // Generate BuildConfig.VERSION_NAME → dipakai layar About & Settings
        // supaya versi yang tampil selalu ikut versionName (tak lagi hardcode/meleset).
        buildConfig = true
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}

// Semua versi dari gradle/libs.versions.toml — JANGAN hardcode versi di sini.
// Versi di toml sudah disamakan dengan versi yang terbukti nge-build (tidak ada upgrade).
dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AppCompat
    implementation(libs.androidx.appcompat)

    // Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // NewPipeExtractor
    implementation(libs.newpipe.extractor)

    // OkHttp
    implementation(libs.okhttp)

    // Image loader
    implementation(libs.coil.compose)

    // Palette API
    implementation(libs.androidx.palette)

    // Compose BOM (ui/material3/icons tanpa versi — diatur BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // StringFog runtime (decrypt string yang di-obfuscate saat build)
    implementation("com.github.megatronking.stringfog:xor:5.0.0")

    // FITUR STRAVA — lokasi (FusedLocationProvider) + Google Maps Compose
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Firebase Analytics (versi diatur Firebase BoM). Lihat AnalyticsManager —
    // dependency aman ada walau google-services.json belum dipasang (inert).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    debugImplementation(libs.androidx.ui.tooling)
}