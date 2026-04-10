package com.gravity.remake.ui.api;

import com.gravity.remake.levelruntime.api.RuntimeSnapshot;
import java.util.List;

public interface UiPresenter {
  List<String> composeHud(RuntimeSnapshot snapshot);

  List<UiAction> processInput(UiInput input);
}
