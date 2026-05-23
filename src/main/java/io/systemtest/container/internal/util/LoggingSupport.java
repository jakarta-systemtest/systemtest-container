package io.systemtest.container.internal.util;

import io.systemtest.container.api.config.LogLevel;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Internal helpers for applying System Test Container log-level filtering before calling the JDK
 * logger.
 *
 * @author Mustapha Zouari
 */
public final class LoggingSupport {

  private LoggingSupport() {}

  public static void log(
      Logger logger,
      LogLevel configuredLevel,
      LogLevel eventLevel,
      String message,
      Object... args) {
    if (!configuredLevel.includes(eventLevel)) {
      return;
    }
    logger.log(systemLevel(eventLevel), message, args);
  }

  private static Level systemLevel(LogLevel level) {
    return switch (level) {
      case ERROR -> Level.ERROR;
      case WARN -> Level.WARNING;
      case INFO -> Level.INFO;
      case DEBUG -> Level.DEBUG;
      case TRACE -> Level.TRACE;
      case OFF -> Level.ALL;
    };
  }
}
