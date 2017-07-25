package com.hdu.wsn.uDiskReader.ui.presenter;

import android.view.View;
import android.widget.TextView;

/**
 * Created by ASUS on 2017/7/24 0024.
 */

public interface FilePresenter {
    public static final int TOOR_BAR_COMMON = 1;
    public static final int TOOR_BAR_LONG_CLICK = 2;
    public static final int TOOR_BAR_PASTE = 3;

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
     * 注销广播
     */
    void unRegisterReceive();

    /**
     * 删除文件列表
     */
    void deleteCheckFileList();

    /**
     * 新建文件夹
     */
    void createFolder();

    /**
     * 复制文件列表
     * @param delete 复制完是否删除
     */
    void copyFileList(boolean delete);

    /**
     * 设置粘贴
     */
    void pasteFileList();

}
