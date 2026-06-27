package de.robv.android.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import cn.xylin.skiprewardad.Start;

public class XposedHelpers {
    public static ClassLoader classLoader = null; // Store it from Start.java

    public static java.lang.reflect.Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type) {
                field.setAccessible(true);
                return field;
            }
        }
        for (java.lang.reflect.Field field : clazz.getFields()) {
            if (field.getType() == type) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldError("Field of type " + type.getName() + " not found in " + clazz.getName());
    }

    public static Object getObjectField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = obj.getClass().getField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception e2) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            Class<?> clazz = obj.getClass();
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName)) {
                    if (args == null) args = new Object[0];
                    if (m.getParameterTypes().length == args.length) {
                        m.setAccessible(true);
                        return m.invoke(obj, args);
                    }
                }
            }
            // fallback to declared methods
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    if (args == null) args = new Object[0];
                    if (m.getParameterTypes().length == args.length) {
                        m.setAccessible(true);
                        return m.invoke(obj, args);
                    }
                }
            }
            throw new NoSuchMethodError(methodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        try {
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                if (args == null) args = new Object[0];
                if (c.getParameterTypes().length == args.length) {
                    c.setAccessible(true);
                    return c.newInstance(args);
                }
            }
            throw new NoSuchMethodError("No matching constructor");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook)) {
            throw new IllegalArgumentException("Last argument must be an XC_MethodHook");
        }

        XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        
        List<Class<?>> paramTypes = new ArrayList<>();
        for (int i = 0; i < parameterTypesAndCallback.length - 1; i++) {
            Object type = parameterTypesAndCallback[i];
            if (type instanceof Class) {
                paramTypes.add((Class<?>) type);
            } else if (type instanceof String) {
                paramTypes.add(findClass((String) type, clazz.getClassLoader()));
            }
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes.toArray(new Class<?>[0]));
            if (Start.instance != null) {
                Start.instance.hook(method).intercept(chain -> {
                    XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
                    param.method = method;
                    param.thisObject = chain.getThisObject();
                    param.args = chain.getArgs().toArray(new Object[0]);

                    try {
                        callback.beforeHookedMethod(param);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                        param.setResult(null);
                        param.returnEarly = true;
                    }

                    if (param.returnEarly) {
                        if (param.hasThrowable()) {
                            throw param.getThrowable();
                        }
                        return param.getResult();
                    }

                    try {
                        Object result = chain.proceed();
                        param.setResult(result);
                    } catch (Throwable t) {
                        param.setThrowable(t);
                    }

                    param.returnEarly = false;
                    
                    try {
                        callback.afterHookedMethod(param);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }

                    if (param.hasThrowable()) {
                        throw param.getThrowable();
                    }
                    return param.getResult();
                });
            }
        } catch (NoSuchMethodException e) {
            XposedBridge.log(e);
        }
    }
}
