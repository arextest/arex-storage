package com.arextest.storage.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:38
 */
@Slf4j
public class RandomUtils {

  private static final int ID_LENGTH = 16;
  private static final SecureRandom RANDOM = new SecureRandom();

  public static String generateRandomId(String identifier) {
    String sourceString = System.currentTimeMillis() + "-" + RANDOM.nextInt() + "-" + identifier;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte[] bytes = md.digest((sourceString).getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
        String hexString = Integer.toHexString(b & 0xff);
        sb.append(hexString);
      }
      return sb.substring(0, ID_LENGTH);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("generateRandomId error", e);
      return null;
    }

  }
}
