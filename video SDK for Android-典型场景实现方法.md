# Video SDK for Android 典型场景实现方法

## 实现视频通话(内部采集、内部渲染)
### 相关接口
API的调用主要使用“com.youme.voiceengine.api”来直接操作(部分接口在com.youme.voiceengine.NativeEngine)，接口使用的基本流程为`初始化`->`收到初始化成功回调通知`->`加入房间`->`收到加入房间成功回调通知`->`使用其它接口`->`离开房间`->`反初始化`，要确保严格按照上述的顺序使用接口。
#### 初始化流程

接口|含义
-----|-----
`api.setLogLevel` | 设置日志级别
`api.SetCallback(YouMeCallBackInterface callBack)` | 注册回调(event、memberchange等)
`api.init` | 引擎初始化，区域参数使用 RTC_DEFAULT_SERVER，以支持全球SDN分配模式

#### 加入房间流程

接口|含义
-----|-----
`api.setVideoPreviewFps` | 设置本地预览帧率，建议设置为 30
`api.setVideoLocalResolution` | 设置本地采集分辨率，建议设置为 480 * 640
`api.setVideoFps` | 设置视频编码帧率（大流），建议设置为 10 - 15
`api.setVideoNetResolution` | 设置编码分辨率（大流），建议设置，建议设置480 * 640
`api.setVBR` | 设置视频编码是否采用VBR动态码率（大流），建议设置为 true
`api.setVideoCodeBitrate` | 设置视频编码码率，若不调用则采用默认设置（大流），建议设置为 600 - 160
`api.setVideoFpsForSecond` | 设置视频帧率（小流），建议设置为 7
`api.setVideoNetResolutionForSecond` | 设置编码分辨率（小流），建议设置为 256 * 336 （16的整数被）
`api.setVBRForSecond` | 设置视频编码是否采用VBR动态码率（小流），建议设置为true
`api.setVideoCodeBitrateForSecond` | 设置视频编码码率，若不调用则采用默认设置（小流），200 - 100
`api.setVideoFpsForShare` | 设置共享流帧率，一般共享建议设置为 10帧
`api.setVideoNetResolutionForShare` | 设置共享流编码分辨率，建议为 720p - 1080p
`setMicLevelCallback` | 设置是否开启讲话音量回调
`setFarendVoiceLevelCallback` | 设置是否开启远端说话人音量回调
`api.setAVStatisticInterval` | 设置音视频统计上报间隔，建议为5秒
`api.setAutoSendStatus`  | 设置状态同步(mic,speaker的状态会通知给接收端)
`api.joinChannelSingleMode` | 加入房间，建议 role建议设置为1，autoRecv设置为false
`api.SetVideoCallback` | 激活视频回调绑定
`VideoRenderer.getInstance().setLocalUserId` | 设置自己的本地的userid
`VideoRenderer.getInstance().addRender` | 创建本端预览


#### 本地设备控制接口
收到`JOIN_OK`事件后可设置：

接口|含义
----|----
`api.startCapturer` | 打开摄像头采集
`api.stopCapturer` | 关闭摄像头采集
`api.switchCamera` | 切换摄像头
`api.setMicrophoneMute`	| 设置麦克风状态
`api.setSpeakerMute`	| 设置扬声器状态

#### 接收远端音视频接收和自定义信令

接口|含义
----|----
`sendMessage` | 在房间内发送自定义信令消息，也可以通过服务器端restapi发送，可用于举手、控麦等操作
`VideoRenderer.getInstance().addRender` | 创建远端用户渲染
`VideoRenderer.getInstance().deleteRender()` | 删除远端用户渲染绑定
`api.setUsersVideoInfo` | 设置观看用户的哪一路流(大小流)，autoRecv设置为false时，需要调用该接口后才会接收对方的视频流
`api.maskVideoByUserId` | 设置是否屏蔽他人视频，屏蔽对方的所有视频流，包括共享流，音频流保留
`api.setListenOtherVoice` | 设置是否听其他人语音
`onMemberChange` | 房间内其它用户加入/离开回调通知

#### 离开房间

接口|含义
----|----
`api.leaveChannelAll` | 退出所有房间

