package cn.xylin.skiprewardad.hook;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AwemeMiniGameHook extends BaseHook {

    public AwemeMiniGameHook(Context ctx) {
        super(ctx);
    }

    @Override
    protected void runHook() throws Throwable {
        // 1. 尝试拦截常见的 JS 注入 (WebView 方案)
        // 很多小游戏的广告回调最终会通过 evaluateJavascript 或 loadUrl 通知前端
        hookWebViewJSBridge();
        
        // 2. 尝试寻找小游戏内部的激励视频 Activity 并秒关
        hookMiniGameActivity();
    }
    
    private void hookWebViewJSBridge() {
        try {
            XposedHelpers.findAndHookMethod(WebView.class, "evaluateJavascript", String.class, ValueCallback.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String script = (String) param.args[0];
                    if (script != null && (script.contains("showVideoAd") || script.contains("operateVideoAd") || script.contains("RewardedVideoAd"))) {
                        // 替换未看完、关闭、失败等状态为成功状态
                        if (script.contains("cancel") || script.contains("fail") || script.contains("isEnded\":false") || script.contains("isEnded\": false")) {
                            log("AwemeMiniGame: 拦截到广告失败 JS 回调，尝试篡改: " + script);
                            script = script.replace("cancel", "ok")
                                           .replace("fail", "ok")
                                           .replace("isEnded\":false", "isEnded\":true")
                                           .replace("isEnded\": false", "isEnded\": true");
                            param.args[0] = script;
                        }
                    }
                }
            });
            
            XposedHelpers.findAndHookMethod(WebView.class, "loadUrl", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String url = (String) param.args[0];
                    if (url != null && url.startsWith("javascript:") && (url.contains("showVideoAd") || url.contains("RewardedVideoAd"))) {
                        if (url.contains("cancel") || url.contains("fail") || url.contains("isEnded\":false")) {
                            log("AwemeMiniGame: 拦截到广告失败 loadUrl JS 回调，尝试篡改: " + url);
                            url = url.replace("cancel", "ok")
                                     .replace("fail", "ok")
                                     .replace("isEnded\":false", "isEnded\":true")
                                     .replace("isEnded\": false", "isEnded\": true");
                            param.args[0] = url;
                        }
                    }
                }
            });
        } catch (Throwable e) {
            log("AwemeMiniGame: WebView Hook 失败 " + e.getMessage());
        }
    }

    private void hookMiniGameActivity() {
        // 拦截 Activity 的启动，如果是疑似激励视频，则直接结束它
        XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String clsName = activity.getClass().getName();
                
                // 抖音内部和穿山甲的激励视频 Activity 特征
                if (clsName.contains("RewardVideo") || clsName.contains("AdActivity") || clsName.contains("TTFullScreenVideo")) {
                    log("AwemeMiniGame: 疑似广告 Activity 被拉起: " + clsName);
                    // 延迟一点点关闭，确保生命周期触发，从而让宿主发出 callback
                    getHandler().postDelayed(() -> {
                        if (!activity.isFinishing()) {
                            activity.finish();
                            log("AwemeMiniGame: 强制关闭广告 Activity " + clsName);
                        }
                    }, 500);
                }
            }
        });
    }

    @Override
    protected String targetPackageName() {
        return "com.ss.android.ugc.aweme";
    }

    @Override
    protected boolean isTarget() {
        return targetPackageName().equals(context.getPackageName());
    }
}
