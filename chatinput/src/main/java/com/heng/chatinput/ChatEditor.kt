package com.heng.chatinput

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.heng.chatinput.InputAudioManager.AudioListener
import com.heng.chatinput.RecorderManager.Format
import java.io.File

class ChatEditor private constructor(
    private val inputView: ChatInputView
) {
    /**
     * @人操作  选择人员后 调用该方法 使输入框增加@人员
     */
    fun onAtMember(user: User) {
        inputView.onAtMember(user)
    }

    /**
     * 是否支持@人操作
     */
    fun canAt(canAt: Boolean) {
        inputView.canAt = canAt
    }

    /**
     * 更换模式
     */
    fun switchModel(model: InputModel) {
        inputView.switchModel(model)
    }

    /**
     * 设置输入框是否可用
     */
    fun setEnabled(isEnabled: Boolean) {
        inputView.isEnabled = isEnabled
    }

    class Build constructor(
        private val inputView: ChatInputView,
        contentView: View
    ) {
        init {
            if (contentView.parent != inputView.parent ||
                inputView.parent !is LinearLayout ||
                (inputView.parent as LinearLayout).orientation != LinearLayout.VERTICAL
            ) {
                throw RuntimeException("inputView和contentView必须同在一个LinearLayout中 并且LinearLayout方向是VERTICAL")
            }
            inputView.setContentView(contentView)
        }

        private var canAt = false
        private var chatInputListener: ChatInputListener? = null
        private var moreModuleList: List<MoreModuleBean>? = null
        private var disableVoice = false
        private var disableEmoji = false
        private var disableMore = false
        private var emojiFragmentFactory: EmojiFragmentFactory = DefaultEmojiFragmentFactory()
        private var moreFragmentFactory: MoreFragmentFactory = DefaultMoreFragmentFactory()
        //按住讲话的逻辑处理器
        private var inputAudioProcessor: ((holdSayView: TextView, recorderManager: IRecorderManager, listener: AudioListener) -> Unit) =
            { holdSayView, recorderManager, listener ->
                //语音录制管理器
                InputAudioManager(holdSayView, recorderManager, listener)
            }
        private var customRecorderManager: IRecorderManager? = null
        //语音录制模式
        private var recordFormat = Format.MP3

        /**
         * 是否支持@人功能
         */
        fun canAt(isCan: Boolean): Build {
            canAt = isCan
            return this
        }

        /**
         * 绑定更多模块内容
         */
        fun bindMoreModules(list: List<MoreModuleBean>): Build {
            moreModuleList = list
            return this
        }

        /**
         * 绑定监听器
         */
        fun bindListener(chatInputListener: ChatInputListener): Build {
            this.chatInputListener = chatInputListener
            return this
        }

        /**
         * 设置语言无法使用  默认可以使用
         */
        fun disableVoice(): Build {
            disableVoice = true
            return this
        }

        /**
         * 禁用emoji
         */
        fun disableEmoji(): Build {
            disableEmoji = true
            return this
        }

        /**
         * 禁用更多功能
         */
        fun disableMore(): Build {
            disableMore = true
            return this
        }

        /**
         * 自定义按住讲话的逻辑处理器
         * 如果不设置则使用默认的处理器
         */
        fun customInputAudioProcessor(
            audioProcessor: (
                holdSayView: View,
                recorderManager: IRecorderManager,
                listener: AudioListener
            ) -> Unit
        ): Build {
            inputAudioProcessor = audioProcessor
            return this
        }

        /**
         * 自定义语音录制管理器  不设置则使用默认的
         */
        fun customRecorderManager(recorderManager: IRecorderManager): Build {
            this.customRecorderManager = recorderManager
            return this
        }

        /**
         * 自定义emoji 展示样式
         */
        fun customEmojiFragmentFactory(emojiFragmentFactory: EmojiFragmentFactory): Build {
            this.emojiFragmentFactory = emojiFragmentFactory
            return this
        }

        /**
         * 自定义更多模块 展示样式
         */
        fun customMoreFragmentFactory(moreFragmentFactory: MoreFragmentFactory): Build {
            this.moreFragmentFactory = moreFragmentFactory
            return this
        }

        /**
         * 设置语言录制格式 支持mp3和amar
         * 如果设置自定义的语音录制管理器 该设置无效
         */
        fun setRecorderFomat(format: Format): Build {
            recordFormat = format
            return this
        }

        fun builder(): ChatEditor {
            val chatEditor = ChatEditor(inputView)
            //设置事件回调
            inputView.chatInputListener = chatInputListener
            //如果有更多模块 设置更多模块配置
            if (moreModuleList != null) {
                inputView.setMoreModuleList(moreModuleList!!)
            }
            //设置是否支持@ 人操作
            chatEditor.canAt(canAt)
            if (disableEmoji) {//如果emoji不支持 禁止emoji
                inputView.disableEmoji()
            }
            if (disableVoice) {//如果不支持录制 禁止录制
                inputView.disableVoice()
            }
            if (disableMore) {//禁用更多功能
                inputView.disableMore()
            }
            //如果自定义的语音录制管理器为空 则使用默认的管理器
            if (customRecorderManager == null) {
                customRecorderManager = RecorderManager(inputView.context, recordFormat)
            }
            //调用语音录制处理器
            inputAudioProcessor.invoke(
                inputView.getHoldSayView(), customRecorderManager!!, object : AudioListener {
                    override fun sendAudio(
                        file: File,
                        recordTime: Long
                    ) {
                        chatInputListener?.sendAudio(file, recordTime)
                    }
                })
            inputView.emojiFragmentFactory = emojiFragmentFactory
            inputView.moreFragmentFactory = moreFragmentFactory
            return chatEditor
        }
    }
}