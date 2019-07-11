package com.vaenow.appupdate.android;

import android.os.Environment;
import android.os.Handler;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * 下载文件线程
 */
public class DownloadApkThread implements Runnable {
    /* 保存解析的JSON信息 */
    private JSONObject mJSONObject;
    /* 下载保存路径 */
    private String mSavePath;
    private DownloadHandler downloadHandler;
    private Handler mHandler;
    private String updateUrl;

    public DownloadApkThread(Handler mHandler, JSONObject mJSONObject, String updateUrl) {
        this.mJSONObject = mJSONObject;
        this.mHandler = mHandler;
        this.updateUrl = updateUrl;
        this.mSavePath = Environment.getExternalStorageDirectory() + "/" + "download"; // SD Path
        this.downloadHandler = new DownloadHandler(this.mSavePath, mJSONObject);
    }


    @Override
    public void run() {
        downloadAndInstall();
    }

    private void downloadAndInstall() {
        try {
            // 判断SD卡是否存在，并且是否具有读写权限
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // 获得存储卡的路径
                String name = mJSONObject.getString("name");
                // 创建连接
                HttpURLConnection conn = Utils.openConnection(updateUrl + name);

                conn.connect();
                // 创建输入流
                InputStream is = conn.getInputStream();

                File file = new File(mSavePath);
                // 判断文件目录是否存在
                if (!file.exists()) {
                    file.mkdir();
                }
                File apkFile = new File(mSavePath, name);
                FileOutputStream fos = new FileOutputStream(apkFile);
                int count = 0;
                // 缓存
                byte buf[] = new byte[1024];

                // 写入到文件中
                do {
                    int numread = is.read(buf);
                    count += numread;
                    if (numread <= 0) {
                        // 下载完成
                        downloadHandler.sendEmptyMessage(Constants.DOWNLOAD_FINISH);
                        mHandler.sendEmptyMessage(Constants.DOWNLOAD_FINISH);
                        break;
                    }
                    // 写入文件
                    fos.write(buf, 0, numread);
                } while (true);
                fos.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}