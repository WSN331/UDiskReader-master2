package com.hdu.wsn.uDiskReader.ui.presenter;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.view.View;
import android.widget.Toast;

import com.hdu.wsn.uDiskReader.ui.view.DocumentFileAdapter;
import com.hdu.wsn.uDiskReader.ui.view.FileView;
import com.hdu.wsn.uDiskReader.usb.file.FileUtil;
import com.hdu.wsn.uDiskReader.usb.jnilib.UDiskLib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ASUS on 2017/7/19 0019.
 */

public class DocumentFilePresenter implements FilePresenter{

    private FileView fileView;
    private Context context;
    private boolean loginFlag = false;    // 登录标记
    private List<DocumentFile> fileList;
    private List<DocumentFile> currentFolderList = new ArrayList<>();
    private DocumentFile currentFolder;  //当前目录
    private Uri rootUri;
    private boolean pasteFlag = false, deleteAfterPaste;
    Map<Integer, DocumentFile> copyFileMap;

    private static DocumentFilePresenter instance;

    /**
     * 获取运行缓存uri
     * @return
     */
    public static Uri getUri() {
        return instance!=null? instance.getRootUri() : null;
    }

    /**
     * 初始化解释器
     * @param fileView 页面
     * @param context 上下文
     * @param rootUri 根路径
     * @return 解释器
     */
    public static DocumentFilePresenter newInstance(FileView fileView, Context context, Uri rootUri) {
        DocumentFilePresenter.newInstance(fileView, context);
        instance.setRootUri(rootUri);
        return instance;
    }

    /**
     * 初始化解释器
     * @param fileView 页面
     * @param context 上下文
     * @return 解释器
     */
    public static DocumentFilePresenter newInstance(FileView fileView, Context context) {
        if (instance == null) {
            synchronized (DocumentFilePresenter.class) {
                instance = new DocumentFilePresenter();
            }
        }
        instance.setFileView(fileView);
        instance.setContext(context);
        instance.registerReceiver();
        return instance;
    }

    /**
     * 构造函数
     */
    protected DocumentFilePresenter() {
        fileList = new ArrayList<>();
        copyFileMap = new HashMap<>();
    }

