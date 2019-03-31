package com.heng.chatinput

import java.io.File

interface ChatInputListener {
  //发送文本消息
  fun sendText(
    text: String,
    userIds: List<String>?
  )

  //输入框模式改变时的回调
  fun onChangeModel(inputModel: InputModel)

  //点击更多模块中的tab的回调
  fun onMoreModule(moreModule: MoreModuleBean)

  //发送语音
  fun sendAudio(
    file: File,
    recordTime: Long
  )
  //@人的回调
  fun toAtMember()
}
abstract class ChatInputListenerAdapter:ChatInputListener{
  override fun sendText(
    text: String,
    userIds: List<String>?
  ) {

  }

  override fun onChangeModel(inputModel: InputModel) {

  }

  override fun onMoreModule(moreModule: MoreModuleBean) {

  }

  override fun sendAudio(
    file: File,
    recordTime: Long
  ) {

  }

  override fun toAtMember() {

  }

}