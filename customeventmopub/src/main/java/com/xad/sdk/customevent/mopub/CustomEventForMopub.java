package com.xad.sdk.customevent.mopub;

import android.content.Context;
import android.text.TextUtils;

import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;
import com.xad.sdk.AdRequest;
import com.xad.sdk.AdSize;
import com.xad.sdk.BannerView;
import com.xad.sdk.DisplaySdk;
import com.xad.sdk.ErrorCode;
import com.xad.sdk.RefreshInterval;
import com.xad.sdk.listeners.BannerViewListener;
import com.xad.sdk.utils.Logger;

import java.util.Map;

/**
 * Created by Ray.Wu on 1/23/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class CustomEventForMopub extends CustomEventBanner {

    private BannerView mAdView;
    private static final String TAG = "CustomEventFroMopub";

    @Override
    protected void loadBanner(Context context,
                              CustomEventBannerListener customEventBannerListener,
                              Map<String, Object> localExtras,
                              Map<String, String> serverExtras) {
        DisplaySdk.sharedInstance().init(context);
        mAdView = new BannerView(context, AdSize.BANNER, serverExtras.get("accessKey"));

        mAdView.setAdListener(new BannerAdListenerForwarder(customEventBannerListener));
        mAdView.setRefresh(RefreshInterval.STATIC);

        mAdView.setAdRequest(buildRequest(localExtras, serverExtras));
        mAdView.loadAd();

        Logger.logDebug(TAG, "create a new instance of BannerView in Adapter");
    }

    @Override
    protected void onInvalidate() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        DisplaySdk.sharedInstance().destroy();
    }


    private AdRequest buildRequest(Map<String, Object> localExtras,
                                   Map<String, String> serverExtras) {

        AdRequest.Builder builder = new AdRequest.Builder()
                .setGender(genderFromString((String)localExtras.get("m_gender")))
                .setAge(ageFromString((String)localExtras.get("m_age")));

        String isTestingString = (String)localExtras.get("test_mode");
        if(!TextUtils.isEmpty(isTestingString)) {
            serverExtras.remove("test_mode");
            if(Boolean.valueOf(isTestingString)) {
                builder.setTestMode(true);
            }
        }

        for(Map.Entry<String, String> entry : serverExtras.entrySet()) {
            builder.addExtras(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private AdRequest.Gender genderFromString(String genderStr) {
        if(genderStr == null) return AdRequest.Gender.UNKNOWN;
        switch (genderStr) {
            case "male":
                return AdRequest.Gender.MALE;
            case "female":
                return AdRequest.Gender.FEMALE;
            default:
                return AdRequest.Gender.UNKNOWN;
        }
    }

    private int ageFromString(String ageStr) {
        int age = -1;
        try {
            age = Integer.valueOf(ageStr);
        } catch (NumberFormatException e) {
            Logger.logWarning(TAG, "No age field found");
        }
        return age;
    }

    private static class BannerAdListenerForwarder implements BannerViewListener {
        private CustomEventBannerListener customEventBannerListener;
        BannerAdListenerForwarder(CustomEventBannerListener listener) {
            this.customEventBannerListener = listener;
        }

        public void onAdLoaded(BannerView view) {
            if(customEventBannerListener != null) {
                customEventBannerListener.onBannerLoaded(view);
            } else {
                Logger.logError(TAG, "No banner listener found");
            }
        }
        public void onAdFetchFailed(BannerView view, ErrorCode code) {
            if(customEventBannerListener == null) {
                Logger.logError(TAG, "No banner listener found");
                return;
            }
            switch (code) {
                case BAD_REQUEST:
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
                    break;
                case NO_INVENTORY:
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    break;
                case NETWORK_ERROR:
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_TIMEOUT);
                    break;
                case UNKNOWN:
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.MRAID_LOAD_ERROR);
            }
        }
        public void onAdClosed(BannerView view) {
            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerCollapsed();
            }
        }
        public void onAdOpened(BannerView view) {
            if (customEventBannerListener != null) {
                customEventBannerListener.onBannerExpanded();
            }
        }
        public void onAdLeftApplication(BannerView view) {
            if (customEventBannerListener != null) {
                customEventBannerListener.onLeaveApplication();
            }
        }
    }
}

