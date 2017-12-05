package com.xad.sdk.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by Ray.Wu on 8/15/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class CreativeEvent extends BaseEvent implements Serializable{
    private static final long serialVersionUID = -4621161488429270074L;

    public final boolean IsFromTestChannel;

    @NonNull public final String CreativeString;
    @Nullable public final String VendorId;
    @Nullable public final String CampaignId;
    @Nullable public final String adGroupId;
    //If creative is from test channel, only Creative Id is valid, all the other ids are null
    @Nullable public final String CreativeId;

    CreativeEvent() {
        super();
        IsFromTestChannel = false;
        CreativeString = "";
        VendorId = null;
        CampaignId = null;
        adGroupId = null;
        CreativeId = null;
    }

    public CreativeEvent(Object requester, @NonNull String creativeString, @Nullable String vendorId, @Nullable String campaignId, @Nullable String adGroupId, @Nullable String creativeId, boolean isFromTestChannel) {
        super(requester);
        this.IsFromTestChannel = isFromTestChannel;
        this.CreativeString = creativeString;
        this.VendorId = vendorId;
        this.CampaignId = campaignId;
        this.adGroupId = adGroupId;
        this.CreativeId = creativeId;
    }

    public CreativeEvent(Object requester, @NonNull String creativeString) {
        this(requester, creativeString, null, null, null, null, false);
    }

    public CreativeEvent(Object requester, long timestamp, @NonNull String creativeString) {
        this(requester, timestamp, creativeString, null, null, null, null, false);
    }

    public CreativeEvent(Object requester, long timestamp, @NonNull String creativeString, @Nullable String vendorId, @Nullable String campaignId, @Nullable String adGroupId, @Nullable String creativeId, boolean isFromTestChannel) {
        super(requester, timestamp);
        this.IsFromTestChannel = isFromTestChannel;
        this.CreativeString = creativeString;
        this.VendorId = vendorId;
        this.CampaignId = campaignId;
        this.adGroupId = adGroupId;
        this.CreativeId = creativeId;
    }

    @Override
    public BaseEvent clone(long timestamp) {
        return new CreativeEvent(this.Requester,
                timestamp,
                this.CreativeString);
    }
}
