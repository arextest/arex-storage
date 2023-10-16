package com.arextest.storage.mock.internal.matchkey.impl;

import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The request should be large use streaming to md5
 *
 * @author jmo
 * @since 2022/11/23
 */
public class MessageDigestWriter extends OutputStream {

  private static final String MD5_ALGORITHM_NAME = "MD5";
  private final MessageDigest messageDigest;

  public MessageDigestWriter(MessageDigest messageDigest) {
    this.messageDigest = messageDigest;
  }

  @Override
  public void write(int b) {
    messageDigest.update((byte) b);
  }

  public static MessageDigest getMD5Digest() {
    try {
      return MessageDigest.getInstance(MD5_ALGORITHM_NAME);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(
          "Could not find MessageDigest with algorithm \"" + MD5_ALGORITHM_NAME +
              "\"", exception);
    }
  }
}