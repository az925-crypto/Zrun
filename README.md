# StravaMusic 🏃‍♂️🎵

An Android app that records your runs/rides/walks like Strava **and** lets you
control music while you move. Built with **Kotlin + Jetpack Compose**.

## Features

- 📍 **GPS tracking + map** — records your route live on a Google Map (route
  polyline, current position).
- 📊 **Real-time stats** — distance, duration, current speed, and pace.
- ⏯️ **Start / pause / resume / stop** with a foreground service, so recording
  keeps running when the screen is off or the app is in the background.
- 📜 **Activity history** — finished activities are saved to a local Room
  database and shown in a list; tap one to see its map + summary.
- 🎵 **Integrated music bar** — a persistent mini-player (play/pause, next,
  previous, progress) so you can control music while recording.

## Project structure

```
app/src/main/java/com/stravamusic/app/
├── MainActivity.kt              # Entry, permission handling
├── StravaMusicApp.kt            # Application: DB + notification channel
├── data/
│   ├── local/                   # Room: entity, DAO, database, converters
│   └── repository/              # ActivityRepository
├── tracking/
│   ├── TrackingService.kt       # Foreground GPS recording service
│   ├── TrackingBus / State      # Shared live tracking state
│   ├── TrackingViewModel.kt
│   └── LocationUtils.kt         # Haversine distance + formatters
├── music/
│   ├── MusicController.kt        # ← interface to plug in YOUR music code
│   ├── MediaPlayerMusicController # default MediaPlayer-based impl
│   └── MusicViewModel.kt
└── ui/                          # Compose screens, components, theme, nav
```

## Setup

1. **Clone & open in Android Studio** (Hedgehog or newer recommended).
2. **Add a Google Maps API key:**
   - Copy `local.properties.example` → `local.properties`.
   - Set `MAPS_API_KEY=...` (enable *Maps SDK for Android* in Google Cloud).
   - The map shows blank without a valid key; everything else still works.
3. **Run** on a device or emulator with Google Play services.
4. Grant **location** (and **notifications** on Android 13+) when prompted.

Or from the command line:

```bash
./gradlew assembleDebug
```

## Plugging in your own music code 🎵

The UI only talks to the [`MusicController`](app/src/main/java/com/stravamusic/app/music/MusicController.kt)
interface. To use your own playback source:

- **Option A** — replace the bodies in `MediaPlayerMusicController` with your
  engine's calls, or
- **Option B** — create a new class implementing `MusicController` and point
  `MusicViewModel` at it:

  ```kotlin
  private val controller: MusicController = MyAwesomeMusicController(app)
  ```

Then feed your real songs with `controller.setQueue(listOfTracks)` instead of
the placeholder `SampleTracks.demo`.

## Notes

- `minSdk = 24`, `targetSdk = 35`.
- Background location recording uses a `location` foreground service type.
- The demo music queue streams two short sample WAV files just so the player has
  something to play out of the box — swap them for your library.
