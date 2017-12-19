package com.xad.sdk.mraid.properties;

import org.json.JSONException;

/**
 * Created by Ray.Wu on 12/12/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class JSONInvalidKeyException extends JSONException {
    public JSONInvalidKeyException(String key) {
        super(key);
        this.invalidKey = key;
    }

    public final String invalidKey;
}
