package com.xad.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.xad.sdk.events.AdViewRequestEvent;
import com.xad.sdk.events.CreativeEvent;
import com.xad.sdk.events.ErrorEvent;
import com.xad.sdk.listeners.TestListener;
import com.xad.sdk.listeners.VideoAdListener;
import com.xad.sdk.utils.Logger;
import com.xad.sdk.vast.VASTPlayer;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Ray.Wu on 8/19/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class VideoAd implements IAdViewBase, VASTPlayer.VASTPlayerListener {
    public static final String TAG = "VideoAd";

    private int vmax = 10;
    private int vmin = 30;

    private Context mContext;
    private VideoAdListener mListener;
    private String accessKey;
    private AdRequest adRequest;
    private VASTPlayer mCurrentPlayer;

    private boolean isReady = false;
    private boolean hasFailed = false;
    private boolean hasBeenRequest = false;

    private TestListener mTestListener;

    @Deprecated
    public VideoAd(Context context) {
        this.mContext = context;
        DisplaySdk.sharedInstance().init(context);
        AdViewUtils.registerOnBus(this);
    }

    @SuppressWarnings("deprecation")
    public VideoAd(Context context, int vmin, int vmax, @NonNull String accessKey) {
        this(context);
        if (vmin > 0 && vmin <= vmax) {
            this.vmin = vmin;
            this.vmax = vmax;
        }
        this.accessKey = accessKey;
    }

    /*********************************************************
    *    Video Setters and Getters
    **********************************************************/
    public void setAccessKey(String id) {
        this.accessKey = id;
    }
    public String getAccessKey() {
        return this.accessKey;
    }
    public void setAdListener(VideoAdListener listener) {
        this.mListener = listener;
    }
    public VideoAdListener getAdListener() {
        return mListener;
    }
    public void setAdRequest(AdRequest adRequest) {
        this.adRequest = adRequest;
    }
    public AdRequest getAdRequest() {
        return this.adRequest;
    }
    public boolean isReady(){
        return this.isReady;
    }

    public void setTestListener(TestListener testListener) {
        this.mTestListener = testListener;
    }

    public void loadAd() {
        if (TextUtils.isEmpty(accessKey)) {
            throw new IllegalArgumentException("Publisher key must be set before load video ad");
        }

        if (this.adRequest == null) {
            throw new IllegalArgumentException("AdRequest must be set before load video ad");
        }

        if (hasBeenRequest) {
            Logger.logWarning(TAG, "Video ad objects can only be used once even with different requests.");
            return;
        }
        hasBeenRequest = true;

        adRequest.vmax = this.vmax;
        adRequest.vmin = this.vmin;

        DisplaySdk.sharedBus().post(
                new AdViewRequestEvent(this,
                        null,
                        this.accessKey,
                        this.adRequest,
                        AdType.REWARDED_VIDEO)
        );

        Logger.logDebug(TAG, "An request for Video is posted");
    }

    public void play() {
        if(this.mCurrentPlayer == null || !this.isReady()) {
            Logger.logWarning(TAG, "Video is not ready. Please wait until you receive \"onVideoLoadSuccess\" callback");
            return;
        }

        if (this.hasFailed) {
            Logger.logWarning(TAG, "Failed to load video. Internal error");
            this.mListener.onPlaybackError(this);
            return;
        }

        mCurrentPlayer.play();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCreativeEventReceived(CreativeEvent event) {
        if(event.Requester != this) {
            return;
        }

        mCurrentPlayer = createVASTPlayer(event);
        VASTPlayer.testListener = this.mTestListener;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEventReceived(ErrorEvent errorEvent) {
        if(errorEvent.Requester != this) {
            return;
        }

        if(this.mListener != null) {
            this.mListener.onVideoFailedToLoad(this, errorEvent.Error);
        }
    }

    private VASTPlayer createVASTPlayer(CreativeEvent creativeEvent) {
        VASTPlayer player = new VASTPlayer(mContext, this);
        player.accessKey = this.accessKey;
        player.loadVideoWithData(creativeEvent);
        return player;
    }

    @Override
    public void resume() {
        AdViewUtils.registerOnBus(this);
    }

    @Override
    public void destroy() {
        AdViewUtils.unregisterOnBus(this);
    }

    @Override
    public void pause() {
        AdViewUtils.unregisterOnBus(this);
        if(this.mCurrentPlayer != null) {
            this.mCurrentPlayer.destroy();
            this.mCurrentPlayer = null;
        }
        this.mContext = null;
    }


    @Override
    public void vastReady() {
        this.isReady = true;
        if(mListener != null) {
            mListener.onVideoLoadSuccess(this);
        }
    }

    @Override
    public void vastError(int error) {
        this.hasFailed = true;
        if(mListener != null) {
            switch (error) {
                case VASTPlayer.ERROR_NO_NETWORK:
                    mListener.onVideoFailedToLoad(this, ErrorCode.NETWORK_ERROR);
                    break;
                default:
                    mListener.onVideoFailedToLoad(this, ErrorCode.UNKNOWN);
            }
        }
    }

    @Override
    public void vastClick() {
        if(mListener != null) {
            mListener.onVideoClicked(this);
        }
    }

    @Override
    public void vastStart() {
        if(mListener != null) {
            mListener.onVideoStarted(this);
        }
    }

    @Override
    public void vastComplete() {
        if(mListener != null) {
            mListener.onVideoCompleted(this);
        }
    }

    @Override
    public void vastDismiss() {
        if(mListener != null) {
            mListener.onVideoClosed(this);
        }
        this.destroy();
    }
}
