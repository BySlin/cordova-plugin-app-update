package com.vaenow.appupdate.android;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by LuoWen on 2015/10/27.
 */
public class CheckAppUpdate extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("checkAppUpdate")) {
            getUpdateManager().options(args, callbackContext);
            getUpdateManager().checkUpdate();
            return true;
        }

        callbackContext.error(Utils.makeJSON(Constants.NO_SUCH_METHOD, "No such method: " + action));
        return false;
    }

    // UpdateManager singleton
    private UpdateManager updateManager = null;

    // Generate or retrieve the UpdateManager singleton
    public UpdateManager getUpdateManager() {
        if (updateManager == null)
            updateManager = new UpdateManager(cordova.getActivity(), cordova);

        return updateManager;
    }
}
