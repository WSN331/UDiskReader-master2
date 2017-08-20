package com.hdu.wsn.uDiskReader.ui.presenter;

import android.content.Context;

/**
 * Created by ASUS on 2017/7/24 0024.
 */

public interface FilePresenter {
    /**
     * 工具栏状态
     */
    int TOOL_BAR_COMMON = 1;
    int TOOL_BAR_LONG_CLICK = 2;
    int TOOL_BAR_PASTE = 3;

    /**
     * 判断是不是根路径
     * @return 是否是根路径
     */
    boolean isRootView();

    /**
     * 返回上一层目录
     */
    void returnPreFolder();

    /**
     * 刷新
     */
    void refresh();

    /**
     * 设置登录状态
     * @param loginFlag 登录状态
     */
    void setLoginFlag(boolean loginFlag);

    /**
     * 获取登录状态
     * @return 登录状态
     */
    boolean isLogin();

    /**
     * 注册接收器
     */
    void registerReceiver();

    /**
     * 注销广播
     */
    void unRegisterReceiver();

    /**
     * 删除文件列表
     */
    void deleteCheckFileList();

    /**
     * 新建文件夹
     */
    void createFolder(String name);

    /**
     * 复制文件列表
     * @param delete 复制完是否删除
     */
    void copyFileList(boolean delete);

    /**
     * 设置粘贴
     */
    void pasteFileList();

    /**
     * 上传到LeanCloud，实现云同步
     */
    void equalFileList(Context context);



}
