package com.gravity.remake.corephysics.api;

/** Immutable snapshot of physically simulated state. */
public interface PhysicsState {
  Vector2 position();

  Vector2 velocity();

  GravityDirection gravityDirection();

  boolean grounded();
}
