package io.systemtest.container.internal.util;

import java.util.List;

/**
 * Internal JSON helpers for compact debug reports.
 *
 * @author Mustapha Zouari
 */
public final class JsonSupport {

  private JsonSupport() {}

  /**
   * Renders strings as a compact JSON array.
   *
   * @param values string values
   * @return JSON array
   */
  public static String stringArray(List<String> values) {
    final var escaped = values.stream().map(value -> "\"" + escape(value) + "\"").toList();
    return "[" + String.join(",", escaped) + "]";
  }

  /**
   * Renders already encoded JSON objects as a compact JSON array.
   *
   * @param jsonValues encoded JSON objects
   * @return JSON array
   */
  public static String jsonObjects(List<String> jsonValues) {
    return "[" + String.join(",", jsonValues) + "]";
  }

  /**
   * Escapes a Java string for JSON string output.
   *
   * @param value source value
   * @return escaped JSON string content without quotes
   */
  public static String escape(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
