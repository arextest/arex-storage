package com.arextest.storage.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:38
 */
@Slf4j
public class RandomUtils {
    private static final int ID_LENGTH = 16;

    public static String generateRandomId(String identifier) {
        String sourceString = System.currentTimeMillis() + "-" + new Random().nextInt() + "-" + identifier;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
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
