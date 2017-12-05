package com.xad.sdk.customeventdemo;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.xad.sdk.DisplaySdk;
import com.xad.sdk.events.CreativeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.Date;


public class DFPFragment extends CustomFragment implements View.OnClickListener {
    public DFPFragment() {
        // Required empty public constructor
        this.title = "DFP";
    }

    public static DFPFragment newInstance() {
        DFPFragment fragment = new DFPFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        DisplaySdk.sharedBus().register(this);
    }

    private PublisherAdView dfpBannerView;
    private InterstitialAd dfpInterstitialAd;

    private Button loadBannerBT;
    private Button loadInterstitialBT;
    private Button loadVideoBT;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dfp, container, false);

        dfpBannerView = (PublisherAdView) view.findViewById(R.id.dfp_banner);
        loadBannerBT = (Button)view.findViewById(R.id.dfp_load_banner);
        loadBannerBT.setOnClickListener(this);

        loadInterstitialBT = (Button) view.findViewById(R.id.dfp_load_interstitial);
        loadInterstitialBT.setOnClickListener(this);

        loadVideoBT = (Button) view.findViewById(R.id.dfp_load_video);
        loadVideoBT.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.YEAR, 1989);
        PublisherAdRequest adRequest = new PublisherAdRequest.Builder()
                .setGender(AdRequest.GENDER_MALE)
                .setBirthday(new Date(todayCal.getTimeInMillis()))
                .build();

        if(v.getId() == loadBannerBT.getId()) {
            dfpBannerView.loadAd(adRequest);
        } else if(v.getId() == loadInterstitialBT.getId()) {
            dfpInterstitialAd = new InterstitialAd(getActivity());
            dfpInterstitialAd.setAdUnitId("/155496285/Custom_Android_Interstitial");
            dfpInterstitialAd.loadAd(new AdRequest.Builder().build());
        } else if(v.getId() == loadVideoBT.getId()) {

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAdResponseReceived(CreativeEvent event) {
        if(!TextUtils.isEmpty(event.CreativeString)) {
            Toast.makeText(getActivity(), "Showing ad from xad through custom event", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        dfpBannerView.resume();
    }

    @Override
    public void onPause() {
        dfpBannerView.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        DisplaySdk.sharedBus().unregister(this);
        dfpBannerView.destroy();
        super.onDestroy();
    }
}