#### 反初始化
接口|含义
----|----
`api.unInit` | 反初始化引擎

### 关键调用顺序

1. 调用初始化（api.init），等待初始化成功的通知事件 `YouMeConst.YouMeEvent.YOUME_EVENT_INIT_OK`

2. 加入房间（api.joinChannelSingleMode），等待加入成功的通知事件 `YouMeConst.YouMeEvent.YOUME_EVENT_JOIN_OK`

3. 激活视频回调绑定 `api.SetVideoCallback()`

4. 打开摄像头（api.startCapture），设置麦克风扬声器等设备（api.setMicrophoneMute，api.setSpeakerMute）

5. 接收到视频数据回调 `YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_ON`，绑定渲染（VideoRenderer.getInstance().addRender）

6. 其它接口(开关摄像头、开关mic/speaker，屏蔽他人语音/视频)

7. 退出房间

8. 反初始化

### 设置回调监听
引擎底层对于耗时操作都采用异步回调的方式，函数调用会立即返回，操作结果java层会同步回调。因此，用户必须实现相关接口并在初始化前通过 `api.SetCallback(this)` 注册接收回调通知的对象。

使用类需要implements接口YouMeCallBackInterface，并实现该接口下的所有回调函数。回调都在子线程中执行，不能用于更新UI等耗时操作。

*  首先要导入相关的包：

```java 
  import com.youme.voiceengine.MemberChange;
  import com.youme.voiceengine.YouMeCallBackInterface;
```

* 然后implements接口YouMeCallBackInterface，具体实现回调方法：

``` java
  @Override
  public void onEvent(int eventType, int iErrorCode, String roomid, Object param){}
  @Override
  public void onRequestRestAPI(int requestID, int iErrorCode, String strQuery, String strResult){}
  @Override
  public void onMemberChange(String channelID, MemberChange[] arrChanges, boolean isUpdate){}
  @Override
  public  void onBroadcast(int bc , String room, String param1, String param2, String content){}
  @Override
  public  void onAVStatistic( int avType,  String userID, int value ){}
  @Override
  public void onVideoPreDecode(String userId, byte[] data, int dataSizeInByte) {}
  @Override
  public  void onTranslateTextComplete( int errorcode, int requestID, String text, int srcLangCode, int destLangCode ){
 }
```

## 实现视频直播 ##

### 相关接口

接口|含义
----|----
`api.init` | 引擎初始化
`api.setVideoLocalResolution` | 设置本地采集分辨率
`api.setVideoNetResolution` | 设置网络传输分辨率
`api.joinChannelSingleMode` | 加入房间
`api.createRender` | 创建渲染
`api.startCapture` | 开始摄像头采集
`api.setMicrophoneMute` | 设置麦克风状态
`api.setSpeakerMute`    | 设置扬声器状态
`api.setHeadsetMonitorOn` | 设置监听
`api.setBackgroundMusicVolume`  | 设置背景音乐播放音量
`api.playBackgroundMusic`   | 播放背景音乐

### 关键调用顺序
1. 以主播身份进入频道 初始化（init）->joinChannelSingleMode(参数三传主播身份YOUME_USER_HOST), 观众传的身份可以是听众 也可以是自由人（住进自由人进入频道 需要关闭麦克风）

2. 主播打开摄像头，麦克风等设备

3. 设置监听 setHeadsetMonitorOn（true,true）参数1表示是否监听麦克风 true表示监听,false表示不监听 ,参数2表示是否监听背景音乐,true表示监听,false表示不监听

4. setBackgroundMusicVolume（70）调节背景音量大小

5. playBackgroundMusic(string pFilePath, bool bRepeat) 播放本地的mp3音乐。 参数一 本地音乐路径， 参数二 是否重复播放 true重复，false不重复

6. 远端有视频流过来，会通知 `YOUME_EVENT_OTHERS_VIDEO_ON` 事件,此时调用createRender创建相关渲染。



## 实现视频会议 ##

### 相关接口

