package com.xad.sdk.mraid;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.Logger;

import java.util.LinkedList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Ray.Wu on 5/24/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class VideoPlayerActivity extends Activity implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener,
        SurfaceHolder.Callback {

    private static final String TAG = "VideoPlayerActivity";
    private Handler mHandler;

    private String mVideoDataUrl;

    private static final double SKIP_INFO_PADDING_SCALE = 0.10;
    private static final double SKIP_INFO_SCALE = 0.15;

    // timer delays
    private static final long TOOLBAR_HIDE_DELAY = 3 * 1000;
    private static final long VIDEO_PROGRESS_TIMER_INTERVAL = 250;
    private static final int HIDE_TOOL_BAR = 0;
    private static final int VIDEO_PROGRESS_TRACKING = 1;

    private LinkedList<Integer> mVideoProgressTracker = null;
    private final int mMaxProgressTrackingPoints = 20;

    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private RelativeLayout mOverlay;
    private RelativeLayout mRootLayout;
    private RelativeLayout mButtonPanel;

    private ImageButton mPlayPauseButton;

    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private int mVideoHeight;
    private int mVideoWidth;
    private int mScreenWidth;
    private int mScreenHeight;
    private boolean mIsVideoPaused = false;
    private int mCurrentVideoPosition;

    private ProgressBar mProgressBar;

    private static final String POSITION_STATUS = "com.xad.sdk.displaysdk.videoplayactivity.position";
    private static final String PAUSE_STATUS = "com.xad.sdk.displaysdk.videoplayactivity.pause";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(TAG, "enter onCreate --(life cycle event)");
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mCurrentVideoPosition = savedInstanceState.getInt(POSITION_STATUS);
            mIsVideoPaused = savedInstanceState.getBoolean(PAUSE_STATUS);
        }

        int currentOrientation = this.getResources().getConfiguration().orientation;
        Logger.logDebug(TAG, "currentOrientation:" + currentOrientation);

        if (currentOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            Logger.logDebug(TAG, "Orientation is not landscape.....forcing landscape");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        } else {
            Logger.logDebug(TAG, "orientation is landscape");
            this.mVideoDataUrl = getIntent().getStringExtra(Constants.KEY_VIDEO_DATA);
            if (TextUtils.isEmpty(this.mVideoDataUrl)) {
                Logger.logError(TAG, "Video url is null. Stopping activity");
                finish();
            } else {
                hideTitleStatusBars();
                mHandler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        switch (msg.what) {
                            case HIDE_TOOL_BAR:
                                hideToolbar();
                                break;
                            case VIDEO_PROGRESS_TRACKING:
                                trackVideoProgress();
                                break;
                        }
                        return true;
                    }
                });
                DisplayMetrics displayMetrics = this.getResources()
                        .getDisplayMetrics();

                mScreenWidth = displayMetrics.widthPixels;
                mScreenHeight = displayMetrics.heightPixels;
                createUIComponents();
            }
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
        }
        cleanActivityUp();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(POSITION_STATUS, mCurrentVideoPosition);
        outState.putBoolean(PAUSE_STATUS, mIsVideoPaused);
    }


    @Override
    protected void onDestroy() {
        Logger.logDebug(TAG, "entered on onDestroy --(life cycle event)");
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        mHandler = null;
    }

    private void hideTitleStatusBars() {
        // hide title bar of application
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // hide status bar of Android
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    private void createUIComponents() {

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        this.createRootLayout(params);
        this.createSurface(params);
        this.createMediaPlayer();
        this.createOverlay(params);
        this.createButtonPanel(mScreenWidth, mScreenHeight);

        int size = Math.min(mScreenWidth, mScreenHeight);
        size = (int) (SKIP_INFO_SCALE * size);

        this.createPlayPauseButton(size);
        this.createCloseButton(size);
        this.setContentView(mRootLayout);

        this.createProgressBar();

    }

    private void createProgressBar() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
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


    private void createRootLayout(RelativeLayout.LayoutParams params) {
        mRootLayout = new RelativeLayout(this);
        mRootLayout.setLayoutParams(params);
        mRootLayout.setPadding(0, 0, 0, 0);
        mRootLayout.setBackgroundColor(Color.BLACK);
    }

    private void createSurface(RelativeLayout.LayoutParams params) {
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
    }

    private void createOverlay(RelativeLayout.LayoutParams params) {
        mOverlay = new RelativeLayout(this);
        mOverlay.setLayoutParams(params);
        mOverlay.setPadding(0, 0, 0, 0);
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        mOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayClicked();
            }
        });
        mRootLayout.addView(mOverlay);
    }

    private void createButtonPanel(int screenWidth, int screenHeight) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mButtonPanel = new RelativeLayout(this);
        mButtonPanel.setLayoutParams(params);
        int padding = Math.min(screenWidth, screenHeight);
        padding = (int) (SKIP_INFO_PADDING_SCALE * padding);
        mButtonPanel.setPadding(padding, 0, padding, 0);
        mButtonPanel.setBackgroundColor(Color.BLACK);
        mButtonPanel.setVisibility(GONE);
        mOverlay.addView(mButtonPanel);
    }

    private void createCloseButton(int size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        ImageButton mCloseButton = new ImageButton(this);
        Drawable drawable = com.xad.sdk.vast.util.Assets.getDrawableFromBase64(getResources(), com.xad.sdk.vast.util.Assets.exit);
        mCloseButton.setImageDrawable(drawable);
        mCloseButton.setLayoutParams(params);
        mCloseButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mCloseButton.setPadding(0, 0, 0, 0);
        mCloseButton.setBackgroundColor(Color.TRANSPARENT);
        mCloseButton.setVisibility(View.VISIBLE);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        mButtonPanel.addView(mCloseButton);
    }

    private void createPlayPauseButton(int size) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mPauseDrawable = com.xad.sdk.vast.util.Assets.getDrawableFromBase64(getResources(), com.xad.sdk.vast.util.Assets.pause);
        mPlayDrawable = com.xad.sdk.vast.util.Assets.getDrawableFromBase64(getResources(), com.xad.sdk.vast.util.Assets.play);
        mPlayPauseButton = new ImageButton(this);
        mPlayPauseButton.setImageDrawable(mPauseDrawable);
        mPlayPauseButton.setLayoutParams(params);
        mPlayPauseButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mPlayPauseButton.setPadding(0, 0, 0, 0);
        mPlayPauseButton.setBackgroundColor(Color.TRANSPARENT);
        mPlayPauseButton.setEnabled(true);
        mPlayPauseButton.setVisibility(VISIBLE);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseButtonClicked();
            }

        });

        mButtonPanel.addView(mPlayPauseButton);
    }

    private void activateButtons (boolean active) {
        Logger.logDebug(TAG, "entered activateButtons:");
        if (active) {
            mButtonPanel.setVisibility(VISIBLE);
        } else {
            mButtonPanel.setVisibility(GONE);
        }
    }

    private void close() {
        Logger.logDebug(TAG, "entered close()");
        cleanActivityUp();
        finish();
        Logger.logDebug(TAG, "leaving close()");
    }

    private void playPauseButtonClicked() {
        Logger.logDebug(TAG, "entered playPauseClicked");
        if(mMediaPlayer==null) {
            Logger.logError(TAG, "MediaPlayer is null when \"playPauseButton\" was clicked");
            return;
        }
        boolean isPlaying = mMediaPlayer.isPlaying();
        Logger.logDebug(TAG, "isPlaying:" + isPlaying);

        if (isPlaying) {
            this.processPauseSteps();
        } else {
            this.processPlaySteps();
        }
    }

    private void processPauseSteps() {
        mIsVideoPaused = true;
        mMediaPlayer.pause();
        this.stopVideoProgressTimer();
        this.stopToolBarTimer();
        mPlayPauseButton.setImageDrawable(mPlayDrawable);
    }


    private void processPlaySteps() {
        mIsVideoPaused = false;
        mMediaPlayer.start();
        mPlayPauseButton.setImageDrawable(mPauseDrawable);
        this.startToolBarTimer();
        this.startVideoProgressTimer();
    }


    @Override
    public void onBackPressed() {
        Logger.logDebug(TAG, "entered onBackPressed");
        this.close();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Logger.logDebug(TAG, "surfaceCreated -- (SurfaceHolder callback)");
        try {
            if(mMediaPlayer==null) {
                createMediaPlayer();
            }
            this.showProgressBar();
            mMediaPlayer.setDisplay(mSurfaceHolder);
            Logger.logDebug(TAG, "URL for media file:" + this.mVideoDataUrl);
            mMediaPlayer.setDataSource(this.mVideoDataUrl);
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            Logger.logError(TAG, e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int arg1, int arg2, int arg3) {
        Logger.logDebug(TAG, "entered surfaceChanged -- (SurfaceHolder callback)");
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

    @Override
    public void onPrepared(MediaPlayer mp) {
        Logger.logDebug(TAG, "entered onPrepared called --(MediaPlayer callback) ....about to play");
        calculateAspectRatio();
        mMediaPlayer.start();
        this.hideProgressBar();

        if (mIsVideoPaused) {
            Logger.logDebug(TAG, "pausing video");
            processPauseSteps();
        } else {
            this.startVideoProgressTimer();
        }

        Logger.logDebug(TAG, "current location in video:" + mCurrentVideoPosition);
        if (mCurrentVideoPosition > 0) {
            Logger.logDebug(TAG, "seeking to location:" + mCurrentVideoPosition);
            mMediaPlayer.seekTo(mCurrentVideoPosition);
        }
        startToolBarTimer();

        if(!mMediaPlayer.isPlaying() && !mIsVideoPaused) {
            mMediaPlayer.start();
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

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
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
        this.cleanUpMediaPlayer();
        this.stopVideoProgressTimer();
        this.stopToolBarTimer();
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
        Logger.logError(TAG, "Shutting down Activity due to Media Player errors: WHAT:" + what +": EXTRA:" + extra+":");
        this.close();

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Logger.logDebug(TAG, "entered onCOMPLETION -- (MediaPlayer callback)");
        close();
    }

    private void overlayClicked() {
        this.startToolBarTimer();
    }

    // Timers
    private void startToolBarTimer() {
        Logger.logDebug(TAG, "entered startToolBarTimer");
        if (mMediaPlayer!= null && mMediaPlayer.isPlaying()) {
            stopToolBarTimer();
            mHandler.sendEmptyMessageDelayed(HIDE_TOOL_BAR, TOOLBAR_HIDE_DELAY);
            mButtonPanel.setVisibility(VISIBLE);
        }

        if (mIsVideoPaused) {
            activateButtons(true);
        }
    }

    private void hideToolbar() {
        Logger.logDebug(TAG, "hiding buttons");
        mButtonPanel.setVisibility(GONE);
    }

    private void stopToolBarTimer() {
        if(mHandler != null && mHandler.hasMessages(HIDE_TOOL_BAR)) {
            Logger.logDebug(TAG, "entered stopToolBarTimer");
            mHandler.removeMessages(HIDE_TOOL_BAR);
        }
    }

    private void startVideoProgressTimer() {
        Logger.logDebug(TAG, "entered startVideoProgressTimer");

        mVideoProgressTracker = new LinkedList<>();
        mHandler.sendEmptyMessageDelayed(VIDEO_PROGRESS_TRACKING, VIDEO_PROGRESS_TIMER_INTERVAL);

    }

    private void trackVideoProgress() {
        mHandler.sendEmptyMessageDelayed(VIDEO_PROGRESS_TRACKING, VIDEO_PROGRESS_TIMER_INTERVAL);
        if (mMediaPlayer == null) {
            return;
        }

        mVideoProgressTracker.addLast(mMediaPlayer.getCurrentPosition());

        if (mVideoProgressTracker.size() == mMaxProgressTrackingPoints) {
            int firstPosition = mVideoProgressTracker.getFirst();
            int lastPosition = mVideoProgressTracker.getLast();

            if (lastPosition > firstPosition) {
                Logger.logVerbose(TAG, "Video progressing (position:"+lastPosition+")");
                mVideoProgressTracker.removeFirst();
            } else {
                Logger.logError(TAG, "Detected video hang, first position: " + firstPosition + ", last position: " + lastPosition);
                stopVideoProgressTimer();
                close();
            }
        }
    }

    private void stopVideoProgressTimer() {
        if (mHandler != null && mHandler.hasMessages(VIDEO_PROGRESS_TRACKING)) {
            Logger.logDebug(TAG, "entered stopVideoProgressTimer");
            mHandler.removeMessages(VIDEO_PROGRESS_TRACKING);
        }
    }
}

