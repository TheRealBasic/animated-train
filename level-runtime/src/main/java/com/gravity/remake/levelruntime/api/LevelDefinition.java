package com.gravity.remake.levelruntime.api;

import java.util.List;

/** Parsed level data consumed by level runtime. */
public record LevelDefinition(String levelId, List<String> entities) {}
