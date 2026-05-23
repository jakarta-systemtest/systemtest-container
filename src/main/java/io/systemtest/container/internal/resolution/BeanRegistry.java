package io.systemtest.container.internal.resolution;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal support type for BeanRegistry.
 *
 * @author Mustapha Zouari
 */
final class BeanRegistry {

  private final Map<Class<?>, Object> beans = new LinkedHashMap<>();
  private final Map<Class<?>, Object> spies = new LinkedHashMap<>();
  private final Map<Class<?>, BeanState> states = new LinkedHashMap<>();

  enum BeanState {
    CREATING,
    REGISTERED,
    COMPLETE
  }

  boolean contains(Class<?> type) {
    return beans.containsKey(type);
  }

  <T> T get(Class<T> type) {
    return type.cast(beans.get(type));
  }

  void put(Class<?> type, Object instance) {
    beans.put(type, instance);
    spies.put(type, instance);
    states.put(type, BeanState.COMPLETE);
  }

  void put(Class<?> type, Object instance, BeanState state) {
    beans.put(type, instance);
    spies.put(type, instance);
    states.put(type, state);
  }

  void putSpyAlias(Class<?> type, Object instance) {
    beans.put(type, instance);
    spies.put(type, instance);
    states.put(type, BeanState.COMPLETE);
  }

  void putMock(Class<?> type, Object instance) {
    beans.put(type, instance);
    states.put(type, BeanState.COMPLETE);
  }

  boolean isComplete(Class<?> type) {
    return states.get(type) == BeanState.COMPLETE;
  }

  BeanState state(Class<?> type) {
    return states.get(type);
  }

  void markCreating(Class<?> type) {
    states.put(type, BeanState.CREATING);
  }

  void markComplete(Class<?> type) {
    states.put(type, BeanState.COMPLETE);
  }

  void removeState(Class<?> type) {
    states.remove(type);
  }

  Map<Class<?>, Object> spiesSnapshot() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(spies));
  }

  List<Class<?>> spyTypes() {
    return List.copyOf(spies.keySet());
  }
}
