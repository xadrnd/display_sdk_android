package com.xad.sdk;

import com.xad.sdk.utils.Logger;

/**
 * Created by Ray.Wu on 5/17/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
interface IAdViewBase {
    void resume();
    void destroy();
    void pause();

    class AdViewUtils {
        static void registerOnBus(Object obj) {
            if (!DisplaySdk.sharedBus().isRegistered(obj)) {
                DisplaySdk.sharedBus().register(obj);
                Logger.logDebug("AdBase", "Register: " + obj + " on bus: " + DisplaySdk.sharedBus().toString());
            }
        }

        static void unregisterOnBus(Object obj) {
            if (DisplaySdk.sharedBus().isRegistered(obj)) {
                DisplaySdk.sharedBus().unregister(obj);
                Logger.logDebug("AdBase", "Unregister: " + obj + " on bus: " + DisplaySdk.sharedBus().toString());
            }
        }
    }
}
