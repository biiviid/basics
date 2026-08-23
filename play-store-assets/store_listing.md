# basics. — Google Play Store Listing Kit

All copy + assets for the store listing live in this folder.
Paths to every file are listed below so you can grab them fast.

---

## App identity

- **App name:** `basics.` (8 chars — the period is part of the name)
- **Package / applicationId:** `com.basicsapp.timer`
- **Category suggestion:** Games → Puzzle (most cubing timers live here), or Tools if you prefer
- **Price:** Free

## Short description (≤ 80 chars)

```
basics. — a minimal speedcube timer. Scrambles, stats, sessions. No noise.
```

## Full description (up to 4000 chars)

```
basics. is a very basic speedcubing timer — a personal, brutalist take on the
classic cubing timer. The name is the ideology: back to the basics. No
unnecessary features, no clutter. Just a timer that's pleasant to use.

FEATURES

• WCA scrambles for 2x2–7x7, Skewb, Pyraminx, Megaminx, Square-1 and Clock,
  with standard WCA notation
• Precise timer with optional 15-second inspection and WCA penalties (+2 / DNF)
• Session stats: best, worst, mean, median, standard deviation, and rolling
  averages ao5, ao12, ao50, ao100, ao200 and ao500 — with correct DNF handling,
  the same way competitive speedcubers expect
• Multiple named sessions per puzzle type
• Full solve history with penalties and delete
• Progress charts for every session
• Import and export your data as JSON
• Dark, minimal monochrome design with the Orbitron font

PRIVACY

basics. stores all data locally on your device. No accounts, no ads, no
analytics, no tracking, and no internet connection required.
```

## What's new (first release / v1.0.0)

```
• First release of basics. — a minimal, brutalist speedcubing timer
• WCA scrambles for 2x2–7x7, Skewb, Pyraminx, Megaminx, Square-1 and Clock
• Rolling averages with correct DNF handling (ao5 / ao12 / ao50 / ao100 / ao200 / ao500)
• Sessions, history, charts, and JSON import/export
```

---

## Asset file map

| Asset | Requirement | File |
|---|---|---|
| Phone screenshots | min 2 (4 provided) | `screenshots/phone/*.png` (1080×2400) |
| 7″ tablet screenshots | 4 provided | `screenshots/7inch/*.png` (1200×1920) |
| 10″ tablet screenshots | 4 provided | `screenshots/10inch/*.png` (2560×1600) |
| App icon | 512×512 PNG | `store-icon-512.png` |
| Feature graphic | 1024×500 PNG | `feature-graphic-1024x500.png` |
| Privacy policy | hosted URL | `privacy-policy.html` (needs hosting — see checklist) |
| Release bundle | signed AAB | `app/build/outputs/bundle/release/app-release.aab` |
