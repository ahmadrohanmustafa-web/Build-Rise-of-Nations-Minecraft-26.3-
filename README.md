# Rise of Nations: AI Civilization (Minecraft 26.3, Fabric)

A story-driven civilization campaign mod. **Early development.** This branch is a clean rebuild; the old
1.21.11 mod is kept untouched in the `mc-1.21.11` copy/branch.

## Status — be honest about what exists

| Area | State |
|---|---|
| Build for Minecraft 26.3 (Java 25, Fabric, Loom) | Compiles and launches (confirmed by the project owner on GitHub Actions + the game) |
| Found a civilization (name, leader, color, government) with validation | **New in phase 1.** Logic tested; screen **not yet run in-game** |
| Civilization overview screen (name, leader, color, government, treasury, population, settlements, stage, economy) | **New.** Not yet run in-game |
| Rename civilization | **New.** Logic tested; screen not yet run in-game |
| Saving with the world (vanilla saved data, schema version 2, refuses newer data) | **New.** Format tested; the Minecraft hookup is not yet run in-game |
| Server-side validation and rate limiting of every create/rename request | **New.** Logic tested |
| `/ron` and `/ron civilization` open the screen; `/ron about` unchanged | **New / kept.** Not yet run in-game |
| Money ledger, wages with cooldown and daily cap | Done and tested (earlier phase). Wages are not connected to gameplay yet |
| Story engine, chapters 1–3 | Done and tested (earlier phase). **Not connected to gameplay events yet** |
| Keybinding to open the screen | **Not done.** The import path of Fabric's new key mapping helper could not be verified |
| Reset / recreate civilization | **Not done on purpose.** Unsafe until treasury and settlements have rules |
| NPCs, jobs, shops, trade | Not started |
| Nations (50), territory, capitals | Not started |
| Diplomacy, government rules, royal families | Not started (the government choice is only stored and shown) |
| Armies, battles, conquest | Not started |

"Tested" means the pure-Java unit tests pass (84 tests). Nothing in the Minecraft layer
(screens, packets, saved data, commands) has been run in a real game by the author of this phase.

## What a civilization is

- One civilization per player, owned by the player's UUID (taken from the connection, never from a packet).
- **Treasury** is the balance of the ledger account `civ:<uuid>`, minted once at creation from `startingSettlementTreasury` in the config (default 5000). There is no separate treasury number anywhere.
- **Population (100) and settlements (1)** are simulated numbers. They are not villagers or buildings in the world.
- **Government** is stored and shown. It has no gameplay effect yet.
- Names: 3–32 characters; letters of any language, digits, single spaces, `'` `-` `.`. Formatting codes, control and invisible characters, emoji and symbols are rejected. Duplicate names are refused regardless of case, spaces and punctuation, across the whole world.

## Manual test plan (in Minecraft 26.3)

1. Install the JAR and Fabric API; start a **new singleplayer world**. In the log look for `Rise of Nations: story loaded (3 chapters, 10 quests).`
2. `/ron about` still prints the version.
3. `/ron` opens the screen and says you have no civilization. Click **Found a Civilization**.
4. Try bad input and watch the red message and the disabled confirm button: empty name, `ab`, 40 letters, a name with `§`, a name with an emoji.
5. Enter a good name and leader, click the color and government buttons to cycle, then **Found Civilization**. The overview should open: treasury 5,000 coins, population 100 (simulated), settlements 1, Settled, Stable.
6. Click **Rename**, try the same name (error), then a new name (works). Close and run `/ron` again: the new name is shown.
7. Run `/ron` again after creating: it must show the existing civilization, not a new form.
8. Save and quit to the title screen, reopen the world, run `/ron`: everything must be the same.
9. Create a second **new** world: `/ron` must say you have no civilization.
10. (Multiplayer, optional) With two accounts try the same name: the second must get "Another civilization already uses that name."
11. Dedicated server: start a server with the JAR and Fabric API. It must reach `Done` with no `client` class errors; then join and repeat steps 3–8.

## Toolchain (and where each number came from)

| Item | Value | Source |
|---|---|---|
| Minecraft | 26.3 | Stable release on Modrinth's Fabric API page; build confirmed working |
| Java | 25 | skill baseline table; build confirmed working |
| Fabric Loader | 0.19.5 | skill baseline table; log confirmed |
| Fabric API | 0.161.0+26.3 | skill baseline table; build confirmed working |
| Loom plugin | `net.fabricmc.fabric-loom` 1.17-SNAPSHOT | confirmed working by the build |
| Gradle | 9.1.0 | confirmed working by the build |

## Where the Minecraft API calls come from

The screens, packets and saved data were written from the official Fabric documentation for 26.2
(the newest published; 26.3 is not documented separately), not from the 26.3 game files:
custom screens (`gui.setScreen`, `extractRenderState`, `GuiGraphicsExtractor.text`), networking
(`PayloadTypeRegistry.clientboundPlay/serverboundPlay`, `StreamCodec.composite`, `ServerPlayNetworking`,
`ClientPlayNetworking`), and saved data (`SavedDataType` with a codec, `getDataStorage().computeIfAbsent`).
If GitHub Actions reports a missing class or method, that is the first place to look.

## How to build (GitHub Actions)

Push, open the **Actions** tab, and download the artifact **RiseOfNationsAI-Minecraft-26.3** when it is green.
The workflow runs the 84 tests as part of `gradle build`; a failing test fails the build.

## Where things are

- `core/` — game logic with **no Minecraft code**: `civ/` (civilizations, names, wire format), `economy/`, `story/`, `persist/`, `config/`.
- `net/` — the three packets and the server handlers (shared by client and dedicated server).
- `server/` — `RonSavedData`, the world-save hookup.
- `client/` — screens and the client entrypoint. **Only** this package may use client classes (a test enforces it).
- `src/test/java/` — tests. `gradle coreTests` runs them.

## Config

`config/riseofnations.json` is created on first start. Values outside safe ranges are corrected and logged.
`startingSettlementTreasury` sets the starting treasury of new civilizations.

## Known limitations

- Changes are saved by Minecraft's normal world saving (autosave and quit). A crash right after founding a civilization can lose that change.
- Look-alike letters from different alphabets (for example a Cyrillic "a" in "Rome") are not detected as duplicates.
- Texts are English only; there is no translation file yet.
- A client without the mod on a server that has it cannot join (normal for mods that add packets).
