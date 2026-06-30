# ── NewPipeExtractor: Rhino JavaScript Engine ─────────────────────────────────
# WAJIB! Rhino digunakan untuk mengeksekusi skrip deobfuscasi parameter 'n' YouTube.
# Tanpa ini, getStreamUrl() akan gagal total di build release.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

# Rhino mereferensikan API JDK desktop yang TIDAK ada di Android dan TIDAK pernah
# dipakai di runtime Android (jalur ScriptEngine/dynalink hanya untuk JVM desktop).
# Tanpa dontwarn ini, R8 GAGAL total di assembleRelease: "Missing class java.beans.*"
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.mozilla.javascript.engine.**

# ── NewPipeExtractor: TimeAgo Patterns ────────────────────────────────────────
# WAJIB untuk v0.25.0+! Parsing tanggal relatif ("2 jam lalu") menggunakan
# refleksi ke kelas timeago patterns. Tanpa ini akan crash di build release.
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }

# ── NewPipeExtractor: General ─────────────────────────────────────────────────
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# ── OkHttp ────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Hilt ──────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**
-keep class dagger.hilt.android.internal.** { *; }
-keep class com.zaaam.Zmusic.Hilt_ZmusicApp { *; }
-keep class * extends dagger.hilt.android.internal.managers.* { *; }

# ── Coil ──────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ══════════════════════════════════════════════════════════════════════════════
# FIX: Tambahan keep rules untuk universal Android 10-15 compatibility
# ══════════════════════════════════════════════════════════════════════════════

# ── Room: Entity classes ──────────────────────────────────────────────────────
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# ── Room: Query result data classes (BUKAN @Entity tapi dipakai di @Query) ───
# R8 bisa strip class ini karena "tidak ada referensi langsung" — Room butuh
# class ini utuh untuk mapping result dari SQL query via reflection.
-keep class com.zaaam.Zmusic.model.entity.TopSongResult { *; }
-keep class com.zaaam.Zmusic.model.entity.TopArtistResult { *; }
-keep class com.zaaam.Zmusic.model.entity.MoodStatResult { *; }
-keep class com.zaaam.Zmusic.model.entity.RecentSongResult { *; }
-keep class com.zaaam.Zmusic.model.entity.PlaylistWithSongs { *; }
# NEW: Untuk hybrid mood detection — query result dari getMostFrequentMoodForSong()
-keep class com.zaaam.Zmusic.model.entity.SongMoodHistoryResult { *; }

# ── Model classes (safety net) ────────────────────────────────────────────────
-keep class com.zaaam.Zmusic.model.** { *; }
-keep class com.zaaam.Zmusic.model.entity.** { *; }

# ── Media3 (ExoPlayer + MediaSession) ────────────────────────────────────────
# Media3 pakai reflection internal untuk notification builder & session callbacks.
# Di Android 10-11 class verification lebih ketat, stripped class langsung crash.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Compose Runtime (state restoration) ──────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**
