package com.gravity.remake.audio.api;

import com.gravity.remake.levelruntime.api.RuntimeSnapshot;
import java.util.List;

public interface AudioMixer {
  List<AudioEvent> deriveEvents(RuntimeSnapshot snapshot);
}
