package top.ss007.binder;

import android.app.Activity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import top.ss007.annotation.internal.BindingSuffix;

/**
 * Copyright (C) 2019 shusheng007
 * 完全享有此软件的著作权，违者必究
 *
 * @author shusheng007
 * @version 1.0
 * @modifier
 * @createDate 2019/5/18 11:05
 * @description
 */
public class Binding {
    private Binding(){}

    private static <T extends Activity> void instantiateBinder(T target,String suffix){
        Class<?> targetClass=target.getClass();
        String className=targetClass.getName();
        try {
            Class<?>bindingClass =targetClass
                    .getClassLoader()
                    .loadClass(className+suffix);
            Constructor<?> classConstructor=bindingClass.getConstructor(targetClass);
            try {
                classConstructor.newInstance(target);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to invoke " + classConstructor, e);
            } catch (InstantiationException e) {
                throw new RuntimeException("Unable to invoke " + classConstructor, e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw new RuntimeException("Unable to create instance.", cause);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find Class for " + className + suffix, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find constructor for " + className + suffix, e);
        }
    }

    public static <T extends Activity> void bind(T activity) {
        instantiateBinder(activity, BindingSuffix.GENERATED_CLASS_SUFFIX);
    }
}
