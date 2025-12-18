# Gravity Warp Trials

A standalone Java 2D platformer built on Swing/Java2D featuring a gravity-warping mechanic. Play solo or link up with a friend over LAN/direct IP to solve cooperative button-door puzzles, collect Flux Orbs, race par times, and clear handcrafted stages with unlockable medals.

An experimental LWJGL 3 stack (window/input/rendering), JOML math, OpenAL audio, and jbox2d physics sandbox is also available via `LwjglGame`, showing how the game could be migrated away from the Swing/Java2D stack.

## Build and run

### Swing/Java2D build

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

### LWJGL/JOML/OpenAL/jbox2d sandbox

1. Ensure Maven can download dependencies (the LWJGL 3 runtime uses native libraries).
2. Build and run the standalone sandbox entry point:

   ```bash
   mvn -q exec:java -Dexec.mainClass=LwjglGame
   ```

   The window demonstrates LWJGL 3 for windowing/input/rendering, JOML vectors, OpenAL sound playback on jumps, and a jbox2d-driven character controller/ground plane.

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
