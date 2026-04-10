package com.gravity.remake.levelruntime.api;

import com.gravity.remake.corephysics.api.InputFrame;

/** Boundary between orchestration/game rules and the simulation core. */
public interface LevelRuntime {
  RuntimeSnapshot initialize(LevelDefinition definition);

  RuntimeSnapshot update(RuntimeSnapshot previous, InputFrame input, double deltaSeconds);
}
