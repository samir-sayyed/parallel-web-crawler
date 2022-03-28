package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object targetMethod;
  private final ProfilingState state;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, Object targetMethod, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.targetMethod = targetMethod;
    this.state = state;

//    System.out.println("inside intercepter");
  }

  private boolean isMethodProfiled(Method method) {
    return method.getAnnotation(Profiled.class) != null;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
//    System.out.println("inside invoke");
    Object result;


    Instant start = null;
    boolean isMethodProfiled = isMethodProfiled(method);

    if (isMethodProfiled){
      start = clock.instant();
    }

    try {
      result = method.invoke(targetMethod, args);
    }catch (InvocationTargetException e){
//      System.out.println("inside catch");
      throw e.getTargetException();
    }catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    finally {
      if (isMethodProfiled){
        Duration duration = Duration.between(start, clock.instant());
        state.record(targetMethod.getClass(), method, duration);
//        System.out.println("inside finally");
      }
    }

    return result;
  }


}
