package com.arextest.storage.model;

import com.google.common.base.Preconditions;
import java.util.Arrays;

/**
 * reference:com.google.common.hash.HashCode.BytesHashCode
 */
public class ByteHashKey {

  final byte[] bytes;

  public ByteHashKey(byte[] bytes) {
    Preconditions.checkNotNull(bytes);
    this.bytes = bytes;
  }


  public int bits() {
    return bytes.length * 8;
  }

  public int asInt() {
    Preconditions.checkState(
        bytes.length >= 4,
        "HashCode#asInt() requires >= 4 bytes (it only has %s bytes).",
        bytes.length);
    return (bytes[0] & 0xFF)
        | ((bytes[1] & 0xFF) << 8)
        | ((bytes[2] & 0xFF) << 16)
        | ((bytes[3] & 0xFF) << 24);
  }

  @Override
  public int hashCode() {
    if (bits() >= 32) {
      return asInt();
    }
    // If we have less than 4 bytes, use them all.
    byte[] internalBytes = getBytesInternal();
    int val = (internalBytes[0] & 0xFF);
    for (int i = 1; i < internalBytes.length; i++) {
      val |= ((internalBytes[i] & 0xFF) << (i * 8));
    }
    return val;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object instanceof ByteHashKey) {
      ByteHashKey that = (ByteHashKey) object;
      return bits() == that.bits() && equalsSameBits(that);
    }
    return false;
  }

  byte[] getBytesInternal() {
    return bytes;
  }


  public boolean equalsSameBits(ByteHashKey other) {
    return Arrays.equals(bytes, other.bytes);
  }
}