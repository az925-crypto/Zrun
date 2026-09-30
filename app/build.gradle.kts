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

android {
    namespace = "com.zaaam.Zmusic"
    compileSdk = 35

    defaultConfig {
        // App baru "ZRun" (Strava + musik). Package kode internal tetap
        // com.zaaam.Zmusic; yang menjadikan ini app berbeda di device = applicationId.
        applicationId = "com.zaaam.zrun"
        minSdk = 26
        targetSdk = 35
        // versionCode WAJIB naik tiap rilis — Android menolak update APK dengan
        // versionCode <= yang terpasang (user terpaksa uninstall = data Room hilang).
        // Konvensi: versionName X.Y.Z -> versionCode X*100+Y*10+Z (1.0.0 -> 100)
        versionCode = 102
        versionName = "1.0.2"

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
            // Kalau keystore rilis (ZMUSIC_*) tidak ada, pakai debug keystore yang
            // di-commit — APK tetap ter-install (R8/minify tetap jalan) dan SHA-1
            // konsisten dengan restriksi Maps API key.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debugFixed")
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

    // FITUR STRAVA — lokasi (FusedLocationProvider) + Google Maps Compose
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    debugImplementation(libs.androidx.ui.tooling)
}