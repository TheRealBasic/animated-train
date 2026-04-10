package com.gravity.remake.persistence.api;

import com.gravity.remake.levelruntime.api.RuntimeSnapshot;
import java.util.Optional;

public interface SaveRepository {
  void save(String slotId, RuntimeSnapshot snapshot);

  Optional<RuntimeSnapshot> load(String slotId);
}
