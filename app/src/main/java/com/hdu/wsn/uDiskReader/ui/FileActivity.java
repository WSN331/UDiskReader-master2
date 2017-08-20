package com.hdu.wsn.uDiskReader.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ethanco.lib.PasswordDialog;
import com.ethanco.lib.abs.OnPositiveButtonListener;
import com.hdu.wsn.uDiskReader.R;
import com.hdu.wsn.uDiskReader.db.util.DBUtil;
import com.hdu.wsn.uDiskReader.ui.presenter.DocumentFilePresenter;
import com.hdu.wsn.uDiskReader.ui.presenter.FilePresenter;
import com.hdu.wsn.uDiskReader.ui.view.DocumentFileAdapter;
import com.hdu.wsn.uDiskReader.ui.view.FileView;
import com.hdu.wsn.uDiskReader.ui.view.MyItemDecoration;
import com.hdu.wsn.uDiskReader.usb.jnilib.UDiskConnection;
import com.hdu.wsn.uDiskReader.usb.jnilib.UDiskLib;
import com.orm.SugarContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class FileActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, FileView {

    public static final int BEFOREPB = 100;
    public static final int AFTERPB = 101;

    private static String TAG = "MainActivity";


    private int index= 0;
    private Context context;
    private ProgressBar pbShow;
    private LinearLayout llShow;

    private TextView tvDebug, preFolder;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private UDiskLib uDiskLib;
    private boolean alreadyLogin = false;    // SDK操作完后判断是否还处于登录的标记

    private FilePresenter filePresenter;

    private DocumentFileAdapter adapter;

    private LinearLayout linearCommon, linearLongClick, linearPaste;

    private AlertDialog dialog;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            llShow.setVisibility(View.INVISIBLE);
            dialog.dismiss();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        SugarContext.init(this);

        initView();
        initPermission();
    }

    /**
     * 初始化权限
     */
    private void initPermission() {
        Uri uri = DocumentFilePresenter.getUri();
        if (uri!=null) {
            initPresenter(uri);
        } else {
            String uriStr = DBUtil.getUri();
            if (uriStr != null && !uriStr.equals("")) {
                uri = Uri.parse(uriStr);
                initPresenter(uri);
            } else {
                intentToOpen();

            }
        }
    }

    /**
     * 进入选择U盘的页面
     */
    private void intentToOpen() {
        new AlertDialog.Builder(FileActivity.this)
                .setTitle("获取U盘读取权限")
                .setMessage("确定手动打开指定U盘读取权限吗？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);//ACTION_OPEN_DOCUMENT
                        startActivityForResult(intent, 42);
                    }
                })
                .setNegativeButton("否", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        Uri rootUri;
        if (requestCode == 42 && resultCode == Activity.RESULT_OK && resultData != null) {
            rootUri = resultData.getData();
            DBUtil.setUri(rootUri.toString());
            initPresenter(rootUri);
        } else {
            DocumentFilePresenter.newInstance(this, context);
        }

    }

    /**
     * 初始化表示器
     * @param rootUri
     */
    private void initPresenter(Uri rootUri) {
        filePresenter = DocumentFilePresenter.newInstance(this, context, rootUri);
        onRefresh();
    }

    /**
     * 初始化界面
     */
    private void initView() {
        pbShow = (ProgressBar) findViewById(R.id.pb_show);

        llShow = (LinearLayout) findViewById(R.id.ll_show);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        preFolder = (TextView) findViewById(R.id.pre_folder);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new MyItemDecoration(this, MyItemDecoration.VERTICAL_LIST));
        swipeRefreshLayout.setColorSchemeColors(Color.YELLOW, Color.BLUE, Color.RED, Color.GREEN);
        swipeRefreshLayout.setOnRefreshListener(this);
        tvDebug = (TextView) findViewById(R.id.tv_debug);
        linearCommon = (LinearLayout) findViewById(R.id.ll_1);
        linearLongClick = (LinearLayout) findViewById(R.id.ll_2);
        linearPaste = (LinearLayout) findViewById(R.id.ll_3);
        initClick();
    }

    /**
     * 初始化点击事件
     */
    private void initClick() {
        preFolder.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (preFolder.getText() != null && !preFolder.getText().equals("")) {
                    onBackPressed();
                }
            }
        });
        initClickTooBar();
    }

    @Override
    public void onRefresh() {
        tvDebug.setText("");
        swipeRefreshLayout.setRefreshing(true);
        linearPaste.setVisibility(View.VISIBLE);
        linearCommon.setVisibility(View.INVISIBLE);
        linearLongClick.setVisibility(View.INVISIBLE);
        if (filePresenter == null) {
            initPermission();
        } else {
            filePresenter.refresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        filePresenter.unRegisterReceiver();
        doLogout();
    }

    @Override
    public void onBackPressed() {
        if (adapter.changeCheckBoxVisibility(DocumentFileAdapter.ViewHolder.CHECK_INVISIBILITY)) {
            if (filePresenter.isRootView()) {
                filePresenter.returnPreFolder();
            } else if (filePresenter.isLogin()) {
                logout();
            } else {
                super.onBackPressed();
            }
        } else {
            setToolBarType(FilePresenter.TOOL_BAR_COMMON);
        }
    }

    @Override
    public void setTitle(final String preText, final String nowText) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                preFolder.setText(preText);
                tvDebug.setText(nowText);
            }
        });

    }

    @Override
    public void setAdapter(final DocumentFileAdapter adapter) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FileActivity.this.adapter = adapter;
                recyclerView.setAdapter(adapter);
                adapter.setRecyclerView(recyclerView);
            }
        });

    }

    @Override
    public DocumentFileAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void setRefreshing(final boolean b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(b);
            }
        });
    }

    @Override
    public void onUDiskInsert(Intent intent) {
        //进行读写操作
        Log.e(TAG, "U盘插入");
        if (filePresenter != null) {
            filePresenter.setLoginFlag(alreadyLogin);
        }
        alreadyLogin = false;
        initPermission();
    }

    @Override
    public void onUDiskRemove(Intent intent) {
        UsbDevice device_out = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device_out != null) {
            //更新界面
            Log.e(TAG, "U盘拔出");
            adapter.notifyDataSetChanged();
            doLogout();
        }
    }

    @Override
    public void setToolBarType(int type) {
        switch (type) {
            case FilePresenter.TOOL_BAR_COMMON:
                linearPaste.setVisibility(View.VISIBLE);
                linearCommon.setVisibility(View.INVISIBLE);
                linearLongClick.setVisibility(View.INVISIBLE);

                break;
            case FilePresenter.TOOL_BAR_LONG_CLICK:
                linearCommon.setVisibility(View.VISIBLE);
                linearLongClick.setVisibility(View.INVISIBLE);
                linearPaste.setVisibility(View.INVISIBLE);
                break;
            case FilePresenter.TOOL_BAR_PASTE:
                linearLongClick.setVisibility(View.VISIBLE);
                linearCommon.setVisibility(View.INVISIBLE);
                linearPaste.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * 初始化工具栏
     */
    private void initClickTooBar() {
        TextView copyBtn, cutBtn, pasteBtn, deleteBtn,
                refreshBtn, createBtn, cancelBtn, equal_btn;
        copyBtn = (TextView) findViewById(R.id.copy_btn);
        cutBtn = (TextView) findViewById(R.id.cut_btn);
        pasteBtn = (TextView) findViewById(R.id.paste_btn);
        deleteBtn = (TextView) findViewById(R.id.delete_btn);
        refreshBtn = (TextView) findViewById(R.id.refresh_btn);
        createBtn = (TextView) findViewById(R.id.create_btn);
        cancelBtn = (TextView) findViewById(R.id.cancel_btn);
        equal_btn = (TextView) findViewById(R.id.equal_btn);

        equal_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressSet();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        pbShow.setProgress(0);
                        filePresenter.equalFileList(getApplicationContext());
                        pbShow.setMax(100);
                        for(int i=1;i<=100;i++)
                        {
                            pbShow.setProgress(i);
                            try {
                                Thread.sleep(10+ new Random().nextInt(20));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        mHandler.sendEmptyMessage(0);
                    }
                }).start();
            }
        });
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(setTextSelect()){
                    filePresenter.copyFileList(false);
                }else{
                    Toast.makeText(getApplicationContext(),"请选择你要复制的文件",Toast.LENGTH_SHORT).show();
                }

            }
        });
        cutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(setTextSelect()){
                    filePresenter.copyFileList(true);
                }else{
                    Toast.makeText(getApplicationContext(),"请选择你要剪切的文件",Toast.LENGTH_SHORT).show();
                }
            }
        });
        pasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filePresenter.pasteFileList();
            }
        });
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(setTextSelect()){
                    filePresenter.deleteCheckFileList();
                }else{
                    Toast.makeText(getApplicationContext(),"请选择你要删除的文件",Toast.LENGTH_SHORT).show();
                }

            }
        });
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearPaste.setVisibility(View.VISIBLE);
                linearCommon.setVisibility(View.INVISIBLE);
                linearLongClick.setVisibility(View.INVISIBLE);
            }
        });
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewFolder();
            }
        });
    }

    /**
     * 上传时效果设置
     */
    public void progressSet(){
        View view = View.inflate(getApplicationContext(),R.layout.view,null);
        dialog = new AlertDialog.Builder(this, R.style.TransparentWindowBg)
                .setView(view)
                .create();

        Window window = dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
        window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);

        dialog.setCancelable(false);
        dialog.show();

        llShow.setVisibility(View.VISIBLE);
    }

    /**
     * 当没有选中项时，三个功能键不能点击
     */
    private boolean setTextSelect() {
        Map<Integer,Boolean> checkmap = new HashMap<>();
        checkmap.clear();
        checkmap = adapter.getCheckMap();
        int count = 0;
        for(Integer position:checkmap.keySet()){
            if(checkmap.get(position)){
                count++;
            }
        }
        if(count==0){
            return false;
        }else{
            return true;
        }
    }


    /**
     * 创建新建文件夹
     */
    private void createNewFolder() {
        final View view = View.inflate(FileActivity.this,R.layout.createfile_layout,null);
        final Dialog dialog = new AlertDialog.Builder(FileActivity.this)
                .setView(view).show();

        Button confirm = (Button) view.findViewById(R.id.confirm_Btn);
        Button can = (Button) view.findViewById(R.id.can_Btn);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etName = (EditText) view.findViewById(R.id.et_name);
                final String name = etName.getText().toString();

                if(!TextUtils.isEmpty(name)){
                    filePresenter.createFolder(name);
                    dialog.dismiss();
                }else{
                    Toast.makeText(FileActivity.this,"请输入文件夹名称",Toast.LENGTH_SHORT).show();
                }
            }
        });
        can.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    /**
     * 执行登录
     * @param text 密码
     */
    private void doLogin(final String text) {
        uDiskLib = UDiskLib.create(context);
        UDiskConnection.create(uDiskLib, new UDiskConnection.Action() {
            @Override
            public int action(UDiskLib diskLib) {
                return uDiskLib.smiLoginDeviceByStr(text);
            }
        }).success(new UDiskConnection.CallBack() {
            @Override
            public void call(int result) {
                Toast.makeText(context, "login success", Toast.LENGTH_SHORT);
                alreadyLogin = true;
                filePresenter.setLoginFlag(alreadyLogin);
                DBUtil.initWrongPassCount();
                onRefresh();
            }
        }).error(new UDiskConnection.CallBack() {
            @Override
            public void call(int result) {
                int wrongCount = DBUtil.getWrongPassCount();
                wrongCount++;
                if (wrongCount>=9) {
                    Toast.makeText(context, "超过9次了", Toast.LENGTH_SHORT).show();

                    destroyAllData();

                    DBUtil.initWrongPassCount();
                } else {
                    Toast.makeText(context, "已经" + wrongCount + "次了", Toast.LENGTH_SHORT).show();
                    DBUtil.setWrongPassCount(wrongCount);
                }
                onRefresh();

            }
        }).close().doAction();

    }

    @Override
    public void showPasswordView() {
        PasswordDialog.Builder builder = new PasswordDialog.Builder(FileActivity.this)
                .setTitle(R.string.please_input_password)  //Dialog标题
                .setBoxCount(4) //设置密码位数
                .setBorderNotFocusedColor(R.color.colorSecondaryText) //边框颜色
                .setDotNotFocusedColor(R.color.colorSecondaryText)  //密码圆点颜色
                .setPositiveListener(new OnPositiveButtonListener() {
                    @Override //确定
                    public void onPositiveClick(DialogInterface dialog, int which, String text) {
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                        doLogin(text);
                    }
                })
                .setNegativeListener(new DialogInterface.OnClickListener() {
                    @Override //取消
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(context, "取消", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.create().show();
    }

    /**
     * 私密空间退出登录的弹框
     */
    private void logout() {
        new AlertDialog.Builder(FileActivity.this)
                .setTitle("退出登录")
                .setMessage("确定退出登录吗？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doLogout();
                    }
                })
                .setNegativeButton("否", null)
                .show();
    }

    /**
     * 私密空间执行退出的方法
     */
    private void doLogout() {

        uDiskLib = UDiskLib.create(context);
        UDiskConnection.create(uDiskLib, new UDiskConnection.Action() {
            @Override
            public int action(UDiskLib diskLib) {
                return uDiskLib.smiLogoutDevice();
            }
        }).success(new UDiskConnection.CallBack() {
            @Override
            public void call(int result) {
                Toast.makeText(context, "logOut success", Toast.LENGTH_SHORT);
                filePresenter.setLoginFlag(false);
                onRefresh();
            }
        }).close().doAction();
    }

    /**
     *  私密空间执行销毁数据的方法
     */
    private void destroyAllData(){
        uDiskLib = UDiskLib.create(context);
        UDiskConnection.create(uDiskLib,new UDiskConnection.Action(){
            @Override
            public int action(UDiskLib diskLib) {
                return uDiskLib.smiEraseAllData();
            }
        }).success(new UDiskConnection.CallBack() {
            @Override
            public void call(int result) {
                Toast.makeText(context,"deleteAllData success",Toast.LENGTH_SHORT);
                onRefresh();
            }
        }).close().doAction();

    }

}
