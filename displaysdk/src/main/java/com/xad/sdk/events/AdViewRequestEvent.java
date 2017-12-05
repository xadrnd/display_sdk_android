package com.xad.sdk.events;

import com.xad.sdk.AdType;

/**
 * Created by Ray.Wu on 8/15/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class AdViewRequestEvent extends BaseEvent {
    public final com.xad.sdk.AdSize AdSize;
    public final String AccessKey;
    public final com.xad.sdk.AdRequest AdRequest;
    public final AdType Type;

    public AdViewRequestEvent(Object requester, com.xad.sdk.AdSize adSize, String accessKey, com.xad.sdk.AdRequest adRequest, AdType type) {
        super(requester);
        AdSize = adSize;
        AccessKey = accessKey;
        AdRequest = adRequest;
        Type = type;
    }

    public AdViewRequestEvent(Object requester, long timestamp, com.xad.sdk.AdSize adSize, String accessKey, com.xad.sdk.AdRequest adRequest, AdType type) {
        super(requester, timestamp);
        AdSize = adSize;
        AccessKey = accessKey;
        AdRequest = adRequest;
        Type = type;
    }

    @Override
    public BaseEvent clone(long timestamp) {
        return null;
    }
}
