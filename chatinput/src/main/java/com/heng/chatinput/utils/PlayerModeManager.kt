package com.heng.chatinput.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.preference.PreferenceManager

/**
 * 播放模式管理类
 * 仿微信 支持语音消息的听筒和外放模式切换切换
 * 非耳机模式时  支持听筒和外放切换
 * 耳机模式时 切换模式 只会存储当前模式 并不会对当前模式进行切换
 *
 */
@SuppressLint("StaticFieldLeak")
object PlayerModeManager {
  enum class PlayMode {
    Speaker,//外放
    Headset,//耳机
    Receiver//听筒
  }

  private const val AUDIO_PLAY_IS_SPEAKER_ON = "audio_play_is_speaker_on"
  private  var defaultIsOpenSpeaker = true//默认外放模式
  private var playMode = PlayerModeManager.PlayMode.Receiver
  private lateinit var context: Context
  fun init(application: Context,defaultIsOpenSpeaker:Boolean=true) {
    context = application.applicationContext
    this.defaultIsOpenSpeaker=defaultIsOpenSpeaker
    playMode =
      if (isSpeakerOn()) PlayerModeManager.PlayMode.Speaker else PlayerModeManager.PlayMode.Receiver
    val intentFilter = IntentFilter()
    intentFilter.addAction(Intent.ACTION_HEADSET_PLUG)
    //监听耳机的插拔
    context.registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?
      ) {
        if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
          val state = intent.getIntExtra("state", 0)
          //耳机插入
          if (state == 1) {
            playMode = PlayerModeManager.PlayMode.Headset
            changeMode()
          } else if (state == 0) {//耳机拔出
            playMode =
              if (isSpeakerOn()) PlayerModeManager.PlayMode.Speaker else PlayerModeManager.PlayMode.Receiver
            changeMode()
          }

        }
      }
    }, intentFilter)
  }

  /**
   *  判断 当前是否是扬声器模式
   *  （注：当前只是记录值 可能也是耳机模式 耳机模式下允许切换模式 但是只有拔下耳机时才生效）
   */
  fun isSpeakerOn(): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(AUDIO_PLAY_IS_SPEAKER_ON, defaultIsOpenSpeaker)
  }

  /**
   *   设置播放模式
   *   @param isSpeaker  true 外放模式   false听筒模式
   */
  fun setSpeakerOn(isSpeaker: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean(AUDIO_PLAY_IS_SPEAKER_ON, isSpeaker)
        .apply()
    if (playMode != PlayerModeManager.PlayMode.Headset) {//当前没有插耳机时 才会切换模式
      playMode =
        if (isSpeaker) PlayerModeManager.PlayMode.Speaker else PlayerModeManager.PlayMode.Receiver
      changeMode()
    }
  }

  /**
   * 切换到外放
   */
  private fun changeToSpeaker() {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.mode = AudioManager.MODE_NORMAL
    audioManager.isSpeakerphoneOn = true
  }

  /**
   * 切换到耳机模式
   */
  private fun changeToHeadset() {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.isSpeakerphoneOn = false
  }

  /**`
   * 切换到听筒
   */
  private fun changeToReceiver() {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    audioManager.isSpeakerphoneOn = false
  }

  /**
   * 更换模式  只有真正播放时才会切换模式
   */
  private fun changeMode() {
    if (!isPlaying) return
    when (playMode) {
      PlayerModeManager.PlayMode.Receiver -> changeToReceiver()
      PlayerModeManager.PlayMode.Speaker -> changeToSpeaker()
      PlayerModeManager.PlayMode.Headset -> changeToHeadset()
    }
  }

  /**
   * 判断当前是否是听筒模式
   * （注：播放时切换到听筒模式 前1-2s 可能无声音 建议 在听筒模式下延迟1-2s播放）
   */
  fun isReceiver(): Boolean {
    return playMode == PlayerModeManager.PlayMode.Receiver
  }

  private var isPlaying = false
  /**
   * 播放时语音时 调用该方法 屏蔽第三方音乐 同时使当前播放模式生效
   */
  fun onPlay() {
    isPlaying = true
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.requestAudioFocus(
        null, AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN
    )
    changeMode()
  }

  /**
   * 语音停止播放时 调用该方法 恢复第三方音乐播放  恢复播放模式
   */
  fun onStop() {
    isPlaying = false
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.abandonAudioFocus(null)
    audioManager.mode = AudioManager.MODE_NORMAL
  }

}