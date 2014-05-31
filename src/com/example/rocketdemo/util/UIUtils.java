package com.example.rocketdemo.util;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

public class UIUtils {

    public static void setWidthHeight(final View view, final int width, final int height) {
        if (view == null) {
            return;
        }

        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

    public static void runInUiThread(final Runnable runnable) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

}
