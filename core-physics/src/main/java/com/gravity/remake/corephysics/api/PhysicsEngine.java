package com.gravity.remake.corephysics.api;

/** Module boundary for deterministic state updates. */
public interface PhysicsEngine {
  PhysicsState step(PhysicsState previous, InputFrame input, double deltaSeconds);
}
