package io.systemtest.container.api.exception;

import java.util.List;

/**
 * Thrown when an interface or abstract class has more than one concrete implementation.
 *
 * @author Mustapha Zouari
 */
public class AmbiguousImplementationException extends SystemTestContainerException {

  /** Requested dependency type. */
  private final Class<?> dependencyType;

  /** Matching concrete implementations. */
  private final List<Class<?>> implementations;

  /**
   * Creates an ambiguity exception.
   *
   * @param dependencyType requested abstract dependency
   * @param implementations matching concrete implementations
   */
  public AmbiguousImplementationException(Class<?> dependencyType, List<Class<?>> implementations) {
    super(message(dependencyType, implementations));
    this.dependencyType = dependencyType;
    this.implementations = List.copyOf(implementations);
  }

  /**
   * Returns the requested dependency type.
   *
   * @return requested dependency type
   */
  public Class<?> dependencyType() {
    return dependencyType;
  }

  /**
   * Returns matching implementations.
   *
   * @return matching implementations
   */
  public List<Class<?>> implementations() {
    return implementations;
  }

  private static String message(Class<?> dependencyType, List<Class<?>> implementations) {
    return "Multiple implementations found for "
        + dependencyType.getName()
        + ": "
        + implementations.stream().map(Class::getName).toList();
  }
}
