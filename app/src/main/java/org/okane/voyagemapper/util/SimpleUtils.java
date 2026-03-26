package org.okane.voyagemapper.util;

import androidx.annotation.Nullable;

public class SimpleUtils {

    public static boolean isEmpty(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }
}
