package com.xad.sdk.listeners;

import okhttp3.Response;

/**
 * Created by Ray.Wu on 5/5/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public interface TestListener {
    boolean interceptRequest(String requestUrl);
    boolean interceptResponse(Response response);
}
