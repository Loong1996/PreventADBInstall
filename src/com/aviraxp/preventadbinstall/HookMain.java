package com.aviraxp.preventadbinstall;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static android.os.Binder.getCallingUid;

public class HookMain implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    public XC_MethodHook installPackageHook;
    public Context mContext;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {

        installPackageHook = new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mContext = AndroidAppHelper.currentApplication();
                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                boolean isInstallStage = "installStage".equals(param.method.getName());
                int flags = 0;
                int id = 0;

                if (isInstallStage) {
                    try {
                        id = 4;
                        flags = (Integer) XposedHelpers.getObjectField(param.args[id], "installFlags");
                        XposedBridge.log("PreventADBInstall: Hook Flags Success!");
                    } catch (Exception e) {
                        XposedBridge.log(e);
                    }
                } else {
                    try {
                        id = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? 2 : 1;
                        flags = (Integer) param.args[id];
                        XposedBridge.log("PreventADBInstall: Hook Flags Success!");
                    } catch (Exception e) {
                        XposedBridge.log(e);
                    }
                }

                if (isInstallStage) {
                    Object sessions = param.args[id];
                    XposedHelpers.setIntField(sessions, "installFlags", flags);
                    param.args[id] = sessions;
                } else {
                    param.args[id] = flags;
                }

                if (getCallingUid() == 2000) || (getCallingUid() == 0) {
                    param.setResult(null);
                    XposedBridge.log("PreventADBInstall: Block Success!");
                    Looper.prepare();
                    Toast.makeText(mContext, "拦截 ADB 安装成功！", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    return;
                }
            }
        };
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if ("android".equals(lpparam.packageName) && "android".equals(lpparam.processName)) {
            Class<?> packageManagerClass = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpparam.classLoader);
            XposedBridge.hookAllMethods(packageManagerClass, "installStage", installPackageHook);
            XposedBridge.log("PreventADBInstall: Hook InstallStage Success!");
        } else {
            return;
        }
    }
}
