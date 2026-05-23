package io.systemtest.container.internal.resolution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the current dependency resolution path.
 *
 * @author Mustapha Zouari
 */
final class ResolutionPath {

  private final ArrayDeque<Class<?>> types = new ArrayDeque<>();

  boolean contains(Class<?> type) {
    return types.contains(type);
  }

  void push(Class<?> type) {
    types.addLast(type);
  }

  void pop() {
    types.removeLast();
  }

  List<String> typeNames() {
    return types.stream().map(Class::getName).toList();
  }

  List<String> typeNamesWith(Class<?> type) {
    final var path = new ArrayList<>(typeNames());
    path.add(type.getName());
    return path;
  }

  String formatCycle(Class<?> repeatedType) {
    final var builder = new StringBuilder();
    for (final Class<?> type : types) {
      builder.append(type.getSimpleName()).append(" -> ");
    }
    builder.append(repeatedType.getSimpleName());
    return builder.toString();
  }
}
