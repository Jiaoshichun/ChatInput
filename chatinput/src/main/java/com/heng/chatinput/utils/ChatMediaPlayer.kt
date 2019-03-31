package com.heng.chatinput.utils

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * 聊天室语音消息播放工具类
 */
class ChatMediaPlayer {
  private val mediaPlayer = MediaPlayer()
  private lateinit var playerListener: PlayerListener

  interface PlayerListener {
    fun onPlayerStop()
  }

  private enum class State {
    PREPARING,//准备中
    PLAYING,//播放中
    STOP//停止中
  }

  private var hasStop = false
  private val handler = Handler(Looper.getMainLooper())
  private var currentState = State.STOP
  fun onCreate(listener: PlayerListener) {
    playerListener = listener
    mediaPlayer.setOnPreparedListener {
      if (hasStop) {
        mediaPlayer.stop()
        currentState = State.STOP
        playerListener.onPlayerStop()
      } else {
        mediaPlayer.start()
        currentState = State.PLAYING
      }
    }
    mediaPlayer.setOnCompletionListener {
      currentState = State.STOP
      listener.onPlayerStop()
      PlayerModeManager.onStop()
    }
    mediaPlayer.setOnErrorListener { _, what, extra ->
      currentState = State.STOP
      PlayerModeManager.onStop()
      false
    }
  }

  fun play(url: String) {
    if (currentState == State.PREPARING) {
      return
    }
    if (currentState == State.PLAYING) {
      mediaPlayer.stop()
    }
    currentState = State.PREPARING
    PlayerModeManager.onPlay()
    if (PlayerModeManager.isReceiver()) {//听筒时延迟1S中播放
      handler.postDelayed({
        toPlay(url)
      }, 1200)
    } else {
      toPlay(url)
    }
  }

  //去播放
  private fun toPlay(filePath: String) {
    mediaPlayer.reset()
    mediaPlayer.setDataSource(filePath)
    mediaPlayer.prepareAsync()
    hasStop = false
  }

  fun stop() {
    if (currentState == State.STOP) return
    if (currentState == State.PREPARING) {
      hasStop = true
    } else if (currentState == State.PLAYING) {
      mediaPlayer.stop()
    }
    playerListener.onPlayerStop()
    PlayerModeManager.onStop()
  }

  fun isPlaying(): Boolean {
    return currentState != State.STOP
  }

  fun onDestroy() {
    mediaPlayer.reset()
    mediaPlayer.release()
  }

}