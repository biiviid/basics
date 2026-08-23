# basics. — SAVE POINT

> **Snapshot of the project as of:** 2026-08-23 (v1.1.0, versionCode 7)
> **Purpose:** personal reference / save point. Not a hand-off doc — but detailed enough
> to rebuild or re-understand every part of the app from memory.
> Repo: `https://github.com/biiviid/basics` (formerly CubeChron)

---

## 1. Identity

| | |
|---|---|
| App name | `basics.` (the period is part of the name) |
| Package / applicationId | `com.basicsapp.timer` |
| Kotlin package root | `com.basicsapp.timer` |
| What it is | A minimal, brutalist speedcubing timer. Back-to-the-basics ideology: no fluff, just a pleasant timer. |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| Current version | 1.1.0 (versionCode 7) |
| Store category | Tools → "Clock, alarm & timer" |
| Privacy policy URL | `https://biiviid.github.io/basics/privacy-policy.html` |

## 2. Tech stack (from `gradle/libs.versions.toml`)

| Component | Version |
|---|---|
| Kotlin | 2.0.21 |
| AGP | 8.9.1 |
| Compose BOM | 2024.09.00 (Compose UI 1.7.x) |
| Material3 | via BOM |
| Room | 2.6.1 |
| Hilt | 2.51.1 |
| Hilt Navigation Compose | 1.2.0 |
| Gson | 2.11.0 |
| Coroutines | 1.9.0 |
| core-ktx | 1.15.0 |
| lifecycle-runtime | 2.8.7 |
| activity-compose | 1.9.3 |
| KSP | 2.0.21-1.0.27 |
| Gradle wrapper | 8.11.1 |
| JDK | 21 (Temurin) |

Plugins applied: android-application, kotlin-android, kotlin-compose, ksp, hilt.

## 3. Project structure (every source file)

```
basics/
├── build.gradle.kts                      # root: declares plugin aliases (apply false)
├── settings.gradle.kts                   # rootProject.name = "Basics"; repos; include(":app")
├── gradle.properties                     # jvmargs, useAndroidX, nonTransitiveRClass
├── gradle/libs.versions.toml             # version catalog (see above)
├── keystore.properties                   # ⚠️ GITIGNORED — signing secrets (storeFile, storePassword, keyAlias, keyPassword)
├── release-keystore.jks                  # ⚠️ GITIGNORED — release signing keystore
├── local.properties                      # ⚠️ GITIGNORED — sdk.dir
├── privacy-policy.html                   # the hosted privacy policy source
├── play-store-assets/                    # all Play Store listing assets (screenshots phone/7"/10", icon, feature graphic, store_listing.md, release_checklist.md)
├── app/build.gradle.kts                  # app module: SDK versions, signingConfigs.release, R8 minify
├── app/proguard-rules.pro                # proguard rules
└── app/src/main/
    ├── AndroidManifest.xml               # no permissions; label "basics."; Theme.Basics; .BasicsApp; .MainActivity
    ├── res/
    │   ├── values/{colors,strings,themes}.xml   # ic_launcher_background #131313; app_name "basics."
    │   ├── drawable/ic_launcher_foreground.xml  # 3×3 cube grid vector (the icon)
    │   ├── mipmap-anydpi-v26/ic_launcher.xml    # adaptive icon
    │   ├── mipmap-hdpi/ic_launcher.webp
    │   └── font/orbitron_{regular,bold}.ttf + orbitron.xml
    └── kotlin/com/basicsapp/timer/
        ├── BasicsApp.kt                  # @HiltAndroidApp Application
        ├── MainActivity.kt               # single activity; BasicsMainScreen = whole app UI shell + settings/privacy dialogs
        ├── data/
        │   ├── models/Session.kt         # Room entity "sessions"
        │   ├── models/Solve.kt           # Room entity "solves" (FK → sessions, cascade)
        │   ├── database/BasicsDatabase.kt# Room db "basics_database", version 4, destructive migration
        │   ├── database/SessionDao.kt    # CRUD + getAllSessions flow (updatedAt DESC)
        │   ├── database/SolveDao.kt      # CRUD + by-session flow + best-solve-per-puzzle query
        │   └── repository/TimerRepository.kt # @Singleton facade over the two DAOs
        ├── di/DatabaseModule.kt          # Hilt: provides database + DAOs
        ├── ui/
        │   ├── components/
        │   │   ├── AdaptiveTimerText.kt  # timer text: caps font scale @1.2, single line, auto-shrinks to fit (TextMeasurer)
        │   │   ├── ArmedText.kt          # (in TimerScreen.kt) text with rising fill effect for "armed"
        │   │   ├── TimerDisplay.kt       # AdaptiveTimerText wrapper (component form)
        │   │   ├── SolveCard.kt          # history row: time, penalties, date, repeated-scramble yellow, selection
        │   │   ├── StatItem.kt           # label/value stat row
        │   │   ├── CubeNet.kt            # 12×9 cube-net renderer from scramble
        │   │   └── CubieCube.kt          # csTimer CubieCube port (move tables, facelets)
        │   ├── screens/
        │   │   ├── TimerScreen.kt        # the timer: gesture state machine, scramble, stats strip
        │   │   ├── HistoryScreen.kt      # solve list with filters/sort/selection/penalties
        │   │   ├── StatsScreen.kt        # session stats panel
        │   │   └── ChartsScreen.kt       # time progression line + distribution histogram (Canvas)
        │   └── theme/Theme.kt            # BasicsColors palette + AppFont (Orbitron)
        ├── utils/
        │   ├── StatsCalculator.kt        # csTimer-compatible session stats + SessionStats
        │   ├── TimeFormatter.kt          # time formatting + DNF/sentinel handling
        │   ├── ScrambleGenerator.kt      # PuzzleType enum + scramble generators per puzzle
        │   └── FileExporter.kt           # BasicsExport JSON schema + export/import
        ├── viewmodel/TimerViewModel.kt   # the heart: all state + timer/inspection/session logic
        └── test/kotlin/com/basicsapp/timer/utils/StatsCalculatorTest.kt  # 13 unit tests
```

