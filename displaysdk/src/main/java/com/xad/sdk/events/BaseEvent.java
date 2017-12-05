package com.xad.sdk.events;

/**
 * Created by Ray.Wu on 5/16/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public abstract class BaseEvent {
    public final long Timestamp;
    public final transient Object Requester;

    BaseEvent() {
        this.Requester = null;
        this.Timestamp = 0;
    }

    public BaseEvent(Object requester) {
        this.Requester = requester;
        Timestamp = System.currentTimeMillis();
    }

    public BaseEvent(Object requester, long timestamp) {
        this.Requester = requester;
        this.Timestamp = timestamp;
    }

    public abstract BaseEvent clone(long timestamp);
}
