# basics. — Release notes for the 0.9.0 closed-test update (versionCode 9)

Upload to: **Play Console → Testing → Closed testing → Alpha → Create new release**
(with `app/build/outputs/bundle/release/app-release.aab`)

> The Alpha track currently has 1.0.2 (versionCode 3). versionCode 9 > 3, so
> testers keep the same opt-in link and see an "Update" button. The 14-day
> closed-test countdown (started Aug 23) is NOT reset by updating the release.

---

## Release notes field (≤ 500 chars — copy this)

```
• Manual cube input: paint your cube's state, validated before saving
• Built-in Kociemba solver — manual solves get real WCA scrambles
• csTimer export now includes real scrambles
• Fullscreen timer overlay while solving
• Undo penalty button
• Hide millis + hold-to-start inspection options
• Puzzle picker is 3×3 for now (others coming soon)
• Charts: tap for solve and bucket details
```

## Longer version (if you want to expand in a blog/devlog later)

What's new in 0.9.0 since 1.0.2:

- **Manual cube input.** Set a custom scramble by painting your cube's actual
  facelet state (54 stickers, colors 0-5). The state is strictly validated:
  centers, color counts, piece validity, unique permutation, parity, and corner
  orientation — impossible states are rejected before they can be saved.
- **Built-in Kociemba solver.** A two-phase solver (move tables derived from the
  existing cube model — no hand-transcribed constants) runs on a background
  thread and converts any valid manual cube state into a real WCA scramble in
  ~8-15 ms after a one-time table build.
- **csTimer JSON export with real scrambles.** Manual `manual:` solves are now
  converted into genuine WCA scrambles via the solver during export, so the file
  imports cleanly into csTimer.
- **Fullscreen running overlay.** While a solve is running only the timer is
  visible; tap anywhere to stop it.
- **Undo penalty.** The penalty row's "clear" is now "undo" and only appears
  when a penalty is applied.
- **Settings.** New "hide millis while solving" (seconds-only display) and
  "hold to start inspection" options.
- **Puzzle selection gated to 3×3.** Other puzzles are greyed out in a
  non-tappable "coming soon" section (existing non-3×3 sessions are untouched).
- **Interactive charts.** Tap progression nodes for a time + date tooltip (and a
  full popup), and distribution bars for bucket details. Info strips are
  always-visible and fixed-height — no layout shifts.
