package com.zaaam.Zmusic.data

import android.app.Application
import android.util.Log
import com.zaaam.Zmusic.ZmusicApp
import com.zaaam.Zmusic.data.local.PlayHistoryDao
import com.zaaam.Zmusic.data.local.PlaylistDao
import com.zaaam.Zmusic.data.local.SearchHistoryDao
import com.zaaam.Zmusic.data.local.SongDao
import com.zaaam.Zmusic.model.Mood
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.MoodStatResult
import com.zaaam.Zmusic.model.entity.PlayHistoryEntity
import com.zaaam.Zmusic.model.entity.PlaylistEntity
import com.zaaam.Zmusic.model.entity.PlaylistSongCrossRef
import com.zaaam.Zmusic.model.entity.PlaylistWithSongs
import com.zaaam.Zmusic.model.entity.SearchHistoryEntity
import com.zaaam.Zmusic.model.entity.TopArtistResult
import com.zaaam.Zmusic.model.entity.TopSongResult
import com.zaaam.Zmusic.model.entity.RecentSongResult
import com.zaaam.Zmusic.model.entity.toEntity
import com.zaaam.Zmusic.model.entity.toSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private data class MoodKeyword(val word: String, val weight: Float)

@Singleton
class MusicRepository @Inject constructor(
    private val app: Application,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val playHistoryDao: PlayHistoryDao,
    private val searchHistoryDao: SearchHistoryDao
) {

    private fun ensureNewPipeReady() {
        val zmusicApp = app as? ZmusicApp ?: return
        if (!zmusicApp.isNewPipeReady) {
            zmusicApp.initNewPipe()
        }
    }

    // FIX #20: Regex dicompile sekali saat init, bukan setiap panggilan fungsi
    private val videoIdRegex = Regex("""(?:v=|youtu\.be/|/v/)([\w-]{11})""")
    private val splitRegex = Regex("""[\s,;:.!?()\[\]"'\-/]+""")

    private fun extractVideoId(url: String): String? =
        videoIdRegex.find(url)?.groupValues?.get(1)

    private fun StreamInfoItem.toSong(): Song? {
        val videoId = extractVideoId(url) ?: return null
        val hdThumbnail = "https://img.youtube.com/vi/$videoId/sddefault.jpg"
        return Song(
            id = videoId,
            title = name ?: return null,
            artist = uploaderName?.removeSuffix(" - Topic") ?: "Unknown",
            thumbnailUrl = hdThumbnail,
            duration = duration * 1000L
        )
    }

    // ── NewPipe: Search ────────────────────────────────────────────────────

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        ensureNewPipeReady()
        val youtubeService = NewPipe.getService(ServiceList.YouTube.serviceId)
        val searchHandler = youtubeService.searchQHFactory
            .fromQuery(query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), "")
        val searchInfo = SearchInfo.getInfo(youtubeService, searchHandler)

        // BUG FIX: Sebelumnya .take(20) memotong hasil secara paksa, dan hanya
        // 1 halaman yang di-fetch (~20 item). Sekarang fetch hingga 3 halaman
        // agar hasil pencarian bisa mencapai ~50-60 lagu.
        val allItems = mutableListOf<StreamInfoItem>()
        allItems.addAll(searchInfo.relatedItems.filterIsInstance<StreamInfoItem>())

        var nextPage: Page? = searchInfo.nextPage
        var pagesLoaded = 1
        while (nextPage != null && pagesLoaded < 3 && allItems.size < 50) {
            try {
                val morePage = SearchInfo.getMoreItems(youtubeService, searchHandler, nextPage)
                allItems.addAll(morePage.items.filterIsInstance<StreamInfoItem>())
                nextPage = morePage.nextPage
                pagesLoaded++
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                break // Gagal load halaman berikutnya — kembalikan yang sudah ada
            }
        }

        allItems
            .mapNotNull { it.toSong() }
            .distinctBy { it.id }
    }

    // ── NewPipe: Stream URL ────────────────────────────────────────────────

    suspend fun getStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
        val localPath = songDao.getLocalPath(videoId)
        if (localPath != null && File(localPath).exists()) return@withContext localPath

        ensureNewPipeReady()
        try {
            val youtubeService = NewPipe.getService(ServiceList.YouTube.serviceId)
            val streamInfo = StreamInfo.getInfo(
                youtubeService,
                "https://www.youtube.com/watch?v=$videoId"
            )

            val streams = streamInfo.audioStreams.filter { it.content != null }
            val bestStream = streams.filter { it.averageBitrate > 0 }
                .maxByOrNull { it.averageBitrate }
                ?: streams.firstOrNull()

            // ── DIAGNOSTIK ─────────────────────────────────────────────────
            Log.d(
                "ZmusicService",
                "getStreamUrl $videoId — totalAudio=${streamInfo.audioStreams.size}, " +
                    "lolosFilter=${streams.size}, " +
                    "picked=${bestStream?.deliveryMethod}, " +
                    "isUrl=${bestStream?.isUrl}, " +
                    "bitrate=${bestStream?.averageBitrate}, " +
                    "format=${bestStream?.format}, " +
                    "url=${bestStream?.content?.take(80)}"
            )

            // ── ERRORS PER-CLIENT (ditelan diam-diam oleh extractor) ───────
            Log.d(
                "ZmusicService",
                "getStreamUrl $videoId — videoStreams=${streamInfo.videoStreams.size}, " +
                    "videoOnly=${streamInfo.videoOnlyStreams.size}, " +
                    "hlsUrl=${!streamInfo.hlsUrl.isNullOrEmpty()}, " +
                    "dashMpd=${!streamInfo.dashMpdUrl.isNullOrEmpty()}"
            )
            streamInfo.errors.forEachIndexed { i, err ->
                Log.w("ZmusicService", "getStreamUrl $videoId — extractorError[$i]: " +
                    "${err.javaClass.simpleName}: ${err.message}")
            }

            bestStream?.content
                ?: run {
                    // ── FALLBACK MUXED ─────────────────────────────────────
                    // Tanpa poToken, YouTube sering balikin response "degraded":
                    // audio-only dihilangkan tapi muxed stream (video+audio,
                    // biasanya itag 18 / 360p) masih ada. ExoPlayer bisa
                    // memutarnya dan audio tetap keluar.
                    // Trade-off: bandwidth lebih besar (ikut narik track video).
                    val muxed = streamInfo.videoStreams
                        .filter { it.content != null }
                        .minByOrNull { it.height }  // resolusi terendah = hemat kuota
                    if (muxed != null) {
                        Log.w(
                            "ZmusicService",
                            "getStreamUrl $videoId — audio-only kosong, FALLBACK ke muxed ${muxed.resolution}"
                        )
                    }
                    muxed?.content
                }
                ?: throw Exception("Tidak ada stream audio tersedia untuk video ini")
        } catch (e: Exception) {
            // Caller (MusicService) menelan exception ini, jadi kita log di sini
            Log.e("ZmusicService", "getStreamUrl GAGAL untuk videoId=$videoId", e)
            throw e
        }
    }

    // ── NewPipe: Discovery ─────────────────────────────────────────────────

    suspend fun getDiscovery(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val kiosk = getTrending()
            if (kiosk.isNotEmpty()) return@withContext kiosk
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ZmusicService", "getTrending gagal — fallback ke discovery via search", e)
        }
        getDiscoveryFromSearch()
    }

    suspend fun getTrending(): List<Song> = withContext(Dispatchers.IO) {
        ensureNewPipeReady()
        val youtubeService = NewPipe.getService(ServiceList.YouTube.serviceId)
        val kioskList = youtubeService.kioskList
        val kioskId = when {
            "Trending" in kioskList.availableKiosks -> "Trending"
            else -> kioskList.defaultKioskId
        }
        val kioskExtractor = kioskList.getExtractorById(kioskId, null)
        kioskExtractor.fetchPage()
        kioskExtractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .filter { it.duration in 30..900 }
            .mapNotNull { it.toSong() }
            .distinctBy { it.id }
            .take(50)
    }

    suspend fun searchByGenre(queries: List<String>): List<Song> = coroutineScope {
        queries.map { query ->
            async {
                try {
                    search(query)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ZmusicService", "searchByGenre: query \"$query\" gagal", e)
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }.take(25)
    }

    private suspend fun getDiscoveryFromSearch(): List<Song> = coroutineScope {
        ensureNewPipeReady()
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val queries = listOf("lagu populer $year", "top hits indonesia", "musik trending terbaru")
        queries.map { query ->
            async {
                try {
                    val youtubeService = NewPipe.getService(ServiceList.YouTube.serviceId)
                    val searchHandler = youtubeService.searchQHFactory
                        .fromQuery(query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), "")
                    val searchInfo = SearchInfo.getInfo(youtubeService, searchHandler)
                    searchInfo.relatedItems
                        .filterIsInstance<StreamInfoItem>()
                        .mapNotNull { it.toSong() }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ZmusicService", "getDiscoveryFromSearch: query \"$query\" gagal", e)
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }.take(30)
    }

    // ── Mood-based Search ──────────────────────────────────────────────────

    suspend fun searchByMood(mood: Mood): List<Song> = coroutineScope {
        mood.queries.map { query ->
            async {
                try {
                    search(query)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ZmusicService", "searchByMood(${mood.name}): query \"$query\" gagal", e)
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }.take(25)
    }

    // ── NEW: Artist-based Search ───────────────────────────────────────────
    //
    // Mencari lagu-lagu dari artis tertentu via YouTube.
    // Strategi:
    //   1. Search "$artistName songs" → filter hasil yang artist-nya match
    //   2. Kalau hasil yang match < 3 → kembalikan semua hasil (mungkin nama artis
    //      YouTube berbeda dengan nama di metadata, misal "NOAH - Topic")
    //   3. Take max 30 lagu untuk queue yang cukup kaya
    // CATATAN ERROR HANDLING: fungsi ini sengaja MELEMPAR exception saat query
    // utama gagal (misal jaringan mati). ArtistViewModel.loadArtist() sudah punya
    // try/catch -> ArtistScreenState.Error + tombol retry. Sebelumnya exception
    // ditelan jadi emptyList() sehingga "internet mati" tampil sebagai
    // "artis tidak ditemukan" — menyesatkan user.
    suspend fun searchByArtist(artistName: String): List<Song> = withContext(Dispatchers.IO) {
        val rawResults = search("$artistName official songs")
        // Normalisasi: hapus " - Topic" suffix yang ditambah YouTube
        val artistNormalized = artistName.lowercase()
            .removeSuffix(" - topic").trim()

        val filtered = rawResults.filter { song ->
            val songArtist = song.artist.lowercase()
                .removeSuffix(" - topic").trim()
            songArtist.contains(artistNormalized) ||
            artistNormalized.contains(songArtist) ||
            // Cek nama pertama artis saja (misal "Tulus" dari "Tulus - Topic")
            songArtist.split(" ").first() == artistNormalized.split(" ").first()
        }

        // Kalau hasil filter terlalu sedikit, coba query tambahan
        if (filtered.size >= 3) {
            filtered.take(30)
        } else {
            // Coba query yang lebih spesifik — kegagalan query tambahan tidak fatal
            // (hasil query utama sudah ada), cukup di-log
            val moreResults = try {
                search("$artistName lagu terbaru")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ZmusicService", "searchByArtist: query tambahan gagal untuk \"$artistName\"", e)
                emptyList()
            }

            (rawResults + moreResults)
                .distinctBy { it.id }
                .take(30)
        }
    }

    // ── Mood-Compatible Queue Filtering ───────────────────────────────────
    //
    // Menyaring lagu dari queue agar hanya yang mood-nya kompatibel dengan
    // mood target yang dimasukkan. Menggunakan moodProximity map sebagai
    // referensi kompatibilitas mood.
    //
    // Contoh: target SAD → kompatibel dengan SAD, CHILL, ROMANCE, HAPPY
    //         → lagu HYPE dan ENERGETIC dibuang dari queue
    fun filterMoodCompatible(songs: List<Song>, targetMood: String): List<Song> {
        val compatibleMoods = moodProximity[targetMood.uppercase()] ?: return songs
        return songs.filter { song ->
            val detectedMood = detectMoodByKeyword(song) ?: return@filter false
            detectedMood.uppercase() in compatibleMoods
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ── HYBRID MOOD DETECTION ────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun detectMoodHybrid(
        song: Song,
        sourceQuery: String? = null,
        moodScreenHint: String? = null
    ): String? {
        val keywordMood = detectMoodByKeyword(song)
        if (keywordMood != null) return keywordMood

        val historyMood = detectMoodByHistory(song.id)
        if (historyMood != null) return historyMood

        if (sourceQuery != null) {
            val queryMood = detectMoodByQueryContext(sourceQuery)
            if (queryMood != null) return queryMood
        }

        if (moodScreenHint != null) return moodScreenHint

        // FIX: Sebelumnya return null sehingga kolom mood di play_history kosong
        // -> query "WHERE mood IS NOT NULL" return 0 baris -> Mood Detector selalu
        // tampil "Belum ada data mood" meski sudah banyak lagu diputar.
        //
        // Fallback ke CHILL (mood paling netral) agar setiap play tetap punya
        // mood value -> Mood Detector langsung bisa menampilkan data.
        // Mood ini dioverride oleh deteksi keyword/history di play berikutnya
        // jika lagu sudah punya konteks mood yang lebih akurat.
        return Mood.CHILL.name
    }

    // ── Layer 1: Weighted Keyword Matching ────────────────────────────────

    private val moodKeywordsWeighted: Map<Mood, List<MoodKeyword>> = mapOf(

        Mood.SAD to buildList {
            addAll(listOf(
                "sedih", "galau", "kecewa", "patah hati", "menangis", "tangis",
                "lonely", "broken", "tears", "cry", "hurt", "sakit hati",
                "heartbreak", "hancur", "pilu", "derita", "duka", "menyesal",
                "nelangsa", "terluka", "pedih"
            ).map { MoodKeyword(it, 2.0f) })
            addAll(listOf(
                "maaf", "pergi", "hujan", "gelap", "dingin", "lupa", "pisah",
                "jauh", "tunggu", "hilang", "sesal", "diam", "akhir", "pupus",
                "rapuh", "hampa", "sendiri", "sunyi", "sepi", "rindu",
                "kehilangan", "perih", "lara", "nestapa", "kepedihan",
                "berakhir", "usai", "pulang", "tinggal", "benci",
                "malam", "terakhir", "kenangan", "ingat", "bayangan",
                "menghilang", "sirna", "pudar", "luka", "sakitnya",
                "kecewaku", "air mata", "tangisan", "sesak"
            ).map { MoodKeyword(it, 1.0f) })
        },

        Mood.ROMANCE to buildList {
            addAll(listOf(
                "cinta", "sayang", "romantis", "love", "kasih", "jatuh cinta",
                "miss you", "sweetheart", "darling", "dear",
                "kekasih", "valentine", "mesra", "peluk",
                "satu hati", "belahan", "i love you", "sayangku"
            ).map { MoodKeyword(it, 2.0f) })
            addAll(listOf(
                "bunga", "indah", "bintang", "kamu", "hatiku", "mimpi",
                "berdua", "duet", "pacar", "hati", "senja",
                "matahari", "cantik", "rindu", "dekat", "genggam",
                "setia", "selamanya", "bersamamu", "cintaku", "sayangku",
                "jiwaku", "nafas", "bidadari", "surgaku", "duniaku",
                "bahagia", "cintai", "mencinta", "tersayang", "kaulah",
                "engkau", "dirimu", "aku dan kamu", "kita", "berdansa",
                "memeluk", "hangat", "teduh", "forever", "always",
                "beautiful", "angel", "heart", "kiss"
            ).map { MoodKeyword(it, 1.0f) })
        },

        Mood.HAPPY to buildList {
            addAll(listOf(
                "happy", "ceria", "senang", "bahagia", "gembira", "senyum",
                "sunshine", "joy", "riang", "cheerful", "wonderful",
                "semangat pagi", "good day", "good life", "tawa"
            ).map { MoodKeyword(it, 2.0f) })
            addAll(listOf(
                "tersenyum", "teman", "hari", "dunia", "hidup", "baru",
                "pesta", "rayakan", "syukur", "bersyukur", "indahnya",
                "cerah", "terang", "canda", "tertawa", "bermain",
                "asyik", "mantap", "seru", "keren", "yuk",
                "ayo", "mari", "datang", "sambut", "penuh",
                "warna", "pelangi", "cahaya", "mentari",
                "smile", "bright", "blessed", "grateful", "alive",
                "celebrate", "amazing", "good", "best", "great"
            ).map { MoodKeyword(it, 1.0f) })
        },

        Mood.CHILL to buildList {
            addAll(listOf(
                "santai", "lofi", "relax", "tenang", "calm", "mellow",
                "chill", "dreamy", "peaceful", "lullaby", "damai", "hening"
            ).map { MoodKeyword(it, 2.0f) })
            addAll(listOf(
                "pulang", "rumah", "kopi", "pagi", "cerita", "tidur",
                "sore", "langit", "awan", "angin", "laut", "pantai",
                "taman", "duduk", "diam", "pelan", "slow",
                "easy", "soft", "gentle", "quiet", "rest",
                "sleep", "night", "evening", "morning",
                "melamun", "menatap", "berharap", "biarkan",
                "terlelap", "bermimpi"
            ).map { MoodKeyword(it, 1.0f) })
            addAll(listOf(
                "acoustic", "cover", "instrumental", "piano", "gitar",
                "guitar", "unplugged", "stripped", "session", "live",
                "jazz", "bossa", "lo-fi", "ambient"
            ).map { MoodKeyword(it, 0.5f) })
        },

        Mood.ENERGETIC to buildList {
            addAll(listOf(
                "semangat", "energetic", "power", "strong", "energy",
                "unstoppable", "warrior", "pejuang", "fire", "beast"
            ).map { MoodKeyword(it, 2.0f) })
            addAll(listOf(
                "bangkit", "maju", "juara", "menang", "lari", "berlari",
                "kuat", "pantang", "tangguh", "gagah", "berani",
                "perjuangan", "berjuang", "melawan", "bertahan",
                "workout", "gym", "pump", "run", "sport", "fight",
                "olahraga", "latihan", "push", "harder",
                "never give up", "rise", "stand", "glory",
                "rock", "metal", "hardcore"
            ).map { MoodKeyword(it, 1.0f) })
        },

        Mood.HYPE to buildList {
            addAll(listOf(
                "party", "hype", "dance", "rave", "turn up", "lit",
                "dugem", "goyang", "joget", "disco", "nonstop"
            ).map { MoodKeyword(it, 2.0f) })
            addAll(listOf(
                "dj", "bass", "drop", "edm", "remix", "club",
                "trending", "viral", "tiktok", "challenge",
                "banger", "bounce", "beats", "mixtape",
                "nge-hits", "gaspol", "gas", "gaskeun",
                "asik", "pecah", "meledak", "liar", "gila"
            ).map { MoodKeyword(it, 1.0f) })
            addAll(listOf(
                "koplo", "dangdut", "funkot", "breakbeat", "house",
                "techno", "trap", "reggaeton"
            ).map { MoodKeyword(it, 0.5f) })
        }
    )

    // FIX #11: Pre-build index untuk single-word lookup O(words) bukan O(keywords)
    private val singleWordIndex: Map<String, MutableList<Pair<Mood, Float>>> = buildMap {
        for ((mood, keywords) in moodKeywordsWeighted) {
            for (kw in keywords) {
                if (!kw.word.contains(' ')) {
                    getOrPut(kw.word) { mutableListOf() }.add(mood to kw.weight)
                }
            }
        }
    }

    private val multiWordKeywords: Map<Mood, List<MoodKeyword>> = moodKeywordsWeighted.mapValues { (_, keywords) ->
        keywords.filter { it.word.contains(' ') }
    }

    fun detectMoodByKeyword(song: Song): String? {
        val text = "${song.title} ${song.artist}".lowercase()
        val words = text.split(splitRegex).toSet()

        val scores = FloatArray(Mood.entries.size)

        // FIX #11: Single-word lookup via pre-built index — O(words) bukan O(all keywords)
        for (word in words) {
            singleWordIndex[word]?.forEach { (mood, weight) ->
                scores[mood.ordinal] += weight
            }
        }

        // Multi-word: masih perlu contains() tapi jumlahnya jauh lebih sedikit
        for ((mood, keywords) in multiWordKeywords) {
            for (kw in keywords) {
                if (text.contains(kw.word)) {
                    scores[mood.ordinal] += kw.weight
                }
            }
        }

        var bestMood: Mood? = null
        var bestScore = 0f
        for (mood in Mood.entries) {
            if (scores[mood.ordinal] > bestScore) {
                bestScore = scores[mood.ordinal]
                bestMood = mood
            }
        }

        return if (bestScore >= 1.0f) bestMood?.name else null
    }

    // ── Layer 2: Query/Genre Context ──────────────────────────────────────

    private val queryMoodMap: Map<String, String> = buildMap {
        val mappings = mapOf(
            Mood.HAPPY to listOf("happy", "ceria", "semangat pagi", "fun", "cheerful"),
            Mood.SAD to listOf("sedih", "galau", "broken heart", "sad song", "lagu sedih"),
            Mood.ENERGETIC to listOf("workout", "gym", "semangat", "pump", "sport", "olahraga"),
            Mood.CHILL to listOf("santai", "lofi", "relax", "chill", "acoustic", "senja", "malam"),
            Mood.ROMANCE to listOf("romantis", "love song", "romantic", "cinta", "sayang"),
            Mood.HYPE to listOf("party", "hype", "dance", "dj", "remix", "dugem", "edm",
                "trending", "viral", "tiktok", "hits", "populer", "koplo", "dangdut")
        )
        for ((mood, keywords) in mappings) {
            for (keyword in keywords) {
                put(keyword, mood.name)
            }
        }
    }

    private fun detectMoodByQueryContext(query: String): String? {
        val queryLower = query.lowercase()
        val queryWords = queryLower.split(splitRegex).toSet()

        val moodCounts = mutableMapOf<String, Int>()
        for ((keyword, mood) in queryMoodMap) {
            val matched = if (keyword.contains(' ')) {
                queryLower.contains(keyword)
            } else {
                keyword in queryWords
            }
            if (matched) {
                moodCounts[mood] = (moodCounts[mood] ?: 0) + 1
            }
        }

        return moodCounts.maxByOrNull { it.value }?.key
    }

    // ── Layer 3: History-based Detection ──────────────────────────────────

    private suspend fun detectMoodByHistory(songId: String): String? {
        val directMood = playHistoryDao.getMostFrequentMoodForSong(songId)
        if (directMood != null) return directMood.mood

        val lastPlayed = playHistoryDao.getLastPlayedAt(songId) ?: return null
        val sessionWindow = 30 * 60 * 1000L
        val sessionMood = playHistoryDao.getSessionMood(
            sessionStart = lastPlayed - sessionWindow,
            sessionEnd = lastPlayed + sessionWindow,
            excludeSongId = songId
        )
        return sessionMood?.mood
    }

    fun detectMood(song: Song): String? = detectMoodByKeyword(song)

    // ── Smart Shuffle Helpers ─────────────────────────────────────────────

    val moodProximity: Map<String, List<String>> = mapOf(
        "CHILL"     to listOf("CHILL", "ROMANCE", "SAD", "HAPPY"),
        "ROMANCE"   to listOf("ROMANCE", "CHILL", "SAD", "HAPPY"),
        "HAPPY"     to listOf("HAPPY", "ROMANCE", "ENERGETIC", "CHILL"),
        "ENERGETIC" to listOf("ENERGETIC", "HAPPY", "HYPE", "ROMANCE"),
        "HYPE"      to listOf("HYPE", "ENERGETIC", "HAPPY", "ROMANCE"),
        "SAD"       to listOf("SAD", "CHILL", "ROMANCE", "HAPPY")
    )

    suspend fun getRecentMoodPattern(count: Int = 20): List<MoodStatResult> =
        playHistoryDao.getRecentMoodPattern(count)

    fun detectMoodBatch(songs: List<Song>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (song in songs) {
            val mood = detectMoodByKeyword(song)
            if (mood != null) {
                result[song.id] = mood
            }
        }
        return result
    }

    // ── Offline: Download Management ───────────────────────────────────────

    suspend fun clearLocalPath(songId: String) {
        songDao.updateLocalPath(songId, null)
    }

    fun getDownloadedSongs(): Flow<List<Song>> =
        songDao.getDownloadedSongs().map { list -> list.map { it.toSong() } }

    // ── Playlist ───────────────────────────────────────────────────────────

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insert(PlaylistEntity(name = name))

    suspend fun addToPlaylist(playlistId: Long, song: Song) {
        songDao.insertOrIgnore(song.toEntity())
        val position = playlistDao.getSongCount(playlistId)
        playlistDao.addSong(PlaylistSongCrossRef(playlistId, song.id, position))
    }

    suspend fun removeFromPlaylist(playlistId: Long, songId: String) =
        playlistDao.removeSong(playlistId, songId)

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs> =
        playlistDao.getPlaylistWithSongs(playlistId)

    suspend fun deletePlaylist(playlistId: Long) = playlistDao.delete(playlistId)

    // ── Play History / Stats ───────────────────────────────────────────────

    suspend fun recordPlay(
        song: Song,
        durationListened: Long,
        moodScreenHint: String? = null,
        sourceQuery: String? = null
    ) {
        if (durationListened < 10_000) return
        val resolvedMood = detectMoodHybrid(song, sourceQuery, moodScreenHint)
        playHistoryDao.insert(
            PlayHistoryEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationListened = durationListened,
                mood = resolvedMood,
                sourceQuery = sourceQuery
            )
        )
    }

    fun getTotalPlays(): Flow<Int> = playHistoryDao.getTotalPlays()
    fun getTotalDuration(): Flow<Long?> = playHistoryDao.getTotalDuration()
    fun getTopSongs(limit: Int = 5): Flow<List<TopSongResult>> = playHistoryDao.getTopSongs(limit)
    fun getTopArtists(limit: Int = 3): Flow<List<TopArtistResult>> = playHistoryDao.getTopArtists(limit)

    // FIX POTENSI #2: Date-filtered variants untuk WrappedViewModel
    fun getTotalPlaysBetween(startMs: Long, endMs: Long): Flow<Int> =
        playHistoryDao.getTotalPlaysBetween(startMs, endMs)
    fun getTotalDurationBetween(startMs: Long, endMs: Long): Flow<Long?> =
        playHistoryDao.getTotalDurationBetween(startMs, endMs)
    fun getTopSongsBetween(startMs: Long, endMs: Long, limit: Int = 5): Flow<List<TopSongResult>> =
        playHistoryDao.getTopSongsBetween(startMs, endMs, limit)
    fun getTopArtistsBetween(startMs: Long, endMs: Long, limit: Int = 3): Flow<List<TopArtistResult>> =
        playHistoryDao.getTopArtistsBetween(startMs, endMs, limit)
    fun getMoodDistributionBetween(startMs: Long, endMs: Long): Flow<List<MoodStatResult>> =
        playHistoryDao.getMoodDistributionBetween(startMs, endMs)

    fun getMoodDistribution(): Flow<List<MoodStatResult>> = playHistoryDao.getMoodDistribution()
    fun getRecentMood(): Flow<MoodStatResult?> = playHistoryDao.getRecentMood()

    // FIX #3: Pakai JOIN query, bukan N+1 individual getDuration() per song
    fun getRecentSongs(limit: Int = 6): Flow<List<Song>> =
        playHistoryDao.getRecentlyPlayedWithDuration(limit).map { list ->
            list.map { recent ->
                Song(
                    id = recent.songId,
                    title = recent.title,
                    artist = recent.artist,
                    thumbnailUrl = recent.thumbnailUrl,
                    duration = recent.originalDuration ?: 0L
                )
            }
        }

    suspend fun deleteHistoryBySongId(songId: String) =
        playHistoryDao.deleteBySongId(songId)

    suspend fun clearAllHistory() =
        playHistoryDao.clearAll()

    // ── Wrapper methods untuk ViewModel & DailyMixGenerator ───────────────

    fun getRecentlyPlayedRaw(limit: Int = 50): Flow<List<RecentSongResult>> =
        playHistoryDao.getRecentlyPlayed(limit)

    suspend fun getTopSongsOnce(limit: Int = 8): List<Song> =
        playHistoryDao.getTopSongs(limit).first().map { top ->
            Song(
                id = top.songId,
                title = top.title,
                artist = top.artist,
                thumbnailUrl = top.thumbnailUrl,
                duration = 0L
            )
        }

    // ── Smart Playlist Generation ──────────────────────────────────────────
    // Semua fungsi ini membangun playlist dari data play_history lokal.
    // Tidak perlu network — instant dan offline-friendly.

    /** Top Picks: lagu paling sering diputar, diurutkan play count desc. */
    suspend fun generateTopPicksPlaylist(limit: Int = 30): List<Song> =
        playHistoryDao.getTopSongs(limit).first().map { top ->
            Song(id = top.songId, title = top.title, artist = top.artist,
                 thumbnailUrl = top.thumbnailUrl, duration = 0L)
        }

    /**
     * Mood Mix: ambil mood paling dominan dari 50 play terakhir,
     * lalu ambil lagu-lagu yang pernah diputar dengan mood itu.
     */
    suspend fun generateMoodMixPlaylist(limit: Int = 30): List<Song> {
        val dominantMood = playHistoryDao.getRecentMoodPattern(50)
            .maxByOrNull { it.playCount }?.mood ?: return emptyList()

        return playHistoryDao.getSongsByMood(dominantMood, limit).map { row ->
            Song(id = row.songId, title = row.title, artist = row.artist,
                 thumbnailUrl = row.thumbnailUrl, duration = 0L)
        }
    }

    /**
     * Recent Faves: lagu yang dimainkan dalam 30 hari terakhir
     * dan sudah diputar ≥ 2 kali — "baru tapi suka banget".
     */
    suspend fun generateRecentFavesPlaylist(limit: Int = 30): List<Song> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        return playHistoryDao.getRecentFaveSongs(thirtyDaysAgo, minPlays = 2, limit = limit)
            .map { row ->
                Song(id = row.songId, title = row.title, artist = row.artist,
                     thumbnailUrl = row.thumbnailUrl, duration = 0L)
            }
    }

    // ── Search History ─────────────────────────────────────────────────────

    suspend fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        searchHistoryDao.upsertSearchQuery(trimmed)
    }

    fun getSearchHistory() = searchHistoryDao.getRecentHistory()

    suspend fun deleteSearchHistoryById(id: Long) =
        searchHistoryDao.deleteById(id)

    suspend fun clearSearchHistory() =
        searchHistoryDao.clearAll()
}
