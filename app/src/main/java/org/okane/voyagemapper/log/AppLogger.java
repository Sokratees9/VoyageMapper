package org.okane.voyagemapper.log;

public interface AppLogger {
    void w(String tag, String msg);
    void w(String tag, String msg, Throwable t);
    void d(String tag, String msg);
    void d(String tag, String msg, Throwable t);
    void e(String tag, String msg);
    void e(String tag, String msg, Throwable t);
}
