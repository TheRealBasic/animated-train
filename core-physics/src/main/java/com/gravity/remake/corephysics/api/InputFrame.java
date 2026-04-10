package com.gravity.remake.corephysics.api;

/** Normalized player intent for one simulation tick. */
public record InputFrame(
    double moveAxis, boolean jumpPressed, GravityDirection requestedGravity, long tick) {}
