package com.heng.chatinput;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;


/**
 * 输入框防止键盘弹出跳动管理类
 * 转自 https://github.com/shinezejian/emotionkeyboard  有改动
 */
public class EmotionKeyboard {

    private static final String SHARE_PREFERENCE_NAME = "EmotionKeyboard";
    private static final String SHARE_PREFERENCE_SOFT_INPUT_HEIGHT = "soft_input_height";

    private Activity mActivity;
    private InputMethodManager mInputManager;//软键盘管理类
    private static SharedPreferences sp;
    private View mEmotionLayout;//表情布局
    private EditText mEditText;//
    private View mContentView;//内容布局view,即除了表情布局或者软键盘布局以外的布局，用于固定bar的高度，防止跳闪
    private final static String TAG = "EmotionKeyboard";
    private double usableHeightPrevious;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

    private EmotionKeyboard() {

    }

    /**
     * 外部静态调用
     *
     */
    public static EmotionKeyboard with(Activity activity) {
        EmotionKeyboard emotionInputDetector = new EmotionKeyboard();
        sp = activity.getApplication().getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        emotionInputDetector.mActivity = activity;
        emotionInputDetector.mInputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        return emotionInputDetector;
    }

    /**
     * 绑定内容view，此view用于固定bar的高度，防止跳闪
     *
     */
    public EmotionKeyboard bindToContent(View contentView) {
        mContentView = contentView;
        onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                possiblyResizeChildOfContent();
            }
        };
        mContentView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        return this;
    }

    public void removeListener() {
        mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    private void possiblyResizeChildOfContent() {
        Rect r = new Rect();
        mContentView.getRootView().getWindowVisibleDisplayFrame(r);
        int usableHeightNow = (r.bottom - r.top);
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = mContentView.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
            if (heightDifference > (usableHeightSansKeyboard / 4)) {
                // 键盘弹出
            } else {
                // 键盘收起
                if (!mEmotionLayout.isShown() && mListener != null) {
                    mListener.hideKeyBoard();
                }
            }
            usableHeightPrevious = usableHeightNow;
        }
    }

    /**
     * 绑定编辑框
     *
     */
    @SuppressLint("ClickableViewAccessibility") public EmotionKeyboard bindToEditText(EditText editText) {
        mEditText = editText;
        mEditText.requestFocus();
        mEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && mEmotionLayout.isShown()) {
                    lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
                    hideEmotionLayout(true);//隐藏表情布局，显示软件盘

                    //软件盘显示后，释放内容高度
                    mEditText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            unlockContentHeightDelayed();
                        }
                    }, 200L);
                }
                if (mListener != null) {
                    mListener.showKeyBoard();
                }
                return false;
            }
        });
        return this;
    }

    private Listener mListener;

    public EmotionKeyboard bindListener(Listener listener) {
        this.mListener = listener;
        return this;
    }


    public interface Listener {
        void showKeyBoard();

        void hideKeyBoard();
    }

    //显示键盘
    public void showKeyBoard() {
        if (mEmotionLayout.isShown()) {
            lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
            hideEmotionLayout(true);//隐藏表情布局，显示软件盘
            unlockContentHeightDelayed();//软件盘显示后，释放内容高度
        } else {
            showSoftInput();
        }
    }

    //显示emoji布局
    public void showEmojiView() {
        if (mEmotionLayout.isShown()) return;
        if (isSoftInputShown()) {
            lockContentHeight();//锁定内容高度，防止跳闪。
            showEmotionLayout();//显示表情布局
            unlockContentHeightDelayed();//解锁内容高度
        } else {
            showEmotionLayout();//两者都没显示，直接显示表情布局
        }
    }

    /**
     * 设置表情内容布局
     *
     */
    public EmotionKeyboard bindEmotionView(View emotionView) {
        mEmotionLayout = emotionView;
        return this;
    }

    public EmotionKeyboard build() {
        //设置软件盘的模式：SOFT_INPUT_ADJUST_RESIZE  这个属性表示Activity的主窗口总是会被调整大小，从而保证软键盘显示空间。
        //从而方便我们计算软件盘的高度
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        //隐藏软件盘
        hideSoftInput();
        return this;
    }

    /**
     * 点击返回键时先隐藏表情布局
     */
    public boolean interceptBackPress() {
        if (mEmotionLayout.isShown()) {
            hideEmotionLayout(false);
            return true;
        }
        return false;
    }

    private void showEmotionLayout() {
        int softInputHeight = getSupportSoftInputHeight();
        if (softInputHeight == 0) {
            softInputHeight = getKeyBoardHeight();
        }
        hideSoftInput();
        mEmotionLayout.getLayoutParams().height = softInputHeight;
        mEmotionLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏表情布局
     *
     * @param showSoftInput 是否显示软件盘
     */
    private void hideEmotionLayout(boolean showSoftInput) {
        if (mEmotionLayout.isShown()) {
            mEmotionLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mEmotionLayout.setVisibility(View.GONE);
                }
            }, 200);
            if (showSoftInput) {
                showSoftInput();
            }
        }
    }

    public void onlyHideEmotion() {
        if (mEmotionLayout.isShown()) {
            mEmotionLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 锁定内容高度，防止跳闪
     */
    private void lockContentHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentView.getLayoutParams();
        params.height = mContentView.getHeight();
        params.weight = 0.0F;
    }

    /**
     * 释放被锁定的内容高度
     */
    private void unlockContentHeightDelayed() {
        mEditText.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((LinearLayout.LayoutParams) mContentView.getLayoutParams()).weight = 1.0F;
            }
        }, 200L);
    }

    /**
     * 编辑框获取焦点，并显示软件盘
     */
    private void showSoftInput() {
        mEditText.requestFocus();
        mEditText.post(new Runnable() {
            @Override
            public void run() {
                mInputManager.showSoftInput(mEditText, 0);
            }
        });
    }

    /**
     * 隐藏软件盘
     */
    public void hideSoftInput() {
        mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    /**
     * 是否显示软件盘
     */
    private boolean isSoftInputShown() {
        return getSupportSoftInputHeight() != 0;
    }

    /**
     * 获取软件盘的高度
     */
    private int getSupportSoftInputHeight() {
        Rect r = new Rect();
        /**
         * decorView是window中的最顶层view，可以从window中通过getDecorView获取到decorView。
         * 通过decorView获取到程序显示的区域，包括标题栏，但不包括状态栏。
         */
        mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        //获取屏幕的高度
        int screenHeight = mActivity.getWindow().getDecorView().getRootView().getHeight();
        //计算软件盘的高度
        int softInputHeight = screenHeight - r.bottom;

        /**
         * 某些Android版本下，没有显示软键盘时减出来的高度总是144，而不是零，
         * 这是因为高度是包括了虚拟按键栏的(例如华为系列)，所以在API Level高于20时，
         * 我们需要减去底部虚拟按键栏的高度（如果有的话）
         */
        if (Build.VERSION.SDK_INT >= 20) {
            // When SDK Level >= 20 (Android L), the softInputHeight will contain the height of softButtonsBar (if has)
            softInputHeight = softInputHeight - getSoftButtonsBarHeight();
        }

        if (softInputHeight < 0) {
            Log.w(TAG, "EmotionKeyboard--Warning: value of softInputHeight is below zero!");
        }
        //存一份到本地
        if (softInputHeight > 0) {
            sp.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, softInputHeight).apply();
        }
        return softInputHeight;
    }


    /**
     * 底部虚拟按键栏的高度
     *
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        //这个方法获取可能不是真实屏幕的高度
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        //获取当前屏幕的真实高度
        mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }

    /**
     * 获取软键盘高度，由于第一次直接弹出表情时会出现小问题，787是一个均值，作为临时解决方案
     */
    public static int getKeyBoardHeight() {
        return sp.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, 787);
    }

}
