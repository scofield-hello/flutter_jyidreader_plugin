# JyIDReader

捷宇Z20一体机 身份证阅读器插件.

## Getting Started

```dart
///读取到身份证信息时回调
jyIDReader.onReceiveCardData.listen((data) async {
    setState(() {
      description = "证件信息：${data.name}\r${data.id}";
    });
});

///初始化身份证阅读器.
///[usbTimeout] 设置身份证模块实际通讯的超时时间，默认2000毫秒.
///[cycle] 设置身份证模块读卡的循环周期，默认为300毫秒读卡一次.
await jyIDReader.init(usbTimeout: 2000, cycle: 300);

///如果打开身份证识别模块，成功返回true,失败返回false.
bool success = await jyIDReader.openDevice();

///返回身份证SAMID.
String samid = await jyIDReader.samid;

///开启循环读取身份证信息线程，成功返回true,失败返回false.
bool success = await jyIDReader.startIDThread();

///关闭循环读取身份证信息线程，成功返回true,失败返回false.
await jyIDReader.stopIDThread();

///关闭身份证识别模块,成功返回true,失败返回false.
bool success = await jyIDReader.closeDevice();

@override
void dispose() {
  super.dispose();
  jyIDReader.dispose();
}

```

