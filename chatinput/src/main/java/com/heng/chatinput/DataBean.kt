package com.heng.chatinput

import android.support.annotation.DrawableRes
import com.iyao.eastat.span.DataBindingSpan

data class User(
  val id: String,
  var name: String
) : DataBindingSpan

open class MoreModuleBean(var name: String, @DrawableRes var resId: Int)