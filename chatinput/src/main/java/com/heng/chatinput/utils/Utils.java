package com.heng.chatinput.utils;

import android.app.Activity;
import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.EditText;

public class Utils {
    /**
     * dip转为px
     */
    public static int dip2px(Context context, float dip) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dip * density + 0.5f);
    }

    public static float px2dip(Context context, int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return (float) px / density;
    }

    /**
     * 获取屏幕宽度
     */
    public static int getScreenWidthPixels(Activity context) {
        DisplayMetrics metric = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    /**
     * 获取屏幕高度
     */
    public static int getScreenHeightPixels(Activity context) {
        DisplayMetrics metric = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.heightPixels;
    }

    public static int[] getScreenWidthAndHeight(Context context) {
        int[] ints = new int[2];
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        // 方法1,获取屏幕的默认分辨率
        Display display = manager.getDefaultDisplay(); // getWindowManager().getDefaultDisplay();
        ints[0] = display.getWidth(); // 屏幕宽（像素，如：480px）
        ints[1] = display.getHeight(); // 屏幕高（像素，如：800px）
        return ints;
    }

    /**
     * @param maxLength 最大
     */
    public static void limitEditTextLength(final EditText editText, final int maxLength,
                                           final Runnable runnable) {
        if (editText == null) return;
        // Create a new filter
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                                       int dend) {
                if (dstart != dend) return null;
                try {
                    if (dest.length() + source.length() > maxLength) {
                        runnable.run();
                        int len = maxLength - dest.length();
                        if (len > 0) {
                            return source.subSequence(0, len);
                        }
                        return "";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        InputFilter[] originFilters = editText.getFilters();
        InputFilter[] inputFilters = new InputFilter[originFilters.length + 1];
        System.arraycopy(originFilters, 0, inputFilters, 0, originFilters.length);
        inputFilters[originFilters.length] = filter;
        editText.setFilters(inputFilters);
    }
}
