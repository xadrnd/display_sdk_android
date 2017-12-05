package com.xad.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.moat.analytics.mobile.MoatFactory;
import com.moat.analytics.mobile.NativeDisplayTracker;
import com.xad.sdk.events.AdViewRequestEvent;
import com.xad.sdk.events.CreativeEvent;
import com.xad.sdk.events.ErrorEvent;
import com.xad.sdk.listeners.BannerViewListener;
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
 * The View to display banner ads. The ad request must be set prior to calling loadAd();
 *
 * Created by Ray.Wu on 8/15/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class BannerView extends RelativeLayout implements IAdViewBase, MRAIDNativeFeatureListener, MRAIDViewListener {
    private static final String TAG = "BannerView";
    private static final int REFRESH_MESSAGE = 0;

    //Required before load ad
    private Context mContext;
    private BannerViewListener mListener;
    private AdSize adSize;
    private String accessKey;
    private AdRequest adRequest;

    //Internal use only
    private RefreshInterval adInterval = RefreshInterval.MEDIUM;
    private Handler mRefreshHandler;
    private HandlerThread mRefreshThread;

    private boolean hasBeenRequested = false;

    private MRAIDView mraidView;

    private TestListener mTestListener;

    /**
     * Constructor when programmatically create an instance.
     * @param context Context instance needed.
     * @param adSize Specify banner size from four supported different sizes. Must set nonnull value prior to request banner.
     * @param accessKey Specify access key. You can contact {@link mailto:supply_support@xad.com} to get access key if you don't have one.
     */
    public BannerView(Context context, AdSize adSize, String accessKey) {
        this(context, null);
        this.adSize = adSize;
        this.accessKey = accessKey;
    }

    /**
     * Constructor when added in XML layout
     */
    public BannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        if(attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.XAdAttrs);
            try {
                this.accessKey = ta.getString(R.styleable.XAdAttrs_AccessKey);
                int sizeEnum = ta.getInteger(R.styleable.XAdAttrs_AdSize, 0);
                this.adSize = AdSize.values()[sizeEnum];
                int intervalEnum = ta.getInteger(R.styleable.XAdAttrs_AdInterval, 0);
                this.adInterval = RefreshInterval.values()[intervalEnum];
            } finally {
                ta.recycle();
            }
        }

        DisplaySdk.sharedInstance().init(context);
        AdViewUtils.registerOnBus(this);
        startRefreshThread();

        if(context instanceof Activity) {
            mMoatFactory = MoatFactory.create((Activity)context);
            mDisplayTracker = mMoatFactory.createNativeDisplayTracker(this, Constants.MOAT_DISPLAY_PARTNER_CODE);
        }
    }

    MoatFactory mMoatFactory;
    NativeDisplayTracker mDisplayTracker;

    /*********************************************************
     *    view Setters && Getters
     **********************************************************/
    public void setRefresh(RefreshInterval interval) {
        this.adInterval = interval;
    }
    public RefreshInterval getRefresh() {
        return this.adInterval;
    }

    public void setSize(AdSize adSize) {
        this.adSize = adSize;
    }
    public AdSize getAdSize() {
        return this.adSize;
    }

    public void setAccessKey(String id) {
        this.accessKey = id;
    }
    public String getAccessKey() {
        return this.accessKey;
    }

    /**
     * Set a Listener for banner view life cycle, i.e. when the banner is ready, or when fail to receive an banner due to a certain error, or when user click on the banner.
     */
    public void setAdListener(BannerViewListener listener) {
        this.mListener = listener;
    }
    public BannerViewListener getAdListener() {
        return mListener;
    }

    /**
     *Set an ad request for banner. Before request a banner, you need specify a request use AdRequest.Builer to help ad server to find a best matched creative
     */
    public void setAdRequest(AdRequest adRequest) {
        this.adRequest = adRequest;
    }
    public AdRequest getAdRequest() {
        return this.adRequest;
    }

    public void setTestListener(TestListener testListener) {
        this.mTestListener = testListener;
    }


    /**
     * Starts loading the ad on a background thread. The banner will automatically show once the request finished.
     */
    public void loadAd() {
        if (adSize == null) {
            throw new IllegalArgumentException("AdSize must be set before load ad");
        }

        if (accessKey == null || "".equals(accessKey)) {
            throw new IllegalArgumentException("Publisher key must be set before load ad");
        }

        if (this.adRequest == null) {
            throw new IllegalArgumentException("AdRequest must be set before load ad");
        }

        if (this.hasBeenRequested) {
            Logger.logWarning(TAG, "Banner will auto refresh with certain time, please don't load request multiple times.");
            return;
        }

        hasBeenRequested = true;
        raiseAdRequest();
    }

    private void raiseAdRequest() {
        DisplaySdk.sharedBus().post(
                new AdViewRequestEvent(this,
                        this.adSize,
                        this.accessKey,
                        this.adRequest,
                        AdType.BANNER));
        Logger.logDebug(TAG, "An request for Banner is posted");
        fireNextAdRequestTask();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCreativeEventReceived(CreativeEvent event) {
        if(event.Requester != this) {
            return;
        }

        int width = this.adSize.getWidthInPixels(mContext);
        int height = this.adSize.getHeightInPixels(mContext);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        mraidView = new MRAIDView(mContext, event, this, this);
        mraidView.setLayoutParams(params);

        mraidView.setTestListener(this.mTestListener);

        removeAllViews();
        addView(mraidView);
        startTracker(event);
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

    private void fireNextAdRequestTask() {
        final long elapsedTime = adInterval.getRefreshIntervalInSeconds();
        if(elapsedTime < 0) return;
        Message msg = mRefreshHandler.obtainMessage(REFRESH_MESSAGE);
        this.mRefreshHandler.sendMessageDelayed(msg, elapsedTime);
    }

    private void startRefreshThread() {
        mRefreshThread = new HandlerThread("RefreshThread");
        mRefreshThread.start();
        this.mRefreshHandler = new Handler(mRefreshThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(msg.what == REFRESH_MESSAGE) {
                    raiseAdRequest();
                    return true;
                }
                return false;
            }
        });
    }

    private void stopRefreshThread() {
        if(mRefreshHandler == null) return;
        mRefreshHandler.removeCallbacksAndMessages(null);
        mRefreshHandler = null;
        mRefreshThread.quitSafely();
    }

    //=============Activity lifecycle callback================
    @Override
    public void resume() {
        AdViewUtils.registerOnBus(this);
        if(mRefreshHandler != null
                && !mRefreshHandler.hasMessages(REFRESH_MESSAGE)
                && hasBeenRequested) {
            Logger.logVerbose(TAG, "Resuming banner view" + this);
            fireNextAdRequestTask();
        }
    }

    @Override
    public void destroy() {
        stopRefreshThread();
        if(this.mraidView != null) {
            removeView(mraidView);
            this.mraidView.removeAllViewFromParent();
            this.mraidView.destroy();
            this.mraidView = null;
        }
        this.mContext = null;
        Logger.logDebug(TAG, "BannerView " + this + " is destroyed");
        if(mDisplayTracker != null) {
            mDisplayTracker.stopTracking();
        }
    }

    @Override
    public void pause() {
        if(mRefreshHandler != null && mRefreshHandler.hasMessages(REFRESH_MESSAGE)) {
            mRefreshHandler.removeMessages(REFRESH_MESSAGE);
            Logger.logVerbose(TAG, "Pausing banner view" + this);
        }
        AdViewUtils.unregisterOnBus(this);
    }

    /******************************************************************************
     * MRAIDViewListener Implementation.
     ******************************************************************************/
    @Override
    public void mraidViewLoaded(MRAIDView mraidView) {
        Logger.logDebug(TAG, "Ad is loaded");
        if (this.mListener != null) {
            this.mListener.onAdLoaded(this);
        }
    }

    @Override
    public void mraidViewFailed(MRAIDView mraidView) {
        Logger.logError(TAG, "Fail to load ad");
        if(mListener != null) {
            mListener.onAdFetchFailed(this, ErrorCode.UNKNOWN);
        }
    }

    @Override
    public void mraidViewExpand(MRAIDView mraidView) {
        //Called when an ad opens an overlay that covers the screen, e.g. mraid.expand()
        Logger.logDebug(TAG, "Ad is expanded");
        if(this.mListener != null) {
            this.mListener.onAdOpened(this);
        }
    }

    @Override
    public void mraidViewClose(MRAIDView mraidView) {
        Logger.logDebug(TAG, "Ad is closed");
        if (this.mListener != null) {
            this.mListener.onAdClosed(this);
        }
    }

    @Override
    public boolean mraidViewResize(MRAIDView mraidView, int width, int height, int offsetX, int offsetY) {
        Logger.logDebug(TAG, "Ad is resizing");
        return true;
    }

    /******************************************************************************
     * MRAIDNativeFeatureListener Implementation.
     ******************************************************************************/
    @Override
    public void mraidNativeFeatureCallTel(String url) {
            MRAIDNativeFeatureProvider.callTel(mContext, url);
            if (mListener != null) {
                mListener.onAdLeftApplication(this);
            }
    }

    @Override
    public void mraidNativeFeatureCreateCalendarEvent(String eventJSON) {
            MRAIDNativeFeatureProvider.createCalendarEvent(mContext, eventJSON);
    }

    @Override
    public void mraidNativeFeaturePlayVideo(String url) {
            MRAIDNativeFeatureProvider.playVideo(mContext, url);
    }

    @Override
    public void mraidNativeFeatureOpenBrowser(String url) {
        Logger.logDebug(TAG, "Launching Landing page.");
        MRAIDNativeFeatureProvider.openBrowser(mContext, url);
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
