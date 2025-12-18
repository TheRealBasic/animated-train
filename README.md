# Gravity Warp Trials

A standalone Java 2D platformer built on Swing/Java2D featuring a gravity-warping mechanic. Play solo or link up with a friend over LAN/direct IP to solve cooperative button-door puzzles, collect Flux Orbs, race par times, and clear handcrafted stages with unlockable medals.

## Features at a glance

- **Gravity-warping traversal**: Flip gravity up, down, left, or right at any time to pathfind around hazards, invert platform layouts, and create fresh routes through every stage.
- **Tight 2D platforming**: Responsive movement with coyote time, jump buffering, sprint nudges, and consistent physics tuned for keyboard controls.
- **Solo campaign**: Progress through handcrafted stages, collect all Flux Orbs, and race par times to earn Bronze, Silver, or Gold medals while unlocking the next level.
- **Co-op levels**: Dedicated two-player maps (prefixed `zcoop`) built around synchronized buttons, doors, and orb collection with shared victory conditions.
- **LAN/direct multiplayer**: Host or join sessions over your network or direct IP with automatic level syncing and shared progression for the session.
- **Accessible configuration**: In-game Settings menu for rebinding keys, adjusting screen scale, toggling HUD/CRT visual effects, and tuning master volume.
- **Debug visibility**: Optional debug HUD with FPS overlay and adjustable post-processing toggles for development or performance tuning.
- **Procedural audio & bundled assets**: All sound effects are generated at runtime; level JSON files and sprite assets ship with the repository for easy modding.
- **Portable save system**: Save files live under `save/` in the game directory, making it easy to back up or transfer progression and settings.

## Build and run

1. Install a JDK (version 17 or newer recommended).
2. Compile the sources into the `out` directory:

   ```bash
   javac -d out src/*.java
   ```

3. Launch the game window:

   ```bash
   java -cp out Main
   ```

4. (Optional) On Windows you can bundle a runnable jar after compiling with:

   ```bat
   build_game.bat
   ```

No external libraries are required—the project uses only the Java standard library and ships its own assets under `assets/`.

## How the game works

Gravity Warp Trials is built around deliberate platforming where manipulating gravity is as important as jumping. Each level contains Flux Orbs scattered across the map; collecting all of them unlocks the exit gate. You start with gravity pointing downward, but at any moment you can warp gravity to any cardinal direction (`I`/`J`/`K`/`L`). Changing gravity immediately reorients your character and alters which surfaces are walkable, enabling wall-walking, ceiling traversal, or rapid descents. Hazards like spikes, pits, or crushing ceilings force careful timing—deaths reset you to your latest checkpoint with your current gravity direction preserved.

Progression is gated through medals: finishing under par time awards Bronze, Silver, or Gold, with later levels unlocking as you clear the previous stage. Co-op levels remix the formula by requiring both players to manipulate buttons and doors together; success depends on coordinating gravity shifts so each player can reach their side of a puzzle. All levels can be replayed from Level Select to chase better times or refine routes.

Multiplayer sessions synchronize the selected co-op level from the host. While connected, deaths, orb collection, and exits are tracked for both players; either can return to the main menu via the pause menu to end the session. Settings and save data are stored locally and persist between sessions, so preferred bindings, visual effect toggles, and the last joined IP are remembered.

## Controls

- Move: `A` / `D` or Arrow Left / Arrow Right (rebindable in Settings)
- Jump: `Space`, `W`, or Arrow Up (rebindable)
- Gravity warp: `I` (up), `J` (left), `K` (down), `L` (right)
- Sprint/nudge: `Shift` (minor speed boost)
- Pause/menus: `Esc`
- Restart current level: `R`

## Game flow

- Main Menu → Continue/New Game/Level Select/Multiplayer/Settings/Credits/Quit.
- Solo levels unlock sequentially; clear a stage to earn a medal (Gold/Silver/Bronze) based on par time and unlock the next.
- In-level objectives: collect all Flux Orbs to unlock the exit gate. Checkpoints update your respawn position and current gravity direction. Spikes or falling out of bounds trigger a respawn and increment the death counter.
- Multiplayer levels (prefixed `zcoop`) focus on cooperative buttons and linked doors. They require two players but still track orbs and exits the same way.

## Multiplayer

- Host: choose Multiplayer → Host to advertise on LAN and wait for a client. The host’s currently selected co-op level is synced to the client.
- Join: choose Multiplayer → Join (direct IP) or LAN Search to auto-discover a host on your network. The client will sync to the host’s selected level once connected.
- Progress is synced for the session; you can return to the main menu from the pause menu to close the connection.

## Settings

Settings are available from the main or pause menu and include:

- Master volume (affects all generated tones)
- Screen scale (1x–2x pixel scaling)
- Debug HUD and FPS overlay toggles
- Individual screen-effect toggles for distortion, scanline overlay, and CRT bezel (legacy combined toggle still honored)
- Rebindable Left/Right/Jump keys
- Last joined IP for quick multiplayer reconnects

## Save data and assets

Save data and settings are stored under `save/` in the project directory (`save/save.properties` for progression, `save/settings.properties` for bindings and preferences). Level data lives in `assets/levels/*.json` and can be edited or extended easily. Sound effects are generated procedurally at runtime—no extra assets required.
