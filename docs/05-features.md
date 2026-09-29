# ✨ 04 — Features / Screens (8 Layar)

> Tour tiap layar: apa yang dilihat user, data dari mana, file apa yang disentuh kalau mau ubah.

## 🏠 Dashboard (`DashboardScreen.kt`) — START

- 👋 Greeting + 🎯 target mingguan **25 km** (`ProgressRing`) + 🔥 CTA **Mulai Lari** → route `run`.
- 🎵 Now-playing card (dari `PlayerViewModel.queue`) + 🕘 riwayat singkat (dari `ActivitiesViewModel.activities`).
- ✏️ Ubah target? Cari konstanta `25` di file ini (belum ada settings).

## ▶️ Run (`RunScreen.kt` + `tracking/*`)

- 🗺️ GoogleMap live + Polyline rute + 📊 `StatTile` jarak / durasi / pace / speed.
- 🔘 Start / Pause / Resume / Stop + 💾 SaveDialog (input judul) → `RecordViewModel.saveActivity()` → Room → 💨 discard.
- 🔐 Minta `FINE/COARSE (+POST_NOTIFICATIONS di 33+)` via permission launcher.
- 🧮 `pace()` dihitung lokal dari *moving time*.
- ⚠️ Lihat juga [📍 GPS Engine](06-tracking-gps.md) sebelum ubah logika.

## 📜 Feed (`FeedScreen.kt`)

- 📊 Header total km + durasi + 📃 `LazyColumn FeedCard` (judul, tanggal, jarak, durasi, pace).
- Sumber: `ActivitiesViewModel` (`observeAll ORDER BY startTime DESC`).
- 💡 Ide next: filter per minggu/bulan, pull-to-refresh (belum ada).

## 🙋 Profile (`ProfileScreen.kt`)

- 👤 Avatar placeholder + "Pelari · N aktivitas" + 📊 total lari / km / pace terbaik (>300 m).
- 🎯 Target tahunan **1000 km** + 🏅 Badge emoji (🔓/🔒).
- Data: agregasi `ActivitiesViewModel` di memori (belum ada query agregat SQL).

## 🎵 MusicHome (`MusicHomeScreen.kt`)

- 🔍 Search bar → route `search` + 🔥 hero/trending (`HomeViewModel` discovery kiosk + fallback search) + 🕘 recent (Room) + 📃 playlist horizontal + ➕ `CreatePlaylistDialog`.
- ▶️ Tap lagu → `queueManager.setQueue + requestPlay` + ➕ `AddToPlaylistDialog` (`PlaylistDialogs.kt`).
- VM: `HomeViewModel` + `LibraryViewModel`.

## 🔍 Search (`SearchScreenZR.kt`)

- ⌨️ TextField + 🕘 history Room + states: `Idle / Loading / Empty / Success / Error`.
- Hasil → play / tambah ke playlist. Search YouTube 3 halaman ~50 lagu.

## 📃 Playlist Detail (`PlaylistScreenZR.kt`)

- 🌈 Header gradient + ▶️ play-all / 🔀 shuffle + 🎵 `SongRow` list + 🗑️ hapus lagu / hapus playlist.
- VM ganda: `LibraryViewModel` + `PlaylistDetailViewModel` (arg `playlistId: Long`).

## ⏯️ Player (`PlayerScreenZR.kt`)

- 🖼️ Artwork Coil / `ArtBox` fallback + 🎚️ Slider progress + ⏯️⏭️⏮️ + 🔀🔁 (via `QueueManager`) + 🏃 speed/pitch + 😴 sleep timer + ⬇️ download offline + 📝 lirik (`LyricsRepository`) + 📃 queue list (remove/move).
- VM: `PlayerViewModel` (MediaController untuk progress/isPlaying).

## 🗺️ Matriks Data

| Layar | ViewModel | Sumber data |
|-------|-----------|-------------|
| Dashboard | Activities + Player | Room activities + queue |
| Run | Record | TrackingStateHolder → Room |
| Feed/Profile | Activities | Room activities |
| Music/Search/Playlist | Home/Search/Library/PlaylistDetail | NewPipe + Room musik |
| Player | Player | QueueManager + MediaController + Room |

⏭️ Mesin di baliknya: [📍 GPS](06-tracking-gps.md) · [🎵 Musik](07-music-engine.md) · [🗄️ DB](08-database.md).
