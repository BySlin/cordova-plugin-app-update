package com.vaenow.appupdate.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

import org.apache.cordova.LOG;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by LuoWen on 2015/12/14.
 */
public class DownloadHandler extends Handler {
    private String TAG = "DownloadHandler";

    private Context mContext;
    /* 更新进度条 */
    private ProgressBar mProgress;
    /* 记录进度条数量 */
    private int progress;
    /* 下载保存路径 */
    private String mSavePath;
    /* 保存解析的JSON信息 */
    private JSONObject mJSONObject;
    private com.vaenow.appupdate.android.MsgHelper msgHelper;
    private AlertDialog mDownloadDialog;

    public DownloadHandler(Context mContext, ProgressBar mProgress, AlertDialog mDownloadDialog, String mSavePath, JSONObject mJSONObject) {
        this.msgHelper = new com.vaenow.appupdate.android.MsgHelper(mContext.getPackageName(), mContext.getResources());
        this.mDownloadDialog = mDownloadDialog;
        this.mContext = mContext;
        this.mProgress = mProgress;
        this.mSavePath = mSavePath;
        this.mJSONObject = mJSONObject;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            // 正在下载
            case com.vaenow.appupdate.android.Constants.DOWNLOAD:
                // 设置进度条位置
                mProgress.setProgress(progress);
                break;
            case com.vaenow.appupdate.android.Constants.DOWNLOAD_FINISH:
                updateMsgDialog();
                // 安装文件
                installApk();
                break;
            default:
                break;
        }
    }

    public void updateProgress(int progress) {
        this.progress = progress;
    }

    public void updateMsgDialog() {
        mDownloadDialog.setTitle(msgHelper.getString(com.vaenow.appupdate.android.MsgHelper.DOWNLOAD_COMPLETE_TITLE));
        if (mDownloadDialog.isShowing()) {
            mDownloadDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE); //Update in background
            mDownloadDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.VISIBLE); //Install Manually
            mDownloadDialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE); //Download Again

            mDownloadDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(downloadCompleteOnClick);
        }
    }

    private OnClickListener downloadCompleteOnClick = new OnClickListener() {
        @Override
        public void onClick(View view) {
            installApk();
        }
    };

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

            Process process = null;
            OutputStream out = null;
            InputStream in = null;

            try {
                process = Runtime.getRuntime().exec("su");
                out = process.getOutputStream();

                out.write(("pm install -r " + path + "\n").getBytes());
                out.flush();
//                in = process.getInputStream();
//                int len = 0;
//                byte[] bs = new byte[256];
//
//                while (-1 != (len = in.read(bs))) {
//                    String state = new String(bs, 0, len);
//                    if (state.equals("Success\n")) {
//
//                    }
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
