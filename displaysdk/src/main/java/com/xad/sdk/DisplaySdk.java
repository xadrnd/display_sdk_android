package com.xad.sdk;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.LocationListener;
import com.xad.sdk.events.AdViewRequestEvent;
import com.xad.sdk.events.CreativeEvent;
import com.xad.sdk.events.ErrorEvent;
import com.xad.sdk.utils.AdTestUrlGenerator;
import com.xad.sdk.utils.AdUrlGenerator;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.ErrorPosting;
import com.xad.sdk.utils.Logger;
import com.xad.sdk.utils.UrlGenerator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import static com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo;

/**
 * Created by Ray.Wu on 8/15/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class DisplaySdk implements LocationListener {
    public static final String TAG = "DisplaySdk";

    @Override
    public void onLocationChanged(Location location) {
        Logger.logDebug(TAG, "Location updates: " + location.toString());
    }

    //========Singleton SDK, share instance=======
    private static class InstanceLoader{
        private static final DisplaySdk sInstance = new DisplaySdk();
        private static EventBus displayEventBus;
    }

    public static DisplaySdk sharedInstance() {
        return InstanceLoader.sInstance;
    }

    public static EventBus sharedBus() {
        return InstanceLoader.displayEventBus;
    }
    public Context getApplicationContext() {
        return this.mContext;
    }

    private DisplaySdk() {
        InstanceLoader.displayEventBus = EventBus.builder()
                .logNoSubscriberMessages(Logger.getLevel() == Logger.Level.DEBUG)
                .eventInheritance(false)
                .build();

        Logger.logDebug(TAG, "DisplaySdk instance is created");
    }

    private Context mContext;

    private LocationProvider mLocationProvider;

    public void init(Context context) {
        registerOnBus();
        if(mContext == null) {
            Logger.logDebug(TAG, "Init DisplaySdk");
            sharedInstance().mContext = context.getApplicationContext();
            this.mLocationProvider = new LocationProvider(mContext, this);
            this.mLocationProvider.initProvider(Constants.LOCATION_INTERVAL_MS, Constants.LOCATION_FASTEST_INTERVAL_MS, Constants.LOCATION_DISTANCE_FILTER);
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onAdViewRequestEventReceived(AdViewRequestEvent adViewRequestEvent) {
        UrlGenerator generator;

        if (adViewRequestEvent.AdRequest.isTesting()) {
            generator = new AdTestUrlGenerator(Constants.xAdServerTestUrl, adViewRequestEvent.AdRequest.getTestType(), adViewRequestEvent.AdRequest.getTestChannelId())
                    .withAdTypeAndAdSize(adViewRequestEvent.Type, adViewRequestEvent.AdSize)
                    .withAccessKey(adViewRequestEvent.AccessKey);

        } else {
            generator = new AdUrlGenerator(mContext, Constants.xAdServerUrl)
                    .withFormat(adViewRequestEvent.Type)
                    .withAdRequest(adViewRequestEvent.AdRequest)
                    .withAdSize(adViewRequestEvent.AdSize)
                    .withAccessKey(adViewRequestEvent.AccessKey);

            //Get Advertising Info and build url request with it.
            try {
                Info info = getAdvertisingIdInfo(mContext);
                ((AdUrlGenerator)generator).withAdvertisingInfo(info);
            } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException e) {
                Logger.logWarning(TAG, "Google Play Service is not available, instead will use Android ID as user ID");
                ((AdUrlGenerator)generator).withAndroidID(mContext);
                e.printStackTrace();
            }

            Location currentLocation = mLocationProvider.getLastLocation();
            if(currentLocation != null) {
                ((AdUrlGenerator)generator).withLocation(currentLocation);
            }
        }

        String requestUrl = generator.generateUrlString();

        CreativeEvent creativeEvent = fetchAdCreative(adViewRequestEvent, requestUrl);
        if(creativeEvent != null) {
            DisplaySdk.sharedBus().post(creativeEvent);
        } else {
            //Network issue or bad request or no fill
        }
        Logger.logDebug(TAG, "Creative has been successfully fetched and posted");
    }

    //Create http request and fetch creative. Running in the caller's thread, thus caller should be in worker thread.
    @Nullable private CreativeEvent fetchAdCreative(AdViewRequestEvent adViewRequestEvent, String url) {
        Logger.logDebug(TAG, "Request url is: " + url);
        if (!isNetworkAvailable()) {
            Logger.logError(TAG, "Network is not available");
            DisplaySdk.sharedBus().post(new ErrorEvent(adViewRequestEvent.Requester, ErrorCode.NETWORK_ERROR));
            return null;
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Constants.AD_REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .build();
        Request request;

        if(Constants.DEBUG_ON_BACKEND) {
             request = new Request.Builder()
                    .addHeader("x-xad-diag-dump-demand-request", "true")
                    .addHeader("x-xad-diag-dump-demand-response", "true")
                    .url(url)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(url)
                    .build();
        }
        Response response;
        try {
            response = client.newCall(request).execute();
            Logger.logDebug(TAG, "Response code: " + response.code());
            if (!response.isSuccessful()) {
                response.body().close();
                DisplaySdk.sharedBus().post(new ErrorEvent(adViewRequestEvent.Requester, ErrorCode.BAD_REQUEST));
                ErrorPosting.sendError(mContext, ErrorPosting.SERVER_ERROR_TO_POST, "Response status code: " + response.code());
                return null;
            }

            String responseBody = response.body().string();
            if(TextUtils.isEmpty(responseBody)) {
                Logger.logWarning(TAG, "No ad matched for current request");
                DisplaySdk.sharedBus().post(new ErrorEvent(adViewRequestEvent.Requester, ErrorCode.NO_INVENTORY));
                return null;
            }

            if(Constants.DEBUG_ON_BACKEND) {
                Logger.logDebug(TAG, "Start debugging, no creative will be shown");
                try {
                    JSONObject bodyJSON = new JSONObject(responseBody);
                    JSONObject demandPartnerJSON = bodyJSON.getJSONObject("demand-partner");
                    JSONObject neptuneRequestJSON = demandPartnerJSON.getJSONObject("neptunenormandy");
                    JSONObject demandRequestJSON = neptuneRequestJSON.getJSONObject("demand-request");
                    //URI from Atlantic to Neptune
                    String uriForNeptune = demandRequestJSON.getString("uri");
                    Logger.logDebug(TAG,"URI for Neptune:" + uriForNeptune);

                    //Neptune response
                    JSONObject demandResponseJSON = neptuneRequestJSON.getJSONObject("demand-response");
                    String demandResponseData = demandResponseJSON.getString("data");
                    JSONObject demandResponseDataJSON = new JSONObject(demandResponseData);
                    JSONObject demandResponseResultJSON = demandResponseDataJSON.getJSONObject("results");
                    JSONObject paidListingsJSON = demandResponseResultJSON.getJSONObject("paid_listings");
                    JSONArray paidListsJSON = paidListingsJSON.getJSONArray("listing");
                    for(int i=0; i<paidListsJSON.length(); i++) {
                        JSONObject json = paidListsJSON.getJSONObject(i);
                        Logger.logDebug(TAG, "Neptune Response:" + json.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            //get adGroupId
            String adGroupInfo;
            String adVendorId = null;
            String adCampaign = null;
            String adGroupId = null;
            String adCreativeId = null;
            if(adViewRequestEvent.AdRequest.isTesting()) {
                adGroupInfo= response.header("x-channel-id");
                String[] adGroupIds = adGroupInfo.split("-");
                if (adGroupIds.length > 0) {
                    adCreativeId = adGroupIds[0];
                }
            } else {
                adGroupInfo= response.header("x-xad-ad-ref");
                String[] adGroupIds = adGroupInfo.split("-");
                adVendorId = adGroupIds.length > 0 ? adGroupIds[0] : null;
                adCampaign = adGroupIds.length > 1 ? adGroupIds[1] : null;
                adGroupId = adGroupIds.length > 2 ? adGroupIds[2] : null;
                adCreativeId = adGroupIds.length > 3 ? adGroupIds[3] : null;
            }

            Logger.logDebug(TAG, "Ad Group Id: " + adGroupInfo);
            return new CreativeEvent(adViewRequestEvent.Requester,
                    responseBody,
                    adVendorId,
                    adCampaign,
                    adGroupId,
                    adCreativeId,
                    adViewRequestEvent.AdRequest.isTesting());
        } catch (IOException e) {
            DisplaySdk.sharedBus().post(new ErrorEvent(adViewRequestEvent.Requester, ErrorCode.NETWORK_ERROR));
            e.printStackTrace();
            return null;
        }
    }

    public void resume() {
        registerOnBus();
        if (mLocationProvider != null) {
            mLocationProvider.requestLocationUpdate();
        }
    }

    public void pause() {
        if (mLocationProvider != null) {
            mLocationProvider.removeLocationUpdate();
        }
        unregisterOnBus();
    }

    public void destroy() {
        Logger.logDebug(TAG, "DisplaySdk instance is destroyed");
        unregisterOnBus();
        if (this.mLocationProvider != null) {
            this.mLocationProvider.destroy();
        }
        this.mContext = null;
    }

    private void registerOnBus() {
        if (!sharedBus().isRegistered(this)) {
            sharedBus().register(this);
        }
    }

    private void unregisterOnBus() {
        if (sharedBus().isRegistered(this)) {
            sharedBus().unregister(this);
        }
    }

    private boolean isNetworkAvailable() {
        if(mContext == null) return false;
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
