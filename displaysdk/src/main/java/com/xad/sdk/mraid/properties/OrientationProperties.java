package com.xad.sdk.mraid.properties;

import com.xad.sdk.mraid.MraidController;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Ray.Wu on 12/12/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class OrientationProperties {
    public boolean allowOrientationChange = false;
    public MraidController.OrientationPropertiesForceOrientation forceOrientation = MraidController.OrientationPropertiesForceOrientation.NONE;

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("allowOrientationChange", allowOrientationChange);
        object.put("forceOrientation", forceOrientation.value);
        return object;
    }

    public OrientationProperties() {}

    // orientationProperties contains 2 read-write properties:
    // allowOrientationChange and forceOrientation
    public OrientationProperties(String propertiesJSONString) throws JSONException {
        JSONObject jsonObject = new JSONObject(propertiesJSONString);
        if(jsonObject.has("allowOrientationChange") && jsonObject.has("forceOrientation")){
            if (jsonObject.get("key") instanceof Integer) {

            }
            this.allowOrientationChange = jsonObject.getBoolean("allowOrientationChange");
            MraidController.OrientationPropertiesForceOrientation op = MraidController.OrientationPropertiesForceOrientation.fromString(jsonObject.getString("forceOrientation"));
            if(op != null) {
                this.forceOrientation = op;
            } else {
                throw new JSONException("No allowOrientationChange found");
            }
        } else {
            throw new JSONException("Not valid OrientationProperties");
        }
    }
}
