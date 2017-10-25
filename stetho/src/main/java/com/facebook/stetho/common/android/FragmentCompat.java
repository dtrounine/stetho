/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.app.Activity;
import android.os.Build;
import com.facebook.stetho.common.ReflectionUtil;
import com.facebook.stetho.common.Util;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Compatibility abstraction which allows us to generalize access to both the
 * support library's fragments and the built-in framework version.  Note: both versions
 * can be live at the same time in a single application and even on a single object
 * instance.
 * <p/>
 * Type safety is enforced via generics internal to the implementation but treated
 * as opaque from the outside.
 *
 * @param <FRAGMENT>
 * @param <DIALOG_FRAGMENT>
 * @param <FRAGMENT_MANAGER>
 * @param <FRAGMENT_ACTIVITY>
 */
@NotThreadSafe
public abstract class FragmentCompat<
    FRAGMENT,
    DIALOG_FRAGMENT,
    FRAGMENT_MANAGER,
    FRAGMENT_ACTIVITY extends Activity> {
  private static FragmentCompat sFrameworkInstance;
  private static FragmentCompat sSupportInstance;

  private static final boolean sHasSupportFragment;

  static {
    sHasSupportFragment = ReflectionUtil.tryGetClassForName(
        "android.support.v4.app.Fragment") != null;
  }

  @Nullable
  public static FragmentCompat getFrameworkInstance() {
    if (sFrameworkInstance == null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      sFrameworkInstance = new FragmentCompatFramework();
    }
    return sFrameworkInstance;
  }

  @Nullable
  public static FragmentCompat getSupportLibInstance() {
    if (sSupportInstance == null &&
        sHasSupportFragment) {
      sSupportInstance = new FragmentCompatSupportLib();
    }
    return sSupportInstance;
  }

  FragmentCompat() {
  }

  public abstract Class<FRAGMENT> getFragmentClass();
  public abstract Class<DIALOG_FRAGMENT> getDialogFragmentClass();
  public abstract Class<FRAGMENT_ACTIVITY> getFragmentActivityClass();

  public abstract FragmentAccessor<FRAGMENT, FRAGMENT_MANAGER> forFragment();
  public abstract DialogFragmentAccessor<DIALOG_FRAGMENT, FRAGMENT, FRAGMENT_MANAGER>
      forDialogFragment();
  public abstract FragmentManagerAccessor<FRAGMENT_MANAGER, FRAGMENT> forFragmentManager();
  public abstract FragmentActivityAccessor<FRAGMENT_ACTIVITY, FRAGMENT_MANAGER> forFragmentActivity();

  static class FragmentManagerAccessorViaReflection<FRAGMENT_MANAGER, FRAGMENT>
      implements FragmentManagerAccessor<FRAGMENT_MANAGER, FRAGMENT> {

    @Nullable
    private Field mFieldMAdded;
    @Nullable
    private Class mFieldMAddedClass;

    @Nullable
    private Method mMethodGetFragments;
    @Nullable
    private Class mMethodGetFragmentsClass;

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public List<FRAGMENT> getAddedFragments(FRAGMENT_MANAGER fragmentManager) {
      if (mFieldMAddedClass != fragmentManager.getClass()) {
        // Implementation of FragmentManager has changed since previous call. Reset field
        mFieldMAdded = null;
      }
      // This field is actually sitting on FragmentManagerImpl, which derives from FragmentManager.
      if (mFieldMAdded == null) {
        Field fieldMAdded = ReflectionUtil.tryGetDeclaredField(
            fragmentManager.getClass(),
            "mAdded");

        if (fieldMAdded != null) {
          fieldMAdded.setAccessible(true);
          mFieldMAdded = fieldMAdded;
          mFieldMAddedClass = fragmentManager.getClass();
        }
      }

      return (mFieldMAdded != null)
          ? (List<FRAGMENT>)ReflectionUtil.getFieldValue(mFieldMAdded, fragmentManager)
          : getActiveFragments(fragmentManager); // fallback to getFragments() method
    }

    private List<FRAGMENT> getActiveFragments(FRAGMENT_MANAGER fragmentManager) {
      if (mMethodGetFragmentsClass != fragmentManager.getClass()) {
        // Implementation has changed since last call, reset the method.
        mMethodGetFragments = null;
      }
      if (mMethodGetFragments == null) {
        mMethodGetFragments = ReflectionUtil.tryGetPublicMethod(fragmentManager.getClass(),
                "getFragments");
        mMethodGetFragmentsClass = fragmentManager.getClass();
      }
      return mMethodGetFragments != null
          ? (List<FRAGMENT>)ReflectionUtil.callMethod(mMethodGetFragments, fragmentManager)
          : null;
    }
  }
}
