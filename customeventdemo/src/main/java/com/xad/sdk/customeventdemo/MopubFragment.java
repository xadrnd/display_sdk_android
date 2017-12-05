package com.xad.sdk.customeventdemo;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.mopub.mobileads.MoPubView;
import com.xad.sdk.events.CreativeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class MopubFragment extends CustomFragment implements View.OnClickListener {
    public MopubFragment() {
        // Required empty public constructor
        this.title = "Mopub";
    }

    public static MopubFragment newInstance() {
        MopubFragment fragment = new MopubFragment();
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

    private MoPubView mopubBanner;
    private Button loadBannerBT;
    private Button loadInterstitialBT;
    private Button loadVideoBT;

    private static final String AdUnitId = "8949067d999744b99bb1a9e73c92dddd";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mopub, container, false);
        mopubBanner = (MoPubView) view.findViewById(R.id.mopub_banner);
        mopubBanner.setAdUnitId(AdUnitId);
        mopubBanner.setKeywords("m_age:24,m_gender:m");

        loadBannerBT = (Button) view.findViewById(R.id.mopub_load_banner);
        loadBannerBT.setOnClickListener(this);
        loadInterstitialBT = (Button) view.findViewById(R.id.mopub_load_interstitial);
        loadInterstitialBT.setOnClickListener(this);
        loadVideoBT = (Button) view.findViewById(R.id.mopub_load_video);
        loadVideoBT.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == loadBannerBT.getId()) {

            mopubBanner.loadAd();
        } else if(v.getId() == loadInterstitialBT.getId()) {

        } else if(v.getId() == loadVideoBT.getId()) {

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAdResponseReceived(CreativeEvent event) {
        if(event.CreativeString != null && !event.CreativeString.isEmpty()) {
            Toast.makeText(getActivity(), "Showing ad from xad through custom event", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        mopubBanner.destroy();
        super.onDestroy();
    }
}
