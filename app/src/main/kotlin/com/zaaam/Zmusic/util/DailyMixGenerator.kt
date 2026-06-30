package com.zaaam.Zmusic.util

import android.util.Log
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.MoodStatResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DailyMixGenerator — Menghasilkan playlist harian yang cerdas.
 *
 * Algoritma:
 * 1. Ambil top songs dari play history
 * 2. Deteksi mood dominan user hari ini / minggu ini
 * 3. Cari lagu baru sesuai mood dominan
 * 4. Mix: 40% familiar (top songs) + 40% mood-based + 20% discovery baru
 * 5. Shuffle dengan anti-cluster mood
 */
@Singleton
class DailyMixGenerator @Inject constructor(
    private val repository: MusicRepository
) {

    companion object {
        private const val TAG = "ZmusicDailyMix"
    }

    data class DailyMix(
        val songs: List<Song>,
        val dominantMood: String?,
        val title: String,
        val subtitle: String
    )

    // FIX POTENSI #4: In-memory cache Daily Mix per hari.
    //
    // MASALAH LAMA: generate() tidak punya caching sama sekali. Setiap kali
    // HomeScreen recompose atau user navigasi kembali ke Home, generate() dipanggil
    // ulang → 3 network request paralel (familiar + mood + discovery) yang
    // identik dengan request sebelumnya. Ini boros bandwidth, lambat, dan bisa
    // memicu rate limiting dari YouTube.
    //
    // FIX: Cache hasil generate() dalam memori dengan key = tanggal hari ini
    // (format "YYYY-MM-DD"). Cache otomatis invalid saat hari berganti karena
    // key-nya berbeda. Tidak perlu timer atau manual invalidation.
    private var cachedDate: String = ""
    private var cachedMix: DailyMix? = null

    private fun todayKey(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}-" +
               "${cal.get(java.util.Calendar.MONTH) + 1}-" +
               "${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    }
    /**
     * Generate Daily Mix, dengan caching per hari.
     * Hasil di-cache dalam memori selama hari yang sama.
     * Cache otomatis invalid saat hari berganti (key berbeda).
     */
    suspend fun generate(): DailyMix = withContext(Dispatchers.IO) {
        // FIX POTENSI #4: Return cache kalau masih hari yang sama.
        // Tanpa ini, generate() membuat 3 network request paralel setiap kali
        // HomeScreen recompose atau user navigasi kembali → boros bandwidth,
        // lambat, dan rentan rate limiting YouTube.
        val today = todayKey()
        cachedMix?.let { if (cachedDate == today) return@withContext it }

        val result = try {
            coroutineScope {
                // 1. Ambil mood pattern terbaru
                val recentPattern = repository.getRecentMoodPattern(30)
                val dominantMood  = recentPattern.maxByOrNull { it.playCount }?.mood

                // 2. Parallel: fetch familiar + mood + discovery
                val familiarDeferred = async {
                    try {
                        val topSongs = repository.getTopSongsOnce(8)
                        topSongs
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "fetch familiar songs gagal", e)
                        emptyList()
                    }
                }

                val moodDeferred = async {
                    if (dominantMood != null) {
                        try {
                            val moodEnum = com.zaaam.Zmusic.model.Mood.values()
                                .firstOrNull { it.name.equals(dominantMood, ignoreCase = true) }
                            if (moodEnum != null) repository.searchByMood(moodEnum).take(12)
                            else emptyList()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(TAG, "fetch mood songs ($dominantMood) gagal", e)
                            emptyList()
                        }
                    } else emptyList()
                }

                val discoveryDeferred = async {
                    try {
                        repository.getDiscovery().take(8)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "fetch discovery songs gagal", e)
                        emptyList()
                    }
                }

                val familiar  = familiarDeferred.await()
                val moodSongs = moodDeferred.await()
                val discovery = discoveryDeferred.await()

                // 3. Combine & deduplicate
                val allIds = mutableSetOf<String>()
                val mixed  = mutableListOf<Song>()

                // 40% familiar
                familiar.forEach { s ->
                    if (allIds.add(s.id)) mixed.add(s)
                }
                // 40% mood
                moodSongs.shuffled().take(10).forEach { s ->
                    if (allIds.add(s.id)) mixed.add(s)
                }
                // 20% discovery
                discovery.shuffled().take(6).forEach { s ->
                    if (allIds.add(s.id)) mixed.add(s)
                }

                // 4. Shuffle dengan anti-cluster
                val moodCache = repository.detectMoodBatch(mixed)
                val shuffled  = smartShuffle(mixed, moodCache)

                // 5. Title & subtitle
                val hour  = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val title = when {
                    hour < 11 -> "Daily Mix Pagi"
                    hour < 15 -> "Daily Mix Siang"
                    hour < 18 -> "Daily Mix Sore"
                    else      -> "Daily Mix Malam"
                }
                val subtitle = when (dominantMood?.lowercase()) {
                    "happy"     -> "Ceria & Semangat buat kamu hari ini"
                    "sad"       -> "Menemanimu di saat tenang"
                    "energetic" -> "Energi penuh sepanjang hari"
                    "chill"     -> "Santai dan mellow untukmu"
                    "romance"   -> "Melodi penuh kasih sayang"
                    "hype"      -> "Nonstop bangers favoritmu"
                    else        -> "${shuffled.size} lagu pilihan untukmu hari ini"
                }

                DailyMix(
                    songs        = shuffled.take(25),
                    dominantMood = dominantMood,
                    title        = title,
                    subtitle     = subtitle
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generate Daily Mix gagal total — tampil fallback kosong", e)
            DailyMix(emptyList(), null, "Daily Mix", "Tidak tersedia")
        }

        // Simpan ke cache hanya jika berhasil dapat lagu (bukan empty fallback)
        if (result.songs.isNotEmpty()) {
            cachedDate = today
            cachedMix = result
        }
        result
    }

    private fun smartShuffle(songs: List<Song>, moodCache: Map<String, String>): List<Song> {
        if (songs.size <= 2) return songs.shuffled()
        val shuffled = songs.toMutableList().apply { shuffle() }
        for (i in 2 until shuffled.size) {
            val mI = moodCache[shuffled[i].id]
            val m1 = moodCache[shuffled[i - 1].id]
            val m2 = moodCache[shuffled[i - 2].id]
            if (mI != null && mI == m1 && mI == m2) {
                val swap = ((i + 1) until shuffled.size)
                    .firstOrNull { moodCache[shuffled[it].id] != mI }
                if (swap != null) {
                    val tmp = shuffled[i]; shuffled[i] = shuffled[swap]; shuffled[swap] = tmp
                }
            }
        }
        return shuffled
    }
}
