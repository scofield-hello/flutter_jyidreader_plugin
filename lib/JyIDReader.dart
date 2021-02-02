import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class IDCardInfo {
  final int type;
  final String id;
  final String name;
  final String sex;
  final String nation;
  final String birth;
  final String address;
  final String depart;
  final String validityTime;
  final Uint8List bitmap;
  IDCardInfo(
      {this.type,
      this.id,
      this.name,
      this.sex,
      this.nation,
      this.birth,
      this.address,
      this.depart,
      this.validityTime,
      this.bitmap});
}

class JyIDReader {
  static JyIDReader _instance;
  static const MethodChannel _channel = const MethodChannel('JyIDReader');
  static const _eventChannel = const EventChannel("JyIDReader/onRead");
  factory JyIDReader() => _instance ??= JyIDReader._();

  JyIDReader._() {
    _eventChannel.receiveBroadcastStream().listen(_onEvent);
  }

  final _onReadCardData = StreamController<IDCardInfo>.broadcast();

  Stream<IDCardInfo> get onReceiveCardData => _onReadCardData.stream;

  void _onEvent(dynamic data) {
    if(!_onReadCardData.isClosed){
      var cardInfo = IDCardInfo(
          type: data["type"],
          id: data["id"],
          name: data["name"],
          sex: data["sex"],
          nation: data["nation"],
          birth: data["birth"],
          address: data["address"],
          depart: data["depart"],
          validityTime: data["validityTime"],
          bitmap: data["bitmap"]);
      _onReadCardData.add(cardInfo);
    }
  }

  ///初始化身份证阅读器.
  ///[usbTimeout] 设置身份证模块实际通讯的超时时间，默认2000毫秒.
  ///[cycle] 设置身份证模块读卡的循环周期，默认为300毫秒读卡一次.
  Future<void> init({int usbTimeout = 2000, int cycle = 300}) async {
    await _channel.invokeMethod('init', {"usbTimeout": usbTimeout, "cycle": cycle});
  }

  ///如果打开身份证识别模块，成功返回true,失败返回false.
  Future<bool> openDevice() async {
    return await _channel.invokeMethod("openDevice");
  }

  ///返回身份证SAMID.
  Future<String> get samid async {
    return await _channel.invokeMethod("getSAMID");
  }

  ///开启循环读取身份证信息线程，成功返回true,失败返回false.
  Future<bool> startIDThread() async {
    return await _channel.invokeMethod("startIDThread");
  }

  ///关闭循环读取身份证信息线程，成功返回true,失败返回false.
  Future<void> stopIDThread() async {
    await _channel.invokeMethod("stopIDThread");
  }

  ///关闭身份证识别模块,成功返回true,失败返回false.
  Future<bool> closeDevice() async {
    return await _channel.invokeMethod("closeDevice");
  }

  void dispose() {
    _onReadCardData.close();
    _instance = null;
  }
}
