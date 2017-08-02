package com.hdu.wsn.uDiskReader.db.util;

import com.hdu.wsn.uDiskReader.db.bean.AppInfo;

import java.util.Iterator;

/**
 * Created by ASUS on 2017/7/27 0027.
 */

public class DBUtil {

    /**
     * 获取输错密码次数
     * @return 次数
     */
    public static int getWrongPassCount() {
        return getAppInfo().getWrongPass();
    }

    /**
     * 设置输错密码次数
     * @param count 次数
     */
    public static void setWrongPassCount(int count) {
        AppInfo appInfo = getAppInfo();
        appInfo.setWrongPass(count);
        appInfo.save();
    }

    /**
     * 初始化输错密码次数
     */
    public static void initWrongPassCount() {
        setWrongPassCount(0);
    }

    /**
     * 获取缓存uri
     * @return
     */
    public static String getUri() {
        return getAppInfo().getRootUriPath();
    }

    /**
     * 设置缓存uri
     * @param uri
     */
    public static void setUri(String uri) {
        AppInfo appInfo = getAppInfo();
        appInfo.setRootUriPath(uri);
        appInfo.save();
    }

    /**
     * 获取应用消息
     * @return 应用消息
     */
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