接口|含义
----|----
`api.SetCallback` | 设置回调
`api.init` | 引擎初始化
`api.setVideoLocalResolution` | 设置本地采集分辨率，建议设置为 720 * 960
`api.setVideoPreviewFps` | 设置本地预览帧率，建议设置为 15 - 30
`api.setVideoNetResolution` | 设置网络传输分辨率（大流），建议设置为 720 * 960
`api.setVideoFps` | 设置视频传输帧率，建议设置为 15 - 30
`api.setVideoNetResolutionForShare` | 设置共享流编码分辨率，建议为 720p * 1280p
`api.setVideoFpsForShare` | 设置共享流帧率，一般共享建议设置为 15帧
`api.setVideoNetResolutionForSecond` | 设置视频编码分辨率（小流），建议设置为 480 * 640 （16的整数被）
`api.setVideoFpsForSecond` | 设置视频传输帧率（小流），建议设置为 15
`api.joinChannelSingleMode` | 加入会议房间，建议role设置为1，autoRecv设置为false
`api.setVideoCallback` | 设置视频渲染回调
`api.startCapture` | 开始摄像头采集
`api.createRender` | 创建渲染
`api.setMicrophoneMute` | 设置麦克风状态
`api.setSpeakerMute`    | 设置扬声器状态
`VideoRenderer.getInstance().setLocalUserId` | 设置自己的本地的userid
`VideoRenderer.getInstance().addRender` | 创建本端/远端渲染
`VideoRenderer.getInstance().deleteRender` | 删除本端/远端预览

### 关键调用顺序
1. 设置接收回调 `api.SetCallback` 需要在初始化之前调用，用于接收onEvent回调事件

2. 初始化SDK api.init 等待OnEvent监听里返回的初始化成功的通知事件 `YouMeConst.YouMeEvent.YOUME_EVENT_INIT_OK`，初始化成功之后才可以调用其他加入频道相关接口

3. 设置以上相关接口中的分辨率、帧率等参数接口，需要在加入会议房间之前调用

4. 加入会议频道 joinChannelSingleMode 等待加入成功的通知事件 `YouMeConst.YouMeEvent.YOUME_EVENT_JOIN_OK`

5. 设置视频回调绑定 setVideoCallback 

6. 打开摄像头（api.startCapture），设置麦克风扬声器等设备（api.setMicrophoneMute，api.setSpeakerMute），需要在加入会议房间成功后调用

7. 接收到远端视频数据回调 `YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_ON`，绑定渲染（VideoRenderer.getInstance().addRender）

8. 可以根据业务需求控制其他用户的麦克风、扬声器 setOtherMicMute / setOtherSpeakerMute

9. 离开会议频道 leaveChannelAll


## 视频实时直播+rtmp推流

### 相关接口
接口|含义
---|---
`api.SetCallback` | 设置回调
`api.init` | 引擎初始化
`api.setVideoLocalResolution` | 设置本地采集分辨率
`api.setVideoNetResolution` | 设置网络传输分辨率
`api.joinChannelSingleMode` | 加入房间
`api.startCapture` | 开始摄像头采集
`api.createRender` | 创建渲染
`api.setMicrophoneMute` | 设置麦克风状态
`api.setSpeakerMute`    | 设置扬声器状态
`api.setHeadsetMonitorOn` | 设置监听
`api.setBackgroundMusicVolume` | 设置背景音乐播放音量
`api.playBackgroundMusic`  | 播放背景音乐
`set_single_rtmp_param` | 设置单路rtmp服务端推流（服务端restapi接口）
`del_single_rtmp_param` | 关闭单路rtmp服务端推流（服务端restapi接口）

### 关键调用顺序
1. 以主播身份进入频道 初始化（init）->joinChannelSingleMode(参数三传主播身份YOUME_USER_HOST), 观众传的身份可以是听众 也可以是自由人（注意：自由人进入频道 需要关闭麦克风和摄像头）

2. 主播打开摄像头，麦克风等设备，听众只需要打开扬声器；

3. 调用`set_single_rtmp_param`把主播的视频流推流到CDN上；

4. 设置监听 `setHeadsetMonitorOn（true,true`）参数1表示是否监听麦克风 true表示监听,false表示不监听 ,参数2表示是否监听背景音乐,true表示监听,false表示不监听