## 4. Data layer

### Session entity (`data/models/Session.kt`)
Room table `sessions`:
- `id: Int` — PK autoGenerate
- `name: String` — session name (lowercased on create)
- `puzzleType: String` — enum name, e.g. `"THREE_BY_THREE"`, default `"3x3"`
- `createdAt: Long` — ms
- `updatedAt: Long` — ms (sessions ordered by this DESC in the list)

### Solve entity (`data/models/Solve.kt`)
Room table `solves`:
- `id: Int` — PK autoGenerate
- `sessionId: Int` — FK → sessions.id, `onDelete = CASCADE`, indexed
- `timeMs: Long` — raw solve time, **penalty not included**
- `penalty: Int` — **`0` = OK, `2` = +2 (adds 2000ms), `-1` = DNF**
- `scramble: String`
- `createdAt: Long` — ms (used for ordering, dates, month filters)

### Database (`data/database/BasicsDatabase.kt`)
- Name: `basics_database`, **version 4**, `exportSchema = false`
- Uses **`fallbackToDestructiveMigration()`** — schema changes wipe data. Fine for a timer, but remember this exists.
- Singleton via `synchronized` companion.

### SessionDao
- `insert/update/delete` (suspend)
- `getAllSessions(): Flow<List<Session>>` ordered `updatedAt DESC`
- `getSessionById(id)` suspend
- `deleteAllSessions()`

### SolveDao
- `insert/update/delete` (suspend)
- `getSolvesBySession(sessionId): Flow<List<Solve>>` ordered `createdAt DESC`
- `getLastSolveForSession(sessionId)` suspend (LIMIT 1)
- `deleteAllSolvesForSession(sessionId)`
- `updatePenalty(id, penalty)`
- `getBestSolveForPuzzleType(puzzleType)`: fastest **non-DNF** solve across all sessions for a puzzle type (drives "overall pb")

### Repository (`data/repository/TimerRepository.kt`)
`@Singleton`, thin facade over the DAOs — the ViewModel talks only to this.

### DI (`di/DatabaseModule.kt`)
Hilt `@Module @InstallIn(SingletonComponent::class)` — provides `BasicsDatabase`, `SessionDao`, `SolveDao`.

---

## 5. ViewModel (`viewmodel/TimerViewModel.kt`) — the heart

