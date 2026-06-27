package cn.xylin.skiprewardad;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

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
import io.github.libxposed.api.XposedModuleInterface;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;

public class Start extends XposedModule {
    public static Start instance;
    private int hash;

    public Start(@NonNull XposedModuleInterface.Instantiator instantiator) {
        super(instantiator);
        instance = this;
    }

    @Override
    public void onPackageLoaded(@NonNull XposedModuleInterface.PackageLoadedParam param) {
        if (!param.isFirstPackage()) {
            return;
        }

        boolean isMainProcess = param.getPackageName().equals(param.getProcessName());
        boolean isAwemeMiniApp = param.getPackageName().equals("com.ss.android.ugc.aweme") && 
                (param.getProcessName().contains(":miniapp") || param.getProcessName().contains(":appbrand"));
        
        if (!isMainProcess && !isAwemeMiniApp) {
            return;
        }
        
        // Initialize XposedHelpers classloader for the current package
        XposedHelpers.classLoader = param.getClassLoader();

        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam hookParam) throws Throwable {
                    if (hash == hashCode()) {
                        return;
                    }
                    hash = hashCode();
                    startHook((Context) hookParam.args[0]);
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
