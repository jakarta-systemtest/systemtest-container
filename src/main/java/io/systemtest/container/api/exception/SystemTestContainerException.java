package io.systemtest.container.api.exception;

/**
 * Base runtime exception thrown by the system test container.
 *
 * @author Mustapha Zouari
 */
public class SystemTestContainerException extends RuntimeException {

  /**
   * Creates an exception with a message.
   *
   * @param message exception message
   */
  public SystemTestContainerException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a message and cause.
   *
   * @param message exception message
   * @param cause original cause
   */
  public SystemTestContainerException(String message, Throwable cause) {
    super(message, cause);
  }
}
