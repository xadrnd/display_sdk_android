/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xad.sdk.listeners;

import com.xad.sdk.BannerView;
import com.xad.sdk.ErrorCode;

public interface BannerViewListener {
    //When banner ad is received
    void onAdLoaded(BannerView bannerView);
    //When banner ad is failed to load, e.g. no response for request, creative not well-formatted
    void onAdFetchFailed(BannerView bannerView, ErrorCode code);
    //When the user is about to return to the application after clicking on banner ad. Only available when banner ad is expanded
    void onAdClosed(BannerView bannerView);
    //When banner ad opens an overlay that covers the screen after user click on the banner ad. Expand or open in ChromeCustomTab
    void onAdOpened(BannerView bannerView);
    //When banner ad leaves the application (e.g., to call/sms).
    void onAdLeftApplication(BannerView bannerView);
}
