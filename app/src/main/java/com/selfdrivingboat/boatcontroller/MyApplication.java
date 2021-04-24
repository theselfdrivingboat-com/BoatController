package com.selfdrivingboat.boatcontroller;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.datadog.android.Datadog;
import com.datadog.android.core.configuration.Configuration;
import com.datadog.android.core.configuration.Credentials;
import com.datadog.android.privacy.TrackingConsent;

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
        Configuration config = new Configuration.Builder(
                true,
                true,
                true,
                false
        ).useEUEndpoints().build();
        Credentials credentials = new Credentials(
                "pubb238590d7056e5ef565d077f7dec1946",
                "prod",
                "debug",
                "",
                ""
        );
        Datadog.initialize(this, credentials, config, TrackingConsent.GRANTED);
        Datadog.setVerbosity(Log.WARN);

    }

    public static Context context() {
        return context;
    }

    public static int getmPId() {
        return mPId;
    }
}
