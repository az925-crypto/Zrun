# 🗄️ 07 — Database (Room v8, `zmusic.db`)

> 6 tabel: 5 musik + 1 olahraga. `exportSchema=false`. Semua DAO disediakan di `di/AppModule.kt`.

## 📋 Skema

| Tabel | Kolom kunci | Indeks |
|-------|-------------|--------|
| `songs` 🎵 | `id PK, title, artist, thumbnailUrl, duration, localPath?` | — |
| `playlists` 📃 | `id auto, name, createdAt` | — |
| `playlist_songs` 🔗 | `playlistId, songId, position` (composite PK) | `songId` |
| `play_history` 🕘 | `id auto, songId, title, artist, thumbnailUrl, playedAt, durationListened, mood?, sourceQuery?` | `songId`, `playedAt` |
| `search_history` 🔍 | `id auto, query UNIQUE, searchedAt` | unique `query` |
| `activities` 🏃 | `id auto, title, type="Run", startTime, durationMillis, movingMillis default 0, distanceMeters, avgSpeedKmh, route TEXT` | — |

- 🗺️ `route: List<GeoPoint>` ↔ `String "lat,lng,ts;..."` via `RouteConverters`.
- 🏃 `avgPaceSecPerKm` = computed (pakai `movingMillis`, fallback `durationMillis`).

## 🧰 DAO (file di `data/local/`)

- `SongDao` — insertOrIgnore/Replace, getById, delete, `localPath` (offline).
- `PlaylistDao` — insert/delete, `getAllPlaylists Flow`, `getPlaylistWithSongs @Transaction Flow`.
- `PlayHistoryDao` — top songs/artists, recent + WithDuration (JOIN songs), mood stats, Wrapped (`Between`), `deleteBySongId/clearAll`.
- `SearchHistoryDao` — `upsertSearchQuery` (`INSERT OR REPLACE`, atomik berkat unique index).
- `ActivityDao` — `observeAll Flow ORDER BY startTime DESC`, getById, insert→Long, delete.

## 🪜 Migrasi 1→8 (jangan dihapus!)

| v | Perubahan |
|---|-----------|
| 1→2 | `songs.localPath` |
| 2→3 | `play_history.mood` |
| 3→4 | create `search_history` |
| 4→5 | `play_history.sourceQuery` |
| 5→6 | index `play_history(songId/playedAt)` + unique `search_history(query)` |
| 6→7 | create `activities` |
| 7→8 | `activities.movingMillis DEFAULT 0` |

## ➕ Cara Ubah Skema (Wajib 3 Langkah!)

```kotlin
// 1. AppDatabase.kt: version = 8 → 9
// 2. Tulis MIGRATION_8_9
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activities ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
    }
}
// 3. Daftarkan di di/AppModule.kt (addMigrations)
```

> ❌ Lupa satu langkah = crash di user lama / data hilang. Jangan pakai `fallbackToDestructiveMigration()` — itu hapus DB user!

## 🔍 Query Debug

```bash
# via adb (root/debug build)
adb shell "run-as com.zaaam.zrun ls files/"
```

Atau pakai **App Inspection** di Android Studio → Database Inspector → `zmusic.db`.

⏭️ Rilis dengan migrasi → [📦 Build & CI](09-build-release-ci.md).
