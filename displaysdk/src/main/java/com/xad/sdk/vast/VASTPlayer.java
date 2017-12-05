//
//  VastPlayer.java
//
//  Copyright (c) 2014 Nexage. All rights reserved.
//

package com.xad.sdk.vast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xad.sdk.events.CreativeEvent;
import com.xad.sdk.listeners.TestListener;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.ErrorPosting;
import com.xad.sdk.utils.Logger;
import com.xad.sdk.vast.activity.VASTActivity;
import com.xad.sdk.vast.model.VASTModel;
import com.xad.sdk.vast.processor.VASTMediaPicker;
import com.xad.sdk.vast.processor.VASTProcessor;
import com.xad.sdk.vast.util.DefaultMediaPicker;
import com.xad.sdk.vast.util.HttpTools;
import com.xad.sdk.vast.util.NetworkTools;

import java.util.List;

public class VASTPlayer {

    private static final String TAG = "VASTPlayer";

    // errors that can be returned in the vastError callback method of the
    // VASTPlayerListener
    public static final int ERROR_NONE                           = 0;
    public static final int ERROR_NO_NETWORK                     = 1;
    public static final int ERROR_XML_OPEN_OR_READ               = 2;
    public static final int ERROR_XML_PARSE                      = 3;
    public static final int ERROR_SCHEMA_VALIDATION              = 4;
    public static final int ERROR_NO_MEDIA_FILE_OR_NO_IMPRESSION = 5;
    public static final int ERROR_EXCEEDED_WRAPPER_LIMIT         = 6;
    public static final int ERROR_MEDIA_FILE_PLAYBACK            = 7;

    private Context mContext;

    private CreativeEvent creativeEvent; //For Error Report
    public String accessKey; //For Moat track

    public interface VASTPlayerListener {
        void vastReady();
        void vastError(int error);
        void vastClick();
        void vastStart();
        void vastComplete();
        void vastDismiss();
    }

    public static VASTPlayerListener listener;

    public static TestListener testListener;

    private VASTModel vastModel;

    public VASTPlayer(Context context, VASTPlayerListener listener) {
        this.mContext = context;
        VASTPlayer.listener = listener;
    }

    public void loadVideoWithData(final CreativeEvent creativeEvent) {
        this.creativeEvent = creativeEvent;
        Logger.logVerbose(TAG, "loadVideoWithData\n" + creativeEvent.CreativeString);
        vastModel = null;
        if (NetworkTools.connectedToInternet(mContext)) {
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    VASTMediaPicker mediaPicker = new DefaultMediaPicker(mContext);
                    VASTProcessor processor = new VASTProcessor(mediaPicker);
                    int error = processor.process(creativeEvent.CreativeString);
                    vastModel = processor.getModel();
                    if (error == ERROR_NONE && vastModel != null) {
                        sendReady();
                    } else {
                        if(error == ERROR_NO_MEDIA_FILE_OR_NO_IMPRESSION) {
                            processErrorEvent();
                        }
                        sendError(error);
                    }
                }
            })).start();
        }
        else {
            sendError(ERROR_NO_NETWORK);
        }
    }

    private void processErrorEvent() {
        List<String> errorUrls = vastModel.getErrorUrl();
        fireUrls(errorUrls);
    }

    private void fireUrls(List<String> urls) {
        Logger.logDebug(TAG, "entered fireUrls");
        if (urls != null) {
            for (String url : urls) {
                Logger.logVerbose(TAG, "\tfiring url:" + url);
                HttpTools.httpGetURL(url);
                VASTPlayer.testListener.interceptRequest(url);
            }
        }else {
            Logger.logDebug(TAG, "\turl list is null");
        }
    }

    public void play() {
        Logger.logDebug(TAG, "play");
        if (vastModel != null) {
            if (NetworkTools.connectedToInternet(mContext)) {
                Intent vastPlayerIntent = new Intent(mContext, VASTActivity.class);
                Bundle extras = new Bundle();
                extras.putSerializable(Constants.KEY_VAST_MODEL, vastModel);
                extras.putSerializable(Constants.KEY_CREATIVE_EVENT, this.creativeEvent);
                extras.putString(Constants.KEY_ACCESS_KEY, this.accessKey);
                vastPlayerIntent.putExtras(extras);
                mContext.startActivity(vastPlayerIntent);
            } else {
                sendError(ERROR_NO_NETWORK);
            }
        } else {
            Logger.logWarning(TAG, "VastModel is null; nothing to play");
        }
    }

    private void sendReady() {
        Logger.logDebug(TAG, "sendReady");
        if (listener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.vastReady();
                }
            });
        }
    }

    private void sendError(final int error) {
        Logger.logDebug(TAG, "sendError");
        switch (error) {
            case ERROR_XML_OPEN_OR_READ:
                ErrorPosting.sendError(mContext,
                        ErrorPosting.CONTENT_XML_CANNOT_OPEN_OR_READ_ERROR_TO_POST,
                        creativeEvent.CreativeString,
                        creativeEvent.adGroupId);
                break;
            case ERROR_XML_PARSE:
                ErrorPosting.sendError(mContext,
                        ErrorPosting.CONTENT_XML_CANNOT_PARSE_ERROR_TO_POST,
                        creativeEvent.CreativeString,
                        creativeEvent.adGroupId);
                break;
            case ERROR_NO_MEDIA_FILE_OR_NO_IMPRESSION:
                ErrorPosting.sendError(mContext,
                        ErrorPosting.CONTENT_VAST_NOT_VALID_ERROR_TO_POST,
                        creativeEvent.CreativeString,
                        creativeEvent.adGroupId);
                break;
            case ERROR_EXCEEDED_WRAPPER_LIMIT:
                ErrorPosting.sendError(mContext,
                        ErrorPosting.CONTENT_XML_EXCEEDED_WRAPPER_LIMIT_ERROR_TO_POST,
                        creativeEvent.CreativeString,
                        creativeEvent.adGroupId);
                break;
            default:
                ErrorPosting.sendError(mContext,
                        ErrorPosting.CONTENT_ERROR_TO_POST);
        }
        if (listener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.vastError(error);
                }
            });
        }
    }

    public void destroy() {
        this.mContext = null;
    }
}
