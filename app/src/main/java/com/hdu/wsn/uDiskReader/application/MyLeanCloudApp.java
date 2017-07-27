package com.hdu.wsn.uDiskReader.application;

import android.app.Application;

import com.avos.avoscloud.AVOSCloud;

/**
 * Created by Administrator on 2017/7/27.
 */
public class MyLeanCloudApp extends Application{

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化参数依次为 this, AppId, AppKey
        AVOSCloud.initialize(this,"Jmx6d9EShQT994I6vO3jQ3ju-gzGzoHsz","DSfHM6efV5TEHOnhKvlYuM1W");
        // 放在 SDK 初始化语句 AVOSCloud.initialize() 后面，只需要调用一次即可
        AVOSCloud.setDebugLogEnabled(true);
    }
}
