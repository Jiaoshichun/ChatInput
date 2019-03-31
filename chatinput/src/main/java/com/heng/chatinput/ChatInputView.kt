package com.heng.chatinput

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.support.annotation.AttrRes
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.heng.chatinput.InputModel.MODEL_KEYBOARD
import com.heng.chatinput.InputModel.MODEL_NONE
import com.heng.chatinput.utils.AtMemberManager
import com.heng.chatinput.utils.Utils

enum class InputModel {
  MODEL_KEYBOARD,//键盘模式
  MODEL_NONE,//非输入模式模式
  MODEL_EMOJI,//表情模式
  MODEL_MORE//更多模式
}

class ChatInputView(
  context: Context,
  attrs: AttributeSet?,
  @AttrRes defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr),
    ChatInputMoreFragment.MoreListener,
    EmotionKeyboard.Listener,
    EmojiListener {

  constructor(
    context: Context,
    attrs: AttributeSet
  ) : this(context, attrs, 0)

  constructor(context: Context) : this(context, null, 0)

  private var edtText: EditText
  private var imgEmoji: ImageView
  private var imgMore: ImageView
  private var isEnableMore = true //更多模块是否能用
  private var fLayoutEmoji: FrameLayout
  private var txtSend: TextView
  private var txtHoldSay: TextView
  private var imgVoice: ImageView
  var currentModel = InputModel.MODEL_NONE;//默认键盘模式
  var emotionKeyboard: EmotionKeyboard
  var chatInputListener: ChatInputListener? = null
  internal lateinit var emojiFragmentFactory: EmojiFragmentFactory
  internal lateinit var moreFragmentFactory: MoreFragmentFactory
  private var emojiFragment: Fragment? = null
  private var moreFragment: Fragment? = null
  private var isVoice = false//当前是否是语音模式
  internal var canAt = false
  private val emojiIcon: Int
  private val keyboardIcon: Int
  private val audioIcon: Int
  private val moreIcon: Int

  init {
    val typedArray = context.resources.obtainAttributes(attrs, R.styleable.ChatInputView)

    emojiIcon =
      typedArray.getResourceId(R.styleable.ChatInputView_emoji_icon, R.drawable.keyboard_emoji)
    keyboardIcon =
      typedArray.getResourceId(R.styleable.ChatInputView_keyboard_icon, R.drawable.keyboard_normal)
    audioIcon =
      typedArray.getResourceId(R.styleable.ChatInputView_audio_icon, R.drawable.keyboard_audio)
    moreIcon =
      typedArray.getResourceId(R.styleable.ChatInputView_more_icon, R.drawable.keyboard_add)
    val maxLength = typedArray.getInt(R.styleable.ChatInputView_edt_max_length, 500)
    val btnSendTextColor =
      typedArray.getColor(R.styleable.ChatInputView_btn_send_text_color, Color.WHITE)
    val btnSendBg = typedArray.getResourceId(
        R.styleable.ChatInputView_btn_send_bg, R.drawable.bg_blue_round5_selector
    )
    val edtTextColor = typedArray.getColor(
        R.styleable.ChatInputView_edt_text_color, resources.getColor(R.color.gray_44)
    )

    typedArray.recycle()
    val view = LayoutInflater.from(context)
        .inflate(R.layout.view_chat_input, this)
    edtText = findViewById(R.id.edt_text)
    edtText.setTextColor(edtTextColor)
    imgEmoji = findViewById(R.id.img_emoji)
    imgEmoji.setImageResource(emojiIcon)
    imgEmoji.setOnClickListener { clickEmoji() }
    imgMore = findViewById(R.id.img_more)
    imgMore.setOnClickListener { clickMore() }
    fLayoutEmoji = findViewById(R.id.flayout_emoji)
    txtSend = findViewById(R.id.txt_send)
    txtSend.setTextColor(btnSendTextColor)
    txtSend.setBackgroundResource(btnSendBg)
    txtSend.setOnClickListener { sendText() }
    txtHoldSay = findViewById(R.id.txt_hold_say)
    imgVoice = findViewById(R.id.img_voice)
    imgVoice.setImageResource(audioIcon)
    imgVoice.setOnClickListener { onVoice() }
    view.layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    edtText.setOnEditorActionListener { v, actionId, event ->
      if (actionId == EditorInfo.IME_ACTION_SEND
          || actionId == EditorInfo.IME_ACTION_DONE
          || (event != null && KeyEvent.KEYCODE_ENTER == event.keyCode && KeyEvent.ACTION_DOWN == event.action)
      ) {
        edtText.append("\n")
        true
      } else {
        false
      }
    }
    AtMemberManager.init(edtText)
    txtSend.isEnabled = false
    edtText.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        changeSendAndMore()
      }

      override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
      ) {

      }

      override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
      ) {
      }
    })
    edtText.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
      if (canAt && "@" == source) {
        chatInputListener?.toAtMember()
      }
      null
    })
    Utils.limitEditTextLength(edtText, maxLength) {
      Toast.makeText(context.applicationContext, "文字超过最大长度", Toast.LENGTH_SHORT)
          .show()
    }
    emotionKeyboard = EmotionKeyboard.with(context as Activity)
        .bindEmotionView(fLayoutEmoji)
        .bindToEditText(edtText)
        .bindListener(this)
        .build()

    //拦截返回键
    edtText.setOnKeyListener { v, keyCode, event ->
      if (keyCode == KeyEvent.KEYCODE_BACK
          && event.action == KeyEvent.ACTION_UP
      ) {
        return@setOnKeyListener (emotionKeyboard.interceptBackPress()
            || currentModel == MODEL_KEYBOARD).apply {
          switchModel(InputModel.MODEL_NONE)
        }
      }
      return@setOnKeyListener false
    }
  }

  internal fun getHoldSayView() = txtHoldSay
  /**
   * 变化发送和更多按钮状态
   */
  private fun changeSendAndMore() {
    if (isEnableMore) {
      if (edtText.text?.isEmpty() != false) {
        imgMore.visibility = View.VISIBLE
        txtSend.visibility = View.GONE
      } else {
        imgMore.visibility = View.GONE
        txtSend.visibility = View.VISIBLE
      }
    }
    txtSend.isEnabled = edtText.text?.isEmpty() == false
  }

  //切换语音模式
  private fun switchVoice(isVoice: Boolean) {
    if (this.isVoice == isVoice) return
    this.isVoice = isVoice
    imgVoice.setImageResource(if (isVoice) keyboardIcon else audioIcon)
    edtText.visibility = if (isVoice) View.GONE else View.VISIBLE
    txtHoldSay.visibility = if (isVoice) View.VISIBLE else View.GONE

  }

  //设置输入键盘上面的布局  键盘和表情布局切换时锁定 内容布局防止跳动
  fun setContentView(contentView: View) {
    emotionKeyboard.bindToContent(contentView)
  }

  //切换语音文字模式
  private fun onVoice() {
    switchVoice(!isVoice)
    if (isVoice) {
      switchModel(InputModel.MODEL_NONE)
      if (isEnableMore) {
        imgMore.visibility = View.VISIBLE
        txtSend.visibility = View.GONE
      }
    } else {
      switchModel(InputModel.MODEL_KEYBOARD)
      if (!TextUtils.isEmpty(edtText.text) && isEnableMore) {
        imgMore.visibility = View.VISIBLE
        txtSend.visibility = View.GONE
      }
    }
  }

  //发送文字
  private fun sendText() {
    edtText.text?.toString()
        ?.let { s ->
          chatInputListener?.sendText(s,
              edtText.text.getSpans(0, edtText.length(), User::class.java).map { it.id })
          edtText.setText("")
        }
  }

  //点击emoji图标
  private fun clickEmoji() {
    if (isVoice) {
      switchVoice(false)
      if (!TextUtils.isEmpty(edtText.text) && isEnableMore) {
        imgMore.visibility = View.GONE
        txtSend.visibility = View.VISIBLE
      }
    }
    if (currentModel != InputModel.MODEL_EMOJI) {
      switchModel(InputModel.MODEL_EMOJI)
    } else {
      switchModel(InputModel.MODEL_KEYBOARD)
    }
  }

  //点击加号图标
  private fun clickMore() {
    if (isVoice) {
      switchVoice(false)
    }
    if (currentModel != InputModel.MODEL_MORE) {
      switchModel(InputModel.MODEL_MORE)
    } else {
      switchModel(InputModel.MODEL_KEYBOARD)
    }
  }

  override fun setEnabled(enabled: Boolean) {
    if (!enabled) {
      switchModel(MODEL_NONE)
      edtText.isCursorVisible = false
    }
    super.setEnabled(enabled)
  }

  override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    if (!isEnabled) return false
    return super.dispatchTouchEvent(ev)
  }

  /**
   * 切换模式
   */
  internal fun switchModel(model: InputModel) {
    if (currentModel == model) return
    //避免快速点击切换模式时造成的跳动
    imgMore.isEnabled = false
    imgEmoji.isEnabled = false
    imgMore.postDelayed({
      imgMore.isEnabled = true
      imgEmoji.isEnabled = true
    }, 100)

    val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
    currentModel = model
    imgEmoji.setImageResource(
        if (currentModel == InputModel.MODEL_EMOJI) keyboardIcon else emojiIcon
    )
    edtText.isCursorVisible = true
    when (currentModel) {
      InputModel.MODEL_EMOJI -> {//发送表情模式
        edtText.requestFocus()
        emotionKeyboard.showEmojiView()
        if (emojiFragment == null) {
          emojiFragment = emojiFragmentFactory.create(this)
          transaction.add(R.id.flayout_emoji, emojiFragment!!)
        }
        transaction.show(emojiFragment!!)
        if (moreFragment != null) {
          transaction.hide(moreFragment!!)
        }
        postDelayed({ chatInputListener?.onChangeModel(currentModel) }, 50)
      }
      InputModel.MODEL_MORE -> {//更多模式
        edtText.isCursorVisible = false
        emotionKeyboard.showEmojiView()
        if (moreFragment == null) {
          moreFragment = moreFragmentFactory.create(
              this,
              moreModuleList ?: arrayListOf()
          )
          transaction.add(R.id.flayout_emoji, moreFragment!!)
        }
        transaction.show(moreFragment!!)
        if (emojiFragment != null) {
          transaction.hide(emojiFragment!!)
        }
        postDelayed({ chatInputListener?.onChangeModel(currentModel) }, 50)
      }
      InputModel.MODEL_KEYBOARD -> {//展示键盘模式
        changeSendAndMore()
        emotionKeyboard.showKeyBoard()
        if (moreFragment != null) {
          transaction.hide(moreFragment!!)
        }
        if (emojiFragment != null) {
          transaction.hide(emojiFragment!!)
        }
        postDelayed({ chatInputListener?.onChangeModel(currentModel) }, 200)
      }
      InputModel.MODEL_NONE -> {//隐藏键盘模式
        emotionKeyboard.hideSoftInput()
        emotionKeyboard.onlyHideEmotion()
        if (moreFragment != null) {
          transaction.hide(moreFragment!!)
        }
        if (emojiFragment != null) {
          transaction.hide(emojiFragment!!)
        }
        chatInputListener?.onChangeModel(currentModel)
      }
    }
    transaction.commitAllowingStateLoss()
  }

  //输入表情的回调
  override fun inputEmoji(emoji: CharSequence) {
    edtText.editableText.insert(edtText.selectionStart, emoji)
  }

  //删除文字的回调
  override fun delete() {
    edtText.apply {
      this.onKeyDown(
          KeyEvent.KEYCODE_DEL,
          KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
      )
    }
        .onKeyUp(KeyEvent.KEYCODE_DEL, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
  }

  //显示键盘
  override fun showKeyBoard() {
    switchModel(InputModel.MODEL_KEYBOARD)
  }

  //隐藏键盘
  override fun hideKeyBoard() {
    switchModel(InputModel.MODEL_NONE)
  }

  override fun onMoreModule(moreModule: MoreModuleBean) {
    chatInputListener?.onMoreModule(moreModule)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    emotionKeyboard.removeListener()
  }

  //@某个人
  internal fun onAtMember(user: User) {
    if (isVoice) {//如果当前是录音模式 更换为文字输入模式
      switchVoice(false)
    }
    val index = edtText.selectionStart
    if (index > 0 && edtText.text[index - 1] == '@') {
      edtText.text.delete(index - 1, index)
    }
    val builder = edtText.text as SpannableStringBuilder
    val start = builder.subSequence(0, index)
    val end = if (edtText.length() != 0) {
      builder.subSequence(index, edtText.length())
    } else {
      ""
    }
    builder.clear()
    val newSpannable = AtMemberManager.newSpannable(user)
    builder.append(start)
        .append(newSpannable)
        .append(end)
    edtText.setSelection(start.length + newSpannable.length)
    postDelayed({switchModel(MODEL_KEYBOARD)}, 200)
  }

  private var moreModuleList: List<MoreModuleBean>? = null
  /**
   * 设置更多 模块的配置
   */
  internal fun setMoreModuleList(moreModuleList: List<MoreModuleBean>) {
    this.moreModuleList = moreModuleList
    isEnableMore = moreModuleList.isNotEmpty()
    changeSendAndMore()
  }

  /**
   * 禁用语言录制
   */
  internal fun disableVoice() {
    imgVoice.visibility = View.GONE
    switchVoice(false)
  }

  /**
   * 禁用emoji表情
   */
  internal fun disableEmoji() {
    imgEmoji.visibility = View.GONE
    switchModel(MODEL_NONE)
  }

  /**
   * 禁用更多模块
   */
  internal fun disableMore() {
    isEnableMore = false
    changeSendAndMore()
  }

}