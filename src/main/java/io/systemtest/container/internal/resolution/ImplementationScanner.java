package io.systemtest.container.internal.resolution;

import io.systemtest.container.api.config.LogLevel;
import io.systemtest.container.api.exception.SystemTestContainerException;
import io.systemtest.container.internal.util.LoggingSupport;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Internal support type for ImplementationScanner.
 *
 * @author Mustapha Zouari
 */
public final class ImplementationScanner {

  private static final Logger LOGGER = System.getLogger(ImplementationScanner.class.getName());

  private final ClassLoader classLoader;
  private final LogLevel logLevel;

  public ImplementationScanner(ClassLoader classLoader, LogLevel logLevel) {
    this.classLoader = classLoader;
    this.logLevel = logLevel;
  }

  public List<Class<?>> findImplementations(Class<?> dependencyType) {
    final var packageName = dependencyType.getPackageName();
    final var packagePath = packageName.replace('.', '/');
    log(
        LogLevel.TRACE,
        "Scanning package {0} for implementations of {1}",
        packageName,
        dependencyType.getName());

    final var implementations = new ArrayList<Class<?>>();
    try {
      collectImplementations(dependencyType, packageName, packagePath, implementations);
    } catch (IOException e) {
      throw new SystemTestContainerException(
          "Cannot scan implementations for " + dependencyType.getName(), e);
    }

    return implementations.stream()
        .distinct()
        .sorted(Comparator.comparing(Class::getName))
        .toList();
  }

  private void collectImplementations(
      Class<?> dependencyType,
      String packageName,
      String packagePath,
      List<Class<?>> implementations)
      throws IOException {
    final var resources = classLoader.getResources(packagePath);
    while (resources.hasMoreElements()) {
      final var resource = resources.nextElement();
      if (!"file".equals(resource.getProtocol())) {
        log(LogLevel.DEBUG, "Skipping non-file classpath resource {0}", resource);
        continue;
      }
      implementations.addAll(findImplementationsInDirectory(dependencyType, packageName, resource));
    }
  }

  private List<Class<?>> findImplementationsInDirectory(
      Class<?> dependencyType, String packageName, URL packageDirectory) {
    try {
      final var root = Path.of(packageDirectory.toURI());
      try (var paths = Files.walk(root)) {
        final var implementations = new ArrayList<Class<?>>();
        paths
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".class"))
            .map(path -> className(root, path, packageName))
            .map(this::loadClass)
            .filter(candidate -> isImplementation(dependencyType, candidate))
            .forEach(implementations::add);
        return implementations;
      }
    } catch (IOException | URISyntaxException e) {
      throw new SystemTestContainerException(
          "Cannot scan implementations in " + packageDirectory + " for " + dependencyType.getName(),
          e);
    }
  }

  private String className(Path root, Path classFile, String packageName) {
    final var relativeClassName =
        root.relativize(classFile)
            .toString()
            .replace('\\', '.')
            .replace('/', '.')
            .replaceAll("\\.class$", "");
    if (packageName.isBlank()) {
      return relativeClassName;
    }
    return packageName + "." + relativeClassName;
  }

  private Class<?> loadClass(String className) {
    try {
      return Class.forName(className, false, classLoader);
    } catch (LinkageError | ClassNotFoundException e) {
      log(
          LogLevel.DEBUG,
          "Skipping class {0} because it cannot be loaded: {1}",
          className,
          e.toString());
      return null;
    }
  }

  private boolean isImplementation(Class<?> dependencyType, Class<?> candidate) {
    if (candidate == null || candidate == dependencyType) {
      return false;
    }
    final var modifiers = candidate.getModifiers();
    return dependencyType.isAssignableFrom(candidate)
        && !candidate.isInterface()
        && !Modifier.isAbstract(modifiers);
  }

  private void log(LogLevel eventLevel, String message, Object... args) {
    LoggingSupport.log(LOGGER, logLevel, eventLevel, message, args);
  }
}