`@HiltViewModel`, constructor: `@ApplicationContext context: Context, repository: TimerRepository`.
**SharedPreferences** file: `basics_settings` (all persisted settings live here).

### State flows (all `MutableStateFlow` + `asStateFlow()`)
| Flow | Type | Default | Persisted? |
|---|---|---|---|
| `sessions` | List\<Session\> | empty | (from Room) |
| `currentSessionId` | Int? | null | no |
| `solves` | List\<Solve\> | empty | (from Room) |
| `stats` | SessionStats | empty | computed |
| `timerState` | TimerState | IDLE | no |
| `elapsed` | Long | 0 | no |
| `scramble` | String | "" | no |
| `puzzleType` | PuzzleType | THREE_BY_THREE | no |
| `sessionPb` | Long? | null | no |
| `overallPb` | Long? | null | no |
| `inspectionEnabled` | Boolean | false | ✅ `inspection_enabled` |
| `showAveragesOnTimer` | Boolean | false | ✅ `show_averages_on_timer` |
| `enabledAverages` | Set\<Int\> | empty | ✅ `enabled_averages` (comma list) |
| `holdTimeMs` | Int | 200 (0..1000) | ✅ `hold_time_ms` |
| `inspectionHoldStart` | Boolean | false | ✅ `inspection_hold_start` |
| `inspectionTime` | Int | 15 | no |
| `isInspecting` | Boolean | false | no |
| `inspectionPenalty` | String | "none" | no |
| `lastSolveTime` | Long? | null | no |
| `lastSolvePenalty` | Int | 0 | no |
| `lastSolveId` | Int? | null | no |
| `scrambleHistory` | List\<String\> | empty | no |
| `scrambleIndex` | Int | 0 | no |
| `lastSolveUsedPrevScramble` | Boolean | false | no |

### Timer state machine
`TimerState` enum: **IDLE → ARMED → RUNNING → IDLE** (STOPPED exists but is unused).

- `armTimer()` → ARMED · `disarmTimer()` → IDLE · `startTimer()` → RUNNING (launches 16ms ticker) · `stopTimer()` → IDLE + records solve
- `stopTimer()` applies inspection +2 if `inspectionPenalty == "plus2"`; only records when `elapsedMs in 1..3_599_999` (drops 0ms / >1h)

### Inspection
- `startInspection()`: starts 15s countdown via coroutine; every second decrements; at 0→none, −1/−2→**plus2**, ≤−3→**dnf**. Sets `isInspecting=true`.
- `startFromInspection()`: cancels countdown; if dnf → records a DNF solve (0ms) + new scramble; else `startTimer()`.
- `cancelInspection()`: cancels countdown, resets time/penalty.

### Settings setters (all persist to SharedPreferences)
- `toggleInspection()`, `toggleShowAverages()`, `toggleInspectionHoldStart()`, `setHoldTimeMs(ms)` (coerced 0..1000), `toggleAverage(size)`.

### Scramble management
- `generateNewScramble()`: appends to `scrambleHistory` at `scrambleIndex+1`, truncates future, advances index.
- `previousScramble()`: steps back through history, marks `lastSolveUsedPrevScramble=true`.
- `refreshScramble()`, `setCustomScramble(text)`.

### Sessions
- `createSession(name, puzzle)`, `selectSession(id)`, `renameSession(id, name)`, `deleteSession(id)` (falls back to creating a fresh default session).

### Import/export
- `getSessionForExport(id)` / `getSolvesForExport(id)` (suspend).
- `importSession(BasicsExport)`: re-creates sessions + solves.

### init
- Loads all persisted settings from SharedPreferences.
- Collects all sessions → sets current session (first, or creates `default`).
- Collects solves for current session → recomputes stats, session PB, overall PB.

## 6. UI shell (`MainActivity.kt` → `BasicsMainScreen`)

Single activity, no navigation library — the "tabs" are just a `when(selectedTab)` switch.

### Root layout
- `Column(fillMaxSize().background(Background).windowInsetsPadding(WindowInsets.systemBars))`
  - `.windowInsetsPadding(systemBars)` is what fixes edge-to-edge overlap (Android 15+ enforced edge-to-edge at targetSdk 35+). Do NOT remove.

