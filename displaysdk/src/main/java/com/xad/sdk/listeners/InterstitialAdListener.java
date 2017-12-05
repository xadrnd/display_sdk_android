package com.xad.sdk.listeners;

import com.xad.sdk.ErrorCode;
import com.xad.sdk.InterstitialAd;

/**
 * Created by Ray.Wu on 2/21/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public interface InterstitialAdListener {
    //When interstitial is received
    void onAdLoaded(InterstitialAd interstitialAd);
    //When interstitial is failed to load, e.g. no response for request
    void onAdFetchFailed(InterstitialAd interstitialAd, ErrorCode code);
    //When interstitial showing successfully after call 'interstitial.show()'
    void onInterstitialShown(InterstitialAd interstitialAd);
    //When interstitial is failed to show due to creative not well-formatted or other unknown error
    void onInterstitialFailedToShow(InterstitialAd interstitialAd);
    //When user click close button
    void onAdClosed(InterstitialAd interstitialAd);
    //When user click on the interstitial, and open in ChromeCustomTab
    void onAdOpened(InterstitialAd interstitialAd);
    //When interstitial leaves the application (e.g., to call/sms).
    void onAdLeftApplication(InterstitialAd interstitialAd);
}
