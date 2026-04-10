package com.gravity.remake.corephysics.api;

public record Vector2(double x, double y) {
  public static final Vector2 ZERO = new Vector2(0.0, 0.0);
}
