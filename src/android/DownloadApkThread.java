package android;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * 下载文件线程
 */
public class DownloadApkThread implements Runnable {
    private String TAG = "DownloadApkThread";

    /* 保存解析的JSON信息 */
    private JSONObject mJSONObject;
    /* 下载保存路径 */
    private String mSavePath;
    /* 记录进度条数量 */
    private int progress;
    /* 是否取消更新 */
    private boolean cancelUpdate = false;
    private AlertDialog mDownloadDialog;
    private DownloadHandler downloadHandler;
    private Handler mHandler;
    private AuthenticationOptions authentication;
    private String updateUrl;

    public DownloadApkThread(Context mContext, Handler mHandler, ProgressBar mProgress, AlertDialog mDownloadDialog, JSONObject mJSONObject, String updateUrl, JSONObject options) {
        this.mDownloadDialog = mDownloadDialog;
        this.mJSONObject = mJSONObject;
        this.mHandler = mHandler;
        this.authentication = new AuthenticationOptions(options);

        this.updateUrl = updateUrl;

        this.mSavePath = Environment.getExternalStorageDirectory() + "/" + "download"; // SD Path
        this.downloadHandler = new DownloadHandler(mContext, mProgress, mDownloadDialog, this.mSavePath, mJSONObject);
    }


    @Override
    public void run() {
        downloadAndInstall();
        // 取消下载对话框显示
        // mDownloadDialog.dismiss();
    }

    public void cancelBuildUpdate() {
        this.cancelUpdate = true;
    }

    private void downloadAndInstall() {
        try {
            // 判断SD卡是否存在，并且是否具有读写权限
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // 获得存储卡的路径
                String name = mJSONObject.getString("name");
                // 创建连接
                HttpURLConnection conn = Utils.openConnection(updateUrl + name);

                if (this.authentication.hasCredentials()) {
                    conn.setRequestProperty("Authorization", this.authentication.getEncodedAuthorization());
                }

                conn.connect();
                // 获取文件大小
                int length = conn.getContentLength();
                // 创建输入流
                InputStream is = conn.getInputStream();

                File file = new File(mSavePath);
                // 判断文件目录是否存在
                if (!file.exists()) {
                    file.mkdir();
                }
                File apkFile = new File(mSavePath, mJSONObject.get("name") + ".apk");
                FileOutputStream fos = new FileOutputStream(apkFile);
                int count = 0;
                // 缓存
                byte buf[] = new byte[1024];

                // 写入到文件中
                do {
                    int numread = is.read(buf);
                    count += numread;
                    // 计算进度条位置
                    progress = (int) (((float) count / length) * 100);
                    downloadHandler.updateProgress(progress);
                    // 更新进度
                    downloadHandler.sendEmptyMessage(Constants.DOWNLOAD);
                    if (numread <= 0) {
                        // 下载完成
                        downloadHandler.sendEmptyMessage(Constants.DOWNLOAD_FINISH);
                        mHandler.sendEmptyMessage(Constants.DOWNLOAD_FINISH);
                        break;
                    }
                    // 写入文件
                    fos.write(buf, 0, numread);
                } while (!cancelUpdate);// 点击取消就停止下载.
                fos.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
