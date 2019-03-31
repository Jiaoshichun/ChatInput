package com.msxf.chat.ui.widget.input

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

/**
 * 拦截事件传递
 */
class InterceptLinearLayout(context: Context, attrs: AttributeSet?,
                            @AttrRes defStyleAttr: Int)
    : LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)
    
    var touchListener: TouchListener? = null
    
    interface TouchListener {
        fun onTouch()
    }
    
    var isIntercept = false
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (isIntercept) {
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                touchListener?.onTouch()
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}