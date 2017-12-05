package com.xad.sdk.mraid;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.xad.sdk.events.CreativeEvent;
import com.xad.sdk.listeners.TestListener;
import com.xad.sdk.mraid.internal.MRAIDHtmlProcessor;
import com.xad.sdk.mraid.internal.MRAIDNativeFeatureManager;
import com.xad.sdk.mraid.internal.MRAIDParser;
import com.xad.sdk.mraid.properties.MRAIDOrientationProperties;
import com.xad.sdk.mraid.properties.MRAIDResizeProperties;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.ErrorPosting;
import com.xad.sdk.utils.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class MRAIDView extends RelativeLayout {
    private interface JavascriptInjectionHandler{
        void beforeInjecting();
        void afterInjected();
    }

    private final static String TAG = "MRAIDView";

    public final static int STATE_LOADING  = 0;
    public final static int STATE_DEFAULT  = 1;
    public final static int STATE_EXPANDED = 2;
    public final static int STATE_RESIZED  = 3;
    public final static int STATE_HIDDEN   = 4;

    // in dip
    private final static int CLOSE_REGION_SIZE = 50;
       
    // UI elements
    private WebView webView;
    private WebView webViewPart2;
    private WebView currentWebView;
    private MRAIDWebChromeClient mraidWebChromeClient;
    private MRAIDWebViewClient mraidWebViewClient;
    private RelativeLayout expandedView;
    private RelativeLayout resizedView;
    private ImageButton closeRegion;

    private Context mContext;
    
    // gesture detector for capturing unwanted gestures
    private GestureDetector gestureDetector; 

    // state
    private final boolean isInterstitial;
    private int state;

    private String getStateString() {
        switch (state) {
            case STATE_LOADING:
                return "Loading";
            case STATE_DEFAULT:
            return "Default";
            case STATE_EXPANDED:
                return "Expanded";
            case STATE_RESIZED:
                return "Resized";
            case STATE_HIDDEN:
                return "Hidden";
        }
        return "";
    }

    public int getState() {
        return state;
    }
    
    private boolean isViewable;
    
    // The only property of the MRAID expandProperties we need to keep track of
    // on the native side is the useCustomClose property.
    // The width, height, and isModal properties are not used in MRAID v2.0.
    private boolean useCustomClose;
    private MRAIDOrientationProperties orientationProperties;
    private MRAIDResizeProperties resizeProperties;
    
    // listeners
    private MRAIDViewListener listener;
    private MRAIDNativeFeatureListener nativeFeatureListener;
    private TestListener mTestListener;

    // used for setting positions and sizes (all in pixels, not dpi)
    private DisplayMetrics displayMetrics;
    private int contentViewTop;
    private int statusHeight;
    private Rect currentPosition;
    private Rect defaultPosition;
    private final class Size {
        public int width;
        public int height;
    }
    private Size maxSize;
    private Size screenSize;
    // state to help set positions and sizes
    private boolean isPageFinished;
    private boolean isLaidOut;
    private boolean isForcingFullScreen;
    private boolean isExpandingFromDefault;
    private boolean isExpandingPart2;
    private boolean isClosing;

    // used to force full-screen mode on expand and to restore original state on close
    private View titleBar;
    private boolean isFullScreen;
    private boolean isForceNotFullScreen;
    private int origTitleBarVisibility;
    private boolean isActionBarShowing;
    private boolean isSupportActionBarShowing;

    // Stores the requested orientation for the Activity to which this MRAIDView belongs.
    // This is needed to restore the Activity's requested orientation in the event that 
    // the view itself requires an orientation lock.
    private final int originalRequestedOrientation;
       
    // This is the contents of mraid.js. We keep it around in case we need to inject it
    // into webViewPart2 (2nd part of 2-part expanded ad).
    private String mraidJs;

    private Handler handler;

    private CreativeEvent creativeEvent; //For error report

    /*********************************************************
    *    Constructor for BannerView
    **********************************************************/
    public MRAIDView(Context context, CreativeEvent creativeEvent, MRAIDViewListener listener, MRAIDNativeFeatureListener nativeFeatureListener) {
        this(context, creativeEvent, listener, nativeFeatureListener, false);
    }
    /*********************************************************
    *    Constructor for Interstitial
    **********************************************************/
    public MRAIDView(Context context, CreativeEvent creativeEvent, MRAIDViewListener listener, MRAIDNativeFeatureListener nativeFeatureListener, boolean isInterstitial) {
        super(context);

        this.creativeEvent = creativeEvent;
        this.mContext = context;
        this.isInterstitial = isInterstitial;

        state = STATE_LOADING;
        isViewable = false;
        useCustomClose = false;
        orientationProperties = new MRAIDOrientationProperties();
        resizeProperties = new MRAIDResizeProperties();

        this.listener = listener;
        this.nativeFeatureListener = nativeFeatureListener;
        
        displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        currentPosition = new Rect();
        defaultPosition = new Rect();
        maxSize = new Size();
        screenSize = new Size();
        
        if (context instanceof Activity) {
            originalRequestedOrientation = ((Activity) context).getRequestedOrientation();
        } else {
            originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
        Logger.logDebug(TAG, "originalRequestedOrientation " + getOrientationString(originalRequestedOrientation));
        
        // ignore scroll gestures
        gestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return true;
            }
        });
        
        handler = new Handler(Looper.getMainLooper());

        mraidWebChromeClient = new MRAIDWebChromeClient();
        mraidWebViewClient = new MRAIDWebViewClient();

        webView = createWebView();
        currentWebView = webView;
        String data = MRAIDHtmlProcessor.processRawHtml(creativeEvent.CreativeString);
        if(TextUtils.isEmpty(data)) {
            Logger.logError(TAG, "Ad HTML is invalid, cannot load");
            ErrorPosting.sendError(mContext, ErrorPosting.CONTENT_CANNOT_LOAD_ERROR_TO_POST ,creativeEvent.CreativeString, creativeEvent.adGroupId);
            if (this.listener != null) {
                this.listener.mraidViewFailed(this);
            }
            return;
        }
        final String dataCopy = data;
        injectMraidJS(webView, new JavascriptInjectionHandler() {
            @Override
            public void beforeInjecting() {
            }
            @Override
            public void afterInjected() {
//                injectJavaScript(webView, "mraid.logLevel = mraid.LogLevelEnum." + Logger.getLevel().value + ";");
                injectJavaScript(webView, "mraid.logLevel = mraid.LogLevelEnum.NONE;");
                webView.loadDataWithBaseURL(Constants.XAD_HOST, dataCopy, "text/html", "UTF-8", null);
            }
        });
        addView(webView);

    }

    public void setTestListener(TestListener testListener) {
        this.mTestListener = testListener;
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebView() {
        WebView wv = new WebView(mContext) {
            
            private static final String TAG = "MRAIDView-WebView";
            
            @SuppressWarnings("deprecation")
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                onLayoutWebView(this, changed, left, top, right, bottom);
            }

            @Override
            public void onConfigurationChanged(Configuration newConfig) {
                super.onConfigurationChanged(newConfig);
                Logger.logDebug(TAG, "onConfigurationChanged " + (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? "portrait" : "landscape"));
                if (isInterstitial) {
                    ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                }
            }
            
            @Override
            protected void onVisibilityChanged(View changedView, int visibility) {
                super.onVisibilityChanged(changedView, visibility);
                Logger.logDebug(TAG, "onVisibilityChanged " + getVisibilityString(visibility));
                if (isInterstitial) {
                    setViewable(visibility);
                }
            }

            @Override
            protected void onWindowVisibilityChanged(int visibility) {
                super.onWindowVisibilityChanged(visibility);
                int actualVisibility = getVisibility();
                Logger.logDebug(TAG, "onWindowVisibilityChanged " + getVisibilityString(visibility) +
                        " (actual " + getVisibilityString(actualVisibility) + ")");
                if (isInterstitial) {
                    setViewable(actualVisibility);
                }
                if (visibility != View.VISIBLE) {
                    pauseWebView(this);
                } else if(visibility == View.VISIBLE) {
                    resumeWebView(this);
                }
            }

            @Override
            protected void onSizeChanged(int w, int h, int ow, int oh) {
                Logger.logDebug(TAG, "W:" + w + ", H:" + h + ", OW:" + ow + ", OH:" + oh);
                super.onSizeChanged(w, h, ow, oh);
            }
        };
        
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        wv.setLayoutParams(params);

        wv.setScrollContainer(false);
        wv.setVerticalScrollBarEnabled(false);
        wv.setHorizontalScrollBarEnabled(false);
        wv.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        wv.setFocusableInTouchMode(false);
        wv.setOnTouchListener(new OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    // isTouched = true;
                    if (!v.hasFocus()) {
                        v.requestFocus();
                    }
                    break;
                }
                return false;
            }
        });
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setInitialScale(1);
        wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setLoadWithOverviewMode(true);
        wv.setWebChromeClient(mraidWebChromeClient);
        wv.setWebViewClient(mraidWebViewClient);

        if (Build.VERSION.SDK_INT >= 21) {
            wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

//TODO
//Used for debug mode. Don't delete
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        wv.getSettings().setMediaPlaybackRequiresUserGesture(false);

        if (Constants.OVERRIDE_JS_ALERT) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                wv.loadUrl("javascript:");
            }
            wv.evaluateJavascript("function alert(){}; function prompt(){}; function confirm(){}", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {

                }
            });
        }

        return wv;
    }
    
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            event.setAction(MotionEvent.ACTION_CANCEL);
        }
        return super.onTouchEvent(event);
    }

    public void clearView() {
        if (webView != null) {
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.loadUrl("about:blank");
        }
    }

    public void destroy() {
        if (webView != null) {
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        this.mContext = null;
        Logger.logDebug(TAG, "MRAID view " + this + " is destroyed");
    }

    /**************************************************************************
     * JavaScript --> native support
     * 
     * These methods are (indirectly) called by JavaScript code. They provide
     * the means for JavaScript code to talk to native code
     **************************************************************************/
    
    // This is the entry point to all the "actual" MRAID methods below.
    private void parseCommandUrl(String commandUrl) {
        Logger.logDebug(TAG, "parseCommandUrl " + commandUrl);
        
        MRAIDParser parser = new MRAIDParser();
        Map<String, String> commandMap = parser.parseCommandUrl(commandUrl);
        if(commandMap == null) {
            Logger.logWarning(TAG, "Can't parse command: " + commandUrl);
            ErrorPosting.sendError(mContext, ErrorPosting.CONTENT_PARSE_COMMAND_ERROR_TO_POST, creativeEvent.CreativeString, creativeEvent.adGroupId);
            return;
        }
        
        String command = commandMap.get("command");
        
        final String[] commandsWithNoParam = {
                "close",
                "resize",
        };
        
        final String[] commandsWithString = {
                "createCalendarEvent",
                "expand",
                "open",
                "playVideo",
                "storePicture",
                "useCustomClose",
        };

        final String[] commandsWithMap = {
                "setOrientationProperties",
                "setResizeProperties",
        };
        
        try {
            if (Arrays.asList(commandsWithNoParam).contains(command)) {
                Method method = getClass().getDeclaredMethod(command);
                method.invoke(this);
            } else if (Arrays.asList(commandsWithString).contains(command)) {
                Method method = getClass().getDeclaredMethod(command, String.class);
                String key;
                switch (command) {
                    case "createCalendarEvent":
                        key = "eventJSON";
                        break;
                    case "useCustomClose":
                        key = "useCustomClose";
                        break;
                    default:
                        key = "url";
                        break;
                }
                String val = commandMap.get(key);
                method.invoke(this, val);
            } else if (Arrays.asList(commandsWithMap).contains(command)) {
                Method method = getClass().getDeclaredMethod(command, Map.class);
                method.invoke(this, commandMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////
    // These are methods in the MRAID API.
    ///////////////////////////////////////////////////////
    
    private void close() {
        Logger.logDebug(TAG + "-JS callback", "close");
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state == STATE_LOADING || (state == STATE_DEFAULT && !isInterstitial) || state == STATE_HIDDEN) {
                    // do nothing
                    return;
                } else if (state == STATE_DEFAULT  || state == STATE_EXPANDED) {
                    closeFromExpanded();
                } else if (state == STATE_RESIZED) {
                    closeFromResized();
                }
            }
        });
    }

    @SuppressWarnings("unused")
    private void createCalendarEvent(String eventJSON) {
        Logger.logDebug(TAG + "-JS callback", "createCalendarEvent " + eventJSON);
        if (nativeFeatureListener != null) {
            nativeFeatureListener.mraidNativeFeatureCreateCalendarEvent(eventJSON);
        }
    }

    // Note: This method is also used to present an interstitial ad.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void expand(String url) {
        Logger.logDebug(TAG + "-JS callback", "expand " + (url != null ? url : "(1-part)"));
        
        // The only time it is valid to call expand on a banner ad is
        // when the ad is currently in either default or resized state.
        // The only time it is valid to (internally) call expand on an interstitial ad is
        // when the ad is currently in loading state.
        if ((isInterstitial && state != STATE_LOADING) || (!isInterstitial && state != STATE_DEFAULT && state != STATE_RESIZED)) {
            // do nothing
            return;
        }
        
        // 1-part expansion
        if (TextUtils.isEmpty(url)) {
            if (isInterstitial || state == STATE_DEFAULT) {
                if(webView.getParent()!=null) {
                    ((ViewGroup)webView.getParent()).removeView(webView);
                } else {
                    removeView(webView);
                }
            } else if (state == STATE_RESIZED) {
                removeResizeView();
            }
            expandHelper(webView);
            return;
        }

        // 2-part expansion
        
        // First, try to get the content of the second (expanded) part of the creative. 

        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }

        // Check to see whether we've been given an absolute or relative URL.
        // If it's relative, prepend the base URL.
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = Constants.XAD_HOST + url;
        }
        
        final String finalUrl = url;
        
        // Go onto a background thread to read the content from the URL.
        (new Thread(new Runnable() {
            @Override
            public void run() {
                final String content = getStringFromUrl(finalUrl);
                if (!TextUtils.isEmpty(content)) {
                    // Get back onto the main thread to create and load a new WebView.
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (state == STATE_RESIZED) {
                                removeResizeView();
                                addView(webView);
                            }
                            webView.setWebChromeClient(null);
                            webView.setWebViewClient(null);
                            webViewPart2 = createWebView();
                            injectMraidJS(webViewPart2, new JavascriptInjectionHandler() {
                                @Override
                                public void beforeInjecting() {
                                }

                                @Override
                                public void afterInjected() {
                                    injectJavaScript(webView, "mraid.logLevel = mraid.LogLevelEnum." + Logger.getLevel().value + ";");
                                    webViewPart2.loadDataWithBaseURL(Constants.XAD_HOST, content, "text/html", "UTF-8", null);
                                }
                            });
                            currentWebView = webViewPart2;
                            isExpandingPart2 = true;
                            expandHelper(currentWebView);
                        }
                    });
                } else {
                    Logger.logError(TAG, "Could not load part 2 expanded content for URL: " + finalUrl);
                    ErrorPosting.sendError(mContext, ErrorPosting.CONTENT__EXPAND_URL_ERROR_TO_POST, creativeEvent.CreativeString, creativeEvent.adGroupId);
                }
            }
        }, "2-part-content")).start();
    }
    
    private void open(String url) {
        try {
            url = URLDecoder.decode(url,"UTF-8");
            Logger.logDebug(TAG + "-JS callback", "open " + url);
            if(nativeFeatureListener != null) {
                if (url.startsWith("sms:")) {
                    nativeFeatureListener.mraidNativeFeatureSendSms(url);
                } else if (url.startsWith("tel:")) {
                    nativeFeatureListener.mraidNativeFeatureCallTel(url);
                } else {
                    nativeFeatureListener.mraidNativeFeatureOpenBrowser(url);
                }
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void playVideo(String url) {
        try {
            url = URLDecoder.decode(url,"UTF-8");
            Logger.logDebug(TAG + "-JS callback", "playVideo " + url);
            if (nativeFeatureListener != null) {
                nativeFeatureListener.mraidNativeFeaturePlayVideo(url);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void resize() {
        Logger.logDebug(TAG + "-JS callback", "resize");
        
        // We need the cooperation of the app in order to do a resize. 
        if (listener == null) {
            return;
        }
        boolean isResizeOK = listener.mraidViewResize(this,
                resizeProperties.width, resizeProperties.height, resizeProperties.offsetX, resizeProperties.offsetY);
        if (!isResizeOK) {
            return;
        }

        state = STATE_RESIZED;

        if (resizedView == null) {
            resizedView = new RelativeLayout(mContext);
            removeAllViews();
            resizedView.addView(webView);
            addCloseRegion(resizedView);
            FrameLayout rootView = (FrameLayout)getRootView().findViewById(android.R.id.content);
            rootView.addView(resizedView);
        }
        setCloseRegionPosition(resizedView);
        setResizedViewSize();
        setResizedViewPosition();

        handler.post(new Runnable() {
            @Override
            public void run() {
                fireStateChangeEvent();
            }
        });
    }

    @SuppressWarnings("unused")
    private void setOrientationProperties(Map<String, String> properties) {
        boolean allowOrientationChange = Boolean.parseBoolean(properties.get("allowOrientationChange"));
        String forceOrientation = properties.get("forceOrientation");
        Logger.logDebug(TAG + "-JS callback", "setOrientationProperties "
                + allowOrientationChange + " " + forceOrientation);
        if (orientationProperties.allowOrientationChange != allowOrientationChange ||
                orientationProperties.forceOrientation !=
                MRAIDOrientationProperties.forceOrientationFromString(forceOrientation)) {
            orientationProperties.allowOrientationChange = allowOrientationChange;
            orientationProperties.forceOrientation =
                    MRAIDOrientationProperties.forceOrientationFromString(forceOrientation);
            if (isInterstitial || state == STATE_EXPANDED) {
                applyOrientationProperties();
            }
        }
    }

    @SuppressWarnings("unused")
    private void setResizeProperties(Map<String, String> properties) {
        int width = 0;
        int height = 0;
        int offsetX = 0;
        int offsetY = 0;
        try {
            width = Integer.parseInt(properties.get("width"));
            height = Integer.parseInt(properties.get("height"));
            offsetX = Integer.parseInt(properties.get("offsetX"));
            offsetY = Integer.parseInt(properties.get("offsetY"));
        } catch (NumberFormatException e) {
            Logger.logError(TAG, "Resize properties is not correct");
            ErrorPosting.sendError(mContext, ErrorPosting.CONTENT_RESIZE_PROPERTIES_ERROR_TO_POST, creativeEvent.CreativeString, creativeEvent.adGroupId);
        }
        String customClosePosition = properties.get("customClosePosition");
        boolean allowOffscreen = Boolean.parseBoolean(properties.get("allowOffscreen"));
        Logger.logDebug(TAG + "-JS callback", "setResizeProperties "
                + width + " " + height + " "
                + offsetX + " " + offsetY + " "
                + customClosePosition + " " + allowOffscreen);
        resizeProperties.width = width;
        resizeProperties.height = height;
        resizeProperties.offsetX = offsetX;
        resizeProperties.offsetY = offsetY;
        resizeProperties.customClosePosition =
                MRAIDResizeProperties.customClosePositionFromString(customClosePosition);
        resizeProperties.allowOffscreen = allowOffscreen;
    }

    @SuppressWarnings("unused")
    private void storePicture(String url) {
        try {
            url = URLDecoder.decode(url,"UTF-8");
            Logger.logDebug(TAG + "-JS callback", "storePicture " + url);
            if (nativeFeatureListener != null) {
                nativeFeatureListener.mraidNativeFeatureStorePicture(url);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void useCustomClose(String useCustomCloseString) {
        Logger.logDebug(TAG + "-JS callback", "useCustomClose " + useCustomCloseString);
        boolean useCustomClose = Boolean.parseBoolean(useCustomCloseString);
        if (this.useCustomClose != useCustomClose) {
            this.useCustomClose = useCustomClose;
            if (useCustomClose) {
                removeDefaultCloseButton();
            } else {
                showDefaultCloseButton(closeRegion);
            }
        }
    }

    /**************************************************************************
     * JavaScript --> native support helpers
     * 
     * These methods are helper methods for the ones above.
     **************************************************************************/
    
    private String getStringFromUrl(String url) {
        
        // Support second part from file system - mostly not used on real web creative
        if (url.startsWith("file:///")) {
            return getStringFromFileUrl(url);
        }

        String content = null;
        InputStream is = null;
        try {
            HttpURLConnection conn = (HttpURLConnection)(new URL(url)).openConnection();
            int responseCode = conn.getResponseCode();
            Logger.logDebug(TAG, "response code " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Logger.logDebug(TAG, "getContentLength " + conn.getContentLength());
                is = conn.getInputStream();
                byte[] buf = new byte[1500];
                int count;
                StringBuilder sb = new StringBuilder();
                while ((count = is.read(buf)) != -1) {
                    String data = new String(buf, 0, count);
                    sb.append(data);
                }
                content = sb.toString();
                Logger.logDebug(TAG, "getStringFromUrl ok, length=" + content.length());
            }
            conn.disconnect();
        } catch (IOException e) {
            Logger.logError(TAG, "getStringFromUrl failed " + e.getLocalizedMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        return content;
    }
    
    private String getStringFromFileUrl(String fileURL) {

        StringBuilder mLine = new StringBuilder("");
        String[] urlElements = fileURL.split("/");
        if (urlElements[3].equals("android_asset")) {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(mContext.getAssets().open(urlElements[4])));

                // do reading, usually loop until end of file reading
                String line = reader.readLine();
                mLine.append(line);
                while (line != null) {
                    line = reader.readLine();
                    mLine.append(line); 
                }

                reader.close();
            } catch (IOException e) {
                Logger.logError(TAG,"Error fetching file: " + e.getMessage());
            }
            
            return mLine.toString();
        } else {
            Logger.logError(TAG,"Unknown location to fetch file content");
        }
        
        return "";
    }
    
    public void showAsInterstitial() {
        expand(null);
    }

    private void expandHelper(WebView webView) {
        if (!isInterstitial) {
            state = STATE_EXPANDED;
        }
        // If this MRAIDView is an interstitial, we'll set the state to default and
        // fire the state change event after the view has been laid out.
        applyOrientationProperties();
        forceFullScreen();
        expandedView = new RelativeLayout(mContext);
        expandedView.addView(webView);
        addCloseRegion(expandedView);
        setCloseRegionPosition(expandedView);
        ((Activity) mContext).addContentView(expandedView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        isExpandingFromDefault = true;
        fireStateChangeEvent();
        if(isInterstitial) {
            isLaidOut = true;
            state = STATE_DEFAULT;
            fireReadyEvent();
            this.fireStateChangeEvent();
        }
    }
    
    private void setResizedViewSize() {
        Logger.logDebug(TAG, "setResizedViewSize");
        int widthInDip = resizeProperties.width;
        int heightInDip = resizeProperties.height;
        Log.d(TAG, "setResizedViewSize " + widthInDip + "x" + heightInDip);
        int width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthInDip, displayMetrics);
        int height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightInDip, displayMetrics);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        resizedView.setLayoutParams(params);
    }

    private void setResizedViewPosition() {
        Logger.logDebug(TAG, "setResizedViewPosition");
        // resizedView could be null if it has been closed.
        if (resizedView == null) {
            return;
        }
        int widthInDip = resizeProperties.width;
        int heightInDip = resizeProperties.height;
        int offsetXInDip = resizeProperties.offsetX;
        int offsetYInDip = resizeProperties.offsetY;
        int width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthInDip, displayMetrics);
        int height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightInDip, displayMetrics);
        int offsetX = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, offsetXInDip, displayMetrics);
        int offsetY = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, offsetYInDip, displayMetrics);
        int x = defaultPosition.left + offsetX;
        int y = defaultPosition.top + offsetY;
        if(!resizeProperties.allowOffscreen) {
            if(x + width > screenSize.width) {
                x = screenSize.width - width;
            }
            if(y + height > screenSize.height) {
                y = screenSize.height - height;
            }
            if (x < 0 || y < 0) {
                Logger.logDebug(TAG, "Resize view position or size can't be satisfied");
            }
            x = x>0? x: 0;
            y = y>0? y: 0;
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)resizedView.getLayoutParams();
        params.leftMargin = x;
        params.topMargin = y;
        resizedView.setLayoutParams(params);
        if (x != currentPosition.left || y != currentPosition.top || width != currentPosition.width() || height != currentPosition.height()) {
            currentPosition.left = x;
            currentPosition.top = y;
            currentPosition.right = x + width;
            currentPosition.bottom = y + height;
            setCurrentPosition();
        }
    }
    
    private void closeFromExpanded() {
        if (state == STATE_DEFAULT && isInterstitial) {
            state = STATE_HIDDEN;
            clearView();
        } else if (state == STATE_EXPANDED || state == STATE_RESIZED) {
            state = STATE_DEFAULT;
        }
        isClosing = true;

        expandedView.removeAllViews();
        
        FrameLayout rootView = (FrameLayout)((Activity) mContext).findViewById(android.R.id.content);
        rootView.removeView(expandedView);
        expandedView = null;
        closeRegion = null;
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                restoreOriginalOrientation();
                restoreOriginalScreenState();
            }
        });
        if (webViewPart2 == null) {
            // close from 1-part expansion
            addView(webView);
        } else {
            // close from 2-part expansion
            webViewPart2.setWebChromeClient(null);
            webViewPart2.setWebViewClient(null);
            webViewPart2.destroy();
            webViewPart2 = null;
            webView.setWebChromeClient(mraidWebChromeClient);
            webView.setWebViewClient(mraidWebViewClient);
            currentWebView = webView;
        }
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                fireStateChangeEvent();
                if (listener != null) {
                    listener.mraidViewClose(MRAIDView.this);
                }
            }
        });
    }
    
    private void closeFromResized() {
        state = STATE_DEFAULT;
        isClosing = true;
        removeResizeView();
        addView(webView);
        handler.post(new Runnable() {
            @Override
            public void run() {
                fireStateChangeEvent();
                if (listener != null) {
                    listener.mraidViewClose(MRAIDView.this);
                }
            }
        });
    }
    
    private void removeResizeView() {
        resizedView.removeAllViews();
        FrameLayout rootView = (FrameLayout)((Activity) mContext).findViewById(android.R.id.content);
        rootView.removeView(resizedView);
        resizedView = null;
        closeRegion = null;
    }

    private void forceFullScreen() {
        Logger.logDebug(TAG, "forceFullScreen");
        Activity activity = (Activity) mContext;
        
        // store away the original state
        int flags = activity.getWindow().getAttributes().flags;
        isFullScreen = ((flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0);
        isForceNotFullScreen = ((flags & WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN) != 0);
        origTitleBarVisibility = -9;

        // First, see if the activity has an action bar.
        boolean hasActionBar = false;
        boolean hasSupportActionBar = false;
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            hasActionBar = true;
            isActionBarShowing = actionBar.isShowing();
            actionBar.hide();
        }
        //Check if is ActionBar in support library
        //Added by Ray.Wu
        if(activity instanceof AppCompatActivity) {
            android.support.v7.app.ActionBar supportActionBar = ((AppCompatActivity)activity).getSupportActionBar();
            if(supportActionBar != null) {
                hasSupportActionBar = true;
                isSupportActionBarShowing = supportActionBar.isShowing();
                supportActionBar.hide();
            }
        }

        // If not, see if the app has a title bar
        if (!hasActionBar && !hasSupportActionBar) {
            // http://stackoverflow.com/questions/6872376/how-to-hide-the-title-bar-through-code-in-android
            titleBar = null;
            try {
                titleBar = (View)activity.findViewById(android.R.id.title).getParent();
            } catch (NullPointerException npe) {
                // do nothing
            }
            if (titleBar != null) {
                origTitleBarVisibility = titleBar.getVisibility();
                titleBar.setVisibility(View.GONE);
            }
        }

        Logger.logDebug(TAG, "isFullScreen " + isFullScreen);
        Logger.logDebug(TAG, "isForceNotFullScreen " + isForceNotFullScreen);
        Logger.logDebug(TAG, "isActionBarShowing " + isActionBarShowing);
        Logger.logDebug(TAG, "isSupportActionBarShowing " + isSupportActionBarShowing);
        Logger.logDebug(TAG, "origTitleBarVisibility " + getVisibilityString(origTitleBarVisibility));

        // force fullscreen mode
        ((Activity) mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ((Activity) mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        
        isForcingFullScreen = !isFullScreen;
    }
    
    private void restoreOriginalScreenState() {
        Activity activity = (Activity) mContext;
        if (!isFullScreen) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (isForceNotFullScreen) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
        if (isActionBarShowing) {
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
        if(isSupportActionBarShowing) {
            android.support.v7.app.ActionBar supportActionBar = ((AppCompatActivity)activity).getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.show();
            }
        }
    }
    
    private static String getVisibilityString(int visibility) {
        switch (visibility) {
            case View.GONE: return "GONE";
            case View.INVISIBLE: return "INVISIBLE";
            case View.VISIBLE: return "VISIBLE";
            default: return "UNKNOWN";
        }
    }
    
    private void addCloseRegion(View view) {
        // The input parameter should be either expandedView or resizedView.

        closeRegion = new ImageButton(mContext);
        closeRegion.setBackgroundColor(Color.TRANSPARENT);
        closeRegion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });

        // The default close button is shown only on expanded banners and interstitials,
        // but not on resized banners.
        if (view == expandedView && !useCustomClose) {
            showDefaultCloseButton(closeRegion);
        }

        ((ViewGroup) view).addView(closeRegion);
    }
    
    private void showDefaultCloseButton(ImageButton closeButton) {
        if (closeButton != null) {
            Drawable closeButtonNormalDrawable = Assets.getDrawableFromBase64(getResources(), Assets.new_close);
            Drawable closeButtonPressedDrawable = Assets.getDrawableFromBase64(getResources(), Assets.new_close_pressed);

            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] { -android.R.attr.state_pressed }, closeButtonNormalDrawable);
            states.addState(new int[] { android.R.attr.state_pressed }, closeButtonPressedDrawable);

            closeButton.setImageDrawable(states);
            closeButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }
    
    private void removeDefaultCloseButton() {
        if (closeRegion != null) {
            closeRegion.setImageResource(android.R.color.transparent);
        }
    }
    
    private void setCloseRegionPosition(View view) {
        // The input parameter should be either expandedView or resizedView.
        
        int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLOSE_REGION_SIZE, displayMetrics);
        LayoutParams params = new LayoutParams(size, size);

        // The close region on expanded banners and interstitials is always in the top right corner.
        // Its position on resized banners is determined by the customClosePosition property of the
        // resizeProperties.
        if (view == expandedView) {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else if (view == resizedView) {

            switch (resizeProperties.customClosePosition) {
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_TOP_LEFT:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_BOTTOM_LEFT:
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                break;
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_TOP_CENTER:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_CENTER:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_BOTTOM_CENTER:
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                break;
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_TOP_RIGHT:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_BOTTOM_RIGHT:
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                break;
            }

            switch (resizeProperties.customClosePosition) {
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_TOP_LEFT:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_TOP_CENTER:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_TOP_RIGHT:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                break;
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_CENTER:
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                break;
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_BOTTOM_LEFT:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_BOTTOM_CENTER:
            case MRAIDResizeProperties.CUSTOM_CLOSE_POSITION_BOTTOM_RIGHT:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            }
        }
        
        closeRegion.setLayoutParams(params);
    }

    /**************************************************************************
     * native --> JavaScript support
     * 
     * These methods provide the means for JavaScript code to talk to native
     * code.
     **************************************************************************/

    private void injectMraidJs(final WebView wv) {
        injectMraidJS(wv, null);
    }

    private void injectMraidJS(final WebView webView, JavascriptInjectionHandler handler) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            webView.loadUrl("javascript:");
        }

        injectJavaScript(webView, getMraidJs(), handler);
    }

    private String getMraidJs() {
        if (TextUtils.isEmpty(mraidJs)) {
            try {
                mraidJs = Assets.readFromAssets(mContext, "mraid.js");//read mraid.js from assets directory
                Logger.logDebug(TAG, "Found mraid.js, length: " + mraidJs.length());
            } catch (IOException e) {
                Logger.logError(TAG, "Can't find mraid.js");
                e.printStackTrace();
            }
        }
        return mraidJs;
    }
    
    private void injectJavaScriptInCurrentWebView(String js) {
        injectJavaScript(currentWebView, js);
    }
    
    private void injectJavaScript(@Nullable WebView webView, String js) {
        if(webView == null) {
            Logger.logDebug(TAG, "WebView is destroyed, while MRAIDView is not");
            return;
        }
        Logger.logDebug(TAG, "evaluating js: "+js);
        injectJavaScript(webView, js, null);
    }

    private void injectJavaScript(@Nullable WebView webView, String js, final JavascriptInjectionHandler handler) {
        if(webView == null) {
            Logger.logDebug(TAG, "WebView is destroyed, while MRAIDView is not");
            return;
        }
        if(handler != null) {
            handler.beforeInjecting();
        }
        if (!TextUtils.isEmpty(js)) {
            webView.evaluateJavascript(js, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if(handler != null) {
                        handler.afterInjected();
                    }
                }
            });
        }
    }
    
    // convenience methods    
    private void fireReadyEvent() {
        Logger.logDebug(TAG, "fireReadyEvent");
        injectJavaScriptInCurrentWebView("mraid.fireReadyEvent();");
    }

    // We don't need to explicitly call fireSizeChangeEvent because it's taken care
    // of for us in the mraid.setCurrentPosition method in mraid.js.
    
    @SuppressLint("DefaultLocale")
    private void fireStateChangeEvent() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Logger.logDebug(TAG, "fireStateChangeEvent - " + stackTraceElements[3].getMethodName());
        String[] stateArray = { "loading", "default", "expanded", "resized", "hidden" };
        injectJavaScriptInCurrentWebView("mraid.fireStateChangeEvent('" + stateArray[state] + "');");
    }

    private void fireViewableChangeEvent() {
        Logger.logDebug(TAG, "fireViewableChangeEvent");
        injectJavaScriptInCurrentWebView("mraid.fireViewableChangeEvent(" + isViewable + ");");
    }
    
    private int px2dip(int pixels) {
        return pixels * DisplayMetrics.DENSITY_DEFAULT / displayMetrics.densityDpi;
        // return pixels;
    }

    private void setCurrentPosition() {
        int x = currentPosition.left;
        int y = currentPosition.top;
        int width = currentPosition.width();
        int height = currentPosition.height();
        Logger.logDebug(TAG, "setCurrentPosition [" + x + "," + y + "] (" + width + "x" + height + ")");
        injectJavaScriptInCurrentWebView("mraid.setCurrentPosition(" + px2dip(x) + "," + px2dip(y) + "," + px2dip(width) + "," + px2dip(height) + ");");
    }
    
    private void setDefaultPosition() {
        int x = defaultPosition.left;
        int y = defaultPosition.top;
        int width = defaultPosition.width();
        int height = defaultPosition.height();
        Logger.logDebug(TAG, "setDefaultPosition [" + x + "," + y + "] (" + width + "x" + height + ")");
        injectJavaScriptInCurrentWebView("mraid.setDefaultPosition(" + px2dip(x) + "," + px2dip(y) + "," + px2dip(width) + "," + px2dip(height) + ");");
    }
    
    private void setMaxSize() {
        Logger.logDebug(TAG, "setMaxSize");
        int width = maxSize.width;
        int height = maxSize.height;
        Logger.logDebug(TAG, "setMaxSize " + width + "x" + height);
        injectJavaScriptInCurrentWebView("mraid.setMaxSize(" + px2dip(width) + "," + px2dip(height) + ");");
    }
    
    private void setScreenSize() {
        Logger.logDebug(TAG, "setScreenSize");
        int width = screenSize.width;
        int height = screenSize.height;
        Logger.logDebug(TAG, "setScreenSize " + width + "x" + height);
        injectJavaScriptInCurrentWebView("mraid.setScreenSize(" + px2dip(width) + "," + px2dip(height) + ");");
    }
    
    private void setSupportedServices() {
        Logger.logDebug(TAG, "setSupportedServices");
        injectJavaScriptInCurrentWebView("mraid.setSupports(mraid.SUPPORTED_FEATURES.CALENDAR, " + MRAIDNativeFeatureManager.isCalendarSupported(this.mContext) + ");");
        injectJavaScriptInCurrentWebView("mraid.setSupports(mraid.SUPPORTED_FEATURES.INLINEVIDEO, " + MRAIDNativeFeatureManager.isInlineVideoSupported() + ");");
        injectJavaScriptInCurrentWebView("mraid.setSupports(mraid.SUPPORTED_FEATURES.SMS, " + MRAIDNativeFeatureManager.isSmsSupported(this.mContext) + ");");
        injectJavaScriptInCurrentWebView("mraid.setSupports(mraid.SUPPORTED_FEATURES.STOREPICTURE, " + MRAIDNativeFeatureManager.isStorePictureSupported(this.mContext) + ");");
        injectJavaScriptInCurrentWebView("mraid.setSupports(mraid.SUPPORTED_FEATURES.TEL, " + MRAIDNativeFeatureManager.isTelSupported(this.mContext) + ");");
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void pauseWebView(WebView webView) {
        Logger.logDebug(TAG, "pauseWebView " + webView.toString());
        // Stop any video/animation that may be running in the WebView.
        // Otherwise, it will keep playing in the background.
        webView.onPause();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void resumeWebView(WebView webView) {
        Logger.logDebug(TAG, "resumeWebView " + webView.toString());
        webView.onResume();
    }

    /**************************************************************************
     * WebChromeClient and WebViewClient
     **************************************************************************/

    private class MRAIDWebChromeClient extends WebChromeClient {

        @Override
        public boolean onConsoleMessage(ConsoleMessage cm) {
            if(cm==null || cm.message()==null) {
                return false;
            }
//            if (!cm.message().contains("Uncaught ReferenceError")) {
            Logger.logDebug("JS console", cm.message()
                    + (cm.sourceId() == null ? "" : " at " + cm.sourceId())
                    + ":" + cm.lineNumber());
//            }
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Logger.logDebug("JS alert", message);
            return handlePopups(result);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            Logger.logDebug("JS confirm", message);
            return handlePopups(result);
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message,    String defaultValue, JsPromptResult result) {
            Logger.logDebug("JS prompt", message);
            return handlePopups(result);
        }

        private boolean handlePopups(JsResult result) {
            return Constants.DISABLE_POPUP;
        }
        
    }

    private class MRAIDWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            Logger.logDebug(TAG, "onPageFinished: " + url);
            super.onPageFinished(view, url);
            handleWebViewStartRendering(view);
        }

        /*
        //onPageFinished only means webview start render web pages, doesn't mean 'ready' states
        @Override
        public void onPageFinished(WebView view, String url) {
            if(mTestListener == null) {
                Logger.logDebug(TAG, "onPageFinished: " + url);
                super.onPageFinished(view, url);
                handleWebViewStartRendering(view);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if(mTestListener != null) {
                Logger.logDebug(TAG, "onPageStarted: " + url);
                super.onPageStarted(view, url, favicon);
                handleWebViewStartRendering(view);
            }
        }
        */

        @RequiresApi(24)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if(Build.VERSION.SDK_INT >= 23) {
                Logger.logDebug(TAG, "onReceivedError: " + error.getDescription() + " with request: " + request.toString());
                super.onReceivedError(view, request, error);
            }
        }

        @TargetApi(23)
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if(Build.VERSION.SDK_INT < 23) {
                Logger.logDebug(TAG, "onReceivedError: " + description + " with failingUrl: " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Logger.logError(TAG, "Http error: " + error.toString());
            super.onReceivedSslError(view, handler, error);
        }

        @SuppressWarnings("deprecation")
        @TargetApi(23)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(Build.VERSION.SDK_INT >= 24) {
                //API >= 24/N: use "shouldOverrideUrlLoading(WebView view, WebResourceRequest request)"
                return false;
            }

            Logger.logDebug(TAG, "shouldOverrideUrlLoading: " + url);
            if(MRAIDView.this.mTestListener != null) {
                if(MRAIDView.this.mTestListener.interceptRequest(url)) {
                    return true;
                }
            }
            if (url.startsWith("mraid://")) {
                parseCommandUrl(url);
            } else if(url.startsWith("console.log")) {
                Logger.logDebug(TAG, "JS-Console: " + url.substring(14));
            } else {
                open(url);
            }
            return true;
        }

        @RequiresApi(24)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if(Build.VERSION.SDK_INT < 24) {
                //API < 24/N: use "shouldOverrideUrlLoading(WebView view, String url)"
                return false;
            }
            String url = request.getUrl().toString();
            Logger.logDebug(TAG, "shouldOverrideUrlLoading: " + url);
            if(MRAIDView.this.mTestListener != null) {
                if(MRAIDView.this.mTestListener.interceptRequest(url)) {
                    return true;
                }
            }
            if (url.startsWith("mraid://")) {
                parseCommandUrl(url);
            } else if(url.startsWith("console.log")) {
                Logger.logDebug(TAG, "JS-Console: " + url.substring(14));
            } else {
                open(url);
            }
            return true;
        }

        @TargetApi(24)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if(url.endsWith("mraid.js")) {
                String mraidJSSrc = "javascript:" + getMraidJs();
                InputStream data = new ByteArrayInputStream(mraidJSSrc.getBytes());
                return new WebResourceResponse("text/javascript", "UTF-8", data);
            }

            if(Build.VERSION.SDK_INT == 25) {
                return super.shouldInterceptRequest(view, url);
            }

            Logger.logDebug(TAG, "shouldInterceptRequest 24: " + url);

            if(MRAIDView.this.mTestListener != null) {
                MRAIDView.this.mTestListener.interceptRequest(url);
            }

            return super.shouldInterceptRequest(view, url);
        }

        @RequiresApi(25)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if(url.endsWith("mraid.js")) {
                String mraidJSSrc = "javascript:" + getMraidJs();
                InputStream data = new ByteArrayInputStream(mraidJSSrc.getBytes());
                return new WebResourceResponse("text/javascript", "UTF-8", data);
            }

            if(Build.VERSION.SDK_INT < 25) {
                return super.shouldInterceptRequest(view, request);
            }

            Logger.logDebug(TAG, "shouldInterceptRequest 25: " + url);

            if(MRAIDView.this.mTestListener != null) {
                MRAIDView.this.mTestListener.interceptRequest(url);
            }

            return super.shouldInterceptRequest(view, request);
        }
    }

    private void handleWebViewStartRendering(final WebView webView) {
        handleWebPageLoaded();
    }

    private void handleWebPageLoaded() {
        Logger.logDebug(TAG, "State: " + getStateString());
        if (state == STATE_LOADING) {
            isPageFinished = true;
            injectJavaScriptInCurrentWebView("mraid.setPlacementType('" + (isInterstitial ? "interstitial" : "inline") + "');");
            setSupportedServices();
            if (isLaidOut) {
                setScreenSize();
                setMaxSize();
                setCurrentPosition();
                setDefaultPosition();
                if (isInterstitial) {
                    showAsInterstitial();
                } else {
                    state = STATE_DEFAULT;
                    fireStateChangeEvent();
                    fireReadyEvent();
                    if (isViewable) {
                        fireViewableChangeEvent();
                    }
                }
            }

            if (listener != null) {
                listener.mraidViewLoaded(MRAIDView.this);
            }
        }
        if (isExpandingPart2) {
            isExpandingPart2 = false;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    injectJavaScriptInCurrentWebView("mraid.setPlacementType('" + (isInterstitial ? "interstitial" : "inline") + "');");
                    setSupportedServices();
                    setScreenSize();
                    setDefaultPosition();
                    Logger.logDebug(TAG, "calling fireStateChangeEvent 2");
                    fireStateChangeEvent();
                    fireReadyEvent();
                    if (isViewable) {
                        fireViewableChangeEvent();
                    }
                }
            });
        }
    }

    /**************************************************************************
     * Methods for responding to changes of size and position.
     **************************************************************************/
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Logger.logDebug(TAG, "onConfigurationChanged " + (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? "portrait" : "landscape"));
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }
    
    @Override
    protected void onAttachedToWindow() {
        Logger.logDebug(TAG, "onAttachedToWindow");
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        Logger.logDebug(TAG, "onDetachedFromWindow");
        super.onDetachedFromWindow();
    }
    
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Logger.logDebug(TAG, "onVisibilityChanged " + getVisibilityString(visibility));
        setViewable(visibility);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        int actualVisibility = getVisibility();
        Logger.logDebug(TAG, "onWindowVisibilityChanged " + getVisibilityString(visibility) +
                " (actual " + getVisibilityString(actualVisibility) + ")");
        setViewable(actualVisibility);
    }
    
    private void setViewable(int visibility) {
        boolean isCurrentlyViewable = (visibility == View.VISIBLE);
        if (isCurrentlyViewable != isViewable) {
            isViewable = isCurrentlyViewable;
            if (isPageFinished && isLaidOut) {
                fireViewableChangeEvent();
            }
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Logger.logDebug(TAG, "onLayout (" + state + ") " +
                changed + " " + left + " " + top + " " + right + " " + bottom);
        if (isForcingFullScreen) {
            Logger.logDebug(TAG, "onLayout ignored");
            return;
        }
        if (state == STATE_EXPANDED || state == STATE_RESIZED) {
            calculateScreenSize();
            calculateMaxSize();
        }
        if (isClosing) {
            isClosing = false;
            currentPosition = new Rect(defaultPosition);
            setCurrentPosition();
        } else {
            calculatePosition(false);
        }
        if (state == STATE_RESIZED && changed) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setResizedViewPosition();
                }
            });
        }
        isLaidOut = true;
        if (state == STATE_LOADING && isPageFinished && !isInterstitial) {
            state = STATE_DEFAULT;
            fireStateChangeEvent();
            fireReadyEvent();
            if (isViewable) {
                fireViewableChangeEvent();
            }
        }
    }
    
    private void onLayoutWebView(WebView wv, boolean changed, int left, int top, int right, int bottom) {
        boolean isCurrent = (wv == currentWebView);
        Logger.logDebug(TAG, "onLayoutWebView " + (wv == webView ? "1 " : "2 ") + isCurrent + " (" + state + ") " +
                changed + " " + left + " " + top + " " + right + " " + bottom);
        if (!isCurrent) {
            Logger.logDebug(TAG, "onLayoutWebView ignored, not current");
            return;
        }
        if (isForcingFullScreen) {
            Logger.logDebug(TAG, "onLayoutWebView ignored, isForcingFullScreen");
            isForcingFullScreen = false;
            return;
        }
        if (state == STATE_LOADING || state == STATE_DEFAULT) {
            calculateScreenSize();
            calculateMaxSize();
        }
        
        // If closing from expanded state, just set currentPosition to default position in onLayout above.
        if (!isClosing) {
            calculatePosition(true);
            if (isInterstitial) {
                // For interstitials, the default position is always the current position
                if (!defaultPosition.equals(currentPosition)) {
                    defaultPosition = new Rect(currentPosition);
                    setDefaultPosition();
                }
            }
        }
        
        if (isExpandingFromDefault) {
            isExpandingFromDefault = false;
            if (isInterstitial) {
                state = STATE_DEFAULT;
                isLaidOut = true;
            }
            if (!isExpandingPart2) {
                Logger.logDebug(TAG, "calling fireStateChangeEvent 1");
                fireStateChangeEvent();
            }
            if (isInterstitial) {
                if (isViewable) {
                    fireViewableChangeEvent();
                }
            }            
            if (listener != null) {
                listener.mraidViewExpand(this);
            }
        }
    }
    
    private void calculateScreenSize() {
        int orientation = getResources().getConfiguration().orientation;
        boolean isPortrait =  (orientation == Configuration.ORIENTATION_PORTRAIT);
        Logger.logDebug(TAG, "calculateScreenSize orientation " + (isPortrait ? "portrait" : "landscape"));
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        Logger.logDebug(TAG, "calculateScreenSize screen size " + width + "x" + height);
        if (width != screenSize.width || height != screenSize.height) {
            screenSize.width = width;
            screenSize.height = height;
            if (isPageFinished) {
                setScreenSize();
            }
        }
    }
    
    private void calculateMaxSize() {
        int width, height;
        Rect frame = new Rect();
        Window window = ((Activity) mContext).getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(frame);
        Logger.logDebug(TAG, "calculateMaxSize frame [" + frame.left + "," + frame.top + "][" + frame.right + "," + frame.bottom + "] (" +
                frame.width() + "x" + frame.height() + ")");
        statusHeight = frame.top;
        contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleHeight = contentViewTop - statusHeight;
        Logger.logDebug(TAG, "calculateMaxSize statusHeight " + statusHeight);
        Logger.logDebug(TAG, "calculateMaxSize titleHeight " + titleHeight);
        Logger.logDebug(TAG, "calculateMaxSize contentViewTop " + contentViewTop);
        width = frame.width();
        height = screenSize.height - contentViewTop;
        Logger.logDebug(TAG, "calculateMaxSize max size " + width + "x" + height);
        if (width != maxSize.width || height != maxSize.height) {
            maxSize.width = width;
            maxSize.height = height;
            if (isPageFinished) {
                setMaxSize();
            }
        }
    }
    
    private void calculatePosition(boolean isCurrentWebView) {
        int x, y, width, height;
        int[] location = new int[2];
        
        View view = isCurrentWebView ? currentWebView : this;
        String name = (isCurrentWebView ? "current" : "default");

        // This is the default location regardless of the state of the MRAIDView.
        view.getLocationOnScreen(location);
        x = location[0];
        y = location[1];
        Logger.logDebug(TAG, "calculatePosition " + name + " locationOnScreen [" + x + "," + y + "]");
        Logger.logDebug(TAG, "calculatePosition " + name + " contentViewTop " + contentViewTop);
        y = y - contentViewTop - statusHeight;

        width = view.getWidth();
        height = view.getHeight();
        
        Logger.logDebug(TAG, "calculatePosition " + name + " position [" + x + "," + y + "] (" + width + "x" + height + ")");
        
        Rect position = isCurrentWebView ? currentPosition : defaultPosition;

        if (x != position.left || y != position.top || width != position.width() || height != position.height()) {
            if (isCurrentWebView) {
                currentPosition = new Rect(x, y, x + width, y + height);
            } else {
                defaultPosition = new Rect(x, y, x + width, y + height);
            }
            if (isPageFinished) {
                if (isCurrentWebView) {
                    setCurrentPosition();
                } else {
                    setDefaultPosition();
                }
            }
        }
    }

    /**************************************************************************
     * Methods for forcing orientation.
     **************************************************************************/
    
    private static String getOrientationString(int orientation) {
        switch (orientation) {
        case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED: return "UNSPECIFIED";
        case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE: return "LANDSCAPE";
        case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT: return "PORTRAIT";
        default: return "UNKNOWN";
        }
    }
        
    private void applyOrientationProperties() {
        Logger.logDebug(TAG, "applyOrientationProperties " +
                orientationProperties.allowOrientationChange + " " + orientationProperties.forceOrientationString());
        
        Activity activity = (Activity) mContext;
        
        int currentOrientation = getResources().getConfiguration().orientation;
        boolean isCurrentPortrait = (currentOrientation == Configuration.ORIENTATION_PORTRAIT);
        Logger.logDebug(TAG, "currentOrientation " + (isCurrentPortrait ? "portrait" : "landscape"));
        
        int orientation = originalRequestedOrientation;
        if (orientationProperties.forceOrientation == MRAIDOrientationProperties.FORCE_ORIENTATION_PORTRAIT) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else if (orientationProperties.forceOrientation == MRAIDOrientationProperties.FORCE_ORIENTATION_LANDSCAPE) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else {
            // orientationProperties.forceOrientation == MRAIDOrientationProperties.FORCE_ORIENTATION_NONE
            if (orientationProperties.allowOrientationChange) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            } else {
                // orientationProperties.allowOrientationChange == false
                // lock the current orientation
                orientation = (isCurrentPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        activity.setRequestedOrientation(orientation);
    }
    
    private void restoreOriginalOrientation() {
        Logger.logDebug(TAG, "restoreOriginalOrientation");
        Activity activity = (Activity) mContext;
        int currentRequestedOrientation = activity.getRequestedOrientation();
        if (currentRequestedOrientation != originalRequestedOrientation) {
            activity.setRequestedOrientation(originalRequestedOrientation);
        }
    }

    public void removeAllViewFromParent() {
        if(this.currentWebView != null) {
            ((ViewGroup)this.currentWebView.getParent()).removeView(this.currentWebView);
        }
    }
}

