package com.gravity.remake.netcode.api;

public interface SessionTransport {
  void send(byte[] payload);

  byte[] poll();
}
