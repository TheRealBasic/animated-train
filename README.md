# Java Platform Movement Scaffold

A standalone Java 2D sandbox that opens a desktop window with a controllable character and simple platforms. Movement, jumping, and collision are handled with lightweight physics so you can extend the project into a fuller game without external libraries.

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

- Move: `A` / `D` or Arrow Left / Arrow Right
- Jump: `W`, `Space`, or Arrow Up

A heads-up display inside the window shows your current position and velocity for quick iteration on physics tweaks.

## Project structure

```
src/
  GamePanel.java  # Swing panel with the game loop, rendering, and input handling
  Main.java       # Application entry point that boots the window and starts the loop
  Platform.java   # Simple platform data class
  Player.java     # Physics, collisions, and drawing for the player
```

## Next steps

- Add sprites, textures, or parallax backgrounds for visual polish.
- Expand the platform layout and introduce hazards or collectibles.
- Extract physics constants (gravity, jump force, friction) into a config class for easy tuning.
- Replace the Swing timer with a more advanced game loop if you need finer control over delta time.