### Top bar (Row)
- Left: **"basics."** title
- Center: `"<session name> · <puzzle display name>"` — clickable → session menu (DropdownMenu)
- Right: gear `\u2699` → settings dialog
- Session menu items: switch session, `+ new session`, `import from file` (OpenDocument JSON launcher)
- Long-press a session in the menu → action sheet (rename / delete / export)

### Tab row
`timer / history / stats / charts` — equal-width boxes, active tab = Background, inactive = Surface. Tap sets `selectedTab`.

### Settings dialog (gear)
Brutalist AlertDialog (0 radius). Contents top→bottom:
1. **ao50 / ao100 / ao200 / ao500 toggles** — rectangular switches, each with a divider (persisted)
2. **wca inspection** toggle — "15s countdown, +2 if over" (persisted)
3. **hold to start inspection** toggle — "press starts countdown, release starts solve" (persisted)
4. **arm hold time** slider — 0.00–1.00s, 50ms steps, value shown live; Material3 Slider themed to palette (persisted)
5. **privacy policy** row → opens the embedded privacy dialog (offline; text matches hosted page)

### Privacy policy dialog
Embedded text — scrollable, lists Data Storage / Internet / Third-Party / Children / Changes / Contact. Contact: `basics.timer.app@gmail.com`.

### New session dialog
Text field (name, ≤50 chars, lowercased) + puzzle grid (3 columns of puzzle chips) + create/cancel.

### Import
`rememberLauncherForActivityResult(OpenDocument)` for `application/json` → parses `BasicsExport` → `viewModel.importSession`.

---

## 7. UI — `TimerScreen.kt` (the timer, most complex screen)

### Gesture state machine (the `pointerInput` block)
`pointerInput(inspectionHoldStart, holdTimeMs)` — keyed so the block does **not** restart when `isInspecting`/`timerState` change mid-gesture (critical for hold-to-start).

On each `awaitFirstDown` (per gesture), one branch runs:

| State at press | Behavior |
|---|---|
| **isInspecting** (standard mode, mid-countdown) | arm → hold ≥ `holdTimeMs` → release ⇒ `startFromInspection()`; released early ⇒ disarm |
| **IDLE + inspection + holdToStart** | `armTimer()` (shows the fill effect) + a coroutine fires `startInspection()` + `disarmTimer()` after `holdTimeMs` (so the countdown engages mid-hold, then displays normally); release after threshold ⇒ `startFromInspection()`; release before threshold ⇒ cancel |
| **IDLE + inspection (standard)** | tap ⇒ `startInspection()` (countdown begins) |
| **IDLE (no inspection)** | arm → hold ≥ `holdTimeMs` → release ⇒ `startTimer()`; early release ⇒ disarm |
| **RUNNING** | tap ⇒ `stopTimer()` |

### Arm fill effect (`armProgress` Animatable)
- `LaunchedEffect(timerState, holdTimeMs)`: on ARMED, animates 0→1 over exactly `holdTimeMs`; else snaps to 0.
- `ArmedText` renders the timer text with the armed color filling bottom→top by `progress`. Used for the normal timer **and** the inspection countdown while holding.

### Timer display states (center, vertically stacked)
- `0:00.000` idle (when no last solve) — via `AdaptiveTimerText`
- Running time — `AdaptiveTimerText`
- Armed (fill effect) — `ArmedText`
- Last solve (with color: error red for DNF/+2, `#CCCC00` yellow if a previous scramble was used) + `+2` / `dnf` / `<<` suffix chips
- Inspection countdown number (white, turns `Tertiary` ≤3s; DNF → "dnf", +2 → "+2"; armed state shows countdown with fill)

### Instruction text (`stateText`)
| Situation | Text |
|---|---|
| IDLE, inspection off | `hold to arm` |
| IDLE, inspection on, standard | `tap to inspect` |
| IDLE, inspection on, hold-to-start | `hold to inspect` |
| Inspecting, standard | `hold to start` |
| Inspecting, hold-to-start | `release to start` |
| RUNNING | `tap to stop` |

### Scramble area (top)
- Two-line split of the scramble (never splits a move+prime), Orbitron bold
- Right controls: `<<` (prev scramble, enabled if index > 0), refresh icon (Canvas-drawn refresh arrow), copy-to-clipboard icon, `in` (custom scramble dialog)
- Tapping the scramble/cube area opens the **CubeNet popup** (12×9 render)

