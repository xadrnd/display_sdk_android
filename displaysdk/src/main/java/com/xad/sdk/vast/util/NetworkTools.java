//
//  NetworkTools.java
//
//  Copyright (c) 2014 Nexage. All rights reserved.
//

package com.xad.sdk.vast.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.xad.sdk.utils.Logger;

public class NetworkTools {

    private static final String TAG = HttpTools.class.getName();

    // This method return true if it's connected to Internet
    public static boolean connectedToInternet(Context context) {
        Logger.logDebug(TAG, "Testing connectivity:");
        
        ConnectivityManager cm = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiNetwork = cm
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            Logger.logDebug(TAG, "Connected to Internet");
            return true;
        }

        NetworkInfo mobileNetwork = cm
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            Logger.logDebug(TAG, "Connected to Internet");
            return true;
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            Logger.logDebug(TAG, "Connected to Internet");
            return true;
        }
        Logger.logDebug(TAG, "No Internet connection");
        return false;
    }
}
