package com.xad.sdk.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.xad.sdk.AdRequest;
import com.xad.sdk.AdSize;
import com.xad.sdk.AdType;

/**
 * Created by Ray.Wu on 1/24/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class AdTestUrlGenerator extends UrlGenerator {
    private static final String TAG = "AdTestUrlGenerator";

    public AdTestUrlGenerator withAdTypeAndAdSize(AdType adType, @Nullable AdSize adSize) {
        if(this.testType == AdRequest.TestType.CHANNEL) {
            return this;
        }

        //only need when test with sandbox/production
        switch (adType) {
            case BANNER:
                addParam("type", "banner");
                if(adSize != null) {
                    addParam("size", adSize.toString());
                }
                break;
            case INTERSTITIAL:
                addParam("type", "interstitial");
                break;
            case REWARDED_VIDEO:
                addParam("type", "video");
                break;
            case NATIVE:
                break;
        }
        return this;
    }

    private final AdRequest.TestType testType;

    public AdTestUrlGenerator(String testUrl, @NonNull AdRequest.TestType testType, @Nullable String channelId) {
        super(testUrl + "/" + testType.value + (testType == AdRequest.TestType.CHANNEL && channelId != null? "/" + channelId : ""));
        this.testType = testType;
        this.addParam("no_redirect", "1");
    }

    public AdTestUrlGenerator withAccessKey(@NonNull String accessKey) {
        addParam("access_key", accessKey);
        return this;
    }
}