### Averages strip (top-right, when `totalSolves > 0`)
- Always shows **ao5** and **ao12**; plus **ao50/ao100/ao200/ao500** if enabled in settings. Each label + `formatStatTime` value.

### Last-solve action row (after a solve, when idle)
`+2` / `dnf` / `clear` / `delete` buttons → `markLastSolvePenalty` / `deleteLastSolveFromButton`.

### Custom scramble dialog
Text input + OK — calls `setCustomScramble`.

## 8. UI — `HistoryScreen.kt`

- `LazyColumn` of `SolveCard`s, alternating Surface/Background rows.
- **Empty state**: "no solves recorded".
- **Month filter** (dropdown from available `MMM yyyy` values).
- **Sorting**: by date (desc/asc toggle) or by time (asc/desc toggle).
- **Selection mode**: tap toggles checkboxes; batch delete.
- **Repeated scramble detection**: scrambles appearing >1× are highlighted **yellow** (`#CCCC00`) with `<<` marker (flags when you re-solved a previous scramble).
- **Per-solve dialog** (tap a card): shows time, scramble, date, penalty text; buttons **+2 / dnf / clear / delete** → `markPenalty(id, ...)`; delete opens a confirm dialog.

## 9. UI — `StatsScreen.kt`

Boxed panel with `StatItem` rows (from `stats` = `SessionStats`):
- **best, worst, mean, median** (via `formatStatTime` — shows "n/a" or "DNF")
- **std dev** (sample σ, "%.2fs", else "n/a")
- **ao5, ao12** (via `formatStatTime`)
- **total** solves, **dnfs** count, **session time**

## 10. UI — `ChartsScreen.kt`

Two Canvas charts (custom drawn, no chart library):
1. **Time progression** — line chart of solves over time; DNFs drawn as red **×** marks; axis labels via `nativeCanvas`.
2. **Time distribution** — histogram; adaptive bucket size (2s/5s/10s/15s based on max); gray bars (#555555), grid, labels.

---

## 11. Components

### `AdaptiveTimerText` (in `TimerDisplay.kt`)
The timer font. **Critical behavior:**
- Caps system font scale at **1.2×** (`LocalDensity` override via `CompositionLocalProvider`)
- `maxLines = 1` + `softWrap = false` — never wraps
- **Auto-shrinks** to fit available width: measures the text with `TextMeasurer` (at capped density), computes `fit = availWidth / textWidth`, renders `72.sp * fit`
- Used for: idle time, running time, last solve, and `TimerDisplay`

### `ArmedText` (in `TimerScreen.kt`)
Text with the **arm fill** — `drawWithContent` + `BlendMode.SrcIn` fills the glyphs bottom→top by `progress`. Caps font scale at 1.2, single line.

### `TimerDisplay` (component)
Wrapper around `AdaptiveTimerText` (currently not used by TimerScreen — the screen uses `AdaptiveTimerText` directly; the component is kept for reuse).

### `SolveCard`
History row: index, time (penalty-aware, error red for DNF/+2), `+2`/`dnf`/`<<` chips, date `h:mma, MMM d`. Supports selection checkboxes. Zebra background.

### `StatItem`
Label (lowercased, tertiary, 11sp) + value (primary, 18sp, Orbitron bold), space-between.

### `CubeNet` / `CubieCube`
- `CubieCube.kt`: port of **csTimer's CubieCube** (ca/ea arrays, cFacelet/eFacelet tables, `cubeMult`, `moveCube[18]`). This is the **correct** scramble state engine — see `.clinerules` section "Cube Net — Critical Technical Details" for the full math (do NOT use dwalton76 swap tables — they're wrong).
- `CubeNet.kt`: renders the scramble as a 12×9 net: `[U]` top, `[L][F][R][B]` middle, `[D]` bottom; `faceletToNetPos(idx)`; 1.5px sticker gap, 1px black border.

---

## 12. Utils

