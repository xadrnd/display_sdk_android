package com.xad.sdk.sdkdemo;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Ray.Wu on 5/15/17.
 * Copyright (c) 2016 xAd. All rights reserved.
 */

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.MyViewHolder> {
    private List<String> list;


    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView logTextView;

        public MyViewHolder(TextView itemView) {
            super(itemView);
            this.logTextView = itemView;
        }
    }

    public LogAdapter(List<String> list) {
        this.list = list;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView logTextView = new TextView(parent.getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        logTextView.setLayoutParams(params);
        return new MyViewHolder(logTextView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.logTextView.setText(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
