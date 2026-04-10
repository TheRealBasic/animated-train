# Remake module architecture

The remake is scaffolded as a strict multi-module Gradle build with explicit API boundaries:

- `core-physics`: deterministic simulation primitives and state stepping contract.
- `level-runtime`: level rules + objective flow that orchestrates physics updates.
- `rendering`: frame extraction and draw-command translation.
- `ui`: HUD/menu presentation and command handling.
- `audio`: runtime-state-to-audio-event mapping.
- `netcode`: packet encoding/decoding and transport abstraction.
- `persistence`: serialization contracts and save repository boundary.

## Boundary contracts

1. **Input** enters via `core-physics` as `InputFrame` and is forwarded by `level-runtime`.
2. **State update** happens in `core-physics.PhysicsEngine#step` and `level-runtime.LevelRuntime#update`.
3. **Draw** boundary is `rendering.Renderer`, producing `DrawCommand` instances for a `DrawSurface`.
4. **Serialization** is owned by `persistence.Serializer<T>` and consumed by `persistence.SaveRepository` + `netcode.NetPacketCodec`.

## Dependency graph

```text
core-physics
   ↑
level-runtime
 ┌──┼─────┬───────┐
 ↓  ↓     ↓       ↓
ui audio rendering persistence
                  ↑
                netcode
```

Rules:
- `core-physics` stays dependency-free.
- Cross-module access uses only `api` interfaces.
- Module internals should not reference another module's implementation classes.


## Boundary enforcement

- Each module now declares a `module-info.java` descriptor (JPMS) to make dependencies explicit at compile time.
- Only API packages are exported; implementation packages can stay private to their modules.
