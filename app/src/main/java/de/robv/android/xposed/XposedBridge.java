package de.robv.android.xposed;

import android.util.Log;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cn.xylin.skiprewardad.Start;

public class XposedBridge {
    public static void log(String text) {
        if (Start.instance != null) {
            Start.instance.log(Log.INFO, "SkipRewardAd", text);
        } else {
            Log.i("SkipRewardAd", text);
        }
    }

    public static void log(Throwable t) {
        if (Start.instance != null) {
            Start.instance.log(Log.ERROR, "SkipRewardAd", "Exception", t);
        } else {
            Log.e("SkipRewardAd", "Exception", t);
        }
    }

    public static Set<XC_MethodHook.MethodHookParam> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.MethodHookParam> hooked = new HashSet<>();
        if (hookClass == null || methodName == null || callback == null) return hooked;

        for (Method method : hookClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                if (Start.instance != null) {
                    Start.instance.hook(method).intercept(chain -> {
                        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
                        param.method = method;
                        param.thisObject = chain.getThisObject();
                        param.args = chain.getArgs();

                        // Call beforeHookedMethod
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

                        // Call original
                        try {
                            Object result = chain.proceed();
                            param.setResult(result);
                        } catch (Throwable t) {
                            param.setThrowable(t);
                        }
                        
                        param.returnEarly = false; // reset for afterHookedMethod

                        // Call afterHookedMethod
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
            }
        }
        return hooked; // Return value is barely used for hookAllMethods, mostly it's Set<Unhook> in original, but here we just stub it
    }
}
