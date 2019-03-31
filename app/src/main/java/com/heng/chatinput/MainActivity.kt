package com.heng.chatinput

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import java.io.File

class MainActivity : AppCompatActivity(), ChatInputListener {
  lateinit var toast: Toast
  override fun sendText(
    text: String,
    userIds: List<String>?
  ) {
    toast.setText(text)
    toast.show()
  }

  override fun onChangeModel(inputModel: InputModel) {
    toast.setText(inputModel.name)
    toast.show()
  }

  override fun onMoreModule(moreModule: MoreModuleBean) {
    toast.setText(moreModule.name)
    toast.show()
  }

  override fun sendAudio(
    file: File,
    recordTime: Long
  ) {
    toast.setText(file.absolutePath)
    toast.show()
  }

  override fun toAtMember() {
    Toast.makeText(this, "toAtMember", Toast.LENGTH_SHORT)
        .show()
  }

  private lateinit var chatEditor: ChatEditor
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
    val recyclerView = findViewById<RecyclerView>(R.id.rcview)
    val inputView = findViewById<ChatInputView>(R.id.input_view)
    chatEditor = ChatEditor.Build(inputView, recyclerView)
        .canAt(true)
        .bindMoreModules(
            listOf(
                MoreModuleBean("拨打电话", R.mipmap.keyboard_call_normal),
                MoreModuleBean("语音聊天", R.mipmap.keyboard_call_normal)
            )
        )
        //.customEmojiFragmentFactory()  自定义 emoji 面板
        //.customInputAudioProcessor()   自定义录音手势处理器
        // .customRecorderManager()      自定义语音录制管理器
        //.customMoreFragmentFactory()   自定义更多 面板
        // .disableEmoji()               禁用 emoji
        //.disableMore()                 禁用更多模式
        //.disableVoice()                禁用语音录制
        .bindListener(this) //绑定事件回调
        .builder()
  }

}

