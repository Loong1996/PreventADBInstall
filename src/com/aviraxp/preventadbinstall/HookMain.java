package com.aviraxp.preventadbinstall;

import android.content.Context;
import android.app.AndroidAppHelper;

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
                boolean isInstallStage = "installStage".equals(param.method
                        .getName());
                int flags = 0 ;
                int id = 0;

                if (isInstallStage) {
                    try {
                        id = 4;
                        flags = (Integer) XposedHelpers.getObjectField(param.args[id], "installFlags");
                        XposedBridge.log("Hook InstallStage Success");
                    } catch (Exception e) {
                        XposedBridge.log(e);
                        XposedBridge.log("Stacktrace follows:");
                        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                            XposedBridge.log("HookDetection: " + stackTraceElement.getClassName() + "->" + stackTraceElement.getMethodName());
                        }
                    }
                } else {
                    try {
                        id = Common.JB_MR1_NEWER ? 2 : 1;
                        flags = (Integer) param.args[id];
                        XposedBridge.log("Hook InstallStage Success");
                    } catch (Exception e) {
                        XposedBridge.log(e);
                        XposedBridge.log("Stacktrace follows:");
                        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                            XposedBridge.log("HookDetection: " + stackTraceElement.getClassName() + "->" + stackTraceElement.getMethodName());
                        }
                    }
                }

                if (isInstallStage) {
                    Object sessions = param.args[id];
                    XposedHelpers.setIntField(sessions, "installFlags",
                            flags);
                    param.args[id] = sessions;
                } else {
                    param.args[id] = flags;
                }

                if (getCallingUid() == Common.SHELL_UID) {
                    param.setResult(null);
                    XposedBridge.log("Prevent ADB Install");
                    return;
                }
            }
        };
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (Common.ANDROID_PKG.equals(lpparam.packageName)
                && Common.ANDROID_PKG.equals(lpparam.processName)) {
            Class<?> packageManagerClass = XposedHelpers.findClass(
                    Common.PACKAGEMANAGERSERVICE, lpparam.classLoader);
            if (Common.LOLLIPOP_NEWER) {
                // 5.0 and newer
                XposedBridge.hookAllMethods(packageManagerClass,
                        "installPackageAsUser", installPackageHook);
                XposedBridge.hookAllMethods(packageManagerClass,
                        "installStage", installPackageHook);
            } else {
                if (Common.JB_MR1_NEWER) {
                    // 4.2 - 4.4
                    XposedBridge.hookAllMethods(packageManagerClass,
                            "installPackageWithVerificationAndEncryption",
                            installPackageHook);
                } else {
                    // 4.0 - 4.1
                    XposedBridge.hookAllMethods(packageManagerClass,
                            "installPackageWithVerification",
                            installPackageHook);
                }
            }
        }
    }
}
