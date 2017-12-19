package com.xad.sdk.mraid.properties;

import android.support.annotation.Nullable;

import com.xad.sdk.mraid.MraidController;
import com.xad.sdk.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by Ray.Wu on 12/12/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class ResizeProperties {
    public int width = 0;
    public int height = 0;
    public MraidController.ResizePropertiesCustomClosePosition customClosePosition = MraidController.ResizePropertiesCustomClosePosition.TOP_RIGHT;
    public int offsetX = 0;
    public int offsetY = 0;
    public boolean allowOffscreen = true;

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("width", width);
        object.put("height", height);
        object.put("customClosePosition", customClosePosition.value);
        object.put("offsetX", offsetX);
        object.put("offsetY", offsetY);
        object.put("allowOffscreen", allowOffscreen);
        return object;
    }

    // resizeProperties contains 6 read-write properties:
    // width, height, offsetX, offsetY, customClosePosition, allowOffscreen

    // The properties object passed into this function must contain width, height, offsetX, offsetY.
    // The remaining two properties are optional.

    public ResizeProperties() {}

    public ResizeProperties(String propertiesJSONString) throws JSONException {
        this(propertiesJSONString, null);
    }

    //Create a new ResizeProperties based on previous properties, because some of optional properties may still use previous setting.
    public ResizeProperties(String propertiesJSONString, @Nullable ResizeProperties previousProperties) throws JSONException {
        JSONObject jsonObject = new JSONObject(propertiesJSONString);
        if(jsonObject.has("width") && (jsonObject.get("width") instanceof Integer)) {
            this.width = jsonObject.getInt("width");
        } else {
            throw new JSONInvalidKeyException("width");
        }

        if(jsonObject.has("height") && (jsonObject.get("height") instanceof Integer)) {
            this.width = jsonObject.getInt("height");
        } else {
            throw new JSONInvalidKeyException("height");
        }

        if(jsonObject.has("offsetX") && (jsonObject.get("offsetX") instanceof Integer)) {
            this.width = jsonObject.getInt("offsetX");
        } else {
            throw new JSONInvalidKeyException("offsetX");
        }

        if(jsonObject.has("offsetY") && (jsonObject.get("offsetY") instanceof Integer)) {
            this.width = jsonObject.getInt("offsetY");
        } else {
            throw new JSONInvalidKeyException("offsetY");
        }

        try {
            this.allowOffscreen = jsonObject.getBoolean("allowOffscreen");
        } catch (JSONException e) {
            if (previousProperties != null) {
                this.allowOffscreen = previousProperties.allowOffscreen;
            }
            Logger.logVerbose("ResizeProperties", "no allowOffscreen property found, will use previous setting");
        }

        MraidController.ResizePropertiesCustomClosePosition position = MraidController.ResizePropertiesCustomClosePosition.fromString(jsonObject.getString("customClosePosition"));
        if(position != null) {
            this.customClosePosition = position;
        } else {
            if (previousProperties != null) {
                this.customClosePosition = previousProperties.customClosePosition;
            }
            Logger.logVerbose("ResizeProperties", "no customClosePosition property found, will use previous setting");
        }
    }

    public void adjustmentForScreen(Rect defaultPosition, Size maxScreenSize) {
        Rect resizeRect = new Rect();
        resizeRect.x = defaultPosition.x + this.offsetX;
        resizeRect.y = defaultPosition.y + this.offsetY;
        resizeRect.width = this.width;
        resizeRect.height = this.height;

        Logger.logVerbose("ResizeProperties", resizeRect.toString());

        Rect maxRect = new Rect();
        maxRect.width = maxScreenSize.width;
        maxRect.height = maxScreenSize.height;

        if(isRectContained(maxRect, resizeRect)) {
            Logger.logVerbose("ResizeProperties", "no adjustment necessary");
            return;
        }

        int adjustmentX = 0;
        int adjustmentY = 0;

        if (resizeRect.x < maxRect.x) {
            adjustmentX = maxRect.x - resizeRect.x;
        } else if ((resizeRect.x + resizeRect.width) > (maxRect.x + maxRect.width)) {
            adjustmentX = (maxRect.x + maxRect.width) - (resizeRect.x + resizeRect.width);
        }
        Logger.logDebug("ResizeProperties", "adjustmentX " + adjustmentX);

        if (resizeRect.y < maxRect.y) {
            adjustmentY = maxRect.y - resizeRect.y;
        } else if ((resizeRect.y + resizeRect.height) > (maxRect.y + maxRect.height)) {
            adjustmentY = (maxRect.y + maxRect.height) - (resizeRect.y + resizeRect.height);
        }
        Logger.logDebug("ResizeProperties", "adjustmentY " + adjustmentY);

        this.offsetX += adjustmentX;
        this.offsetY += adjustmentY;
    }

    public boolean isCloseRegionOnScreen(Rect defaultPosition, Size maxScreenSize) {
        Rect resizeRect = new Rect();
        resizeRect.x = defaultPosition.x + this.offsetX;
        resizeRect.y = defaultPosition.y + this.offsetY;
        resizeRect.width = this.width;
        resizeRect.height = this.height;

        Logger.logVerbose("ResizeProperties", resizeRect.toString());

        Rect closeRect = new Rect(0,0,50,50);

        switch (this.customClosePosition) {
            case TOP_LEFT:
                closeRect.x = resizeRect.x;
                closeRect.y = resizeRect.y;
                break;
            case TOP_CENTER:
                closeRect.x = resizeRect.x + (resizeRect.width / 2) - 25;
                closeRect.y = resizeRect.y;
                break;
            case TOP_RIGHT:
                closeRect.x = resizeRect.x + resizeRect.width - 50;
                closeRect.y = resizeRect.y;
                break;
            case CENTER:
                closeRect.x = resizeRect.x + (resizeRect.width / 2) - 25;
                closeRect.y = resizeRect.y + (resizeRect.height / 2) - 25;
                break;
            case BOTTOM_LEFT:
                closeRect.x = resizeRect.x;
                closeRect.y = resizeRect.y + resizeRect.height - 50;
                break;
            case BOTTOM_CENTER:
                closeRect.x = resizeRect.x + (resizeRect.width / 2) - 25;
                closeRect.y = resizeRect.y + resizeRect.height - 50;
                break;
            case BOTTOM_RIGHT:
                closeRect.x = resizeRect.x + resizeRect.width - 50;
                closeRect.y = resizeRect.y + resizeRect.height - 50;
                break;
        }

        Rect maxRect = new Rect();
        maxRect.width = maxScreenSize.width;
        maxRect.height = maxScreenSize.height;

        return isRectContained(maxRect, closeRect);
    }

    private boolean isRectContained(Rect superRect, Rect subRect) {
        return (subRect.x >= superRect.x &&
                (subRect.x + subRect.width) <= (superRect.x + superRect.width) &&
                subRect.y >= superRect.y &&
                (subRect.y + subRect.height) <= (superRect.y + superRect.height));
    }
}
