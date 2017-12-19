package com.xad.sdk.mraid.properties;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Ray.Wu on 12/12/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class ExpandProperties {
    public int width = 0;
    public int height = 0;
    public boolean useCustomClose = false;
    public boolean isModal = true;

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("width", width);
        object.put("height", height);
        object.put("useCustomClose", useCustomClose);
        object.put("isModal", isModal);
        return object;
    }

    public ExpandProperties(String propertiesJSONString) throws JSONException {
        JSONObject jsonObject = new JSONObject(propertiesJSONString);
        this.width = jsonObject.getInt("width");
        this.height = jsonObject.getInt("height");
        // In MRAID 2.0, the only property in expandProperties we actually care about is useCustomClose.
        // Still, we'll do a basic sanity check on the width and height properties, too.
        if(jsonObject.has("useCustomClose")) {
            this.useCustomClose = jsonObject.getBoolean("useCustomClose");
        } else {
            throw new JSONException("No useCustomClose found");
        }

        this.isModal = jsonObject.getBoolean("isModal");
    }
    public ExpandProperties() {}
}
