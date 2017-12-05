//
//  VastActivity.java
//

//  Copyright (c) 2014 Nexage. All rights reserved.
//

package com.xad.sdk.vast.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.moat.analytics.mobile.MoatAdEvent;
import com.moat.analytics.mobile.MoatAdEventType;
import com.moat.analytics.mobile.MoatFactory;
import com.moat.analytics.mobile.NativeVideoTracker;
import com.xad.sdk.events.CreativeEvent;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.ErrorPosting;
import com.xad.sdk.utils.Logger;
import com.xad.sdk.vast.VASTPlayer;
import com.xad.sdk.vast.model.TRACKING_EVENTS_TYPE;
import com.xad.sdk.vast.model.VASTModel;
import com.xad.sdk.vast.util.HttpTools;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static android.view.View.VISIBLE;

public class VASTActivity extends Activity implements OnCompletionListener,
        OnErrorListener, OnPreparedListener, OnVideoSizeChangedListener,
        SurfaceHolder.Callback {
    private static final String TAG = "VASTActivity";

    //Close Region Size
    private static final float CLOSE_REGION_SIZE = 50.0f;
    private static final float CLOSE_REGION_PADDING = 25.0f;

    // timer delays
    private static final long SHOW_CLOSE_BUTTON_DELAYED_IN_MS = 5 * 1000;
    private static final long QUARTILE_TIMER_INTERVAL_IN_MS = 250;
    private static final long VIDEO_PROGRESS_TIMER_INTERVAL_IN_MS = 250;
    private static final int SHOW_CLOSE_BUTTON = 0;
    private static final int QUARTILE_TRACKING = 1;
    private static final int VIDEO_PROGRESS_TRACKING = 2;


    //Creative
    @NonNull private CreativeEvent mCreativeEvent;
    private String accessKey;

    private LinkedList<Integer> mVideoProgressTracker = null;
    private final int mMaxProgressTrackingPoints = 20;

    private Handler mHandler;

    @NonNull private VASTModel mVastModel;
    private HashMap<TRACKING_EVENTS_TYPE, List<String>> mTrackingEventMap;

    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private RelativeLayout mOverlay;
    private RelativeLayout mRootLayout;

    private DisplayMetrics mDisplayMetrics;

    private ImageButton mCloseButton;

    private int mVideoHeight;
    private int mVideoWidth;
    private int mScreenWidth;
    private int mScreenHeight;    
    private boolean mIsPlayBackError = false;
    private boolean mIsProcessedImpressions = false;
    private boolean mIsCompleted = false;
    private int mCurrentVideoPosition;
    private boolean mCloseButtonIsVisible = false;
    private int mQuartile = 0;
    
    private ProgressBar mProgressBar;

    private NativeVideoTracker mVideoTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(TAG, "entered onCreate --(life cycle event)");
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mIsCompleted = savedInstanceState.getBoolean(COMPLETE_STATUS);
            mCurrentVideoPosition = savedInstanceState.getInt(POSITION_STATUS);
            mCloseButtonIsVisible = savedInstanceState.getBoolean(CLOSE_BUTTON_VISIBLE_STATUS);
        }

        int currentOrientation = this.getResources().getConfiguration().orientation;
        Logger.logDebug(TAG, "currentOrientation:" + currentOrientation);

        if (currentOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            Logger.logDebug(TAG, "Orientation is not landscape.....forcing landscape");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        } else {
            Logger.logDebug(TAG, "orientation is landscape");
            Intent i = getIntent();
            Bundle extras = i.getExtras();
            CreativeEvent creativeEvent = (CreativeEvent)extras.getSerializable(Constants.KEY_CREATIVE_EVENT);
            VASTModel vastModel = (VASTModel) extras.getSerializable(Constants.KEY_VAST_MODEL);
            accessKey = extras.getString(Constants.KEY_ACCESS_KEY);

            MoatFactory mMoatFactory = MoatFactory.create(this);
            mVideoTracker = mMoatFactory.createNativeVideoTracker(Constants.MOAT_VIDEO_PARTNER_CODE);

            if(creativeEvent == null) {
                Logger.logError(TAG, "Creative Event is null. Stopping activity.");
                ErrorPosting.sendError(this, ErrorPosting.CONTENT_VAST_NOT_VALID_ERROR_TO_POST);
                finishVAST();
            } else if (vastModel == null) {
                Logger.logError(TAG, "VAST model is null. Stopping activity.");
                ErrorPosting.sendError(this,
                        ErrorPosting.CONTENT_VAST_NOT_VALID_ERROR_TO_POST,
                        creativeEvent.CreativeString,
                        creativeEvent.adGroupId);
                finishVAST();
            } else {
                this.mCreativeEvent = creativeEvent;
                this.mVastModel = vastModel;
                hideTitleStatusBars();
                mHandler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        switch (msg.what) {
                            case SHOW_CLOSE_BUTTON:
                                if(mCloseButton != null) {
                                    mCloseButton.setVisibility(VISIBLE);
                                    mCloseButtonIsVisible = true;
                                }
                                break;
                            case QUARTILE_TRACKING:
                                quartileTracker();
                                break;
                            case VIDEO_PROGRESS_TRACKING:
                                videoProgressTracker();
                                break;
                        }
                        return true;
                    }
                });
                mDisplayMetrics = this.getResources()
                        .getDisplayMetrics();

                mScreenWidth = mDisplayMetrics.widthPixels;
                mScreenHeight = mDisplayMetrics.heightPixels;
                mTrackingEventMap = mVastModel.getTrackingUrls();
                createUIComponents();

                registerSilentMode();
            }
        }
    }

    BroadcastReceiver ringModeChangeReceiver;

    private void registerSilentMode() {
        ringModeChangeReceiver =new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                Logger.logDebug(TAG, "Ring mode changed: " + manager.getRingerMode());
                if (manager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                    processEvent(TRACKING_EVENTS_TYPE.unmute);
                } else {
                    processEvent(TRACKING_EVENTS_TYPE.mute);
                }
            }
        };

        IntentFilter filter=new IntentFilter(
                AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(ringModeChangeReceiver,filter);
    }

    private void unregisterSilentMode() {
        if (ringModeChangeReceiver != null) {
            unregisterReceiver(ringModeChangeReceiver);
        }
    }

    @Override
    protected void onStart() {
        Logger.logDebug(TAG, "entered onStart --(life cycle event)");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Logger.logDebug(TAG, "entered on onResume --(life cycle event)");
        super.onResume();
        if(mCloseButton != null && mCloseButton.getVisibility() != VISIBLE && mHandler != null) {
            mHandler.sendEmptyMessageDelayed(SHOW_CLOSE_BUTTON, SHOW_CLOSE_BUTTON_DELAYED_IN_MS);
        }
    }

    @Override
    protected void onStop() {
        Logger.logDebug(TAG, "entered on onStop --(life cycle event)");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Logger.logDebug(TAG, "entered on onRestart --(life cycle event)");
        super.onRestart();
        createMediaPlayer();
    }

    @Override
    protected void onPause() {
        Logger.logDebug(TAG, "entered on onPause --(life cycle event)");
        super.onPause();

        if (mMediaPlayer != null) {
            mCurrentVideoPosition = mMediaPlayer.getCurrentPosition();
            processEvent(TRACKING_EVENTS_TYPE.pause);
        }
        cleanActivityUp();
    }

    private static final String COMPLETE_STATUS = "com.xad.sdk.displaysdk.vastactivity.complete";
    private static final String POSITION_STATUS = "com.xad.sdk.displaysdk.vastactivity.position";
    private static final String CLOSE_BUTTON_VISIBLE_STATUS = "com.xad.sdk.displaysdk.vastactivity.closebuttonvisible";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(COMPLETE_STATUS, mIsCompleted);
        outState.putInt(POSITION_STATUS, mCurrentVideoPosition);
        outState.putBoolean(CLOSE_BUTTON_VISIBLE_STATUS, mCloseButtonIsVisible);
    }

    @Override
    protected void onDestroy() {
        Logger.logDebug(TAG, "entered on onDestroy --(life cycle event)");
        unregisterSilentMode();
        super.onDestroy();

    }

    private void hideTitleStatusBars() {
        // hide title bar of application
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // hide status bar of Android
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    private void createUIComponents() {
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        this.createRootLayout(params);
        this.createSurface(params);
        this.createMediaPlayer();
        this.createOverlay(params);
        this.createCloseButton();
        this.setContentView(mRootLayout);
        this.createProgressBar();

    }

    private void createProgressBar() {
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        mProgressBar = new ProgressBar(this);
        mProgressBar.setLayoutParams(params);

        mRootLayout.addView(mProgressBar);
        mProgressBar.setVisibility(View.GONE);
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);    
    }
    
    private void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);    
    }

    
    private void createRootLayout(LayoutParams params) {

        mRootLayout = new RelativeLayout(this);
        mRootLayout.setLayoutParams(params);
        mRootLayout.setPadding(0, 0, 0, 0);
        mRootLayout.setBackgroundColor(Color.BLACK);
        processEvent(TRACKING_EVENTS_TYPE.fullscreen);
    }

    private void createSurface(LayoutParams params) {
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.setLayoutParams(params);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mRootLayout.addView(mSurfaceView);
    }

    private void createMediaPlayer() {

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setScreenOnWhilePlaying(true);
        processEvent(TRACKING_EVENTS_TYPE.creativeView);
    }

    private void createOverlay(LayoutParams params) {

        mOverlay = new RelativeLayout(this);
        mOverlay.setLayoutParams(params);
        mOverlay.setPadding(0, 0, 0, 0);
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        mOverlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayClicked();
            }
        });

        mRootLayout.addView(mOverlay);
    }

    private void createCloseButton() {

        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLOSE_REGION_SIZE, mDisplayMetrics);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLOSE_REGION_PADDING, mDisplayMetrics)/2;

        mCloseButton = new ImageButton(this);
        Drawable closeButtonNormalDrawable = com.xad.sdk.mraid.Assets.getDrawableFromBase64(getResources(), com.xad.sdk.mraid.Assets.new_close);
        Drawable closeButtonPressedDrawable = com.xad.sdk.mraid.Assets.getDrawableFromBase64(getResources(), com.xad.sdk.mraid.Assets.new_close_pressed);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { -android.R.attr.state_pressed }, closeButtonNormalDrawable);
        states.addState(new int[] { android.R.attr.state_pressed }, closeButtonPressedDrawable);

        mCloseButton.setImageDrawable(states);
        mCloseButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        LayoutParams params = new LayoutParams(size, size);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.ALIGN_PARENT_TOP);
        mCloseButton.setLayoutParams(params);
        mCloseButton.setPadding(padding, padding, padding, padding);
        mCloseButton.setBackgroundColor(Color.TRANSPARENT);
        if (!mCloseButtonIsVisible) {
            mCloseButton.setVisibility(View.INVISIBLE);
        }
        mHandler.sendEmptyMessageDelayed(SHOW_CLOSE_BUTTON, SHOW_CLOSE_BUTTON_DELAYED_IN_MS);
        mCloseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closeClicked();
            }
        });

        mRootLayout.addView(mCloseButton);
    }

    private void processClickThroughEvent() {
        Logger.logDebug(TAG, "entered processClickThroughEvent:");
        
        if(VASTPlayer.listener!=null) {
            VASTPlayer.listener.vastClick();
        }
        
        String clickThroughUrl = mVastModel.getVideoClicks().getClickThrough();
        Logger.logDebug(TAG, "clickThrough url: " + clickThroughUrl);

        
        // Before we send the app to the click through url, we will process ClickTracking URL's.
        List<String> urls = mVastModel.getVideoClicks().getClickTracking();
        fireUrls(urls);
        
        // Navigate to the click through url
        try {
            Uri uri = Uri.parse(clickThroughUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            ResolveInfo resolvable = getPackageManager().resolveActivity(intent, PackageManager.GET_RESOLVED_FILTER);
            if(resolvable == null) {
                Logger.logError(TAG, "Clickthrough error occurred, uri is unresolvable");
                if (mCurrentVideoPosition>=mMediaPlayer.getCurrentPosition()*0.99) {
                    mMediaPlayer.start();
                }
            } else {
                navigateToBrowser(clickThroughUrl);
            }
        } catch (NullPointerException e) {
            Logger.logError(TAG, "ClickThroughUrl is null");
            Logger.logError(TAG, e.getMessage(), e);
            ErrorPosting.sendError(this,
                    ErrorPosting.CONTENT_VAST_NOT_VALID_ERROR_TO_POST,
                    mCreativeEvent.CreativeString,
                    mCreativeEvent.adGroupId);
        }
    }

    private void navigateToBrowser(String url) {
        if(Build.VERSION.SDK_INT >= 23) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }

    private void closeClicked() {
        Logger.logDebug(TAG, "entered closeClicked()");
        cleanActivityUp();
        
        if (!mIsPlayBackError) {
            this.processEvent(TRACKING_EVENTS_TYPE.close);
        }
        finishVAST();
        Logger.logDebug(TAG, "leaving closeClicked()");
    }

    @Override
    public void onBackPressed() {
        Logger.logDebug(TAG, "entered onBackPressed");
        if (mIsCompleted) {
            this.closeClicked();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Logger.logDebug(TAG, "surfaceCreated -- (SurfaceHolder callback)");
        try {
            if(mMediaPlayer==null) {
                createMediaPlayer();
            }
            this.showProgressBar();
            mMediaPlayer.setDisplay(mSurfaceHolder);
            String url = mVastModel.getPickedMediaFileURL();

            Logger.logDebug(TAG, "URL for media file:" + url);
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Logger.logError(TAG, e.getMessage(), e);
            ErrorPosting.sendError(this,
                    ErrorPosting.CONTENT_VAST_NOT_VALID_ERROR_TO_POST,
                    mCreativeEvent.CreativeString,
                    mCreativeEvent.adGroupId);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int arg1, int arg2,
            int arg3) {
        Logger.logDebug(TAG,
                "entered surfaceChanged -- (SurfaceHolder callback)");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Logger.logDebug(TAG, "entered surfaceDestroyed -- (SurfaceHolder callback)");
        cleanUpMediaPlayer();

    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Logger.logDebug(TAG, "entered onVideoSizeChanged -- (MediaPlayer callback)");
        mVideoWidth = width;
        mVideoHeight = height;
        Logger.logDebug(TAG, "video size: " + mVideoWidth + "x" + mVideoHeight);
    }

    private void startTracker(CreativeEvent event, MediaPlayer mp, View playerView) {
        if(event.IsFromTestChannel) {
            //Not start tracking for testing
            return;
        }
        if(event == null || mVideoTracker == null) {
            return;
        }

        HashMap<String, String> adIds = new HashMap<>();
        adIds.put("moatClientLevel1", event.VendorId);
        adIds.put("moatClientLevel2", event.CampaignId);
        adIds.put("moatClientLevel3", event.adGroupId);
        adIds.put("moatClientLevel4", event.CreativeId);
        adIds.put("moatClientSlicer1", accessKey);

        mVideoTracker.trackVideoAd(adIds, mp, playerView);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Logger.logDebug(TAG, "entered onPrepared called --(MediaPlayer callback) ....about to play");
        calculateAspectRatio();
        this.hideProgressBar();
        if(mIsCompleted) {
            Logger.logDebug(TAG, "Video's finished playing. stay idle");
            return;
        }

        startTracker(this.mCreativeEvent, mp, this.mSurfaceView);

        mMediaPlayer.start();

        this.startVideoProgressTracker();

        Logger.logDebug(TAG, "current location in video:"
                + mCurrentVideoPosition);
        if (mCurrentVideoPosition > 0) {
            Logger.logDebug(TAG, "seeking to location:"
                    + mCurrentVideoPosition);
            mMediaPlayer.seekTo(mCurrentVideoPosition);
            processEvent(TRACKING_EVENTS_TYPE.resume);
        }

        if (!mIsProcessedImpressions) {
            this.processImpressions();            
        }
        
        startQuartileTracker();

        if(!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        if (VASTPlayer.listener != null) {
            VASTPlayer.listener.vastStart();
        }
    }


    private void calculateAspectRatio() {
        Logger.logDebug(TAG, "entered calculateAspectRatio");
        
        if ( mVideoWidth == 0 || mVideoHeight == 0 ) {
            Logger.logWarning(TAG, "VideoWidth or VideoHeight is 0, skipping calculateAspectRatio");
            return;
        }
        
        Logger.logDebug(TAG, "calculating aspect ratio");
        double widthRatio = 1.0 * mScreenWidth / mVideoWidth;
        double heightRatio = 1.0 * mScreenHeight / mVideoHeight;

        double scale = Math.min(widthRatio, heightRatio);

        int surfaceWidth = (int) (scale * mVideoWidth);
        int surfaceHeight = (int) (scale * mVideoHeight);

        LayoutParams params = new LayoutParams(
                surfaceWidth, surfaceHeight);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        mSurfaceView.setLayoutParams(params);

        mSurfaceHolder.setFixedSize(surfaceWidth, surfaceHeight);

        Logger.logDebug(TAG, " screen size: " + mScreenWidth + "x" + mScreenHeight);
        Logger.logDebug(TAG, " video size:  " + mVideoWidth + "x" + mVideoHeight);
        Logger.logDebug(TAG, " widthRatio:   " + widthRatio);
        Logger.logDebug(TAG, " heightRatio:   " + heightRatio);

        Logger.logDebug(TAG, "surface size: " + surfaceWidth + "x" + surfaceHeight);

    }

    private void cleanActivityUp() {

        if (mHandler != null) {
            Logger.logDebug(TAG, "removing all hanging callbacks");
            mHandler.removeCallbacksAndMessages(null);
        }
        this.cleanUpMediaPlayer();
    }

    private void cleanUpMediaPlayer() {

        Logger.logDebug(TAG, "entered cleanUpMediaPlayer ");

        if (mMediaPlayer != null) {

            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }

            mMediaPlayer.setOnCompletionListener(null);
            mMediaPlayer.setOnErrorListener(null);
            mMediaPlayer.setOnPreparedListener(null);
            mMediaPlayer.setOnVideoSizeChangedListener(null);

            mMediaPlayer.release();
            mMediaPlayer = null;
        }

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Logger.logDebug(TAG, "entered onError -- (MediaPlayer callback)");
        mIsPlayBackError = true;
        Logger.logError(TAG, "Shutting down Activity due to Media Player errors: WHAT:" + what +": EXTRA:" + extra+":");

        processErrorEvent();
        this.closeClicked();

        ErrorPosting.sendError(this,
                ErrorPosting.CONTENT_VIDEO_PLAYBACK_ERROR_TO_POST,
                mCreativeEvent.CreativeString,
                mCreativeEvent.adGroupId);

        return true;
    }

    private void processErrorEvent() {
        Logger.logDebug(TAG, "entered processErrorEvent");

        List<String> errorUrls = mVastModel.getErrorUrl();
        fireUrls(errorUrls);
        VASTPlayer.listener.vastError(VASTPlayer.ERROR_MEDIA_FILE_PLAYBACK);
        finishVAST();
        
    }
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Logger.logDebug(TAG, "entered onCOMPLETION -- (MediaPlayer callback)");
        stopVideoProgressTracker();
        mCloseButton.setVisibility(VISIBLE);
        if ( !mIsPlayBackError && !mIsCompleted) {
            mIsCompleted = true;
            this.processEvent(TRACKING_EVENTS_TYPE.complete);
            if(VASTPlayer.listener!=null) {
                VASTPlayer.listener.vastComplete();
            }
        }

        MoatAdEvent event = new MoatAdEvent(MoatAdEventType.AD_EVT_COMPLETE);
        if(mVideoTracker != null) {
            mVideoTracker.dispatchEvent(event);
        }
    }

    private void overlayClicked() {
        this.processClickThroughEvent();
    }

    private void processImpressions() {
        Logger.logDebug(TAG, "entered processImpressions");
        
        mIsProcessedImpressions = true;
        List<String> impressions = mVastModel.getImpressions();
        fireUrls(impressions);
        
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

    private void startQuartileTracker() {
        Logger.logDebug(TAG, "entered startQuartileTracker");
        stopQuartileTracker();

        if (mIsCompleted) {
            Logger.logDebug(TAG, "ending quartileTimer because the video has been replayed");
            return;
        }

        mHandler.sendEmptyMessageDelayed(QUARTILE_TRACKING, QUARTILE_TIMER_INTERVAL_IN_MS);
    }

    private void stopQuartileTracker() {
        if(mHandler != null && mHandler.hasMessages(QUARTILE_TRACKING)) {
            Logger.logDebug(TAG, "entered stopQuartileTracker");
            mHandler.removeMessages(QUARTILE_TRACKING);
        }
    }

    private void quartileTracker() {
        mHandler.sendEmptyMessageDelayed(QUARTILE_TRACKING, QUARTILE_TIMER_INTERVAL_IN_MS);
        int videoDuration = mMediaPlayer.getDuration();
        int curPos = mMediaPlayer.getCurrentPosition();
        // wait for the video to really start
        if (curPos == 0) {
            return;
        }
        int percentage = 100 * curPos / videoDuration;

        if (percentage >= 25 * mQuartile) {
            if (mQuartile == 0) {
                Logger.logDebug(TAG, "Video at start: (" + percentage
                        + "%)");
                processEvent(TRACKING_EVENTS_TYPE.start);
            } else if (mQuartile == 1) {
                Logger.logDebug(TAG, "Video at first quartile: ("
                        + percentage + "%)");
                processEvent(TRACKING_EVENTS_TYPE.firstQuartile);
            } else if (mQuartile == 2) {
                Logger.logDebug(TAG, "Video at midpoint: ("
                        + percentage + "%)");
                processEvent(TRACKING_EVENTS_TYPE.midpoint);
            } else if (mQuartile == 3) {
                Logger.logDebug(TAG, "Video at third quartile: ("
                        + percentage + "%)");
                processEvent(TRACKING_EVENTS_TYPE.thirdQuartile);
                stopQuartileTracker();
            }
            mQuartile++;
        }
    }

    private void videoProgressTracker() {
        mHandler.sendEmptyMessageDelayed(VIDEO_PROGRESS_TRACKING, VIDEO_PROGRESS_TIMER_INTERVAL_IN_MS);
        mVideoProgressTracker.addLast(mMediaPlayer.getCurrentPosition());
        if (mMediaPlayer == null) {
            return;
        }

        if (mVideoProgressTracker.size() == mMaxProgressTrackingPoints) {
            int firstPosition = mVideoProgressTracker.getFirst();
            int lastPosition = mVideoProgressTracker.getLast();

            if (lastPosition > firstPosition) {
                Logger.logVerbose(TAG, "video progressing (position:"+lastPosition+")");
                mVideoProgressTracker.removeFirst();
            } else {
                Logger.logError(TAG, "Detected video hang, first position: " + mVideoProgressTracker.getFirst() + ", last position: " + mVideoProgressTracker.getLast());
                mIsPlayBackError = true;
                stopVideoProgressTracker();
                processErrorEvent();
                ErrorPosting.sendError(this,
                        ErrorPosting.CONTENT_VIDEO_HANG_ERROR_TO_POST,
                        mCreativeEvent.CreativeString,
                        mCreativeEvent.adGroupId);
                closeClicked();
                finishVAST();
            }
        }
    }

    private void startVideoProgressTracker() {
        Logger.logDebug(TAG, "entered startVideoProgressTracker");
        mVideoProgressTracker = new LinkedList<>();
        mHandler.sendEmptyMessageDelayed(VIDEO_PROGRESS_TRACKING, VIDEO_PROGRESS_TIMER_INTERVAL_IN_MS);

    }

    private void stopVideoProgressTracker() {
        if (mHandler != null && mHandler.hasMessages(VIDEO_PROGRESS_TRACKING)) {
            Logger.logDebug(TAG, "entered stopVideoProgressTracker");
            mHandler.removeMessages(VIDEO_PROGRESS_TRACKING);
        }
    }

    private void processEvent(TRACKING_EVENTS_TYPE eventName) {
        Logger.logDebug(TAG, "entered Processing Event: " + eventName);
        List<String> urls = mTrackingEventMap.get(eventName);
        fireUrls(urls);
    }

    private void finishVAST() {
        if (VASTPlayer.listener != null) {
            VASTPlayer.listener.vastDismiss();
        }
        finish();
    }
}
