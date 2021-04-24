package com.selfdrivingboat.boatcontroller;

import com.datadog.android.log.Logger;
public class DatadogLogger {
    private static Logger logger = null;

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
