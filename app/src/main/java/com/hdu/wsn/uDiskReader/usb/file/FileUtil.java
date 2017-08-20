
package com.hdu.wsn.uDiskReader.usb.file;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.ProgressCallback;
import com.avos.avoscloud.SaveCallback;
import com.hdu.wsn.uDiskReader.R;
import com.hdu.wsn.uDiskReader.ui.view.FileView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by 杨健 on 2017/5/10.
 */

public class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();

    /**
     * 上传文件
     */
    public static boolean transmitFile(DocumentFile file){
        int count=0;
        if(file.isDirectory()){
            for(DocumentFile nextfile:file.listFiles()){
                if(nextfile.isDirectory()){
                    transmitFile(nextfile);
                }else{
                    boolean a = change(nextfile);
                    if (a){
                        count++;
                    }
                }
            }
            if(count>0){
                return true;
            }
        }else{
           return change(file);
        }
        return false;
    }

    /**
     * 文件转换
     * @param file
     */
    public static boolean change(DocumentFile file){

        Uri uri = file.getUri();
        String path = uri.getPath();
        Log.i("TTT",path+"");
        String path1 = path.substring(0,path.lastIndexOf(":"));
        String path11 = path1.substring(path1.lastIndexOf("/"),path1.length());
        String path2 = path.substring(path.lastIndexOf(":")+1,path.length());

        String pathLast = "/storage"+path11+"/"+path2;

        Log.i("TTTT",path+"\n"+path1+"\n"+path11+"\n"+path2+"\n"+pathLast);

        try {
            AVFile newFile = AVFile.withAbsoluteLocalPath(file.getName(), pathLast);

            newFile.saveInBackground(new SaveCallback() {
                @Override
                public void done(AVException e) {
                    Log.i("save", "success!");
                }
            });
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 新建文件夹
     */
    public static DocumentFile createFile(Context context,String name,DocumentFile outfile){
        try{
            DocumentFile newFile = outfile.createDirectory(name);
            return newFile;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 移动文件
     * @param
     * @param
     */
    public static DocumentFile moveFile(Context context,DocumentFile infile,DocumentFile outfile){
        Log.i("TTTTTT","开始移动");
        InputStream input = null;
        OutputStream output = null;
        try {
            DocumentFile newFile;
            if(infile.isDirectory()){
                newFile = outfile.createDirectory(infile.getName());

                for(DocumentFile file:infile.listFiles()){
                    moveFile(context,file,newFile);
                }

            }else{
                newFile = outfile.createFile(infile.getType(), infile.getName());
            }
            OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri());
            input = context.getContentResolver().openInputStream(infile.getUri());
            byte[] buf = new byte[1024*4];
            int len;
            while((len = input.read(buf))!=-1){
                out.write(buf,0,len);
                out.flush();
            }
            return newFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally{
            if(output!=null){
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(input!=null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 打开文件
     * @param file 文件
     * @param context 系统环境上下文
     */
    public static void openFile(File file, Context context) {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        //获取文件file的MIME类型
        String type = getMIMEType(file);
        //设置intent的data和Type属性。
        intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
        //跳转
        context.startActivity(intent);
    }

    /**
     * 打开Document形式的文件
     * @param file 文件
     * @param context 系统环境上下文
     */
    public static void openDocumentFile(DocumentFile file, Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        //获取文件file的MIME类型
        String type = getMIMEType(file);


        //获得绝对路径
        Uri uri = file.getUri();
        String path = uri.getPath();
        Log.i("TTT",path+"");
        String path1 = path.substring(0,path.lastIndexOf(":"));
        String path11 = path1.substring(path1.lastIndexOf("/"),path1.length());
        String path2 = path.substring(path.lastIndexOf(":")+1,path.length());
        String pathLast = "/storage"+path11+"/"+path2;

        File openfile = new File(pathLast);
        //设置intent的data和Type属性。
        //intent.setDataAndType(file.getUri(), type);
        intent.setDataAndType(Uri.fromFile(openfile),type);


        Log.i("TTTT",file.getUri()+"\n"+file.getUri().getPath()+"\n"+Uri.fromFile(openfile));
        //跳转
        context.startActivity(intent);
    }

    /**
     * 删除文件
     * @param file 文件
     */
    public static boolean deleteFile(DocumentFile file) {
        boolean b = true;
        if (file.exists()) {
            if (file.isFile()) {
                System.gc();
                b = file.delete();
                System.out.println(b);
            } else if (file.isDirectory()) {
                DocumentFile files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    b = b && deleteFile(files[i]);
                }
                b = b && file.delete();
            }
            return b;
        } else {
            System.out.println("文件不存在！"+"\n");
            return false;
        }
    }

    /**
     * 根据文件后缀名获得对应的MIME类型。
     *
     * @param file
     */
    public static String getMIMEType(File file) {

        String type = "*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /* 获取文件的后缀名 */
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < MIME_MapTable.length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    /**
     * Document形式的文件的类型
     * @param file 文件
     * @return 类型
     */
    public static String getMIMEType(DocumentFile file) {

        String type = "*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /* 获取文件的后缀名 */
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < MIME_MapTable.length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    /**
     * 后缀名数组
     */
    private static final String[][] MIME_MapTable = {
            //{后缀名， MIME类型}
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };

    /**
     * 文件图标的数组
     */
    private static final Object[][] FILE_IMAGE = {
            {R.drawable.mp3, "m4a", "mp3", "wav"},
            {R.drawable.mp4, "mp4", "3gp", "avi", "mov"},
            {R.drawable.jpg, "jpg", "png", "jpeg", "bmp", "gif"},
            {R.drawable.doc, "doc", "docx"},
            {R.drawable.ppt, "ppt", "pptx"},
            {R.drawable.xlx, "xlx", "xlsx"},
            {R.drawable.pdf, "pdf"},
            {R.drawable.txt, "txt"}
    };

    /**
     * 获取文件图片
     * @param end 后缀名
     * @return
     */
    public static int getFileImage(String end) {
        for (Object[] oneType : FILE_IMAGE) {
            for (int i=1; i<oneType.length; i++) {
                if(oneType[i].equals(end)) {
                    return (int)oneType[0];
                }
            }
        }
        return R.drawable.unknow;
    }

}
