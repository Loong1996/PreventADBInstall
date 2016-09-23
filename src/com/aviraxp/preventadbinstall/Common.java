package com.aviraxp.preventadbinstall;

import android.os.Build;

public class Common {
    public static final int SDK = Build.VERSION.SDK_INT;
    public static final int SHELL_UID = 2000;
    public static final String PACKAGEMANAGERSERVICE = "com.android.server.pm.PackageManagerService";
    public static final boolean JB_MR1_NEWER = SDK >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    public static final boolean LOLLIPOP_NEWER = SDK >= Build.VERSION_CODES.LOLLIPOP;
    public static final String ANDROID_PKG = "android";
}
