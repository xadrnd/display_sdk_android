package com.xad.sdk;

/**
 * Created by Ray.Wu on 4/22/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

/**
 * There are four different type: static, long(90 sec), medium(60 sec), short(30 sec).
 */
public enum RefreshInterval {
    Long(90),
    MEDIUM(60),
    SHORT(30),
    STATIC(-1);

    private final int interval;

    RefreshInterval(int t) {
        this.interval = t * 1000;
    }

    /**
     * Get refresh interval in seconds
     * @return int unit: second
     */
    public int getRefreshIntervalInSeconds() {
        return interval;
    }
}
