package com.example.touchflow;

import android.app.Application;
import android.content.Context;

import com.example.touchflow.utilities.LocaleHelper;

public class TouchFlowApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocaleHelper.syncAppCompatLocale(this);
    }
}
