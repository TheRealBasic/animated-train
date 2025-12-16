# Gravity Warp Trials

A standalone Java 2D platformer built on Swing/Java2D featuring a gravity-warping mechanic. Move, jump, collect Flux Orbs, hit checkpoints, dodge hazards, and clear ten handcrafted levels with par times and medals.

## Running the program

1. Install a JDK (version 17 or newer recommended).
2. Compile the sources into the `out` directory:

   ```bash
   javac -d out src/*.java
   ```

3. Launch the game window:

   ```bash
   java -cp out Main
   ```

No build tools or package managers are required—the project uses only the Java standard library.

## Controls

- Move: `A` / `D` or Arrow Left / Arrow Right (rebindable in Settings)
- Jump: `W`, `Space`, or Arrow Up (rebindable)
- Pause: `Esc`

## Game flow

- Main Menu → Continue/New Game/Level Select/Settings/Credits.
- Collect all Flux Orbs in a level to unlock the exit gate.
- Checkpoints update your respawn position and gravity direction.
- Spikes and the kill plane cause a respawn and increment the death counter.
- Clear a level to earn a medal (Gold/Silver/Bronze) based on par time and unlock the next stage.

## Save data

Save data and settings are stored under `save/` in the project directory. Level data lives in `assets/levels/*.json` and can be edited or extended easily.
