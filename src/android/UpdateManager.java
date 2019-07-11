package com.vaenow.appupdate.android;

import android.content.Context;
import android.os.Handler;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LuoWen on 2015/10/27.
 * <p/>
 * Thanks @coolszy
 */
public class UpdateManager {
    public static final String TAG = "UpdateManager";

    /*
     * 远程的版本文件格式
     *   <update>
     *       <version>2222</version>
     *       <name>name</name>
     *       <url>http://192.168.3.102/android.apk</url>
     *   </update>
     */
    private String updateUrl;
    private JSONArray args;
    private CordovaInterface cordova;
    private CallbackContext callbackContext;
    private String packageName;
    private Context mContext;
    private Boolean isDownloading = false;
    private List<Version> queue = new ArrayList<>(1);
    private CheckUpdateThread checkUpdateThread;
    private DownloadApkThread downloadApkThread;

    public UpdateManager(Context context, CordovaInterface cordova) {
        this.cordova = cordova;
        this.mContext = context;
        packageName = mContext.getPackageName();
    }

    public UpdateManager(JSONArray args, CallbackContext callbackContext, Context context, JSONObject options) {
        this(args, callbackContext, context, "http://192.168.3.102:8080/update_apk/version.xml", options);
    }

    public UpdateManager(JSONArray args, CallbackContext callbackContext, Context context, String updateUrl, JSONObject options) {
        this.args = args;
        this.callbackContext = callbackContext;
        this.updateUrl = updateUrl;
        this.mContext = context;
        packageName = mContext.getPackageName();
    }

    public UpdateManager options(JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        this.args = args;
        this.callbackContext = callbackContext;
        this.updateUrl = args.getString(0);
        return this;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case Constants.NETWORK_ERROR:
                    //暂时隐藏错误
                    callbackContext.error(Utils.makeJSON(Constants.NETWORK_ERROR, "network error"));
                    break;
                case Constants.VERSION_COMPARE_START:
                    compareVersions();
                    break;
                case Constants.DOWNLOAD_CLICK_START:
                    emitNoticeDialogOnClick();
                    break;
                case Constants.DOWNLOAD_FINISH:
                    isDownloading = false;
                    break;
                case Constants.VERSION_UPDATING:
                    callbackContext.success(Utils.makeJSON(Constants.VERSION_UPDATING, "success, version updating."));
                    break;
                case Constants.VERSION_NEED_UPDATE:
                    callbackContext.success(Utils.makeJSON(Constants.VERSION_NEED_UPDATE, "success, need date."));
                    break;
                case Constants.VERSION_UP_TO_UPDATE:
                    callbackContext.success(Utils.makeJSON(Constants.VERSION_UP_TO_UPDATE, "success, up to date."));
                    break;
                case Constants.VERSION_COMPARE_FAIL:
                    callbackContext.error(Utils.makeJSON(Constants.VERSION_COMPARE_FAIL, "version compare fail"));
                    break;
                case Constants.VERSION_RESOLVE_FAIL:
                    callbackContext.error(Utils.makeJSON(Constants.VERSION_RESOLVE_FAIL, "version resolve fail"));
                    break;
                case Constants.REMOTE_FILE_NOT_FOUND:
                    callbackContext.error(Utils.makeJSON(Constants.REMOTE_FILE_NOT_FOUND, "remote file not found"));
                    break;
                default:
                    callbackContext.error(Utils.makeJSON(Constants.UNKNOWN_ERROR, "unknown error"));
            }

        }
    };

    /**
     * 检测软件更新
     */
    public void checkUpdate() {
        LOG.d(TAG, "checkUpdate..");
        checkUpdateThread = new CheckUpdateThread(mContext, mHandler, queue, packageName, updateUrl);
        this.cordova.getThreadPool().execute(checkUpdateThread);
    }

    /**
     * 对比版本号
     */
    private void compareVersions() {
        Version version = queue.get(0);
        int versionCodeLocal = version.getLocal();
        int versionCodeRemote = version.getRemote();
        //比对版本号
        //检查软件是否有更新版本
        if (versionCodeLocal < versionCodeRemote) {
            if (isDownloading) {
                mHandler.sendEmptyMessage(Constants.VERSION_UPDATING);
            } else {
                LOG.d(TAG, "need update");
                mHandler.sendEmptyMessage(Constants.DOWNLOAD_CLICK_START);
            }
        } else {
            mHandler.sendEmptyMessage(Constants.VERSION_UP_TO_UPDATE);
        }
    }

    private void emitNoticeDialogOnClick() {
        isDownloading = true;
        // 下载文件
        downloadApk();
    }

    /**
     * 下载apk文件
     */
    private void downloadApk() {
        // 启动新线程下载软件
        downloadApkThread = new DownloadApkThread(mHandler, checkUpdateThread.getMJSONObject(), updateUrl);
        this.cordova.getThreadPool().execute(downloadApkThread);
        // new Thread(downloadApkThread).start();
    }

}
