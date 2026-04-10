package com.gravity.remake.rendering.api;

import com.gravity.remake.corephysics.api.Vector2;

/** Render primitive generated from world state. */
public record DrawCommand(String spriteId, Vector2 position, double rotationRadians) {}
