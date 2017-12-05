package com.xad.sdk.mraid;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ray.Wu on 5/23/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class ExpandFragment extends Fragment {
    ViewGroup myContainer;
    final List<View> childViews = new ArrayList<>();
    public ExpandFragment() {
        //Empty default constructor
    }

    public static ExpandFragment newInstance() {
        ExpandFragment fragment = new ExpandFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myContainer = new RelativeLayout(getActivity());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        myContainer.setLayoutParams(params);
        for (View childView : childViews) {
            myContainer.addView(childView);
        }
        return myContainer;
    }

    public void addView(View view) {
        this.childViews.add(view);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
