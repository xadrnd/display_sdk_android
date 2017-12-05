package com.xad.sdk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.webkit.WebView;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.xad.sdk.AdRequest;
import com.xad.sdk.AdSize;
import com.xad.sdk.AdType;
import com.xad.sdk.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Ray.Wu on 4/26/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class AdUrlGenerator extends UrlGenerator {
    private static final String TAG = "AdUrlGenerator";

    public AdUrlGenerator(Context context) {
        super();
        withHttp();
        addDeviceParams(context);
    }

    public AdUrlGenerator(Context context, String url) {
        super(url);
        addDeviceParams(context);
    }

    private void addDeviceParams(Context context) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        addParam("sdk", "xad_display_sdk_android");
        addParam("sdkv", BuildConfig.VERSION_NAME);
        addParam("os", "android");
        addParam("osv", Integer.toString(Build.VERSION.SDK_INT));
        addParam("v", "1.2");
        addParam("sdk_conf", "jssdk");
        addParam("dev_model", Build.MODEL);
        addParam("dev_type", deviceType(context));
        addParam("carrier", tm.getNetworkOperatorName());
        addParam("lang", Locale.getDefault().getDisplayLanguage());
        addParam("devid", userAgent(context));
        addParam("wifi", wifiInfo.getSSID());
        addParam("secure", "1");

        try {
            int labelStringId = context.getApplicationInfo().labelRes;
            addParam("appname", context.getString(labelStringId));
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            addParam("appver", packageInfo.versionName);
            addParam("bundle", packageInfo.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String deviceType(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE ? "tablet" : "phone";
    }

    private static String userAgent;

    private String userAgent(final Context context) {
        if(TextUtils.isEmpty(userAgent)) {
            Handler handler = new Handler(context.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    WebView webView = new WebView(context);
                    userAgent = webView.getSettings().getUserAgentString();
                }
            });
        }
        return userAgent != null ? userAgent : System.getProperty("http.agent");
    }

    public AdUrlGenerator withAdRequest(AdRequest adRequest) {
        //only apply for Video ad
        if(adRequest.getVmax() > 0 && adRequest.getVmin() > 0) { //default value for vmax & vmin are -1
            addParam("vmin", Integer.toString(adRequest.getVmin()));
            addParam("vmax", Integer.toString(adRequest.getVmax()));
        }

        if(adRequest.getAge() > 0) {
            addParam("age", Integer.toString(adRequest.getAge()));
        }
        addParam("gender", adRequest.getGender().value);

        if (adRequest.getZipCode() != null) {
            addParam("zip", adRequest.getZipCode());
        }
        if(adRequest.getState() != null && adRequest.getCity() != null) {
            addParam("loc", adRequest.getCity() + "," + adRequest.getState());
        }

        if (adRequest.getExtras() != null) {
            for(Map.Entry<String, String> entry : adRequest.getExtras().entrySet()) {
                addParam(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public AdUrlGenerator withFormat(AdType type) {
        switch (type) {
            case BANNER:
                this.addParam("o_fmt", "html5,exp");
                this.addParam("api", "3");
                this.addParam("api", "5");
                this.addParam("instl", "0");
                break;
            case INTERSTITIAL:
                this.addParam("o_fmt", "html5,exp");
                this.addParam("api", "3");
                this.addParam("api", "5");
                this.addParam("instl", "1");
                break;
            case REWARDED_VIDEO:
                this.addParam("o_fmt", "video");
                this.addParam("vmime", "video/mp4");
                this.addParam("vmime", "video/mpeg-4");
                this.addParam("vmime", "video/3gpp");
                this.addParam("vmime", "video/3gp");
                this.addParam("vmime", "video/mkv");
                this.addParam("vmime", "video/webm");
                this.addParam("vlinearity", "1");
                this.addParam("vprotocol", "1");
                this.addParam("vprotocol", "2");
                this.addParam("vprotocol", "4");
                this.addParam("vprotocol", "5");
                break;
            case NATIVE:
                break;

        }
        return this;
    }

    //For Interstitial, Video, adSize should be null
    public AdUrlGenerator withAdSize(@Nullable AdSize adSize) {
        if(adSize != null) {
            addParam("size", adSize.toString());
        } else {
            Logger.logDebug(TAG, "Request for video ad should not include size");
        }
        return this;
    }

    public AdUrlGenerator withAccessKey(@NonNull String accessKey) {
        addParam("k", accessKey);
        return this;
    }

    public AdUrlGenerator withLocation(@Nullable Location location) {
        if(location == null) {
            return this;
        }

        addParam("lat", Double.toString(location.getLatitude()));
        addParam("long", Double.toString(location.getLongitude()));
        if (location.hasAccuracy()) {
            addParam("ha", Float.toString(location.getAccuracy()));
        }
        addParam("alt", Double.toString(location.getAltitude()));

        if(location.hasBearing()) {
            addParam("course", Float.toString(location.getBearing()));
        }
        addParam("timestamp", Long.toString(location.getTime()));

        return this;
    }

    //If Google Play Service is available, otherwise please use method below
    public AdUrlGenerator withAdvertisingInfo(AdvertisingIdClient.Info advertisingInfo) {
        addParam("dnt", advertisingInfo.isLimitAdTrackingEnabled()?"1":"0");
        addParam("uid", advertisingInfo.getId());
        addParam("uid_type", "GIDFA|RAW"); //using Google ID for Advertising
        return this;
    }

    //Used when Google Play Service is not available
    public AdUrlGenerator withAndroidID(Context context) {
        addParam("dnt", "0");
        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        try {
            addParam("uid", SHA1(androidId));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        addParam("uid_type", "Android_Id|SHA1");
        return this;
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte aData : data) {
            int halfByte = (aData >>> 4) & 0x0F;
            int twoHalf = 0;
            do {
                if ((0 <= halfByte) && (halfByte <= 9))
                    buf.append((char) ('0' + halfByte));
                else
                    buf.append((char) ('a' + (halfByte - 10)));
                halfByte = aData & 0x0F;
            } while (twoHalf++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash;
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }
}
