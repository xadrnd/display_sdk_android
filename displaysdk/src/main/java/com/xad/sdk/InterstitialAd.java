package com.xad.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Surface;
import android.view.WindowManager;

import com.moat.analytics.mobile.MoatFactory;
import com.moat.analytics.mobile.NativeDisplayTracker;
import com.xad.sdk.events.AdViewRequestEvent;
import com.xad.sdk.events.CreativeEvent;
import com.xad.sdk.events.ErrorEvent;
import com.xad.sdk.listeners.InterstitialAdListener;
import com.xad.sdk.listeners.TestListener;
import com.xad.sdk.mraid.MRAIDNativeFeatureListener;
import com.xad.sdk.mraid.MRAIDView;
import com.xad.sdk.mraid.MRAIDViewListener;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.Logger;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;

/**
 * Created by Ray.Wu on 8/19/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class InterstitialAd implements IAdViewBase, MRAIDViewListener, MRAIDNativeFeatureListener {
    public static final String TAG = "InterstitialAd";

    //Required before load ad
    private Context mContext;
    private InterstitialAdListener mListener;
    private String accessKey;
    private AdRequest adRequest;

    //Internal only
    private MRAIDView mMraidViewAsInterstitial;
    private boolean isReady = false;
    private boolean hasFailed = false;
    private boolean hasBeenRequest = false;

    private TestListener mTestListener;

    @Deprecated
    public InterstitialAd(Context context) {
        this.mContext = context;
        DisplaySdk.sharedInstance().init(context);
        AdViewUtils.registerOnBus(this);
        if(context instanceof Activity) {
            mMoatFactory = MoatFactory.create((Activity)context);
        }
    }

    MoatFactory mMoatFactory;
    NativeDisplayTracker mDisplayTracker;

    @SuppressWarnings("deprecation")
    public InterstitialAd(Context context, @NonNull String accessKey) {
        this(context);
        this.accessKey = accessKey;
    }

    /*********************************************************
    *    Interstitial Setters and Getters
    **********************************************************/
    public void setAccessKey(String id) {
        this.accessKey = id;
    }
    public String getAccessKey() {
        return this.accessKey;
    }

    public void setAdListener(InterstitialAdListener listener) {
        this.mListener = listener;
    }
    public InterstitialAdListener getAdListener() {
        return mListener;
    }

    public void setAdRequest(AdRequest adRequest) {
        this.adRequest = adRequest;
    }
    public AdRequest getAdRequest() {
        return this.adRequest;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setTestListener(TestListener testListener) {
        this.mTestListener = testListener;
    }

    public void loadAd() {
        if(TextUtils.isEmpty(accessKey)) {
            throw new IllegalArgumentException("Access key must be set before load ad");
        }

        if(this.adRequest == null) {
            throw new IllegalArgumentException("AdRequest must be set before load ad");
        }

        if(hasBeenRequest) {
            Logger.logWarning(TAG, "Interstitial ad objects can only be used once even with different requests.");
            return;
        }
        hasBeenRequest = true;

        DisplaySdk.sharedBus().post(
                new AdViewRequestEvent(this,
                        AdSize.getInterstitialSize(this.mContext),
                        this.accessKey,
                        this.adRequest,
                        AdType.INTERSTITIAL));
        Logger.logDebug(TAG, "An request for Interstitial is posted ");
    }

    public void show() {
        if(this.mMraidViewAsInterstitial == null || !this.isReady()) {
            Logger.logWarning(TAG, "Interstitial is not ready. Please wait until you receive \"onAdLoaded\" callback");
            return;
        }

        if (this.hasFailed) {
            Logger.logWarning(TAG, "Failed to load interstitial. Internal error");
            this.mListener.onInterstitialFailedToShow(this);
            return;
        }
        this.mMraidViewAsInterstitial.showAsInterstitial();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCreativeEventReceived(CreativeEvent event) {
        if(event.Requester != this) {
            return;
        }
        this.mMraidViewAsInterstitial = new MRAIDView(this.mContext, event, this, this, true);
        this.mMraidViewAsInterstitial.setTestListener(this.mTestListener);

        if (mMoatFactory != null) {
            mDisplayTracker = mMoatFactory.createNativeDisplayTracker(this.mMraidViewAsInterstitial, Constants.MOAT_DISPLAY_PARTNER_CODE);
            startTracker(event);
        }
    }

    private void startTracker(CreativeEvent event) {
        if(event.IsFromTestChannel) {
            //Not start tracking for testing
            return;
        }
        HashMap<String, String> adIds = new HashMap<>();
        adIds.put("moatClientLevel1", event.VendorId);
        adIds.put("moatClientLevel2", event.CampaignId);
        adIds.put("moatClientLevel3", event.adGroupId);
        adIds.put("moatClientLevel4", event.CreativeId);
        adIds.put("moatClientSlicer1", accessKey);
        if(mDisplayTracker != null) {
            mDisplayTracker.track(adIds);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEventReceived(ErrorEvent errorEvent) {
        if(errorEvent.Requester != this) {
            return;
        }

        if(this.mListener != null) {
            this.mListener.onAdFetchFailed(this, errorEvent.Error);
        }
    }

    //Activity Lifecycle Callbacks
    @Override
    public void resume() {
        AdViewUtils.registerOnBus(this);
    }

    @Override
    public void destroy() {
        if(this.mMraidViewAsInterstitial != null) {
            this.mMraidViewAsInterstitial.removeAllViewFromParent();
            this.mMraidViewAsInterstitial.destroy();
            this.mMraidViewAsInterstitial = null;
        }
        this.mContext = null;
        Logger.logDebug(TAG, "Interstitial " + this + " is destroyed");
        if(mDisplayTracker != null) {
            mDisplayTracker.stopTracking();
        }
    }

    @Override
    public void pause() {
        AdViewUtils.unregisterOnBus(this);
    }

    public void disableOrientation(){
        if(mContext == null || !(mContext instanceof Activity)) return;
        int orientation;
        int rotation = ((WindowManager) mContext.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_180:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case Surface.ROTATION_270:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            default:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
        }

        ((Activity)mContext).setRequestedOrientation(orientation);
    }

    public void enableOrientation(){
        if(mContext == null) return;
        ((Activity)mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /******************************************************************************
     * InterstitialListener Implementation.
     ******************************************************************************/
    @Override
    public void mraidViewLoaded(MRAIDView mraidView) {
        Logger.logDebug(TAG, "Interstitial Loaded");
        this.isReady = true;
        if (this.mListener != null) {
            this.mListener.onAdLoaded(this);
        }
    }

    @Override
    public void mraidViewFailed(MRAIDView mraidView) {
        Logger.logDebug(TAG, " Interstitial Failed");
        this.hasFailed = true;
    }

    @Override
    public void mraidViewExpand(MRAIDView mraidView) {
        Logger.logDebug(TAG, "Interstitial showed");
        if (this.mListener != null) {
            this.mListener.onInterstitialShown(this);
        }
        disableOrientation();
    }

    @Override
    public void mraidViewClose(MRAIDView mraidView) {
        Logger.logDebug(TAG, "mraidViewClose");
        this.isReady = false;
        this.mMraidViewAsInterstitial = null;
        if (this.mListener != null) {
            this.mListener.onAdClosed(this);
        }
        enableOrientation();
    }

    @Override
    public boolean mraidViewResize(MRAIDView mraidView, int width, int height, int offsetX, int offsetY) {
        //Interstitial ad should not be able to resize
        return false;
    }


    /******************************************************************************
     * MRAIDNativeFeatureListener Implementation.
     ******************************************************************************/
    @Override
    public void mraidNativeFeatureCallTel(String url) {
            MRAIDNativeFeatureProvider.callTel(this.mContext, url);
            if (mListener != null) {
                mListener.onAdLeftApplication(this);
            }
    }

    @Override
    public void mraidNativeFeatureCreateCalendarEvent(String eventJSON) {
            MRAIDNativeFeatureProvider.createCalendarEvent(this.mContext, eventJSON);
    }

    @Override
    public void mraidNativeFeaturePlayVideo(String url) {
            MRAIDNativeFeatureProvider.playVideo(this.mContext, url);
    }

    @Override
    public void mraidNativeFeatureOpenBrowser(String url) {
        MRAIDNativeFeatureProvider.openBrowser(this.mContext, url);
        if(mListener != null) {
            mListener.onAdOpened(this);
        }
    }

    @Override
    public void mraidNativeFeatureStorePicture(String url) {
        MRAIDNativeFeatureProvider.storePicture(this.mContext, url);
    }

    @Override
    public void mraidNativeFeatureSendSms(String url) {
        MRAIDNativeFeatureProvider.sendSms(this.mContext, url);
        if(mListener != null) {
            mListener.onAdLeftApplication(this);
        }
    }
}
