package com.xad.sdk.customevent.googlemobileads;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventBanner;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitial;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener;
import com.xad.sdk.AdRequest;
import com.xad.sdk.AdSize;
import com.xad.sdk.BannerView;
import com.xad.sdk.DisplaySdk;
import com.xad.sdk.ErrorCode;
import com.xad.sdk.InterstitialAd;
import com.xad.sdk.RefreshInterval;
import com.xad.sdk.listeners.BannerViewListener;
import com.xad.sdk.listeners.InterstitialAdListener;
import com.xad.sdk.utils.Logger;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Ray.Wu on 3/15/16.
 */
public class CustomEventForAdmob implements BannerViewListener, InterstitialAdListener, CustomEventBanner, CustomEventInterstitial {

    private static final String TAG = "CustomEventForAdmob";

    private BannerView mAdView;
    private InterstitialAd mInterstitial;

    private CustomEventBannerListener mBannerListener;
    private CustomEventInterstitialListener mInterstitialListener;

    @Override
    public void requestBannerAd(Context context,
                                CustomEventBannerListener customEventBannerListener,
                                String serverParameterAsAccessKey,
                                com.google.android.gms.ads.AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle customExtras) {
        mAdView = new BannerView(context, AdSize.createAdSizeFromAdMobAdSize(adSize), serverParameterAsAccessKey);

        this.mBannerListener = customEventBannerListener;
        mAdView.setAdListener(this);
        mAdView.setRefresh(RefreshInterval.STATIC);

        mAdView.setAdRequest(buildRequest(mediationAdRequest, customExtras));
        mAdView.loadAd();
        Logger.logDebug(TAG, "create a new instance of BannerView in Adapter");
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      CustomEventInterstitialListener customEventInterstitialListener,
                                      String serverParameter,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle customExtras) {
        DisplaySdk.sharedInstance().init(context);
        this.mInterstitialListener = customEventInterstitialListener;

        this.mInterstitial = new InterstitialAd(context);
        mInterstitial.setAdListener(this);
        mInterstitial.setAccessKey(serverParameter);
        mInterstitial.setAdRequest(buildRequest(mediationAdRequest, customExtras));
        mInterstitial.loadAd();
        Logger.logDebug(TAG, "create a new instance of BannerView in Adapter");
    }

    @Override
    public void showInterstitial() {
        if(mInterstitial != null) {
            mInterstitial.show();
        }
    }

    @Override
    public void onDestroy() {
        //Activity/Context destroy
        Logger.logDebug("Ads--" + this, "Destroy BannerView in Adapter");
        if (mAdView != null) {
            mAdView.destroy();
        }
        if(mInterstitial != null) {
            mInterstitial.destroy();
        }
        DisplaySdk.sharedInstance().destroy();
    }

    @Override
    public void onPause() {
        //Activity/Context pause
        Logger.logDebug("Ads--" + this, "Pause BannerView in Adapter");
        if (mAdView != null) {
            mAdView.pause();
        }
        if(mInterstitial != null) {
            mInterstitial.pause();
        }
        DisplaySdk.sharedInstance().pause();
    }

    @Override
    public void onResume() {
        //Activity/Context resume
        Logger.logDebug(TAG, "Resume BannerView in Custom Event");
        DisplaySdk.sharedInstance().resume();
        if (mAdView != null) {
            mAdView.resume();
        }
        if(mInterstitial != null) {
            mInterstitial.resume();
        }
    }

    private AdRequest buildRequest(MediationAdRequest request, Bundle customExtras) {
        AdRequest.Builder builder = new AdRequest.Builder();

        Calendar birthdayCal = Calendar.getInstance();
        Date birthdayDate = request.getBirthday();
        if (birthdayDate != null) {
            birthdayCal.setTime(birthdayDate);
            builder.setBirthday(birthdayCal);
        }

        switch (request.getGender()) {
            case com.google.android.gms.ads.AdRequest.GENDER_MALE:
                builder.setGender(AdRequest.Gender.MALE);
                break;
            case com.google.android.gms.ads.AdRequest.GENDER_FEMALE:
                builder.setGender(AdRequest.Gender.FEMALE);
                break;
            case com.google.android.gms.ads.AdRequest.GENDER_UNKNOWN:
                builder.setGender(AdRequest.Gender.UNKNOWN);
                break;
        }

        return builder.build();
    }

    /*********************************************************
    *    Implement Interstitial Listener
    **********************************************************/

    @Override
    public void onAdLoaded(InterstitialAd interstitialAd) {
        if(mInterstitialListener != null) {
            mInterstitialListener.onAdLoaded();
        }
    }

    @Override
    public void onAdFetchFailed(InterstitialAd interstitialAd, ErrorCode code) {
        switch (code) {
            case UNKNOWN:
                if(mInterstitialListener != null) {
                    mInterstitialListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }

                break;
            case BAD_REQUEST:
                if (mInterstitialListener != null) {
                    mInterstitialListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_INVALID_REQUEST);
                }

                break;
            case NETWORK_ERROR:
                if (mInterstitialListener != null) {
                    mInterstitialListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_NETWORK_ERROR);
                }

                break;
            case NO_INVENTORY:
                if (mInterstitialListener != null) {
                    mInterstitialListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL);
                }
                break;
        }
    }

    @Override
    public void onAdClosed(InterstitialAd interstitialAd) {
        if(mInterstitialListener != null) {
            mInterstitialListener.onAdClosed();
        }
    }

    @Override
    public void onAdOpened(InterstitialAd interstitialAd) {
        if(mInterstitialListener != null) {
            mInterstitialListener.onAdClicked();
        }
    }

    @Override
    public void onAdLeftApplication(InterstitialAd interstitialAd) {
        if(mInterstitialListener != null) {
            mInterstitialListener.onAdLeftApplication();
        }
    }

    @Override
    public void onInterstitialShown(InterstitialAd interstitialAd) {
        if(mInterstitialListener != null) {
            mInterstitialListener.onAdOpened();
        }
    }

    @Override
    public void onInterstitialFailedToShow(InterstitialAd interstitialAd) {

    }

    /*********************************************************
    *    Implement BannerViewListener
    **********************************************************/

    @Override
    public void onAdLoaded(BannerView bannerView) {
        if(mBannerListener != null) {
            mBannerListener.onAdLoaded(bannerView);
        }
    }

    @Override
    public void onAdFetchFailed(BannerView bannerView, ErrorCode code) {
        switch (code) {
            case UNKNOWN:
                if (mBannerListener != null) {
                    mBannerListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
                break;
            case BAD_REQUEST:
                if (mBannerListener != null) {
                    mBannerListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
                break;
            case NETWORK_ERROR:
                if (mBannerListener != null) {
                    mBannerListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_NETWORK_ERROR);
                }
                break;
            case NO_INVENTORY:
                if (mBannerListener != null) {
                    mBannerListener.onAdFailedToLoad(com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL);
                }
                break;
        }
    }

    @Override
    public void onAdClosed(BannerView bannerView) {
        if(mBannerListener != null) {
            mBannerListener.onAdClosed();
        }
    }

    @Override
    public void onAdOpened(BannerView bannerView) {
        if(mBannerListener != null) {
            mBannerListener.onAdOpened();
        }
    }

    @Override
    public void onAdLeftApplication(BannerView bannerView) {
        if(mBannerListener != null) {
            mBannerListener.onAdLeftApplication();
        }
    }
}
