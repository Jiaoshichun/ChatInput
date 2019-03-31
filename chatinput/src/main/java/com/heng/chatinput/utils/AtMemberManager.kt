package com.heng.chatinput.utils

import android.text.Spannable
import android.view.KeyEvent
import android.widget.EditText
import com.heng.chatinput.User
import com.iyao.eastat.KeyCodeDeleteHelper
import com.iyao.eastat.NoCopySpanEditableFactory
import com.iyao.eastat.SpanFactory
import com.iyao.eastat.span.DataBindingSpan
import com.iyao.eastat.watcher.SelectionSpanWatcher

/**
 * @ 人 工具类
 * https://github.com/iYaoy/easy_at
 */
object AtMemberManager {
    fun init(editText: EditText) {
        editText.text = null
        editText.setEditableFactory(NoCopySpanEditableFactory(
            SelectionSpanWatcher(
                DataBindingSpan::class)))
        editText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                KeyCodeDeleteHelper.onDelDown((v as EditText).text)
            }
            return@setOnKeyListener false
        }
    }
    
    fun newSpannable(user: User): Spannable {
        return SpanFactory.newSpannable("@${user.name} ", user)
    }
    
}

