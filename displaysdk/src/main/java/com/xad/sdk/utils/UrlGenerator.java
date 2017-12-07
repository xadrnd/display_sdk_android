package com.xad.sdk.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import okhttp3.HttpUrl;

/**
 * Created by Ray.Wu on 4/25/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class UrlGenerator {
    private static final String TAG = "UrlGenerator";

    HttpUrl.Builder httpUrlBuilder;

    public UrlGenerator() {
        httpUrlBuilder = new HttpUrl.Builder();
    }

    public UrlGenerator (String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if(httpUrl == null) {
            throw new IllegalArgumentException("url is cant resolved");
        }
        this.httpUrlBuilder =  httpUrl.newBuilder();
    }

    public UrlGenerator withHost(String serverUrl, String pathSegments) {
        httpUrlBuilder.host(serverUrl);
        httpUrlBuilder.addPathSegments(pathSegments);
        return this;
    }

    public UrlGenerator withHost(String serverUrl, int port, String pathSegments) {
        httpUrlBuilder.host(serverUrl);
        httpUrlBuilder.port(port);
        httpUrlBuilder.addPathSegments(pathSegments);
        return this;
    }

    public UrlGenerator withHttps() {
        httpUrlBuilder.scheme("https");
        return this;
    }

    public UrlGenerator withHttp() {
        httpUrlBuilder.scheme("http");
        return this;
    }

    public HttpUrl generateUrl() {
        return httpUrlBuilder.build();
    }

    public String generateUrlString() {
        return generateUrl().toString();
    }

    public void addParam(@NonNull String key, @NonNull String value) {
        if(TextUtils.isEmpty(value)) {
            return;
        }
        httpUrlBuilder.addQueryParameter(key, value);
    }

    public void removeParam(@NonNull String key) {
        httpUrlBuilder.removeAllQueryParameters(key);
    }
}

