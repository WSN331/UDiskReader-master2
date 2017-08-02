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
import android.os.storage.StorageManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ethanco.lib.PasswordDialog;
import com.ethanco.lib.abs.OnPositiveButtonListener;
import com.hdu.wsn.uDiskReader.R;
import com.hdu.wsn.uDiskReader.db.bean.AppInfo;
import com.hdu.wsn.uDiskReader.db.util.DBUtil;
import com.hdu.wsn.uDiskReader.ui.presenter.DocumentFilePresenter;
import com.hdu.wsn.uDiskReader.ui.presenter.FilePresenter;
import com.hdu.wsn.uDiskReader.ui.view.DocumentFileAdapter;
import com.hdu.wsn.uDiskReader.ui.view.FileView;
import com.hdu.wsn.uDiskReader.ui.view.MyItemDecoration;
import com.hdu.wsn.uDiskReader.usb.jnilib.UDiskConnection;
import com.hdu.wsn.uDiskReader.usb.jnilib.UDiskLib;
import com.orm.SugarContext;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FileActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, FileView {
    private static String TAG = "MainActivity";

    private Context context;
    private TextView tvDebug, preFolder;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private UDiskLib uDiskLib;
    private boolean alreadyLogin = false;    // SDK操作完后判断是否还处于登录的标记
    private FilePresenter filePresenter;
    private DocumentFileAdapter adapter;

    private LinearLayout l1,l2,l3;


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
                Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);//ACTION_OPEN_DOCUMENT
                startActivityForResult(intent, 42);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == 42 && resultCode == Activity.RESULT_OK) {
            Uri rootUri;
            if (resultData != null) {
                rootUri = resultData.getData();
                DBUtil.setUri(rootUri.toString());
                initPresenter(rootUri);
            }
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
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        preFolder = (TextView) findViewById(R.id.pre_folder);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new MyItemDecoration(this, MyItemDecoration.VERTICAL_LIST));
        swipeRefreshLayout.setColorSchemeColors(Color.YELLOW, Color.BLUE, Color.RED, Color.GREEN);
        swipeRefreshLayout.setOnRefreshListener(this);
        tvDebug = (TextView) findViewById(R.id.tv_debug);
        l1 = (LinearLayout) findViewById(R.id.ll_1);
        l2 = (LinearLayout) findViewById(R.id.ll_2);
        l3 = (LinearLayout) findViewById(R.id.ll_3);
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
        if (filePresenter == null) {
            initPermission();
        } else {
            filePresenter.refresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        filePresenter.unRegisterReceive();
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
    public void setTitle(String preText, String nowText) {
        preFolder.setText(preText);
        tvDebug.setText(nowText);
    }

    @Override
    public void setAdapter(DocumentFileAdapter adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);
        adapter.setRecyclerView(recyclerView);
    }

    @Override
    public DocumentFileAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void setRefreshing(boolean b) {
        swipeRefreshLayout.setRefreshing(b);
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

    @Override
    public void onUDiskInsert(Intent intent) {
        //进行读写操作
        Log.e(TAG, "U盘插入");
        filePresenter.setLoginFlag(alreadyLogin);
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
                l3.setVisibility(View.VISIBLE);
                l1.setVisibility(View.INVISIBLE);
                l2.setVisibility(View.INVISIBLE);
                break;
            case FilePresenter.TOOL_BAR_LONG_CLICK:
                l1.setVisibility(View.VISIBLE);
                l2.setVisibility(View.INVISIBLE);
                l3.setVisibility(View.INVISIBLE);
                break;
            case FilePresenter.TOOL_BAR_PASTE:
                l2.setVisibility(View.VISIBLE);
                l1.setVisibility(View.INVISIBLE);
                l3.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * 初始化工具栏
     */
    private void initClickTooBar() {
        TextView copyBtn, cutBtn, pasteBtn, deleteBtn,refreshBtn,createBtn,cancelBtn,equal_btn;
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
                filePresenter.equalFileList(getApplicationContext());
            }
        });

        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filePresenter.copyFileList(false);
            }
        });
        cutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filePresenter.copyFileList(true);
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
                filePresenter.deleteCheckFileList();
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filePresenter.refresh();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                l3.setVisibility(View.VISIBLE);
                l1.setVisibility(View.INVISIBLE);
                l2.setVisibility(View.INVISIBLE);
            }
        });

        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View view = View.inflate(FileActivity.this,R.layout.createfile_layout,null);
                final Dialog dialog = new AlertDialog.Builder(FileActivity.this)
                        .setView(view).show();

                Button confirm = (Button) view.findViewById(R.id.confirm_Btn);
                Button can = (Button) view.findViewById(R.id.can_Btn);
                //Button cancel = (Button) view.findViewById(R.id.cancel_btn);

                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText etname = (EditText) view.findViewById(R.id.et_name);
                        final String name = etname.getText().toString();

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
        });
    }

    /**
     * 执行登录
     *
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
                    DBUtil.initWrongPassCount();
                } else {
                    Toast.makeText(context, "已经" + wrongCount + "次了", Toast.LENGTH_SHORT).show();
                    DBUtil.setWrongPassCount(wrongCount);
                }
                onRefresh();

            }
        }).close().doAction();

    }

    /**
     * 退出登录的弹框
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
     * 执行登出
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

}
