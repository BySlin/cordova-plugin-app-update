package android;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;

import org.apache.cordova.LOG;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Created by LuoWen on 2015/12/14.
 */
public class CheckUpdateThread implements Runnable {
    private String TAG = "CheckUpdateThread";

    /* 保存解析的JSON信息 */
    private JSONObject mJSONObject;
    private Context mContext;
    private List<Version> queue;
    private String packageName;
    private String updateXmlUrl;
    private AuthenticationOptions authentication;
    private Handler mHandler;

    public void setMJSONObject(JSONObject mJSONObject) {
        this.mJSONObject = mJSONObject;
    }

    public JSONObject getMJSONObject() {
        return mJSONObject;
    }

    public CheckUpdateThread(Context mContext, Handler mHandler, List<Version> queue, String packageName, String updateXmlUrl, JSONObject options) {
        this.mContext = mContext;
        this.queue = queue;
        this.packageName = packageName;
        this.updateXmlUrl = updateXmlUrl;
        this.authentication = new AuthenticationOptions(options);
        this.mHandler = mHandler;
    }

    @Override
    public void run() {
        int versionCodeLocal = getVersionCodeLocal(mContext); // 获取当前软件版本
        int versionCodeRemote = getVersionCodeRemote();  //获取服务器当前软件版本

        queue.clear(); //ensure the queue is empty
        queue.add(new Version(versionCodeLocal, versionCodeRemote));

        if (versionCodeLocal == 0 || versionCodeRemote == 0) {
            mHandler.sendEmptyMessage(Constants.VERSION_RESOLVE_FAIL);
        } else {
            mHandler.sendEmptyMessage(Constants.VERSION_COMPARE_START);
        }
    }

    /**
     * 通过url返回文件
     *
     * @param path
     * @return
     */
    private InputStream returnFileIS(String path) {
        LOG.d(TAG, "returnFileIS..");
        InputStream is = null;
        try {
            HttpURLConnection conn = Utils.openConnection(path);//利用HttpURLConnection对象,我们可以从网络中获取网页数据.

            if (this.authentication.hasCredentials()) {
                conn.setRequestProperty("Authorization", this.authentication.getEncodedAuthorization());
            }

            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream(); //得到网络返回的输入流
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(Constants.REMOTE_FILE_NOT_FOUND);
        } catch (IOException e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(Constants.NETWORK_ERROR);
        }

        return is;
    }

    /**
     * 获取软件版本号
     * <p/>
     * It's weird, I don't know why.
     * <pre>
     * versionName -> versionCode
     * 0.0.1    ->  12
     * 0.3.4    ->  3042
     * 3.2.4    ->  302042
     * 12.234.221 -> 1436212
     * </pre>
     *
     * @param context
     * @return
     */
    private int getVersionCodeLocal(Context context) {
        LOG.d(TAG, "getVersionCode..");

        int versionCode = 0;
        try {
            // 获取软件版本号，对应AndroidManifest.xml下android:versionName
            versionCode = Integer.valueOf(context.getPackageManager().getPackageInfo(packageName, 0).versionName.replaceAll(".", ""));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取服务器软件版本号
     *
     * @return
     */
    private int getVersionCodeRemote() {
        int versionCodeRemote = 0;
        InputStream is = returnFileIS(updateXmlUrl);

        //解析json
        try {
            setMJSONObject(new JSONObject(inputStream2String(is)));
            if (null != getMJSONObject()) {
                versionCodeRemote = Integer.valueOf(getMJSONObject().getString("version").replaceAll(".", ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return versionCodeRemote;
    }

    public static String inputStream2String(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = -1;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }
        return baos.toString();
    }

}
