package com.xad.sdk.customevent.mopub;

import android.content.Context;
import android.text.TextUtils;

import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.xad.sdk.AdRequest;
import com.xad.sdk.DisplaySdk;
import com.xad.sdk.ErrorCode;
import com.xad.sdk.InterstitialAd;
import com.xad.sdk.listeners.InterstitialAdListener;
import com.xad.sdk.utils.Logger;

import java.util.Map;

/**
 * Created by Ray.Wu on 2/21/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class CustomEventInterstitialForMopub extends CustomEventInterstitial {
    private static final String TAG = "CustomEventInterstitialFroMopub";
    private InterstitialAd mInterstitial;
    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {
        DisplaySdk.sharedInstance().init(context);

        mInterstitial = new InterstitialAd(context);
        mInterstitial.setAccessKey(serverExtras.get("accessKey"));
        mInterstitial.setAdListener(new InterstitialAdListenerForwarder(customEventInterstitialListener));

        mInterstitial.setAdRequest(buildRequest(localExtras, serverExtras));
        mInterstitial.loadAd();

        Logger.logDebug(TAG, "create a new instance of InterstitialAd in Adapter");
    }

    @Override
    protected void showInterstitial() {
        if(mInterstitial != null && mInterstitial.isReady()) {
            mInterstitial.show();
        } else {
            Logger.logWarning(TAG, "Interstitial is not ready to show, will skip");
        }
    }

    @Override
    protected void onInvalidate() {
        if (mInterstitial != null) {
            mInterstitial.destroy();
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

    private static class InterstitialAdListenerForwarder implements InterstitialAdListener {
        private CustomEventInterstitialListener customEventInterstitialListener;

        InterstitialAdListenerForwarder(CustomEventInterstitialListener listener) {
            this.customEventInterstitialListener = listener;
        }

        public void onAdLoaded(InterstitialAd interstitialAd) {
            if(customEventInterstitialListener != null) {
                customEventInterstitialListener.onInterstitialLoaded();
            } else {
                Logger.logError(TAG, "No banner listener found");
            }
        }
        public void onAdFetchFailed(InterstitialAd interstitialAd, ErrorCode code) {
            if(customEventInterstitialListener == null) {
                Logger.logError(TAG, "No interstitial listener found");
                return;
            }
            switch (code) {
                case BAD_REQUEST:
                    customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                    break;
                case NO_INVENTORY:
                    customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    break;
                case NETWORK_ERROR:
                    customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_TIMEOUT);
                    break;
                case UNKNOWN:
                    customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.MRAID_LOAD_ERROR);
            }
        }

        @Override
        public void onAdClosed(InterstitialAd interstitialAd) {
            if (customEventInterstitialListener != null) {
                customEventInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAdOpened(InterstitialAd interstitialAd) {
            if (customEventInterstitialListener != null) {
                customEventInterstitialListener.onInterstitialClicked();
            }
        }

        @Override
        public void onAdLeftApplication(InterstitialAd interstitialAd) {
            if (customEventInterstitialListener != null) {
                customEventInterstitialListener.onLeaveApplication();
            }
        }

        @Override
        public void onInterstitialShown(InterstitialAd interstitialAd) {
            if(customEventInterstitialListener != null) {
                customEventInterstitialListener.onInterstitialShown();
            }
        }

        @Override
        public void onInterstitialFailedToShow(InterstitialAd interstitialAd) {

        }
    }
}