    public void setFileView(FileView fileView) {
        this.fileView = fileView;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setRootUri(Uri rootUri) {
        this.rootUri = rootUri;
    }

    public Uri getRootUri() {
        return rootUri;
    }

    /**
     * 读取设备
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void readDeviceList() {
        context.getContentResolver().takePersistableUriPermission(rootUri,Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        currentFolder = DocumentFile.fromTreeUri(context, rootUri);
        getAllFiles();
    }

    /**
     * 获取文件
     */
    private void getAllFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                UDiskLib.Init(context);
                fileList.clear();
                DocumentFile files[] = currentFolder.listFiles();
                if (files != null) {
                    for (DocumentFile f : files) {
                        System.out.println(f);
                        fileList.add(f);
                    }
                    Collections.sort(fileList, new Comparator<DocumentFile>() {
                        @Override
                        public int compare(DocumentFile o1, DocumentFile o2) {
                            if (o1.isDirectory()) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    });
                    initAdapter();
                }
            }
        }).start();
    }

    /**
     * 初始化界面相关的内容
     */
    private void initAdapter() {
        changeTitle();
        DocumentFileAdapter adapter = new DocumentFileAdapter(fileList);
        adapter.setSecretHeader(currentFolderList.size() == 0 && !loginFlag);
        fileView.setAdapter(adapter);
        initListener(adapter);
        if (fileList!=null && fileList.size()>0 && fileList.get(0)!=null){
            fileView.setRefreshing(false);
        }
    }

    /**
     * 变化标题
     */
    private void changeTitle() {
        String nowText,preText;
        nowText = " > " + currentFolder.getName();
        if (currentFolderList != null && currentFolderList.size() > 0) {
            if (currentFolderList.size() == 1 && loginFlag) {
                preText = "> 私密空间";
            } else {
                preText = currentFolderList.get(currentFolderList.size() - 1).getName();
            }
        } else if (loginFlag) {
            preText = nowText;
            nowText = "> 私密空间";
        } else {
            preText = "";
        }
        fileView.setTitle(preText, nowText);
    }

    /**
     * 列表项点击事件
     * @param adapter 适配器
     */
    private void initListener(final DocumentFileAdapter adapter) {
        adapter.setOnRecyclerViewItemClickListener(new DocumentFileAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int index = getRealPosition(position);
                if (index > 0) {
                    DocumentFile file = fileList.get(index - 1);
                    if (file.isDirectory()) {
                        currentFolderList.add(currentFolder);
                        currentFolder = file;
                        getAllFiles();
                    } else {
                        FileUtil.openDocumentFile(file, context);
                    }
                } else {
                    fileView.showPasswordView();
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
                final int index = getRealPosition(position);
                if (index > 0) {
                    fileView.setToolBarType(FilePresenter.TOOL_BAR_LONG_CLICK);
                }
            }
            @Override
            public void onItemCheck(View view, int position, boolean check) {

            }
        });

    }

    /**
     * 获取文件下标+1 （因此这个值如果是0代表该项不是真实文件，而是私密空间栏）
     * @param position 列表下标
     * @return 文件下标+1
     */
    private int getRealPosition(int position) {
        int index = position;
        if (currentFolderList.size() > 0 || loginFlag) {
            index ++;
        }
        return index;
    }

    @Override
    public void deleteCheckFileList() {
        Map<Integer, Boolean> checkFileList = new HashMap<>();
        checkFileList.clear();
        checkFileList= fileView.getAdapter().getCheckMap();
        for (Integer index : checkFileList.keySet()) {
            if (checkFileList.get(index)) {
                index = getRealPosition(index);
                doDelete(index, fileList.get(index-1));
            }
        }
        fileView.setToolBarType(FilePresenter.TOOL_BAR_COMMON);
        fileView.getAdapter().changeCheckBoxVisibility(DocumentFileAdapter.ViewHolder.CHECK_INVISIBILITY);
    }

    @Override
    public void equalFileList(Context context) {
        Map<Integer,Boolean> transFileList = new HashMap<>();
        transFileList.clear();
        transFileList= fileView.getAdapter().getCheckMap();
        int count=0;
        if(transFileList.size()<1){
            Toast.makeText(context,"请选择要同步的文件",Toast.LENGTH_SHORT).show();
        }else{
            for(Integer index:transFileList.keySet()){
                index = getRealPosition(index);
                boolean s = FileUtil.transmitFile(fileList.get(index-1));
                if(s){
                    count++;
                }
            }
            if(count>0){
                Toast.makeText(context,"上传成功",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context,"上传失败，文件有问题",Toast.LENGTH_SHORT).show();
            }
        }
        fileView.setToolBarType(FilePresenter.TOOL_BAR_COMMON);
        fileView.getAdapter().changeCheckBoxVisibility(DocumentFileAdapter.ViewHolder.CHECK_INVISIBILITY);
    }

    /**
     * 执行删除文件
     * @param index 文件位置（1开始）
     * @param file 文件
     */
    private void doDelete(int index, DocumentFile file) {
        boolean result = FileUtil.deleteFile(file);
        if (result && index>0) {
            removeData(index-1);
        }
    }

    /**
     * 删除某个item
     * @param index
     */
    private void removeData(int index) {
        fileView.getAdapter().removeData(index);
    }

    /**
     * 添加文件
     * @param file 文件
     */
    private void addData(DocumentFile file) {
        fileView.getAdapter().addData(file);
    }

    @Override
    public void createFolder(String name) {
        DocumentFile newFile = FileUtil.createFile(context,name,currentFolder);
        addData(newFile);
        fileView.setToolBarType(FilePresenter.TOOL_BAR_COMMON);
    }

    @Override
    public void copyFileList(boolean delete) {
        pasteFlag = true;
        deleteAfterPaste = delete;
        fileView.setToolBarType(FilePresenter.TOOL_BAR_PASTE);
        Map<Integer, Boolean> copyMap = fileView.getAdapter().getCheckMap();

        copyFileMap.clear();
        for (Integer index : copyMap.keySet()) {
            if (!copyMap.get(index)) {
                continue;
            }
            index = getRealPosition(index);
            DocumentFile copyFile = fileList.get(index-1);
            copyFileMap.put(index, copyFile);
        }
        fileView.getAdapter().changeCheckBoxVisibility(DocumentFileAdapter.ViewHolder.CHECK_INVISIBILITY);
    }

    @Override
    public void pasteFileList() {
        if(pasteFlag){
            for (Integer index : copyFileMap.keySet()) {
                DocumentFile copyFile = copyFileMap.get(index);
                DocumentFile newFile = FileUtil.moveFile(context, copyFile, currentFolder);
                addData(newFile);
                pasteFlag = false;
                if (fileList.size()<index || fileList.get(index-1) != copyFile) {
                    index = -1;
                }
                if (deleteAfterPaste) {
                    doDelete(index, copyFile);
                }
            }
        }else{
            Toast.makeText(context,"请选择你要移动的文件",Toast.LENGTH_SHORT).show();
        }
        fileView.setToolBarType(FilePresenter.TOOL_BAR_COMMON);
    }




    @Override
    public boolean isRootView() {
        return currentFolderList.size() > 0;
    }

    @Override
    public void returnPreFolder() {
        currentFolder = currentFolderList.get(currentFolderList.size() - 1);
        currentFolderList.remove(currentFolderList.size() - 1);
        getAllFiles();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void refresh() {
        if (currentFolder == null) {
            readDeviceList();
        } else {
            getAllFiles();
        }
    }

    @Override
    public void setLoginFlag(boolean loginFlag) {
        this.loginFlag = loginFlag;
    }

    @Override
    public boolean isLogin() {
        return loginFlag;
    }

    @Override
    public void unRegisterReceiver() {
        if (usbReceiver != null) {
            context.unregisterReceiver(usbReceiver);
        }
    }

    @Override
    public void registerReceiver() {
        //监听otg插入 拔出
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbDeviceStateFilter.addDataScheme("file");

        context.registerReceiver(usbReceiver, usbDeviceStateFilter);

    }

    /**
     * u盘插拔广播接收器
     */
    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                //接收到U盘插入的广播
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    fileView.onUDiskInsert(intent);
                    break;
                //接收到U盘拔出的广播
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    fileView.onUDiskRemove(intent);
                    break;
                case Intent.ACTION_MEDIA_MOUNTED:
                    fileView.onUDiskInsert(intent);
                    break;
                case Intent.ACTION_MEDIA_REMOVED:
                    fileView.onUDiskRemove(intent);
                    break;
            }
        }
    };

}
