package com.gravity.remake.levelruntime.api;

import com.gravity.remake.corephysics.api.PhysicsState;
import java.util.Map;

/** Authoritative world state produced every tick. */
public record RuntimeSnapshot(
    long tick,
    PhysicsState primaryPlayer,
    Map<String, Boolean> objectiveFlags,
    boolean exitUnlocked) {}
