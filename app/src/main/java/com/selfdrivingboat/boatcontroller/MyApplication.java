package com.selfdrivingboat.boatcontroller;

import android.app.Application;
import android.content.Context;
import android.os.Process;

/**
 * Created by WGH on 2017/4/10.
 */

public class MyApplication extends Application{

    private static Context context;
    private static int mPId;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        mPId = Process.myPid();
        DatadogLogger.initialiseLogger(this);
    }

    public static Context context() {
        return context;
    }

    public static int getmPId() {
        return mPId;
    }
}
