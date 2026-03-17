package org.okane.voyagemapper.log;

import android.util.Log;

public class AndroidLogger implements AppLogger {
    @Override
    public void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    @Override
    public void w(String tag, String msg, Throwable t) {
        Log.w(tag, msg, t);
    }

    public void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    public void d(String tag, String msg, Throwable t) {
        Log.d(tag, msg, t);
    }

    @Override
    public void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void e(String tag, String msg, Throwable t) {
        Log.e(tag, msg, t);
    }
}
