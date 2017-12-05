package com.xad.sdk.utils;

import com.xad.sdk.mraid.MRAIDNativeFeature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ray wu on 3/18/16.
 */
public class Constants {
    //TODO must change to false before release
    public static final boolean DEBUG_ON_BACKEND = false;
    public static final String XAD_HOST = "https://display-sdk.xad.com";
    public static final String xAdServerUrl = "https://display.xad.com/rest/banner";
    public static final String xAdServerTestUrl = "https://testchannel.xad.com";
    static final String ServerErrorEndpoint = "https://display.xad.com/sdk/errors";
    public static final long AD_REQUEST_TIMEOUT = 5;
    static final long ERROR_REPORT_REQUEST_TIME_OUT = 30;

    public static final String KEY_CREATIVE_EVENT = "com.xad.displaysdk.creativeevent";
    public static final String KEY_VAST_MODEL = "com.xad.displaysdk.vastmodel";
    public static final String KEY_ACCESS_KEY = "com.xad.displaysdk.accesskey";
    public static final String KEY_VIDEO_DATA = "com.xad.displaysdk.videodataurl";

    public static final long LOCATION_INTERVAL_MS = 10 * 1000;
    public static final long LOCATION_FASTEST_INTERVAL_MS = 5 * 1000;
    public static final float LOCATION_DISTANCE_FILTER = 3.0f;
    public static final boolean DISABLE_POPUP = false;
    public static final boolean OVERRIDE_JS_ALERT = true;

    private static final String[] supportedNativeFeaturesArrays =  {
            MRAIDNativeFeature.CALENDAR,
            MRAIDNativeFeature.INLINE_VIDEO,
            MRAIDNativeFeature.SMS,
            MRAIDNativeFeature.STORE_PICTURE,
            MRAIDNativeFeature.TEL,
    };

    public static final Set<String> supportedNativeFeatures = new HashSet<>(Arrays.asList(supportedNativeFeaturesArrays));
    public static final String MOAT_DISPLAY_PARTNER_CODE = "xaddisplay162341870938";
    public static final String MOAT_VIDEO_PARTNER_CODE = "xadvideo613478971360";
}
