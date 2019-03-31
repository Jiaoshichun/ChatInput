package com.heng.chatinput

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Message
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.heng.chatinput.utils.PermissionUtil
import com.heng.chatinput.utils.StorageUtils
import com.heng.chatinput.utils.Utils
import java.io.File
import java.io.FileInputStream

private const val START_RECORD = 101
private const val CANCEL_RECORD = 102
private const val END_RECORD = 103
private const val UPDATE_VOLUME = 104

@SuppressLint("ClickableViewAccessibility")
class InputAudioManager(
  holdSayView: TextView,
  val mRecorder: IRecorderManager,
  val listener: AudioListener
) : OnTouchListener {

  private val activity = holdSayView.context as Activity
  private val floatingViewTag = "chat.record.floating"
  private var recordFile: File? = null
  private val audioVoiceImageRes = arrayListOf(
      R.drawable.ic_audio_voice1,
      R.drawable.ic_audio_voice2,
      R.drawable.ic_audio_voice3,
      R.drawable.ic_audio_voice4,
      R.drawable.ic_audio_voice5,
      R.drawable.ic_audio_voice6
  )

  interface AudioListener {
    fun sendAudio(
      file: File,
      recordTime: Long
    )
  }

  private var isLeaveCancel = false//当前是否是松手取消发送状态

  init {
    holdSayView.setOnTouchListener(this)
  }

  private val handler = @SuppressLint("HandlerLeak")
  object : Handler() {
    override fun handleMessage(msg: Message) {
      when (msg.what) {
        START_RECORD -> {
          if (mRecorder.isRecording()) {
            mRecorder.stop()
          }
          getFloatingView().visibility = View.VISIBLE
          getFloatingView().setImageResource(audioVoiceImageRes[0])
          isLeaveCancel = false
          recordFile = File(
              StorageUtils.getCacheDir(activity),
              "audio${System.currentTimeMillis()}"
          )
          mRecorder.start(recordFile!!)
          sendEmptyMessage(UPDATE_VOLUME)
        }
        CANCEL_RECORD -> {
          getFloatingView().visibility = View.GONE
          mRecorder.stop()
          removeMessages(UPDATE_VOLUME)
          recordFile?.delete()
        }
        END_RECORD -> {
          if (!mRecorder.isRecording()) return
          getFloatingView().visibility = View.GONE
          mRecorder.stop()
          removeMessages(UPDATE_VOLUME)

          if (recordFile == null ||
              FileInputStream(recordFile).available() < 512 ||
              mRecorder.getRecordDuration() < 1000
          ) {
            Toast.makeText(activity.applicationContext, "录音时间太短", Toast.LENGTH_SHORT)
                .show()
            recordFile?.delete()
            return
          }
          if (recordFile != null) {
            listener.sendAudio(
                recordFile!!,
                (mRecorder.getRecordDuration() + 500) / 1000
            )
          }
        }
        UPDATE_VOLUME -> {
          //最大录制60秒钟
          if (mRecorder.getRecordDuration() >= 60 * 1000) {
            isPress = false
            holdSayView.isSelected = false
            holdSayView.setText(R.string.tab_hold_say)
            sendEmptyMessage(END_RECORD)
            return
          }
          if (mRecorder.isRecording()) {
            if (!isLeaveCancel) {
              val volumeScale = mRecorder.getVolumeProportion()
              val pos = when {
                volumeScale < 0.2 -> 0
                volumeScale < 0.4 -> 1
                volumeScale < 0.5 -> 2
                volumeScale < 0.6 -> 3
                volumeScale < 0.8 -> 4
                else -> 5
              }
              getFloatingView().setImageResource(audioVoiceImageRes[pos])
            }
            sendEmptyMessageDelayed(UPDATE_VOLUME, 50)
          }
        }
      }
    }
  }

  /**
   * 获取录音悬浮view
   */
  private fun getFloatingView(): ImageView {
    val contentView = activity.findViewById<FrameLayout>(android.R.id.content)
    var view = contentView.findViewWithTag<ImageView>(floatingViewTag)
    if (view == null) {
      view = ImageView(activity).apply {
        this.tag = floatingViewTag
      }
      contentView.addView(view,
          FrameLayout.LayoutParams(
              Utils.dip2px(activity, 150f), Utils.dip2px(activity, 150f)
          ).apply {
            this.gravity = Gravity.CENTER
          })

    }
    return view
  }

  private var isPress = false//按住讲话 按钮是否在按下状态
  /**
   * 处理按住说话的各种状态
   */
  override fun onTouch(
    v: View,
    event: MotionEvent
  ): Boolean {
    if (v !is TextView) return false
    PermissionUtil.requestPermission(
        activity, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO, callBack = null
    )
    //按住讲话 按钮 是否可见  是否可用 是否有录制和存储权限
    if (v.visibility != View.VISIBLE
        || !v.isEnabled
        || !PermissionUtil.hasPermisssion(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    )
      return false

    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        isPress = true
        v.isSelected = true
        v.setText(R.string.tab_hold_say_release)
        if (!handler.hasMessages(START_RECORD)) {
          handler.sendEmptyMessageDelayed(START_RECORD, 200)
        }
      }
      MotionEvent.ACTION_MOVE -> {
        if (!isPress) return false
        if (!handler.hasMessages(START_RECORD)) {
          if (event.rawY < Utils.getScreenHeightPixels(activity) * 3 / 4) {
            v.setText(R.string.tab_hold_say_cancel)
            isLeaveCancel = true
            getFloatingView().setImageResource(R.drawable.ic_audio_cancel)
          } else {
            isLeaveCancel = false
            v.setText(R.string.tab_hold_say_release)

          }
        }
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        if (!isPress) return false
        isPress = false
        v.isSelected = false
        v.setText(R.string.tab_hold_say)
        if (handler.hasMessages(START_RECORD)) {
          handler.removeMessages(START_RECORD)
          return true
        }
        v.isEnabled = false
        handler.postDelayed({ v.isEnabled = true }, 200)

        if (event.rawY < Utils.getScreenHeightPixels(activity) * 3 / 4) {
          handler.sendEmptyMessage(CANCEL_RECORD)
        } else {
          handler.sendEmptyMessage(END_RECORD)
        }
      }
    }
    return true
  }

}