package io.systemtest.container.internal.util;

import io.systemtest.container.api.exception.SystemTestContainerException;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Internal support type for ReflectionSupport.
 *
 * @author Mustapha Zouari
 */
public final class ReflectionSupport {

  private ReflectionSupport() {}

  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> selectConstructor(Class<T> type) {
    final var injectableConstructors = injectableConstructors(type);

    if (injectableConstructors.size() > 1) {
      throw new SystemTestContainerException(
          "Multiple @Inject constructors found on " + type.getName());
    }
    if (injectableConstructors.size() == 1) {
      return (Constructor<T>) injectableConstructors.get(0);
    }

    try {
      return type.getDeclaredConstructor();
    } catch (NoSuchMethodException ignored) {
      return selectSingleDeclaredConstructor(type);
    }
  }

  public static List<Field> injectableFields(Class<?> type) {
    final var fields = new ArrayList<Field>();
    var current = type;
    while (current != null && current != Object.class) {
      for (final var field : current.getDeclaredFields()) {
        if (isInjectableField(field)) {
          validateInjectableField(field);
          fields.add(field);
        }
      }
      current = current.getSuperclass();
    }
    return fields;
  }

  private static List<Constructor<?>> injectableConstructors(Class<?> type) {
    return Arrays.stream(type.getDeclaredConstructors())
        .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
        .toList();
  }

  @SuppressWarnings("unchecked")
  private static <T> Constructor<T> selectSingleDeclaredConstructor(Class<T> type) {
    final var constructors = type.getDeclaredConstructors();
    if (constructors.length == 1) {
      return (Constructor<T>) constructors[0];
    }
    throw new SystemTestContainerException(
        "No default or @Inject constructor found on " + type.getName());
  }

  private static boolean isInjectableField(Field field) {
    return field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(EJB.class);
  }

  private static void validateInjectableField(Field field) {
    final var modifiers = field.getModifiers();
    if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
      throw new SystemTestContainerException(
          "Field "
              + field.getName()
              + " on "
              + field.getDeclaringClass().getName()
              + " cannot be injected because injection fields cannot be static or final");
    }
  }
}
