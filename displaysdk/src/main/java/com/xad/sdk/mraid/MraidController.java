package com.xad.sdk.mraid;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.xad.sdk.mraid.properties.ExpandProperties;
import com.xad.sdk.mraid.properties.JSONInvalidKeyException;
import com.xad.sdk.mraid.properties.OrientationProperties;
import com.xad.sdk.mraid.properties.Rect;
import com.xad.sdk.mraid.properties.ResizeProperties;
import com.xad.sdk.mraid.properties.Size;
import com.xad.sdk.utils.Logger;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Ray.Wu on 12/8/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class MraidController {
    public static String tag = "MraidController";
    public enum State {
        LOADING("loading"),
        DEFAULT("default"),
        EXPANDED("expanded"),
        RESIZED("resized"),
        HIDDEN("hidden");

        State(String value) {
            this.value = value;
        }

        @Nullable
        public static State fromString(String str) {
            for(State e : values()) {
                if(e.value.equalsIgnoreCase(str)) {
                    return e;
                }
            }
            return null;
        }

        public final String value;
    }

    public enum PlacementTypes {
        INLINE("inline"),
        INTERSTITIAL("interstitial");

        PlacementTypes(String value) {
            this.value = value;
        }

        @Nullable
        public static PlacementTypes fromString(String str) {
            for(PlacementTypes e : values()) {
                if(e.value.equalsIgnoreCase(str)) {
                    return e;
                }
            }
            return null;
        }

        public final String value;
    }

    public enum ResizePropertiesCustomClosePosition {
        TOP_LEFT("top-left"),
        TOP_CENTER("top-center"),
        TOP_RIGHT("top-right"),
        CENTER("center"),
        BOTTOM_LEFT("bottom-left"),
        BOTTOM_CENTER("bottom-center"),
        BOTTOM_RIGHT("bottom-right");

        ResizePropertiesCustomClosePosition(String value) {
            this.value = value;
        }

        @Nullable
        public static ResizePropertiesCustomClosePosition fromString(String str) {
            for(ResizePropertiesCustomClosePosition e : values()) {
                if(e.value.equalsIgnoreCase(str)) {
                    return e;
                }
            }
            return null;
        }

        public final String value;
    }

    public enum OrientationPropertiesForceOrientation {
        PORTRAIT("portrait"),
        LANDSCAPE("landscape"),
        NONE("none");

        OrientationPropertiesForceOrientation(String value) {
            this.value = value;
        }

        @Nullable
        public static OrientationPropertiesForceOrientation fromString(String str) {
            for(OrientationPropertiesForceOrientation e : values()) {
                if(e.value.equalsIgnoreCase(str)) {
                    return e;
                }
            }
            return null;
        }

        public final String value;
    }

    public enum Event {
        ERROR("error"),
        READY("ready"),
        SIZE_CHANGE("sizeChange"),
        STATE_CHANGE("stateChange"),
        VIEWABLE_CHANGE("viewableChange");

        Event(String value) {
            this.value = value;
        }

        @Nullable public static Event fromString(String str) {
            for(Event e : values()) {
                if(e.value.equalsIgnoreCase(str)) {
                    return e;
                }
            }
            return null;
        }

        public final String value;
    }

    public enum SupportedFeature {
        SMS("sms"),
        TEL("tel"),
        CALENDAR("calendar"),
        STOREPICTURE("storePicture"),
        INLINEVIDEO("inlineVideo");

        SupportedFeature(String value) {
            this.value = value;
        }

        @Nullable public static SupportedFeature fromString(String str) {
            for(SupportedFeature e : values()) {
                if(e.value.equalsIgnoreCase(str)) {
                    return e;
                }
            }
            return null;
        }

        public final String value;
    }

    MraidController(MRAIDView mraidView, Context context) {
        this.mContext = context;
        if(this.mContext instanceof Activity) {
            this.mActivityWeakRef = new WeakReference<>((Activity)this.mContext);
        }
        this.mraidView = mraidView;
        this.eventListeners = new HashMap<>();
        this.eventListeners.put(Event.ERROR, new ArrayList<String>());
        this.eventListeners.put(Event.READY, new ArrayList<String>());
        this.eventListeners.put(Event.SIZE_CHANGE, new ArrayList<String>());
        this.eventListeners.put(Event.STATE_CHANGE, new ArrayList<String>());
        this.eventListeners.put(Event.VIEWABLE_CHANGE, new ArrayList<String>());
    }

    final String VERSION = "2.0";
    @JavascriptInterface
    public String getVersion() {
        Logger.logVerbose(tag, "getVersion");
        return this.VERSION;
    }

    State state = State.LOADING;
    @JavascriptInterface
    public String getState() {
        Logger.logVerbose(tag, "getState");
        return this.state.value;
    }

    PlacementTypes placementTypes = PlacementTypes.INLINE;
    @JavascriptInterface
    public String getPlacementType() {
        Logger.logVerbose(tag, "getPlacementType");
        return this.placementTypes.value;
    }

    SupportedFeature[] supportedFeature = {};

    @JavascriptInterface
    public boolean supports(String feature) {
        for(SupportedFeature sf : this.supportedFeature) {
            if(sf.value.equals(feature)) {
                return true;
            }
        }
        return false;
    }

    boolean isViewable = false;
    @JavascriptInterface
    public boolean isViewable() {
        Logger.logVerbose(tag, "isViewable: " + this.isViewable);
        return this.isViewable;
    }

    boolean hasExpandProperties = false;
    boolean isResizeReady = false;

    ExpandProperties expandProperties = new ExpandProperties();

    @JavascriptInterface
    public String getExpandProperties() {
        Logger.logVerbose(tag, "getExpandProperties");
        try {
            return this.expandProperties.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @JavascriptInterface
    public void setExpandProperties(String propertiesString) {
        Logger.logVerbose(tag, "setExpandProperties");
        try {
            this.expandProperties = new ExpandProperties(propertiesString);
            this.mraidView.useCustomClose(this.expandProperties.useCustomClose);
        } catch (JSONException e) {
            Logger.logError(tag, "failed validation for setExpandProperties");
            e.printStackTrace();
        }
    }

    OrientationProperties orientationProperties = new OrientationProperties();
    @JavascriptInterface
    public String getOrientationProperties() {
        Logger.logVerbose(tag, "getOrientationProperties");
        try {
            return this.orientationProperties.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @JavascriptInterface
    public void setOrientationProperties(String orientationPropertiesString) {
        Logger.logVerbose(tag, "setOrientationProperties");
        try{
            OrientationProperties op = new OrientationProperties(orientationPropertiesString);
            if(op.allowOrientationChange && op.forceOrientation != OrientationPropertiesForceOrientation.NONE) {
                this.fireErrorEvent("allowOrientationChange is true but forceOrientation is " + op.forceOrientation.value, "setOrientationProperties");
                return;
            }
            this.orientationProperties = op;
            this.mraidView.setOrientationProperties(this.orientationProperties);
        } catch (JSONException e) {
            Logger.logError(tag, "failed validation for setOrientationProperties");
            e.printStackTrace();
        }
    }

    ResizeProperties resizeProperties = new ResizeProperties();
    @JavascriptInterface
    public String getResizeProperties() {
        Logger.logVerbose(tag, "getResizeProperties");
        try {
            return this.resizeProperties.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @JavascriptInterface
    public void setResizeProperties(String resizePropertiesString) {
        Logger.logVerbose(tag, "setResizeProperties");
        this.isResizeReady = false;

        try {
            ResizeProperties rp = new ResizeProperties(resizePropertiesString, this.resizeProperties);
            if (!rp.allowOffscreen) {
                if (rp.width > this.maxSize.width || rp.height > this.maxSize.height) {
                    this.fireErrorEvent("resize width or height is greater than the maxSize width or height", "setResizeProperties");
                    return;
                }
                rp.adjustmentForScreen(this.defaultPosition, this.maxSize);
            } else if(!rp.isCloseRegionOnScreen(this.defaultPosition, this.maxSize)) {
                fireErrorEvent("close event region will not appear entirely onscreen", "setResizeProperties");
                return;
            }

            this.resizeProperties = rp;
            this.mraidView.setResizeProperties(this.resizeProperties);
        } catch (JSONException e) {
            if(e instanceof JSONInvalidKeyException) {
                fireErrorEvent("failed validation for required property " + ((JSONInvalidKeyException)e).invalidKey, "setResizeProperties");
            } else {
                e.printStackTrace();
            }
        }
    }

    Rect defaultPosition = new Rect();
    @JavascriptInterface
    public String getCurrentPosition() {
        Logger.logVerbose(tag, "getCurrentPosition");
        try {
            return this.currentPosition.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    Rect currentPosition = new Rect();
    @JavascriptInterface
    public String getDefaultPosition() {
        Logger.logVerbose(tag, "getDefaultPosition");
        try {
            return this.defaultPosition.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    Size maxSize = new Size();
    @JavascriptInterface
    public String getMaxSize() {
        Logger.logVerbose(tag, "getMaxSize");
        try {
            return this.maxSize.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    Size screenSize = new Size();
    @JavascriptInterface
    public String getScreenSize() {
        Logger.logVerbose(tag, "getScreenSize");
        try {
            return this.screenSize.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    Map<Event, List<String>> eventListeners;

    //var currentOrientation = 0; (Not used now)

    MRAIDView mraidView;
    Context mContext;
    WeakReference<Activity> mActivityWeakRef;

    @JavascriptInterface
    public void addEventListener(String eventString, String javascriptListener) {
        Logger.logVerbose(tag, "addEventListener " + eventString + ": " + javascriptListener);
        Event event = Event.fromString(eventString);

        if(event == null) {
            fireErrorEvent("valid event is required for adding event listener", "addEventListener");
            return;
        }
        //prevent duplicate listener insert to the list
        List<String> listeners = this.eventListeners.get(event);
        for(String listener : listeners) {
            if(listener.equals(javascriptListener)) {
                Logger.logVerbose(tag, "Listener " + javascriptListener + " is already registered");
                return;
            }
        }

        if (TextUtils.isEmpty(javascriptListener)) {
            fireErrorEvent("No valid listener found", "addEventListener");
            return;
        }

        this.eventListeners.get(event).add(javascriptListener);
    }

    @JavascriptInterface
    public void removeEventListener(String eventString, String javascriptListener) {
        Logger.logVerbose(tag, "removeEventListener " + eventString + ": " + javascriptListener);
        Event event = Event.fromString(eventString);
        if(event == null) {
            fireErrorEvent("valid event is required for removing event listener", "removeEventListener");
            return;
        }

        if(this.eventListeners.containsKey(event)) {
            if(TextUtils.isEmpty(javascriptListener)) {
                //If empty string provided, remove all listeners for the event
                this.eventListeners.get(event).clear();
            } else {
                List<String> listeners = this.eventListeners.get(event);
                int indexToRemove = -1;
                for(int i=0; i < listeners.size(); i++) {
                    if(listeners.get(i).equals(javascriptListener)) {
                        indexToRemove = i;
                    }
                }
                if(indexToRemove == -1) {
                    Logger.logDebug(tag, "listener " + javascriptListener + " not found for event: " + eventString);
                }
                listeners.remove(indexToRemove);
            }
        }
    }

    @JavascriptInterface
    public void resize() {
        Logger.logVerbose(tag, "resize");
        if(this.placementTypes == PlacementTypes.INTERSTITIAL
                || this.state == State.LOADING
                || this.state == State.HIDDEN) {
            //Not in the right state to call resize
            return;
        }

        if(this.state == State.EXPANDED) {
            this.fireErrorEvent("mraid.resize called when ad is in expanded state", "resize");
            return;
        }

        if(!isResizeReady) {
            this.fireErrorEvent("mraid.resize is not ready to be called", "resize");
        }

        this.mraidView.resize();
    }

    @JavascriptInterface
    public void createCalendarEvent(String[] parameters) {
        //TODO
        //Not implemented since mraid drops this feature and only few creative are using this
        Logger.logInfo(tag, "creating calendar event with parameters: " + Arrays.toString(parameters));
    }

    @JavascriptInterface
    public void close() {
        Logger.logInfo(tag, "close");
        if(this.state == State.LOADING
                || (this.state == State.DEFAULT && this.placementTypes == PlacementTypes.INLINE)
                || this.state == State.HIDDEN) {
            return;
        }

        this.mraidView.close();
    }

    @JavascriptInterface
    public void expand(String url) {
        if(TextUtils.isEmpty(url)) {
            Logger.logInfo(tag, "expand (1-part)");
        } else {
            Logger.logInfo(tag, "expand " + url);
        }

        if(this.placementTypes != PlacementTypes.INLINE
                || (this.state != State.DEFAULT && this.state != State.RESIZED) ) {
            Logger.logWarning(tag, "expand fail, due to invalid state or placement type");
            return;
        }

        this.mraidView.expand(url);
    }

    @JavascriptInterface
    public void open(String url) {
        Logger.logVerbose(tag, "open " + url);
        this.mraidView.open(url);
    }

    @JavascriptInterface
    public void playVideo(String url) {
        Logger.logVerbose(tag, "open video " + url);
        this.mraidView.playVideo(url);
    }

    /***************************************************************************
     * methods not called by Javascript
     **************************************************************************/

    void setCurrentPosition(int x, int y, int width, int height) {
        Logger.logVerbose(tag, String.format(Locale.US, "setCurrentPosition, x: %d, y: %d width: %d, height: %d", x, y, width, height));
        Size previousSize = new Size();
        previousSize.width = this.currentPosition.width;
        previousSize.height = this.currentPosition.height;

        this.currentPosition.x = x;
        this.currentPosition.y = y;
        this.currentPosition.width = width;
        this.currentPosition.height = height;

        if(width != previousSize.width || height != previousSize.height) {
            this.fireSizeChangeEvent(width, height);
        }
    }

    void fireSizeChangeEvent(int width, int height) {
        Logger.logVerbose(tag, "fireSizeChangeEvent");
        //TODO
    }

    void fireReadyEvent() {
        Logger.logVerbose(tag, "fireReadyEvent");
        //TODO
    }

    void fireStateChangeEvent(State state) {
        Logger.logVerbose(tag, "fireStateChangeEvent");
        //TODO
    }

    void fireViewableChangeEvent(boolean newViewable) {
        Logger.logVerbose(tag, "fireViewableChangeEvent");
        //TODO
    }

    public void fireErrorEvent(String msg, String action) {
        Logger.logInfo(tag, "fireErrorEvent " + msg + " " + action);
        fireEvent(Event.ERROR, msg, action);
    }

    public void fireEvent(Event event, String... args) {
        Logger.logVerbose(tag, "fireEvent " + event.value + " " + Arrays.toString(args));
        if(!hasActivity()) {Logger.logError(tag, "no activity found");return;}

        List<String> listeners = this.eventListeners.get(event);
        int listenerCount = listeners.size();
        Logger.logDebug(tag, (listenerCount != 0 ? Integer.toString(listenerCount) : "no")  + " listener(s) found");
        for(String listener : listeners) {
            StringBuilder sb = new StringBuilder();
            sb.append("(").append(listener).append(").call(null");
            for(String arg : args) {
                sb.append(",\"").append(arg).append("\"");
            }
            sb.append(")");
            final String script = sb.toString();
            //TODO test if this will cause any problem, if it does, then try inject listener one by one
            this.mActivityWeakRef.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MraidController.this.mraidView.injectJavaScriptInCurrentWebView(script);
                }
            });
        }
    }

    //Helper methods
    private boolean hasActivity(){
        return this.mActivityWeakRef != null && this.mActivityWeakRef.get() != null;
    }

}
