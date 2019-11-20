# Video SDK for Android 使用指南

## 适用范围
本文档适用于游密实时音视频引擎（Video SDK）Android Studio开发环境下接入。

## SDK目录概述
语音SDK中有lib文件夹，该文件夹下又包含几种cpu系统架构的动态库文件以及youme_voice_engine.jar包。

## Android Studio开发环境集成
1.  将SDK内的lib文件夹的所有文件移至Android工程libs文件夹下（可视实际情况自行放置），如下图所示：

  ![](https://youme.im/doc/images/android_libs_view.png)

2. AndroidManifest.xml配置
##### 添加录音和网络相关权限
  ``` xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH" />

<!-- video -->
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />

<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.CAPTURE_VIDEO_OUTPUT" />
  ```
3. 设置加载so库以及启动相关服务
  在入口Activity类先导入package:   
  ```java
  import android.content.Intent;
  import android.os.Bundle;
  import com.youme.voiceengine.mgr.YouMeManager;
  ```
  然后在onCreate方法里添加如下代码:
  ```java
  YouMeManager.Init(this);
  super.onCreate(savedInstanceState);
  ```
  
### 注：详细API接入手册可查看“Video SDK for Android-接口手册.md”文档





