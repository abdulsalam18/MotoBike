package com.apk.update;

import org.apache.cordova.*;
import org.json.JSONArray;

public class ApkUpdater extends CordovaPlugin {

    @Override
    protected void pluginInitialize() {
        UpdateManager.check(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if(action.equals("check")) {
            UpdateManager.check(cordova.getActivity());
            callbackContext.success();
            return true;
        }
        return false;
    }
}
