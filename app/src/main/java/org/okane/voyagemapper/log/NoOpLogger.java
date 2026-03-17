package org.okane.voyagemapper.log;

public class NoOpLogger implements AppLogger {

    @Override
    public void w(String tag, String msg) {}

    @Override
    public void w(String tag, String msg, Throwable t) {}

    @Override
    public void d(String tag, String msg) {}

    @Override
    public void d(String tag, String msg, Throwable t) {}

    @Override
    public void e(String tag, String msg) {}

    @Override
    public void e(String tag, String msg, Throwable t) {}
}