5. `setBackgroundMusicVolume（70）`调节背景音量大小

6. `playBackgroundMusic(string pFilePath, bool bRepeat) `播放本地的mp3音乐。 参数一 本地音乐路径， 参数二 是否重复播放 true重复，false不重复

7. 远端有视频流过来，会通知 `YOUME_EVENT_OTHERS_VIDEO_ON` 事件,此时调用createRender创建相关渲染。该视频流是于SDK中可实时低延迟收看；旁路推流到CDN的rtmp流由业务端去拉取播放地址并实现相关播放器用于播放；

8. 关闭单路rtmp服务端推流`del_single_rtmp_param`,暂停旁路推流到CDN；

9. 主播和用户离开直播频道`api.leaveChannelAll`


## 教育点播场景

### 相关接口

接口|含义
----|----
`api.SetCallback` | 设置回调
`api.init` | 引擎初始化
`api.setVideoLocalResolution` | 设置本地采集分辨率，建议设置为 720 * 960
`api.setVideoPreviewFps` | 设置本地预览帧率，建议设置为 15 - 30
`api.setVideoNetResolution` | 设置网络传输分辨率（大流），建议设置为 720 * 960
`api.setVideoFps` | 设置视频传输帧率，建议设置为 15 - 30
`api.setVideoNetResolutionForShare` | 设置共享流编码分辨率，建议为 720p * 1280p
`api.setVideoFpsForShare` | 设置共享流帧率，一般共享建议设置为 15帧
`api.setVideoNetResolutionForSecond` | 设置视频编码分辨率（小流），建议设置为 480 * 640 （16的整数倍）
`api.setVideoFpsForSecond` | 设置视频传输帧率（小流），建议设置为 15
`api.joinChannelSingleMode` | 加入会议房间，建议role设置为1，autoRecv设置为false
`api.setVideoCallback` | 设置视频渲染回调
`api.startCapture` | 开始摄像头采集
`api.createRender` | 创建渲染
`api.setMicrophoneMute` | 设置麦克风状态
`api.setSpeakerMute`    | 设置扬声器状态
`VideoRenderer.getInstance().setLocalUserId` | 设置自己的本地的userid
`VideoRenderer.getInstance().addRender` | 创建本端/远端渲染
`VideoRenderer.getInstance().deleteRender` | 删除本端/远端预览
`set_media_recod_param` | 设置用户视频录制（服务端接口）
`del_media_recod_param` | 删除用户视屏录制（服务端接口）
`get_media_recod_infos` | 获取多个房间录制信息（服务端接口）
`get_media_recod_info`  | 获取单个房间详细录制信息（服务端接口）

### 关键调用顺序

1. 教师端以主播身份进入频道 初始化（init）->joinChannelSingleMode(参数三传主播身份YOUME_USER_HOST), 学生端传的身份可以是听众 也可以是自由人（注意：自由人进入频道 需要关闭麦克风和摄像头）

2. 教师打开摄像头，麦克风等设备，听众只需要打开扬声器；

3. 设置监听 `setHeadsetMonitorOn（true,true）`参数1表示是否监听麦克风 true表示监听,false表示不监听 ,参数2表示是否监听背景音乐,true表示监听,false表示不监听

4. `setBackgroundMusicVolume（70）`调节背景音量大小；

5. 教师端打开共享屏幕`ScreenRecorder.startScreenRecorder()`，用于共享屏幕或者窗口画面；

6. 打开共享后，调用服务端restapi接口，`set_media_recod_param` 设置用户视频录制

7. 学生端监听到远端有视频流过来，会通知 `YOUME_EVENT_OTHERS_VIDEO_ON` 事件,此时调用createRender创建相关渲染；

8. 教师端结束直播，调用`ScreenRecorder.stopScreenRecorder()`结束共享屏幕；

9. 调用服务端关闭用户视频录制，`del_media_recod_param`；

10. 此时教师端的共享屏幕视频流已经在服务端录制好，可以调用服务端获取房间录制信息接口查看录制的视频文件url链接，可用于下载直接播放；

11. 教师和学生离开直播频道`api.leaveChannelAll`