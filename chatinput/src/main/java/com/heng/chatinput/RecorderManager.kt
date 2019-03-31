package com.heng.chatinput

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import com.czt.mp3recorder.MP3Recorder
import java.io.File

interface IRecorderManager {
  /**
   * 开始录制
   */
  fun start(filePath: File)

  /**
   * 获取语音文件路径
   */
  fun getFilePath(): File?

  /**
   * 获取录制时长
   */
  fun getRecordDuration(): Long

  /**
   * 停止录制
   */
  fun stop()

  /**
   * 当前是否正在录制中
   */
  fun isRecording(): Boolean

  /**
   * 返回音量比例 范围 0 - 1
   */
  fun getVolumeProportion(): Double
}

/**
 *
 * 语音录制  支持录制mp3和amr格式
 */
class RecorderManager(
  val context: Context,
  val recordFormat: Format
) : IRecorderManager {

  constructor(context: Context) : this(context, Format.MP3)

  private var mediaRecorder: MediaRecorder? = null
  private val audioManager: AudioManager =
    context.applicationContext.getSystemService(
        Context.AUDIO_SERVICE
    ) as AudioManager
  private var mp3MediaRecorder: MP3Recorder? = null
  private var filePath: File? = null
  private var startTime = 0L
  private var endTime = 0L

  enum class Format {
    MP3,
    AMR
  }

  private var isRecording = false
  override fun start(filePath: File) {
    this.filePath = filePath
    startTime = System.currentTimeMillis()
    //请求音频焦点  避免正在播放语音时  把语音录入
    audioManager.requestAudioFocus(
        null, AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN
    )
    if (recordFormat == Format.MP3) {
      startMp3(filePath)
    } else if (recordFormat == Format.AMR) {
      startAmr(filePath)
    }
    isRecording = true
  }

  /**
   * 录制mp3
   */
  private fun startMp3(filePath: File) {
    mp3MediaRecorder = MP3Recorder(filePath)
    mp3MediaRecorder?.start()
  }

  override fun isRecording() = isRecording
  /**
   * 录制amr
   */
  private fun startAmr(filePath: File) {
    mediaRecorder = MediaRecorder()
    mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
    mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
    mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
    mediaRecorder?.setOutputFile(filePath.absolutePath)
    mediaRecorder?.prepare()
    mediaRecorder?.start()
  }

  /**
   * 获取文件路径
   */
  override fun getFilePath(): File? {
    return filePath
  }

  /**
   * 获取录制时长
   */
  override fun getRecordDuration(): Long {
    return if (isRecording) {
      System.currentTimeMillis() - startTime
    } else {
      Math.max(endTime - startTime, 1)
    }
  }

  override fun stop() {
    endTime = System.currentTimeMillis()
    //释放音频焦点
    audioManager.abandonAudioFocus(null)
    if (recordFormat == Format.MP3) {
      stopMp3()
    } else if (recordFormat == Format.AMR) {
      stopAmr()
    }
    isRecording = false
  }

  private fun stopMp3() {
    mp3MediaRecorder?.stop()
    mp3MediaRecorder = null
  }

  private fun stopAmr() {
    mediaRecorder?.stop()
    mediaRecorder?.reset()
    mediaRecorder?.release()
    mediaRecorder = null
  }

  override fun getVolumeProportion(): Double {
    return if (recordFormat == Format.AMR) {
      getAmrVolume()
    } else {
      getMp3Volume()
    }

  }

  private fun getMp3Volume(): Double {
    return (mp3MediaRecorder?.volume?.toDouble() ?: 0.0) / (mp3MediaRecorder?.maxVolume ?: 1)
  }

  private fun getAmrVolume(): Double {
    val amplitude = mediaRecorder?.maxAmplitude ?: 0
    var volume = 0.0
    if (amplitude > 1) {
      volume = 20 * Math.log10(amplitude.toDouble()) / 100
    }
    return volume
  }
}