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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val TAG = "JyIDReaderPlugin"

/** JyIDReaderPlugin */
class JyIDReaderPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler{

    private val threadPool = ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue<Runnable>())
    private val mainHandler = Handler()
    private var events: EventChannel.EventSink? = null
    private var mMediaPlayer:MediaPlayer? = null
    private lateinit var context: Context
    private lateinit var mIDCardRead: IDCardRead
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel

    private val listener: IDCardRead.getIDCardInfoListen =
            IDCardRead.getIDCardInfoListen() { type: Int,//证件类型
                                               idCardInfo: IDCardInfo?,//中国大陆居民身份证
                                               _: IDPRPCardInfo?,//外国人永居证
                                               _: IDCardInfo?,//港澳台居住证
                                               bitmap: Bitmap ->     // 身份证头像
                run {

                    when (type) {
                        1 -> {
                            playSound(R.raw.ic_card_success, 1800)
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
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            data["bitmap"] = outputStream.toByteArray()
                            mainHandler.post { events?.success(data) }

                        }
//          2 -> Log.w(tag, "读取到外国人居住证")
//          3 -> Log.w(tag, "读取到港澳台居住证")
                        else -> {
                            Log.w(TAG, "读取到未知的证件类型")
                            playSound(R.raw.ic_card_failure, 1800)
                        }
                    }
                }
            }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "JyIDReader")
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "JyIDReader/onRead")
        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
    }


    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        if (!threadPool.isShutdown){
            threadPool.shutdownNow()
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        Log.i(TAG, "身份证阅读器调用:${call.method}, 参数:${call.arguments}")
        when (call.method) {
            "init" -> {
                val arguments = call.arguments as Map<*, *>
                mIDCardRead = IDCardRead(context)
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
                result.success(true)
                threadPool.execute{
                    playSound(R.raw.ic_card_began_read, 3000)
                    mIDCardRead.startIDThread()
                }
            }
            "stopIDThread" -> {
                mIDCardRead.stopIDThread()
            }
            "closeDevice" -> {
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

    private fun playSound(resid: Int, waitMillis: Long){
        try {
            mMediaPlayer = MediaPlayer.create(context, resid)
            mMediaPlayer!!.start()
            Thread.sleep(waitMillis)
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
        }catch (e: InterruptedException){
            Log.e(TAG, "线程睡眠waitMillis毫秒失败.${e.message}")
        }catch (e: Exception) {
            Log.e(TAG, "MediaPlayer错误.${e.message}")
        }
    }
}
