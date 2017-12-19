package com.xad.sdk.mraid.properties;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Ray.Wu on 12/18/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class Size {
    public int width = 0;
    public int height = 0;

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("width", width);
        object.put("height", height);
        return object;
    }

    public Size() {}

    public Size(int a, int b) {
        this.width = a;
        this.height = b;
    }
}