### `StatsCalculator.kt` (csTimer-compatible — do not regress)
- **`StatsCalculator.DNF = Long.MAX_VALUE`** — sentinel for DNF results
- Penalty semantics: `+2` → `timeMs + 2000`; DNF → sentinel
- **Average rules (match csTimer defaults):**
  - Window = last N solves **including DNFs** (never drops DNFs from the window)
  - Trim = `ceil(5%)` each side (`trimCount(n)`) → ao5 1/1, ao12 1/1, ao50 3/3, ao100 5/5, ao200 10/10, ao500 25/25
  - Average = DNF iff `dnfCount > trim` (ao5/ao12: 2+ DNFs ⇒ DNF; one DNF is trimmed as worst)
  - Value = mean of the trimmed middle, rounded to ms
- **Sample std dev** (÷ n−1), not population
- Session stats: best/worst/mean/median exclude DNFs; all-DNF session → best/worst/mean = DNF; `null` = not enough data
- 13 unit tests in `app/src/test/.../StatsCalculatorTest.kt` — run before touching stats

### `TimeFormatter.kt`
- `formatTime(ms)` → `M:SS.mmm`
- `formatTimeForDisplay(ms, penalty)` → handles +2 / DNF for solve rows
- `formatStatTime(ms?)` → `null` = "n/a", `StatsCalculator.DNF` = "DNF", else `formatTime`

### `ScrambleGenerator.kt` + `PuzzleType`
- PuzzleType: 2x2(11), 3x3(20), 4x4(44), 5x5(60), 6x6(80), 7x7(100), Skewb(11), Pyraminx(11), Megaminx(11), Square-1(13), Clock(14) — lengths are move counts
- 3x3: 20 moves, no same-face consecutives, filters `R L R` / `U D U` opposite-face patterns
- NxN: face + `w` wide moves; big cubes (6x6+) add inner-layer `2w..` moves
- Skewb: R/L/U/B with `'`; Pyraminx/Megaminx/Square-1/Clock: their own generators (see file)

### `FileExporter.kt` — `BasicsExport` JSON schema
```json
{ "version": 1, "app": "basics.", "exportedAt": <ms>,
  "sessions": [ { "name": "...", "puzzleType": "...", "createdAt": <ms>,
                  "solves": [ { "timeMs": 0, "penalty": 0, "scramble": "", "createdAt": 0 } ] } ] }
```
- Export writes to Downloads via MediaStore (API 29+) or legacy external dir
- Import parses via Gson (`importSession`)

---

## 13. Theme / Design system (`ui/theme/Theme.kt`)

### `BasicsColors` palette
| Token | Value |
|---|---|
| Background | `#131313` |
| Surface | `#1C1B1B` |
| SurfaceContainer | `#20201F` |
| SurfaceHigh | `#2A2A2A` |
| SurfaceHighest | `#353535` |
| Primary | `#FFFFFF` |
| Secondary | `#C6C6C6` |
| Tertiary | `#8E9192` |
| Border | `#444748` |
| BorderHeavy | `#FFFFFF` |
| Accent | `#555555` |
| Error | `#FF4444` |
| DnfRed | `#FFB4AB` |
| Armed | `#88CCFF` |
| Repeated-scramble yellow | `#CCCC00` (inline) |

### `AppFont`
- `Orbitron` family: `orbitron_regular.ttf` (Normal) + `orbitron_bold.ttf` (Bold)

### Aesthetic rules (from `.clinerules`)
0px corners everywhere, 2px/4px borders, no shadows/blur, flat opaque surfaces, all UI text lowercase (except WCA scramble notation), Orbitron app-wide.

## 14. Build & signing

### `app/build.gradle.kts` highlights
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, `versionCode = 7`, `versionName = "1.1.0"`
- Release build type: `isMinifyEnabled = true`, `isShrinkResources = true`, proguard files, signed with `signingConfigs.release`
- Release signing reads `keystore.properties` (gitignored) — keyAlias default `"basics"`
- ⚠️ **Build quirk:** the signing block must use `import java.util.Properties` at the top of the script and `Properties()` (not `java.util.Properties()`) — the fully-qualified form breaks Kotlin DSL script compilation in this environment. Don't "fix" it back.

### Keystore (CRITICAL — do not lose)
- `release-keystore.jks` + `keystore.properties` — **backed up offline + password manager**. Losing them = cannot update the app.
- On Play: this keystore is the **upload key**; Play App Signing manages the actual app-signing key.

