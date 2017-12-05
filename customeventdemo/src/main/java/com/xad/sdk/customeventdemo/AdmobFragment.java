package com.xad.sdk.customeventdemo;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.xad.sdk.customevent.googlemobileads.CustomEventForAdmob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class AdmobFragment extends CustomFragment implements View.OnClickListener {
    public AdmobFragment() {
        // Required empty public constructor
        this.title = "Admob";
    }

    public static AdmobFragment newInstance() {
        AdmobFragment fragment = new AdmobFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    private AdView admobBannerView;
    private Button loadBannerBT;
    private Button loadInterstitialBT;
    private Button loadVideoBT;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admob, container, false);

        admobBannerView = (AdView) view.findViewById(R.id.admob_banner);
        loadBannerBT = (Button)view.findViewById(R.id.admob_load_banner);
        loadBannerBT.setOnClickListener(this);
        loadInterstitialBT = (Button) view.findViewById(R.id.admob_load_interstitial);
        loadInterstitialBT.setOnClickListener(this);
        loadVideoBT = (Button) view.findViewById(R.id.admob_load_video);
        loadVideoBT.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        Bundle extras = new Bundle();//Extras for setting badv, bcat, test mode
        extras.putStringArrayList("badv", new ArrayList<>(Arrays.asList("company1.com")));
        extras.putStringArrayList("bcat", new ArrayList<>(Arrays.asList("IAB1")));
        extras.putBoolean("test_mode", true);
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.YEAR, 1989);
        AdRequest adRequest = new AdRequest.Builder()
                .setGender(AdRequest.GENDER_MALE)
                .setBirthday(new Date(todayCal.getTimeInMillis()))
                .addCustomEventExtrasBundle(CustomEventForAdmob.class, extras)
                .build();

        if(v.getId() == loadBannerBT.getId()) {
            admobBannerView.loadAd(adRequest);
        } else if(v.getId() == loadInterstitialBT.getId()) {

        } else if(v.getId() == loadVideoBT.getId()) {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
