## ChatInput 仿微信输入框
效果如下


+ 支持表情、录音、更多面板的开启和关闭 （默认全部开启） 
+ 支持自定义输入语音逻辑处理器（处理录音手势以及录音浮窗逻辑） customInputAudioProcessor 
+ 支持自定义语音录制管理器（录制语音使用 默认支持 MP3和amr格式录制）  customRecorderManager
+ 支持自定义表情面板  customEmojiFragmentFactory
+ 支持自定义更多模块面板 customMoreFragmentFactory
+ 支持@人功能   canAt

## 使用方式
将ChatInputView放入布局文件中 它的直接父布局必须是LinearLayout  
在Activity的onCreate中初始化ChatEditor  
```
  // 第一个参数是ChatInputView 第二个参数是与ChatInputView放入同一个LinearLayout 权重为1的view
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

```  
目前支持的事件回调有以下几种  
```
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
```
支持的自定义属性如下：  

|名称| 功能 |
| ------ | ------ |
| keyboard_icon | 键盘图标 | 
| emoji_icon | emoji图标 | 
| audio_icon | 语音图标 | 
| more_icon | 更多图标 | 
| edt_text_color | 输入框文字颜色 | 
| edt_max_length | 输入框文字最大长度（默认500） | 
| btn_send_bg | 发送按钮背景 | 
| btn_send_text_color | 发送文字颜色 |   

## 参考及引用  
[emotionkeyboard](https://github.com/shinezejian/emotionkeyboard)  
[easy_at](https://github.com/iYaoy/easy_at)
[AndPermission](https://github.com/yanzhenjie/AndPermission)  
[AndroidMP3Recorder](https://github.com/GavinCT/AndroidMP3Recorder)