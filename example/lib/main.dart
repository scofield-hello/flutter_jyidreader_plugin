import 'dart:async';

import 'package:JyIDReader/JyIDReader.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  JyIDReader jyIDReader = JyIDReader();
  String description;
  IDCardInfo idCardInfo;
  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    jyIDReader.onReceiveCardData.listen((data) async {
      setState(() {
        idCardInfo = data;
        description = "证件信息：${data.name}\r${data.id}";
      });
    });
    await jyIDReader.init(usbTimeout: 2000, cycle: 300);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
            child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text("TEXT:$description"),
            if (idCardInfo != null)
              Image.memory(idCardInfo.bitmap, height: 100.0, width: 80, fit: BoxFit.cover),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                FlatButton(
                    onPressed: () async {
                      bool success = await jyIDReader.openDevice();
                      setState(() {
                        description = "打开设备：$success";
                      });
                    },
                    child: Text("打开设备")),
                FlatButton(
                    onPressed: () async {
                      bool success = await jyIDReader.closeDevice();
                      setState(() {
                        description = "关闭设备：$success";
                      });
                    },
                    child: Text("关闭设备")),
                FlatButton(
                    onPressed: () async {
                      String samid = await jyIDReader.samid;
                      setState(() {
                        description = "获取SAM号：$samid";
                      });
                    },
                    child: Text("获取SAM号")),
                FlatButton(
                    onPressed: () async {
                      bool success = await jyIDReader.startIDThread();
                      setState(() {
                        description = "开始读卡：$success";
                      });
                    },
                    child: Text("开始读卡")),
                FlatButton(
                    onPressed: () async {
                      setState(() {
                        description = "停止读卡";
                        idCardInfo = null;
                      });
                      await jyIDReader.stopIDThread();
                    },
                    child: Text("停止读卡")),
              ],
            )
          ],
        )),
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
    jyIDReader.dispose();
  }
}
