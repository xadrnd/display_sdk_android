package com.xad.sdk.listeners;

import com.xad.sdk.ErrorCode;
import com.xad.sdk.VideoAd;

/**
 * Created by Ray.Wu on 5/9/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public interface VideoAdListener {
    void onVideoLoadSuccess(VideoAd videoAd);
    void onVideoFailedToLoad(VideoAd videoAd, ErrorCode errorCode);
    void onVideoStarted(VideoAd videoAd);
    void onPlaybackError(VideoAd videoAd);
    void onVideoClosed(VideoAd videoAd);
    void onVideoCompleted(VideoAd videoAd);
    void onVideoClicked(VideoAd videoAd);
}
