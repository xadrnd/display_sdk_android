package com.xad.sdk.utils;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.xad.sdk.DisplaySdk;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Ray.Wu on 7/13/16.
 * Copyright (c) 2016 xAd. All rights reserved.
 */
public class Logger {
    private static Level logLevel = Level.INFO;
    private static boolean enablePostLog;
    private static String logTagPrefix = "";

    public static class LogEvent{
        public final String Log;

        public LogEvent(String log) {
            Log = log;
        }
    }

    public enum  Level{
        NONE("NONE"),
        ERROR("ERROR"),
        WARNING("WARNING"),
        INFO("INFO"),
        DEBUG("DEBUG"),
        VERBOSE("VERBOSE");


        Level(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            switch (this) {
                case NONE:
                    return "Logger level: None";
                case ERROR:
                    return "Logger level: Error";
                case WARNING:
                    return "Logger level: Warning";
                case INFO:
                    return "Logger level: Info";
                case DEBUG:
                    return "Logger level: Debug";
                case VERBOSE:
                    return "Logger level: Verbose";
                default:
                    return "None";
            }
        }

        public final String value;
    }

    public static String currentTime() {
        return logTime(new Date());
    }

    private static String logTime(Date date) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ", Locale.US);
        return dateFormatter.format(date);
    }

    private static boolean postLogIsEnabled() {
        return enablePostLog;
    }
    public static void setEnablePostLog(boolean enable) {
        enablePostLog = enable;
    }

    public static void setLogTagPrefix(String prefix) {
        logTagPrefix = prefix;
    }
    public static String getLogTagPrefix() {
        return logTagPrefix;
    }

    public static void setLevel(Level level) {
        Log.d("Logger", "Log level set to " + level.toString());
        logLevel = level;
    }
    public static Level getLevel() {
        return logLevel;
    }

    public static void logError(String tag, String msg) {
        if(logLevel.ordinal() >= Level.ERROR.ordinal()) {
            Log.e(logTagPrefix + tag, msg);
            if(postLogIsEnabled()) {
                DisplaySdk.sharedBus().post(new LogEvent(currentTime() + tag + ": (E) " + msg));
            }
        }
    }

    public static void logError(String tag, String msg, Throwable tr) {
        if(logLevel.ordinal() >= Level.ERROR.ordinal()) {
            Log.e(logTagPrefix + tag, msg, tr);
            if(postLogIsEnabled()) {
                DisplaySdk.sharedBus().post(new LogEvent(currentTime() + tag + ": (E) " + msg));
            }
        }
    }

    public static void logWarning(String tag, String msg) {
        if(logLevel.ordinal() >= Level.WARNING.ordinal()) {
            Log.w(logTagPrefix + tag, msg);
            if(postLogIsEnabled()) {
                DisplaySdk.sharedBus().post(new LogEvent(currentTime() + tag + ": (W) " + msg));
            }
        }
    }

    public static void logInfo(String tag, String msg) {
        if(logLevel.ordinal() >= Level.INFO.ordinal()) {
            Log.i(logTagPrefix + tag, msg);
            if(postLogIsEnabled()) {
                DisplaySdk.sharedBus().post(new LogEvent(currentTime() + tag + ": (I) " + msg));
            }
        }
    }

    public static void logDebug(String tag, String msg) {
        if(logLevel.ordinal() >= Level.DEBUG.ordinal()) {
            Log.d(logTagPrefix + tag, msg);
            if(postLogIsEnabled()) {
                DisplaySdk.sharedBus().post(new LogEvent(currentTime() + tag + ": (D) " + msg));
            }
        }
    }

    public static void logVerbose(String tag, String msg) {
        if(logLevel.ordinal() >= Level.VERBOSE.ordinal()) {
            Log.v(logTagPrefix + tag, msg);
            if(postLogIsEnabled()) {
                DisplaySdk.sharedBus().post(new LogEvent(currentTime() + tag + ": (V) " + msg));
            }
        }
    }

    public static class LoggerLink {
        String tag;
        public LoggerLink(String tag) {
            this.tag = tag;
        }

        @JavascriptInterface
        public void verbose(String msg) {
            Logger.logVerbose(tag, msg);
        }

        @JavascriptInterface
        public void debug(String msg) {
            Logger.logDebug(tag, msg);
        }

        @JavascriptInterface
        public void info(String msg) {
            Logger.logInfo(tag, msg);
        }

        @JavascriptInterface
        public void warning(String msg) {
            Logger.logWarning(tag, msg);
        }

        @JavascriptInterface
        public void error(String msg) {
            Logger.logError(tag, msg);
        }
    }
}
