package com.xad.sdk.sdkdemo;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.xad.sdk.AdType;
import com.xad.sdk.events.AdViewRequestEvent;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.Logger;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }
}