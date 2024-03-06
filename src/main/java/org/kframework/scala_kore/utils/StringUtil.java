// Copyright (c) Runtime Verification, Inc. All Rights Reserved.
package org.kframework.scala_kore.utils;

public class StringUtil {
  /**
   * Removes the first and last double-quote characters and unescapes special characters that start
   * with backslash: newline, carriage return, line feed, tab and backslash. Characters between 127
   * and 255 are stored as \xFF Characters between 256 and 65535 are stored as \uFFFF Characters
   * above 65536 are stored as \u0010FFFF
   *
   * @param str Python like double-quoted string
   * @return unescaped and unquoted string
   */
  public static String unquoteKOREString(String str) {
    StringBuilder sb = new StringBuilder();
    if (str.charAt(0) != '"') {
      throw new IllegalArgumentException(
          "Expected to find double quote at the beginning of string: " + str);
    }
    if (str.charAt(str.length() - 1) != '"') {
      throw new IllegalArgumentException(
          "Expected to find double quote at the end of string: " + str);
    }
    for (int i = 1; i < str.length() - 1; i++) {
      if (str.charAt(i) == '\\') {
        if (str.charAt(i + 1) == '"') {
          sb.append('"');
          i++;
        } else if (str.charAt(i + 1) == '\\') {
          sb.append('\\');
          i++;
        } else if (str.charAt(i + 1) == 'n') {
          sb.append('\n');
          i++;
        } else if (str.charAt(i + 1) == 'r') {
          sb.append('\r');
          i++;
        } else if (str.charAt(i + 1) == 't') {
          sb.append('\t');
          i++;
        } else if (str.charAt(i + 1) == 'f') {
          sb.append('\f');
          i++;
        } else if (str.charAt(i + 1) == 'x') {
          String arg = str.substring(i + 2, i + 4);
          sb.append((char) Integer.parseInt(arg, 16));
          i += 3;
        } else if (str.charAt(i + 1) == 'u') {
          String arg = str.substring(i + 2, i + 6);
          int codePoint = Integer.parseInt(arg, 16);
          StringUtil.throwIfSurrogatePair(codePoint);
          sb.append((char) codePoint);
          i += 5;
        } else if (str.charAt(i + 1) == 'U') {
          String arg = str.substring(i + 2, i + 10);
          int codePoint = Integer.parseInt(arg, 16);
          StringUtil.throwIfSurrogatePair(codePoint);
          sb.append(Character.toChars(codePoint));
          i += 9;
        }
      } else {
        sb.append(str.charAt(i));
      }
    }
    return sb.toString();
  }

  /**
   * Adds double-quote at the beginning and end of the string and escapes special characters with
   * backslash: newline, carriage return, line feed, tab and backslash. Characters between 127 and
   * 255 are stored as \xFF Characters between 256 and 65535 are stored as \uFFFF Characters above
   * 65536 are stored as \u0010FFFF
   *
   * @param value any string
   * @return Python like textual representation of the string
   */
  public static String enquoteKOREString(String value) {
    final int length = value.length();
    StringBuilder result = new StringBuilder();
    result.append("\"");
    for (int offset = 0, codepoint; offset < length; offset += Character.charCount(codepoint)) {
      codepoint = value.codePointAt(offset);
      if (codepoint == '"') {
        result.append("\\\"");
      } else if (codepoint == '\\') {
        result.append("\\\\");
      } else if (codepoint == '\n') {
        result.append("\\n");
      } else if (codepoint == '\t') {
        result.append("\\t");
      } else if (codepoint == '\r') {
        result.append("\\r");
      } else if (codepoint == '\f') {
        result.append("\\f");
      } else {
        result.append(StringUtil.getUnicodeEscape(codepoint));
      }
    }
    result.append("\"");
    return result.toString();
  }

  private static void throwIfSurrogatePair(int codePoint) {
    if (codePoint >= 0xd800 && codePoint <= 0xdfff) {
      // we are trying to encode a surrogate pair, which the unicode
      // standard forbids
      throw new IllegalArgumentException(
          Integer.toHexString(codePoint) + " is not in the accepted unicode range.");
    }
    if (codePoint >= 0x110000)
      throw new IllegalArgumentException(
          Integer.toHexString(codePoint) + " is not in the accepted unicode range.");
  }

  /**
   * Get the escaped string for a Unicode codepoint: Codepoints between 32 and 126 are stored
   * directly as the character Codepoints between 0 and 31 and between 127 and 255 are stored as
   * \xFF Codepoints between 256 and 65535 are stored as \uFFFF Codepoints above 65536 are stored as
   * \u0010FFFF
   *
   * @param codepoint a Unicode codepoint
   * @return representation of the codepoint as an escaped string
   */
  private static String getUnicodeEscape(int codepoint) {
    if (32 <= codepoint && codepoint < 127) {
      return String.valueOf((char) codepoint);
    }
    if (codepoint <= 0xff) {
      return "\\x" + String.format("%02x", codepoint);
    }
    if (codepoint <= 0xffff) {
      return "\\u" + String.format("%04x", codepoint);
    }
    return "\\U" + String.format("%08x", codepoint);
  }
}
