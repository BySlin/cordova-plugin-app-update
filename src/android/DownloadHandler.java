package com.vaenow.appupdate.android;

import android.os.Handler;
import android.os.Message;

import org.apache.cordova.LOG;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by LuoWen on 2015/12/14.
 */
public class DownloadHandler extends Handler {
    private String TAG = "DownloadHandler";

    /* 下载保存路径 */
    private String mSavePath;
    /* 保存解析的JSON信息 */
    private JSONObject mJSONObject;

    public DownloadHandler(String mSavePath, JSONObject mJSONObject) {
        this.mSavePath = mSavePath;
        this.mJSONObject = mJSONObject;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case com.vaenow.appupdate.android.Constants.DOWNLOAD_FINISH:
                // 安装文件
                installApk();
                break;
            default:
                break;
        }
    }

    /**
     * 安装APK文件
     */
    private void installApk() {
        LOG.d(TAG, "Installing APK");

        File apkFile = null;
        try {
            String name = mJSONObject.getString("name");
            apkFile = new File(mSavePath, name);
            if (!apkFile.exists()) {
                LOG.e(TAG, "Could not find APK: " + name);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOG.d(TAG, "APK Filename: " + apkFile.toString());

        new ApkInstallThread(apkFile.getAbsolutePath()).start();
    }

    public class ApkInstallThread extends Thread {
        private String path;

        public ApkInstallThread(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            super.run();
            Process process;
            OutputStream out;

            try {
                process = Runtime.getRuntime().exec("su");
                out = process.getOutputStream();

                out.write(("pm install -r " + path + "\n").getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
