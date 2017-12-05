package com.xad.sdk.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.xad.sdk.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Ray.Wu on 6/7/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class ErrorPosting {
    public static final int CONTENT_ERROR_TO_POST = 0;
    public static final int CONTENT_CANNOT_LOAD_ERROR_TO_POST = 1;
    public static final int CONTENT__EXPAND_URL_ERROR_TO_POST = 2;
    public static final int CONTENT_RESIZE_PROPERTIES_ERROR_TO_POST = 3;
    public static final int CONTENT_INJECT_JAVASCRIPT_ERROR_TO_POST = 4;
    public static final int CONTENT_PARSE_COMMAND_ERROR_TO_POST = 5;
    public static final int CONTENT_DECODING_ERROR_TO_POST = 6;
    public static final int CONTENT_VAST_NOT_VALID_ERROR_TO_POST = 7;
    public static final int CONTENT_VIDEO_PLAYBACK_ERROR_TO_POST = 8;
    public static final int CONTENT_VIDEO_DURATION_ERROR_TO_POST = 9;
    public static final int CONTENT_XML_CANNOT_PARSE_ERROR_TO_POST = 10;
    public static final int CONTENT_XML_CANNOT_OPEN_OR_READ_ERROR_TO_POST = 11;
    public static final int CONTENT_XML_EXCEEDED_WRAPPER_LIMIT_ERROR_TO_POST = 12;
    public static final int CONTENT_VIDEO_HANG_ERROR_TO_POST = 13;
    public static final int INTERNAL_ERROR_TO_POST = 14;
    public static final int SERVER_ERROR_TO_POST = 15;

    public static void sendError(Context context, int error, String payload, String adGroupId) {
        JSONObject jsonObject = new JSONObject();
        try {
            int labelStringId = context.getApplicationInfo().labelRes;
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            jsonObject.put("app", context.getString(labelStringId));
            jsonObject.put("appv", packageInfo.versionName);
            jsonObject.put("sdkv", BuildConfig.VERSION_NAME);
            jsonObject.put("os", "android");
            jsonObject.put("err", error);
            jsonObject.put("pay", payload);
            jsonObject.put("adgroup", adGroupId);
        } catch (PackageManager.NameNotFoundException | JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded") ,jsonObject.toString());
        Request request = new Request.Builder().url(Constants.ServerErrorEndpoint).post(body).build();
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(Constants.ERROR_REPORT_REQUEST_TIME_OUT, TimeUnit.SECONDS).build();
        Logger.logVerbose("ErrorPosting", "Request: " + request.toString() + "\nbody: " + jsonObject.toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.logError("ErrorPosting", "Failed to send error report");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logger.logVerbose("ErrorPosting", "Response: " + response.toString());
            }
        });
    }

    public static void sendError(Context context, int error, String payload) {
        sendError(context, error, payload, "0");
    }

    public static void sendError(Context context, int error) {
        sendError(context, error, "");
    }
}
