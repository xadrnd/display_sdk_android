package com.xad.sdk.events;

import com.xad.sdk.ErrorCode;

/**
 * Created by Ray.Wu on 5/6/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class ErrorEvent {
    public final ErrorCode Error;
    public final Object Requester;

    public ErrorEvent(Object requester, ErrorCode error) {
        this.Error = error;
        this.Requester = requester;
    }

    public ErrorEvent(ErrorCode error) {
        this.Error = error;
        this.Requester = null;
    }
}
