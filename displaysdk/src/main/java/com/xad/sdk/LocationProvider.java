package com.xad.sdk;

/**
 * Created by Ray.Wu on 2/20/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.xad.sdk.utils.Logger;

/**
 * Created by Ray.Wu on 4/4/16.
 */
class LocationProvider implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LocationProvider";
    private LocationListener mLocationListener;
    private Context mContext;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    LocationProvider(Context context, LocationListener listener) {
        this.mLocationListener = listener;
        this.mContext = context.getApplicationContext();
    }

    void initProvider(long interval,
                      long fastestInterval,
                      float smallestDisplacementMeters) {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(fastestInterval);
        mLocationRequest.setInterval(interval);
        mLocationRequest.setSmallestDisplacement(smallestDisplacementMeters);

        mGoogleApiClient.connect();
    }

    public void destroy(){
        if (this.googleApiClientIsValid()) {
            removeLocationUpdate();
            mGoogleApiClient.disconnect();
        } else {
            Logger.logDebug(TAG, "GoogleApiClient has been already disconnected");
        }
        mGoogleApiClient = null;
        mContext = null;
        mLocationListener = null;
    }

    private boolean locationPermissionGranted() {
        if(mContext == null) {
            Logger.logError(TAG, "No context found");
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else  {
            Logger.logError(TAG, "Location permission is disabled. Need explicitly ask user to grant location permission");
            return false;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Logger.logDebug(TAG, "Connected to GoogleApiClient, location service will start soon");
        requestLocationUpdate();
    }

    @SuppressWarnings({"MissingPermission"})
    @Nullable
    Location getLastLocation() {
        if(locationPermissionGranted() && this.googleApiClientIsValid()) {
            return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        return null;
    }

    void removeLocationUpdate() {
        if (this.googleApiClientIsValid()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
        }
        Logger.logDebug(TAG, "Location update is disabled.");
    }

    @SuppressWarnings({"MissingPermission"})
    void requestLocationUpdate() {
        if(locationPermissionGranted() && this.googleApiClientIsValid()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
        }

        Logger.logDebug(TAG, "Location update is enabled.");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Logger.logInfo(TAG, "Connection suspended with error code: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Logger.logError(TAG, "Fail to connect to google api client with code: " + result.getErrorCode());
    }

    private boolean googleApiClientIsValid() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            return true;
        } else  {
            Logger.logError(TAG, "Google Api Client is not available");
            return false;
        }
    }


}
