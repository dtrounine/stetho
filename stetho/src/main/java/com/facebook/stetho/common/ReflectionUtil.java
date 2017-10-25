/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

public final class ReflectionUtil {
  private ReflectionUtil() {
  }

  @Nullable
  public static Class<?> tryGetClassForName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Nullable
  public static Field tryGetDeclaredField(Class<?> theClass, String fieldName) {
    try {
      return theClass.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      LogUtil.d(
          e,
          "Could not retrieve %s field from %s",
          fieldName,
          theClass);

      return null;
    }
  }

  @Nullable
  public static Method tryGetPublicMethod(Class<?> theClass,
                                          String methodName,
                                          Class<?> ... paramTypes) {
    try {
      return theClass.getMethod(methodName, paramTypes);
    } catch (NoSuchMethodException e) {
      LogUtil.d(
              e,
              "Could not retrieve %s method from %s",
              methodName,
              theClass);

      return null;
    }
  }

  @Nullable
  public static Object getFieldValue(Field field, Object target) {
    try {
      return field.get(target);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public static Object callMethod(Method method, Object object, Object ... params) {
    try {
      return method.invoke(object, params);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
