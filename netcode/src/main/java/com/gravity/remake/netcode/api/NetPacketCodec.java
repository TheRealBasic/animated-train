package com.gravity.remake.netcode.api;

import com.gravity.remake.corephysics.api.InputFrame;
import com.gravity.remake.levelruntime.api.RuntimeSnapshot;

public interface NetPacketCodec {
  byte[] encodeInput(InputFrame inputFrame);

  InputFrame decodeInput(byte[] payload);

  byte[] encodeState(RuntimeSnapshot snapshot);

  RuntimeSnapshot decodeState(byte[] payload);
}
