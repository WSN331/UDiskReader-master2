package com.hdu.wsn.uDiskReader.db.util;

import com.hdu.wsn.uDiskReader.db.bean.AppInfo;

import java.util.Iterator;

/**
 * Created by ASUS on 2017/7/27 0027.
 */

public class DBUtil {

    public static int getWrongPassCount() {
        return getAppInfo().getWrongPass();
    }

    public static void setWrongPassCount(int count) {
        AppInfo appInfo = getAppInfo();
        appInfo.setWrongPass(count);
        appInfo.save();
    }

    public static void initWrongPassCount() {
        setWrongPassCount(0);
    }

    private static AppInfo getAppInfo() {
        Iterator<AppInfo> it = AppInfo.findAll(AppInfo.class);
        if (it.hasNext()) {
            AppInfo appInfo = it.next();
            if (appInfo != null) {
                return appInfo;
            }
        }
        AppInfo appInfo = new AppInfo();
        appInfo.setWrongPass(0);
        return appInfo;
    }
}
