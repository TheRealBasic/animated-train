package com.gravity.remake.persistence.api;

public interface Serializer<T> {
  byte[] toBytes(T value);

  T fromBytes(byte[] bytes);
}
