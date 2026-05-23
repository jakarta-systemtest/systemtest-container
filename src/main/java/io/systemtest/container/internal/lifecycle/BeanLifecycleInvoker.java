package io.systemtest.container.internal.lifecycle;

import io.systemtest.container.api.config.LifecycleFailureMode;
import io.systemtest.container.api.exception.SystemTestContainerException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal support type for BeanLifecycleInvoker.
 *
 * @author Mustapha Zouari
 */
public final class BeanLifecycleInvoker {

  public record LifecycleBean(Class<?> type, Object instance) {}

  public record LifecycleFailure(
      LifecycleBean bean, String lifecycleAnnotation, String methodName, Throwable error) {}

  public List<LifecycleFailure> postConstruct(
      List<LifecycleBean> beans, LifecycleFailureMode failureMode) {
    return invoke(beans, PostConstruct.class, failureMode);
  }

  public List<LifecycleFailure> preDestroy(
      List<LifecycleBean> beans, LifecycleFailureMode failureMode) {
    return invoke(beans, PreDestroy.class, failureMode);
  }

  private List<LifecycleFailure> invoke(
      List<LifecycleBean> beans,
      Class<? extends Annotation> lifecycleAnnotation,
      LifecycleFailureMode failureMode) {
    final var failures = new ArrayList<LifecycleFailure>();
    for (final var bean : beans) {
      try {
        invoke(
            bean,
            lifecycleAnnotation,
            lifecycleMethods(bean.type(), lifecycleAnnotation),
            failureMode,
            failures);
      } catch (RuntimeException e) {
        if (failureMode == LifecycleFailureMode.STRICT) {
          throw e;
        }
        failures.add(new LifecycleFailure(bean, lifecycleAnnotation.getSimpleName(), "", e));
      }
    }
    return List.copyOf(failures);
  }

  private List<Method> lifecycleMethods(
      Class<?> beanType, Class<? extends Annotation> lifecycleAnnotation) {
    final var methods = new ArrayList<Method>();
    var current = beanType;
    while (current != null && current != Object.class) {
      for (final var method : current.getDeclaredMethods()) {
        if (method.isAnnotationPresent(lifecycleAnnotation)) {
          validate(beanType, method, lifecycleAnnotation);
          methods.add(method);
        }
      }
      current = current.getSuperclass();
    }
    Collections.reverse(methods);
    return methods;
  }

  private void validate(
      Class<?> beanType, Method method, Class<? extends Annotation> lifecycleAnnotation) {
    if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
      throw new SystemTestContainerException(
          "Invalid @"
              + lifecycleAnnotation.getSimpleName()
              + " method "
              + method.getName()
              + " on "
              + beanType.getName()
              + ": lifecycle methods must be non-static and have no parameters");
    }
  }

  private void invoke(
      LifecycleBean bean,
      Class<? extends Annotation> lifecycleAnnotation,
      List<Method> methods,
      LifecycleFailureMode failureMode,
      List<LifecycleFailure> failures) {
    for (final var method : methods) {
      try {
        method.setAccessible(true);
        method.invoke(bean.instance());
      } catch (IllegalAccessException e) {
        handleFailure(failure(bean, lifecycleAnnotation, method, e), failureMode, failures);
      } catch (InvocationTargetException e) {
        final var cause = e.getCause() == null ? e : e.getCause();
        handleFailure(failure(bean, lifecycleAnnotation, method, cause), failureMode, failures);
      }
    }
  }

  private LifecycleFailure failure(
      LifecycleBean bean,
      Class<? extends Annotation> lifecycleAnnotation,
      Method method,
      Throwable cause) {
    return new LifecycleFailure(
        bean,
        lifecycleAnnotation.getSimpleName(),
        method.getName(),
        new SystemTestContainerException(
            "Lifecycle method " + method.getName() + " failed on " + bean.type().getName(), cause));
  }

  private void handleFailure(
      LifecycleFailure failure, LifecycleFailureMode failureMode, List<LifecycleFailure> failures) {
    if (failureMode == LifecycleFailureMode.STRICT) {
      if (failure.error() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new SystemTestContainerException("Lifecycle method failed", failure.error());
    }
    failures.add(failure);
  }
}
