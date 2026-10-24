# Rise of Nations: AI Civilization (Minecraft 26.3, Fabric)

A story-driven civilization campaign mod. **Early development.** This branch is a clean rebuild; the old
1.21.11 mod is kept untouched in the `mc-1.21.11` copy/branch.

## Status — be honest about what exists

| Area | State |
|---|---|
| Build setup for Minecraft 26.3 (Java 25, Fabric, Loom) | Written. **Not yet compiled** — first real compile happens on GitHub Actions |
| Money ledger (wallets, treasuries, taxes, sinks, no negative balances, no money from thin air) | **Done and tested** (pure Java) |
| Wages with cooldown, daily cap, paid from a real treasury | **Done and tested** |
| Story engine (chapters, quests, automatic progress, branching, no duplicate rewards) | **Done and tested** |
| Story content | Chapters 1–3 only (10 quests). Chapters 4–10 are planned, not written |
| Save format (versioned, defensive, refuses newer data) | **Done and tested** as text. **Not yet connected** to Minecraft world saving |
| Config file (`config/riseofnations.json`) | Written, not compiled |
| `/ron about` command | Written, not compiled. It is the only command |
| NPCs, shops, jobs in the world, screens/HUD | **Not started** |
| Nations (50), capitals, territory, settlements | **Not started** |
| Diplomacy, government, royal families | **Not started** |
| Armies, battles, sieges, conquest | **Not started** |

Nothing has been launched in Minecraft yet. "Tested" above means the game-logic unit tests pass (47 tests),
not that it works in-game.

## Toolchain (and where each number came from)

| Item | Value | Source |
|---|---|---|
| Minecraft | 26.3 | Listed as a stable release on Modrinth's Fabric API page |
| Java | 25 | minecraft-modding skill baseline table |
| Fabric Loader | 0.19.5 | skill baseline table |
| Fabric API | 0.161.0+26.3 | skill baseline table (not re-checked live — see below) |
| Loom plugin | `net.fabricmc.fabric-loom` 1.17-SNAPSHOT | third-party template for 26.2 (ModSmith); same plugin id/style the skill describes |
| Gradle | 9.1.0 | skill says 9.1 or newer is required for Java 25 |

If the first build fails on a version number, change it in `gradle.properties` — all versions live there.

## How to build (GitHub Actions)

1. Upload this project to GitHub (see below).
2. Open the **Actions** tab, choose **Build Rise of Nations (Minecraft 26.3)**, run it (or just push).
3. When it is green, open the run and download the artifact **RiseOfNationsAI-Minecraft-26.3**.
   The workflow also checks that the JAR contains `fabric.mod.json` and compiled classes.

There is no Gradle wrapper yet (it cannot be generated without internet). To add one on your PC:
install Gradle 9.1+, then run `gradle wrapper` once in this folder.

## Where things are

- `src/main/java/com/riseofnations/core/` — game logic with **no Minecraft code** (testable anywhere):
  `economy/` (Ledger, WageTracker), `story/` (QuestEngine, DefaultStory …), `persist/` (StateCodec), `config/` (RonSettings).
- `src/main/java/com/riseofnations/` — thin Minecraft layer: `RiseOfNationsMod`, `ConfigLoader`.
- `src/test/java/` — the tests. `gradle coreTests` runs them; `gradle build` runs them too and fails if any fail.

## Config

`config/riseofnations.json` is created on first start. Values outside safe ranges are corrected and logged.
