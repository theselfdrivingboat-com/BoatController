package com.selfdrivingboat.boatcontroller;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.datadog.android.Datadog;
import com.datadog.android.core.configuration.Configuration;
import com.datadog.android.core.configuration.Credentials;
import com.datadog.android.log.Logger;
import com.datadog.android.privacy.TrackingConsent;

public class DatadogLogger {
    private static Logger logger = null;

    public static void initialiseLogger(Context context) {
        Configuration config = new Configuration.Builder(
                true,
                true,
                true,
                false
        ).useEUEndpoints().build();
        Credentials credentials = new Credentials(
                "pubb238590d7056e5ef565d077f7dec1946",
                "prod",
                BuildConfig.BUILD_TYPE,
                "",
                BuildConfig.APPLICATION_ID
        );
        Datadog.initialize(context, credentials, config, TrackingConsent.GRANTED);
        Datadog.setVerbosity(Log.WARN);
    }


    private static Logger instantiateLogger() {
        return new Logger.Builder()
                .setNetworkInfoEnabled(true)
                .setLogcatLogsEnabled(true)
                .setDatadogLogsEnabled(true)
                .setBundleWithTraceEnabled(true)
                .setLoggerName("DatadogLogger")
                .build();
    }

    public static Logger getInstance() {
        if (logger == null) {
            logger =instantiateLogger();
        }
        return logger;
    }

}
