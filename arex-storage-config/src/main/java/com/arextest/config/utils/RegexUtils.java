package com.arextest.config.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author b_yu
 * @since 2024/12/20
 */

public class RegexUtils {

  private static final Set<Character> SPECIAL_CHARS = new HashSet<>();

  static {
    char[] specialCharsArray = {'.', '\\', '+', '*', '?', '[', '^', ']', '$', '(', ')', '{', '}',
        '=', '!', '<', '>', '|', ':', '-'};
    for (char c : specialCharsArray) {
      SPECIAL_CHARS.add(c);
    }
  }

  public static String getRegexForFuzzySearch(String keyword) {
    if (keyword == null || keyword.isEmpty()) {
      return keyword;
    }
    return ".*?" + escapeSpecialChars(keyword) + ".*";
  }

  private static String escapeSpecialChars(String keyword) {
    if (keyword == null || keyword.isEmpty()) {
      return keyword;
    }
    StringBuilder escapedKeyword = new StringBuilder();
    for (char c : keyword.toCharArray()) {
      if (SPECIAL_CHARS.contains(c)) {
        escapedKeyword.append('\\');
      }
      escapedKeyword.append(c);
    }
    return escapedKeyword.toString();
  }
}
