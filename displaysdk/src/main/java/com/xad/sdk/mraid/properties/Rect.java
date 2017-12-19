package com.xad.sdk.mraid.properties;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by Ray.Wu on 12/18/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class Rect {
    public int x = 0;
    public int y = 0;
    public int width = 0;

    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rect() {}

    public int height = 0;

    public String toJavaScriptObject() {
        return String.format(Locale.US,"{x:%d, y:%d, width: %d, height:%d}", x, y, width, height);
    }

    @Override
    public String toString() {
        return String.format(Locale.US,"x:%d, y:%d, width: %d, height:%d", x, y, width, height);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("x", x);
        object.put("y", y);
        object.put("width", width);
        object.put("height", height);
        return object;
    }
}
