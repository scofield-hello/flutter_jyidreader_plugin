package com.chuangdun.flutter.plugin.JyIDReader

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import com.zkteco.android.IDReader.IDCardRead
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo
import com.zkteco.android.biometric.module.idcard.meta.IDPRPCardInfo

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

/** JyIDReaderPlugin */
class JyIDReaderPlugin: FlutterPlugin{

  private lateinit var channel : MethodChannel
  private lateinit var eventChannel : EventChannel

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "JyIDReader")
    eventChannel = EventChannel(flutterPluginBinding.binaryMessenger,"JyIDReader/onRead")
    val readerHandler = IDReaderHandler(flutterPluginBinding.applicationContext)
    channel.setMethodCallHandler(readerHandler)
    eventChannel.setStreamHandler(readerHandler)
  }


  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  class IDReaderHandler(context: Context): MethodCallHandler, EventChannel.StreamHandler{
    private var mediaPlayer: MediaPlayer?=null
    private val tag = this.javaClass.simpleName
    private val contextRef = WeakReference<Context>(context)
    private val mainHandler = Handler()
    private val listener: IDCardRead.getIDCardInfoListen = IDCardRead.getIDCardInfoListen(){
      type: Int,//证件类型
      idCardInfo: IDCardInfo?,//中国大陆居民身份证
      idprpCardInfo: IDPRPCardInfo?,//外国人永居证
      gangaotaiIdCardInfo: IDCardInfo?,//港澳台居住证
              // 身份证头像
      bitmap: Bitmap ->
      run {

        when (type) {
          1 -> {
            mediaPlayer = MediaPlayer.create(contextRef.get(),R.raw.ic_card_success)
            mediaPlayer?.start()
            val data = mutableMapOf<String, Any>()
            data["type"] = 1
            data["id"] = idCardInfo!!.id
            data["name"] = idCardInfo!!.name
            data["sex"] = idCardInfo!!.sex
            data["nation"] = idCardInfo!!.nation
            data["birth"] = idCardInfo!!.birth
            data["address"] = idCardInfo!!.address
            data["depart"] = idCardInfo!!.depart
            data["validityTime"] = idCardInfo!!.validityTime
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            data["bitmap"] = outputStream.toByteArray()
            mainHandler.post { events?.success(data) }

          }
//          2 -> Log.w(tag, "读取到外国人居住证")
//          3 -> Log.w(tag, "读取到港澳台居住证")
          else -> {
            mediaPlayer = MediaPlayer.create(contextRef.get(),R.raw.ic_card_failure)
            mediaPlayer?.start()
            Log.w(tag, "读取到未知的证件类型")
          }
        }
      }
    }
    private var events: EventChannel.EventSink? = null
    private lateinit var mIDCardRead:IDCardRead

    override fun onMethodCall(call: MethodCall, result: Result) {
      Log.i(tag, "身份证阅读器调用:${call.method}, 参数:${call.arguments}")

      when(call.method){
        "init" -> {
          val arguments = call.arguments as Map<*, *>
          mIDCardRead = IDCardRead(contextRef.get())
          mIDCardRead.setUSBTimeOut(arguments["usbTimeout"] as Int)
          mIDCardRead.setCycle(arguments["cycle"] as Int)
          mIDCardRead.setIDCardLister(listener)
        }
        "openDevice" -> {
          result.success(mIDCardRead.openDevice())
        }
        "getSAMID" -> {
          result.success(mIDCardRead.samid)
        }
        "startIDThread" -> {
          mediaPlayer=MediaPlayer.create(contextRef.get(),R.raw.ic_card_began_read)
          mediaPlayer?.start()
          result.success(mIDCardRead.startIDThread())
        }
        "stopIDThread" -> {
          mIDCardRead.stopIDThread()
        }
        "closeDevice" -> {
          mediaPlayer?.stop()
          mediaPlayer?.release()
          result.success(mIDCardRead.closeDevice())
        }
        else -> result.notImplemented()
      }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
      this.events = events
    }

    override fun onCancel(arguments: Any?) {
      this.events = null
    }
  }
}
