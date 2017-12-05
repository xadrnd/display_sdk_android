package com.xad.sdk.mraid.internal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import com.xad.sdk.mraid.MRAIDNativeFeature;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.Logger;

public class MRAIDNativeFeatureManager {
    
    private final static String TAG = "MRAIDNativeFeatureManager";
    
    public static boolean isCalendarSupported(Context context) {
        boolean retval = Constants.supportedNativeFeatures.contains(MRAIDNativeFeature.CALENDAR) &&
                        PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(Manifest.permission.WRITE_CALENDAR);
        Logger.logDebug(TAG, "isCalendarSupported " + retval);
        return retval;
    }

    public static boolean isInlineVideoSupported() {
        boolean retval = Constants.supportedNativeFeatures.contains(MRAIDNativeFeature.INLINE_VIDEO);
        Logger.logDebug(TAG, "isInlineVideoSupported " + retval);
        return retval;
    }

    public static boolean isSmsSupported(Context context) {
        boolean retval = Constants.supportedNativeFeatures.contains(MRAIDNativeFeature.SMS);
        Logger.logDebug(TAG, "isSmsSupported " + retval);
        return retval;
    }

    public static boolean isStorePictureSupported(Context context) {
        boolean retval = Constants.supportedNativeFeatures.contains(MRAIDNativeFeature.STORE_PICTURE) &&
                PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Logger.logDebug(TAG, "isStorePictureSupported " + retval);
        return retval;
    }
    
    public static boolean isTelSupported(Context context) {
        boolean retval = Constants.supportedNativeFeatures.contains(MRAIDNativeFeature.TEL);
        Logger.logDebug(TAG, "isTelSupported " + retval);
        return retval;
    }
}
