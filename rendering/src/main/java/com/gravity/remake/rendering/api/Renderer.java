package com.gravity.remake.rendering.api;

import com.gravity.remake.levelruntime.api.RuntimeSnapshot;
import java.util.List;

public interface Renderer {
  List<DrawCommand> buildFrame(RuntimeSnapshot snapshot);

  void render(List<DrawCommand> commands, DrawSurface surface);
}
