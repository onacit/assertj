/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2018 the original author or authors.
 */
package org.assertj.core.api;

import static net.bytebuddy.matcher.ElementMatchers.nameContainsIgnoreCase;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.TypeCache.SimpleKey;
import net.bytebuddy.TypeCache.Sort;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

class SoftProxies {

  private final ErrorCollector collector = new ErrorCollector();
  private final TypeCache<TypeCache.SimpleKey> cache = new TypeCache.WithInlineExpunction<>(Sort.SOFT);

  void collectError(Throwable error) {
    collector.addError(error);
  }

  List<Throwable> errorsCollected() {
    return collector.errors();
  }

  @SuppressWarnings("unchecked")
  <V, T> V create(final Class<V> assertClass, Class<T> actualClass, T actual) {

    try {
      Class<V> proxyClass = (Class<V>) cache
                                            .findOrInsert(getClass().getClassLoader(), new SimpleKey(assertClass),
                                                          () -> createProxy(assertClass, collector));

      Constructor<? extends V> constructor = proxyClass.getConstructor(actualClass);
      return constructor.newInstance(actual);
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private <V> Class<?> createProxy(Class<V> assertClass, ErrorCollector collector) {
    Junction<MethodDescription> specialMethods = ElementMatchers.<MethodDescription> nameContainsIgnoreCase("extracting")
                                                                .or(nameContainsIgnoreCase("filteredOn"))
                                                                .or(nameContainsIgnoreCase("map"))
                                                                .or(nameContainsIgnoreCase("flatMap"))
                                                                .or(nameContainsIgnoreCase("flatExtracting"));

    return new ByteBuddy().subclass(assertClass)
                          .method(specialMethods)
                          .intercept(MethodDelegation.to(new ProxifyExtractingResult(this)))
                          .method(ElementMatchers.<MethodDescription> any().and(ElementMatchers.not(specialMethods)))
                          .intercept(MethodDelegation.to(collector))
                          .make()
                          .load(getClass().getClassLoader())
                          .getLoaded();
  }

  public boolean wasSuccess() {
    return collector.wasSuccess();
  }

}
