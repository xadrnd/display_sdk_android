package com.xad.sdk;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

/**
 * Created by xiaoguangwu on 4/4/16.
 */
public enum AdSize {
    BANNER(320, 50),
    NARROW_BANNER(300, 50),
    MEDIUM_RECTANGLE(300, 250),
    LEADER_BOARD(728, 90),

    SMALL_INTERSTITIAL_PORTRAIT(320, 480),
    SMALL_INTERSTITIAL_LANDSCAPE(480, 320),
    LARGE_INTERSTITIAL_PORTRAIT(768, 1024),
    LARGE_INTERSTITIAL_LANDSCAPE(1024, 768);

    final int height;
    final int width;

    AdSize(int width, int height) {
        this.height = height;
        this.width = width;
    }

    public static AdSize getInterstitialSize(Context context) {
        Point screenSize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(screenSize);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = screenSize.x / displayMetrics.density;
        float dpHeight = screenSize.y / displayMetrics.density;
        if(dpWidth < dpHeight) {
            if(dpWidth < 768) {
                return SMALL_INTERSTITIAL_PORTRAIT;
            } else {
                return LARGE_INTERSTITIAL_PORTRAIT;
            }
        } else {
            if(dpHeight < 768) {
                return SMALL_INTERSTITIAL_LANDSCAPE;
            } else {
                return LARGE_INTERSTITIAL_LANDSCAPE;
            }
        }
    }

    public static AdSize createAdSizeFromAdMobAdSize(com.google.android.gms.ads.AdSize adSize) {
        if(com.google.android.gms.ads.AdSize.BANNER.equals(adSize)) {
            return BANNER;
        }

        if(com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE.equals(adSize)) {
            return MEDIUM_RECTANGLE;
        }

        if(com.google.android.gms.ads.AdSize.LEADERBOARD.equals(adSize)){
            return LEADER_BOARD;
        }

        throw new IllegalArgumentException("This ad size(" + adSize.toString() +") is not supported by xAd display sdk, please contact xAd for more detail about ad size");
    }

    public int getWidthInPixels(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, metrics);
    }

    public int getHeightInPixels(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, metrics);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }


    @Override
    public String toString() {
        return width + "x" + height;
    }
}
