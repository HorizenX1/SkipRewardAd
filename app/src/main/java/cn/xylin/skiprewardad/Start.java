package cn.xylin.skiprewardad;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.os.Build;

import cn.xylin.skiprewardad.hook.ApplovinAdHook;
import cn.xylin.skiprewardad.hook.AwemeMiniGameHook;
import cn.xylin.skiprewardad.hook.BaiduAdHook;
import cn.xylin.skiprewardad.hook.FusionAdHook;
import cn.xylin.skiprewardad.hook.GdtAdHook1;
import cn.xylin.skiprewardad.hook.GdtAdHook2;
import cn.xylin.skiprewardad.hook.GoogleAdHook1;
import cn.xylin.skiprewardad.hook.IronSourceHook;
import cn.xylin.skiprewardad.hook.KsAdHook;
import cn.xylin.skiprewardad.hook.MintegralAdHook;
import cn.xylin.skiprewardad.hook.SigmobAdHook;
import cn.xylin.skiprewardad.hook.TTAdHook;
import cn.xylin.skiprewardad.hook.UnityAdHook1;
import cn.xylin.skiprewardad.hook.UnityAdHook2;
import cn.xylin.skiprewardad.hook.VungleAdHook;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;

public class Start extends XposedModule {
    public static Start instance;
    private int hash;

    public Start() {
        super();
        instance = this;
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        XposedHelpers.classLoader = param.getDefaultClassLoader();
        
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam hookParam) throws Throwable {
                    if (hash == hashCode()) {
                        return;
                    }
                    hash = hashCode();
                    Context context = (Context) hookParam.args[0];
                    String processName = "";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        processName = Application.getProcessName();
                    }
                    
                    boolean isMainProcess = param.getPackageName().equals(processName) || processName == null || processName.isEmpty();
                    boolean isAwemeMiniApp = param.getPackageName().equals("com.ss.android.ugc.aweme") && 
                            (processName.contains(":miniapp") || processName.contains(":appbrand"));
                    
                    if (isMainProcess || isAwemeMiniApp) {
                        startHook(context);
                    }
                }
            });
        } catch (Throwable e) {
            log(Log.ERROR, "Start", "Failed to hook Application.attach", e);
        }
    }

    private synchronized void startHook(Context baseContext) {
        new GdtAdHook1(baseContext);
        new GdtAdHook2(baseContext);
        new FusionAdHook(baseContext);
        new TTAdHook(baseContext);
        new BaiduAdHook(baseContext);
        new SigmobAdHook(baseContext);
        new MintegralAdHook(baseContext);
        new UnityAdHook1(baseContext);
        new UnityAdHook2(baseContext);
        new KsAdHook(baseContext);
        new VungleAdHook(baseContext);
        new GoogleAdHook1(baseContext);
        new ApplovinAdHook(baseContext);
        new AwemeMiniGameHook(baseContext);
        new IronSourceHook(baseContext);
    }
}
