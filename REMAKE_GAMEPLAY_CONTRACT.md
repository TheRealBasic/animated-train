# Gravity Warp Trials — Remake Gameplay Contract (Core Idea Lock)

## Purpose
This document defines the **non-negotiable gameplay identity** that must survive any remake, while explicitly listing implementation areas that may change.

## Core pillars (must be preserved)
1. **Four-direction gravity switching**
   - The player can actively switch gravity to **up, down, left, and right** during play.
   - Level traversal and puzzle-solving must materially depend on this mechanic.
2. **Orb collection unlocks exit**
   - Levels contain required collectible orbs.
   - The exit remains locked until required orb collection conditions are met.
3. **Checkpoint-based respawn loop**
   - Hazard failure/death returns the player to the latest activated checkpoint.
   - Respawn behavior must maintain predictable continuity (position/state rules are consistent and learnable).
4. **Solo + co-op progression support**
   - The game supports both single-player progression and cooperative progression.
   - Co-op requires coordinated play toward shared level completion outcomes.

## Free-to-change areas (do not define remake success)
The following may be redesigned without violating this contract:
- Art direction, visual style, animation style, camera polish.
- UI layout, menu flow, onboarding presentation, HUD styling.
- Code architecture, engine/framework, networking stack, data model.
- Rendering pipeline, shader strategy, performance implementation details.
- Save-file structure/format and persistence backend.

## Acceptance criteria for “core idea preserved”
A remake is considered faithful only if all criteria pass:

### A) Gravity identity
- At least one representative level cannot be completed without deliberate use of multiple gravity directions.
- Gravity direction choices are readable and actionable with low ambiguity.

### B) Objective identity
- A locked-exit state is present before objective completion.
- Collecting required orbs deterministically transitions the exit to completable/unlocked.

### C) Failure/learning loop identity
- Failing hazards causes respawn at the last checkpoint, not a full-level restart by default.
- Repeated fail→respawn cycles are fast enough to encourage mastery (minimal friction loop).

### D) Mode/progression identity
- Single-player mode contains valid progression path(s).
- Cooperative mode contains valid progression path(s) that require coordination.
- Both modes end runs through the same high-level objective loop: navigate with gravity, collect orbs, reach unlocked exit.

## Gameplay contract for downstream decisions (one-page rule set)
Use this as a tie-breaker for design, engineering, and production choices:

1. **Protect the verbs first**: If a change weakens gravity-switching decision depth, reject or redesign it.
2. **Protect objective clarity**: Players must always understand that orb completion governs exit unlock.
3. **Protect iteration speed**: Death/respawn flow must remain quick and checkpoint-centered.
4. **Protect both play contexts**: Feature decisions cannot improve solo at the expense of invalidating co-op progression (or vice versa).
5. **Everything else is negotiable**: Presentation, tech stack, and data plumbing are implementation freedoms as long as Rules 1–4 remain true.

## Non-goals (explicitly outside this contract)
- Preserving exact level geometry, exact timings, exact UI layout, exact file formats, or exact code structure.
- Preserving legacy visual/audio aesthetics.
- Preserving specific internal class names, APIs, or asset pipelines.