### Useful commands (PowerShell, from project root)
```powershell
.\gradlew.bat :app:bundleRelease          # signed release AAB
.\gradlew.bat :app:assembleDebug          # installable debug APK
.\gradlew.bat :app:testDebugUnitTest      # unit tests (13)
.\gradlew.bat :app:bundleRelease --rerun-tasks  # clean release build
```
- Release AAB output: `app/build/outputs/bundle/release/app-release.aab`
- Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 15. Google Play state

- App published to **Closed testing** (Alpha track). Review passed for the initial build; subsequent updates go through review again.
- **14-day closed-test requirement in progress** — ≥12 testers opted in, 14 continuous days, then "Apply for production access".
- Store listing complete: name `basics.`, short + full descriptions, icon, feature graphic, phone + 7″ + 10″ screenshots (all in `play-store-assets/`).
- Privacy policy: hosted at `https://biiviid.github.io/basics/privacy-policy.html` (GitHub Pages, repo must stay public) + embedded in-app in Settings.
- Data safety: no data collected/shared; no ads; no accounts; no permissions declared.
- Category: Tools → "Clock, alarm & timer".

### Version code history (for reference)
| versionCode | versionName | Notes |
|---|---|---|
| 1 | 1.0.0 | internal testing build (old name path) |
| 2 | 1.0.1 | closed test first upload |
| 3 | 1.0.2 | embedded privacy policy |
| 4 | 1.0.3 | status-bar insets + adaptive timer text |
| 5 | 1.1.0 | hold-time slider + hold-to-start inspection + persistence |
| 6 | 1.1.0 | hold-to-start respects arm hold time |
| 7 | 1.1.0 | arm fill effect everywhere, speed = threshold |

---

## 16. Known quirks & decisions (do not "fix" without thinking)

1. **`java.util.Properties` in build script** — must use `import java.util.Properties` + `Properties()`. (See §14.)
2. **Edge-to-edge** — `targetSdk ≥ 35` enforces it on Android 15+; the root Column's `.windowInsetsPadding(WindowInsets.systemBars)` is load-bearing.
3. **Font scale capping** — `AdaptiveTimerText`/`ArmedText` cap at 1.2× and never wrap; this was a tester-reported bug fix.
4. **DNF sentinel** — `StatsCalculator.DNF = Long.MAX_VALUE`; UI checks it via `formatStatTime`. Never treat it as a real time.
5. **Pointer-input keying** — the timer gesture block is keyed on `(inspectionHoldStart, holdTimeMs)` intentionally, **not** `isInspecting` — so mid-gesture inspection state changes don't cancel the hold.
6. **DB destructive migration** — `fallbackToDestructiveMigration()`; a schema change wipes solves/sessions. Export before changing schema.
7. **Settings persistence** — all settings live in SharedPreferences `basics_settings`; keys: `inspection_enabled`, `show_averages_on_timer`, `enabled_averages` (comma list), `hold_time_ms`, `inspection_hold_start`.
8. **The old package `com.cubechronapp.timer`** — was installed on the dev phone (Secure Folder) causing install conflicts; fully removed. Don't reintroduce.

---

## 17. Git log (as of this save point)

```
adc2511 feat: arm fill effect everywhere, speed matches hold-time threshold
ef964b8 refine: hold-to-start inspection now respects the arm hold time threshold
08702bf feat: configurable arm hold time slider, hold-to-start inspection, persisted settings
5b5e795 fix: status bar insets (edge-to-edge) + adaptive timer text (no wrap, capped font scale)
6a546bb feat: embed privacy policy in app settings (offline), bump to v3
9bf04fb feat: Google Play store assets (screenshots, icon, feature graphic, listing copy)
b49495c build: target SDK 36, keystore.properties import fix, version code 2
d4c4984 fix: csTimer-compatible averages (DNF windows, 5% trim, sample std dev)
85bd9a1 chore: rename project to basics
87d0a06 Revise project name to 'basics.'
7751ac2 initial commit
```

---

*End of save point. To restore/rebuild: `git clone` the repo, ensure JDK 21 + Android SDK (36) + `local.properties`, run `:app:bundleRelease`. Keys are external (backed up separately).*




