package com.xad.sdk.sdkdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.xad.sdk.AdRequest;
import com.xad.sdk.AdSize;
import com.xad.sdk.BannerView;
import com.xad.sdk.DisplaySdk;
import com.xad.sdk.ErrorCode;
import com.xad.sdk.InterstitialAd;
import com.xad.sdk.RefreshInterval;
import com.xad.sdk.VideoAd;
import com.xad.sdk.listeners.BannerViewListener;
import com.xad.sdk.listeners.InterstitialAdListener;
import com.xad.sdk.listeners.TestListener;
import com.xad.sdk.listeners.VideoAdListener;
import com.xad.sdk.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String ACCESS_KEY = com.xad.sdk.sdkdemo.BuildConfig.ACCESS_KEY;
    private static final String TAG = "MainActivity";

    private RelativeLayout banner_ad_container;
    private LinearLayout channel_id_sec;

    private RelativeLayout mainContainer;

    private Button interstitialBT;
    private Button videoBT;
    private Button bannerBT;
    private Button hideBT;

    private BannerView adView;
    private InterstitialAd mAdInterstitial;
    private VideoAd rewardedVideoAd;
    private ImageView placeHolderImage;
    private EditText channelIdEditText;
    private String channelId;
    private Switch testModeSwitch;
    private boolean enableShowError;
    private Switch showErrorSwitch;
    private boolean isTestMode;
    private MaterialSpinner bannerSizeSpinner;
    private AdSize adSize = AdSize.BANNER;
    private RecyclerView urlLogRecycleView;
    private RecyclerView.Adapter urlLogsAdapter;

    private List<String> urlLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.setLevel(Logger.Level.VERBOSE);
        Logger.setLogTagPrefix("GT_");
        super.onCreate(savedInstanceState);
        DisplaySdk.sharedInstance().init(this);
        setContentView(R.layout.activity_main);

        this.findView();

        bannerBT.setOnClickListener(this);
        interstitialBT.setOnClickListener(this);
        videoBT.setOnClickListener(this);
        hideBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adView == null) {
                    showErrorDialog("No banner ad found");
                    return;
                }
                if(hideBT.getText().toString().toLowerCase().equals("hide")) {
                    hideBT.setText("SHOW");
                    adView.setVisibility(View.INVISIBLE);
                } else {
                    hideBT.setText("HIDE");
                    adView.setVisibility(View.VISIBLE);
                }
            }
        });

        testModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Logger.logDebug(TAG, "test mode switch is: " + b);
                MainActivity.this.isTestMode = b;
            }
        });

        testModeSwitch.setChecked(true);

        showErrorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Logger.logDebug(TAG, "Show error switch is: " + b);
                MainActivity.this.enableShowError = b;
            }
        });

        channelIdEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                CharSequence channelId = textView.getText();
                MainActivity.this.channelId = channelId.toString();
                return false;
            }
        });
        channelIdEditText.clearFocus();

        bannerSizeSpinner.setItems("320x50", "300x50", "300x250", "728x90");
        bannerSizeSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                Logger.logDebug(TAG, "Banner size spinner is in position: " + position);
                switch (position) {
                    case 0:
                        MainActivity.this.adSize = AdSize.BANNER;
                        break;
                    case 1:
                        MainActivity.this.adSize = AdSize.NARROW_BANNER;
                        break;
                    case 2:
                        MainActivity.this.adSize = AdSize.MEDIUM_RECTANGLE;
                        break;
                    case 3:
                        MainActivity.this.adSize = AdSize.LEADER_BOARD;
                        break;
                }
            }
        });
        bannerSizeSpinner.setSelectedIndex(0);

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                //android.Manifest.permission.READ_CONTACTS,
                //android.Manifest.permission.WRITE_CONTACTS,
                //android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                //Manifest.permission.WRITE_CALENDAR,
                //Manifest.permission.SEND_SMS,
                //android.Manifest.permission.CAMERA,
                //Manifest.permission.CALL_PHONE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WAKE_LOCK};

        if(!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context != null
                && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }



    private void findView() {
        mainContainer = (RelativeLayout)findViewById(R.id.container);
        banner_ad_container = (RelativeLayout) findViewById(R.id.banner_ad_container);
        channel_id_sec = (LinearLayout)findViewById(R.id.channel_id_sec);
        bannerBT = (Button) findViewById(R.id.banner_button);
        interstitialBT = (Button) findViewById(R.id.interstitial_button);
        videoBT = (Button) findViewById(R.id.video_button);
        hideBT = (Button) findViewById(R.id.hide_banner);
        placeHolderImage = (ImageView)findViewById(R.id.image_goes_here);
        channelIdEditText = (EditText)findViewById(R.id.channel_id_edit_text);
        testModeSwitch = (Switch)findViewById(R.id.test_mode_switch);
        showErrorSwitch = (Switch)findViewById(R.id.show_error_switch);
        bannerSizeSpinner = (MaterialSpinner)findViewById(R.id.banner_size_spinner);
        urlLogRecycleView = (RecyclerView)findViewById(R.id.url_log_recycle_view);
    }

    private AdRequest createAdRequest() {
        if (this.isTestMode) {
            AdRequest.TestType testType = TextUtils.isEmpty(this.channelId)? AdRequest.TestType.SANDBOX : AdRequest.TestType.CHANNEL;
            return new AdRequest.Builder()
                    .setTestMode(true)
                    .setTestType(testType, channelId)
                    .build();

        } else {
            return new AdRequest.Builder()
                    .setGender(AdRequest.Gender.MALE)
                    .build();
        }
    }

    private void refreshLogListView() {
        this.urlLogs = new ArrayList<>();
        this.urlLogRecycleView.setLayoutManager(new LinearLayoutManager(this));
        this.urlLogsAdapter = new LogAdapter(this.urlLogs);
        this.urlLogRecycleView.setAdapter(urlLogsAdapter);
    }

    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        DisplaySdk.sharedInstance().pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        DisplaySdk.sharedInstance().resume();
        if (adView != null) {
            adView.resume();
        }
        if(this.urlLogsAdapter != null) {
            this.urlLogsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        DisplaySdk.sharedInstance().destroy();
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.banner_button:
                this.loadBannerAd();
                break;
            case R.id.interstitial_button:
                loadAdInterstitial();
                break;
            case R.id.video_button:
                loadVideoAd();
                break;
        }
    }

    private void loadBannerAd() {
        if(this.adView != null) {
            this.adView.destroy();
        }
        this.adView = new BannerView(this, this.adSize, ACCESS_KEY);
        this.adView.setBackgroundColor(Color.RED);
        this.adView.setAdRequest(createAdRequest());
        this.adView.setRefresh(RefreshInterval.STATIC);
        this.refreshLogListView();
        this.adView.setTestListener(new TestListener() {
            @Override
            public boolean interceptRequest(String requestUrl) {
                urlLogs.add(requestUrl);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        urlLogsAdapter.notifyDataSetChanged();
                    }
                });

                return false;
            }

            @Override
            public boolean interceptResponse(Response response) {
                return false;
            }
        });

        this.banner_ad_container.addView(this.adView);

        this.adView.setAdListener(new BannerViewListener(){
            @Override
            public void onAdFetchFailed(BannerView bannerView, ErrorCode code) {
                if (code == ErrorCode.NO_INVENTORY) {
                    Logger.logDebug(TAG, "Failed to load ad due to the backend error");
                    showErrorDialog("Failed to load ad due to the backend error");
                }
            }

            @Override
            public void onAdClosed(BannerView bannerView) {

            }

            @Override
            public void onAdOpened(BannerView bannerView) {

            }

            @Override
            public void onAdLeftApplication(BannerView bannerView) {

            }

            @Override
            public void onAdLoaded(BannerView bannerView) {
                placeHolderImage.setVisibility(View.GONE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)channel_id_sec.getLayoutParams();
                params.addRule(RelativeLayout.BELOW, banner_ad_container.getId());
                channel_id_sec.setLayoutParams(params);
                mainContainer.requestLayout();
            }
        });
        this.adView.loadAd();
        this.channelIdEditText.setText("");
        this.channelId = "";
    }

    private void loadAdInterstitial() {
        mAdInterstitial = new InterstitialAd(this, ACCESS_KEY);
        mAdInterstitial.setAdRequest(createAdRequest());
        mAdInterstitial.setAdListener(new InterstitialAdListener() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                Logger.logDebug(TAG, "Interstitial loaded");
                interstitialAd.show();
            }

            @Override
            public void onAdFetchFailed(InterstitialAd interstitialAd, ErrorCode code) {
                if (code == ErrorCode.NO_INVENTORY) {
                    Logger.logDebug(TAG, "Failed to load ad due to the backend error");
                    showErrorDialog("Failed to load ad due to the backend error");
                }
            }

            @Override
            public void onInterstitialShown(InterstitialAd interstitialAd) {

            }

            @Override
            public void onInterstitialFailedToShow(InterstitialAd interstitialAd) {

            }

            @Override
            public void onAdClosed(InterstitialAd interstitialAd) {

            }

            @Override
            public void onAdOpened(InterstitialAd interstitialAd) {

            }

            @Override
            public void onAdLeftApplication(InterstitialAd interstitialAd) {

            }
        });
        this.mAdInterstitial.setTestListener(new TestListener() {
            @Override
            public boolean interceptRequest(String requestUrl) {
                urlLogs.add(requestUrl);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        urlLogsAdapter.notifyDataSetChanged();
                    }
                });

                return false;
            }

            @Override
            public boolean interceptResponse(Response response) {
                return false;
            }
        });
        this.refreshLogListView();
        mAdInterstitial.loadAd();
        this.channelIdEditText.setText("");
        this.channelId = "";
    }

    private void loadVideoAd() {
        createAdRequest();
        rewardedVideoAd = new VideoAd(this, 10, 60, ACCESS_KEY);
        rewardedVideoAd.setAdRequest(createAdRequest());
        this.refreshLogListView();
        this.rewardedVideoAd.setTestListener(new TestListener() {
            @Override
            public boolean interceptRequest(String requestUrl) {
                urlLogs.add(requestUrl);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        urlLogsAdapter.notifyDataSetChanged();
                    }
                });

                return false;
            }

            @Override
            public boolean interceptResponse(Response response) {
                return false;
            }
        });
        rewardedVideoAd.setAdListener(new VideoAdListener() {
            @Override
            public void onVideoFailedToLoad(VideoAd videoAd, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NO_INVENTORY) {
                    Logger.logDebug(TAG, "Failed to load video ad due to the backend error");
                    showErrorDialog("Failed to load video ad due to the backend error");
                }
            }

            @Override
            public void onVideoStarted(VideoAd videoAd) {

            }

            @Override
            public void onPlaybackError(VideoAd videoAd) {

            }

            @Override
            public void onVideoClosed(VideoAd videoAd) {

            }

            @Override
            public void onVideoClicked(VideoAd videoAd) {

            }

            @Override
            public void onVideoCompleted(VideoAd videoAd) {

            }

            @Override
            public void onVideoLoadSuccess(VideoAd videoAd) {
                videoAd.play();
            }
        });
        rewardedVideoAd.loadAd();
        this.channelIdEditText.setText("");
        this.channelId = "";
    }

    private void showErrorDialog(String errorMsg) {
        if(!this.enableShowError) {
            return;
        }
        AlertDialog errorAlertDialog = new AlertDialog.Builder(this).setMessage(errorMsg).setCancelable(true).create();
        errorAlertDialog.show();
    }
}
