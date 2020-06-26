package im.youme.talk.sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.widget.SeekBar;

import com.youme.voiceengine.MemberChange;
import com.youme.voiceengine.NativeEngine;
import com.youme.voiceengine.VideoRenderer;
import com.youme.voiceengine.YouMeCallBackInterface;
import com.youme.voiceengine.YouMeCallBackInterfacePcm;
import com.youme.voiceengine.YouMeConst;
import com.youme.voiceengine.YouMeCustomDataCallbackInterface;
import com.youme.voiceengine.api;
import com.youme.voiceengine.mgr.YouMeManager;
import com.youme.voiceengine.video.EglBase;
import com.youme.voiceengine.video.RendererCommon;
import com.youme.voiceengine.video.SurfaceViewRenderer;

import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.youme.talk.streaming.core.VideoProducer;
import im.youme.talk.video.PercentFrameLayout;

import com.tencent.bugly.crashreport.CrashReport;


public class VideoCapturerActivity extends Activity implements YouMeCallBackInterface, View.OnClickListener, SeekBar.OnSeekBarChangeListener, YouMeCustomDataCallbackInterface {


    public class RenderInfo
    {
        public String userId;
        public int rendViewIndex;
    }

    //是否使用外置采集，如果是，需要外部采集传给SDK，如果不是，则SDK内部负责采集
    public static boolean isExternalInputMode = false;
    public static boolean isExternalEncode = false;
    //bugly
    private static final String YOUME_BUGLY_APP_ID = "428d8b14e2";

    ///声明video设置块相关静态变量
    public static int _videoWidth = 480;
    public static int _videoHeight = 640;
    public static int _maxBitRate = 0;
    public static int _minBitRate = 0;
    public static int _reportInterval = 5000;
    public static int _farendLevel = 10;
    public static boolean _bHighAudio = false;
    public static boolean _bHWEnable = true;
    public static boolean _bBeautify = false;
    public static boolean _bUseTcpMode = false;
    public static boolean _bFixQuality = false;
    public static boolean _bPlayBGM = false;

    public static int _fps = 30;

    public boolean inited = false;


    ///界面元素
    private EditText mUserIDEditText;
    private EditText mRoomIDEditText;

    public TextView tvState = null;
    private TextView tvMode = null;
    private TextView avTips = null;

    private Button btn_join = null;
    private Button btn_camera_onoff = null;
    private Button btn_camera_switch = null;
    private Button btn_tcp_mode = null;
    private Button btn_play_bgm = null;

    private Button btn_open_mic = null;
    private SeekBar seekbar_beautify = null;

    private SurfaceViewRenderer[] arrRenderViews = null;

    private CameraMgrSample cameraFront = new CameraMgrSample();
    private CameraMgrSample cameraBackground = new CameraMgrSample();
    private CameraMgrSample currentSingleCamera = cameraFront;

    ////
    private static String TAG = "YOUME:" + VideoCapturerActivity.class.getSimpleName();

    ///记录userId 对应的render信息
    private Map<String, RenderInfo> renderInfoMap = null;
    ///记录远端renderView的占用情况
    private int[] m_UserViewIndexEn = {0, 0, 0, 0};
    private List<String> userList = null;

    String local_user_id = null;
    int local_render_id = -1;

    //记录当前状态
    private boolean isCameraOn = false;
    private boolean isJoinedRoom=false;
    private boolean  isMicOpen = false;
    ///大小端数据当前状态
    private int video_id = 0;
    private float beautifyLevel = 0.5f;


    //avstatistic回调数据的tips
    private String strAvTip = null;
    //为了展示avStatistic数据用的
    private long avTime = 0;
    //远端音量展示用的
    public String farendLevel = "0";
    private String currentRoomID = "";
    private MyHandler sampleEventHandler;

    private MemberChange[] memberChange = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getApplicationContext();
        CrashReport.initCrashReport(context, YOUME_BUGLY_APP_ID, false);
//      YouMeManager.setSOName( "youmetalk" );

        CameraMgrSample.init(this);
        YouMeManager.Init(this);
        super.onCreate(savedInstanceState);

        renderInfoMap = new HashMap<>();
        userList = new ArrayList<>();

        sampleEventHandler = new MyHandler(this);

        ///初始化界面相关数据
        arrRenderViews = new SurfaceViewRenderer[ 4 ];

        strAvTip = "";

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_capturer);


        mUserIDEditText = (EditText)findViewById(R.id.editText_userID);
        mRoomIDEditText = (EditText)findViewById(R.id.editText_roomID);

        //随机userid
        local_user_id = "user"+(int)(Math.random() * 10000);
        mUserIDEditText.setText(local_user_id);

        btn_join = (Button)findViewById( R.id.btn_join );

        //int render00 = VideoRenderer.addRender(user_id, mSurfaceView);
        btn_camera_onoff = (Button) findViewById(R.id.btn_camera_onoff);
        btn_camera_onoff.setActivated(false);
        btn_camera_onoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isJoinedRoom ){
                    Toast.makeText(VideoCapturerActivity.this, "进入频道还没有完成", Toast.LENGTH_SHORT).show();
                    return;
                }
                tvState.setText(("the sdkInfo:" + api.getSdkInfo()));
                if(isCameraOn) {
                    stopCamera();
                    btn_camera_onoff.setText("打开摄像头");
                } else {
                    startCamera();
                    //设置视频无渲染帧超时等待时间，单位毫秒
                    api.setVideoNoFrameTimeout(5000);
                    btn_camera_onoff.setText("关闭摄像头");
                }
                isCameraOn = !isCameraOn;
            }
        } );

        btn_camera_switch = (Button) findViewById(R.id.btn_camera_switch);
        btn_camera_switch.setOnClickListener(this);

        btn_tcp_mode = (Button) findViewById( R.id.btn_tcpMode );
        btn_tcp_mode.setOnClickListener( this );

        btn_play_bgm = (Button) findViewById( R.id.btn_playBGM );
        btn_play_bgm.setOnClickListener( this );

        btn_open_mic = (Button) findViewById( R.id.btn_open_mic );
        btn_open_mic.setOnClickListener( this );

        tvState = (TextView) findViewById(R.id.state);
        tvMode = (TextView) findViewById( R.id.external_mode );

        avTips = (TextView) findViewById( R.id.avtip);
        avTips.setTextColor( Color.rgb(0, 0, 0) );

        seekbar_beautify = (SeekBar) findViewById(R.id.seekBar_Beautify );
        seekbar_beautify.setOnSeekBarChangeListener(this);

		//开启摄像头权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST", "Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

		//开启文件存储权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        InitUI();
        //弹出对话框，要求选择外部采集还是内部采集
        askExternalMode();

    }

    private void InitUI(){
        btn_join.setEnabled(false);
        btn_camera_onoff.setEnabled(false);
        btn_camera_switch.setEnabled(false);
        btn_open_mic.setEnabled(false);
        btn_tcp_mode.setEnabled(false);
        btn_play_bgm.setEnabled(false);
    }

    private void InitedUI(){
        btn_join.setEnabled(true);
        btn_camera_onoff.setEnabled(false);
        btn_camera_switch.setEnabled(false);
        btn_open_mic.setEnabled(false);
        btn_tcp_mode.setEnabled(true);
        btn_play_bgm.setEnabled(false);
    }

    private void joingUI(){
        btn_join.setEnabled(false);
        btn_camera_onoff.setEnabled(false);
        btn_camera_switch.setEnabled(false);
        btn_open_mic.setEnabled(false);
        btn_play_bgm.setEnabled(false);
    }

    private void joindedUI(){
        btn_join.setEnabled(true);
        btn_camera_onoff.setEnabled(true);
        btn_camera_switch.setEnabled(true);
        btn_open_mic.setEnabled(true);
        btn_tcp_mode.setEnabled(false);
        btn_play_bgm.setEnabled(true);
    }

    private void leavedUI(){
        btn_join.setEnabled(true);
        btn_camera_onoff.setEnabled(false);
        btn_camera_switch.setEnabled(false);
        btn_open_mic.setEnabled(false);
        btn_tcp_mode.setEnabled(true);
        btn_play_bgm.setEnabled(false);
    }

    private void askExternalMode() {
        AlertDialog.Builder builder = new AlertDialog.Builder(VideoCapturerActivity.this);
        builder.setTitle("选择采集模式");
//        builder.setPositiveButton("外部采集", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                isExternalInputMode = true ;
//                isExternalEncode = false;
//                tvMode.setText("外部采集");
//                initSDK();
//            }
//        });

        builder.setNegativeButton( "内部采集", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isExternalInputMode = false ;
                tvMode.setText("内部采集");
                initSDK();
            }
        });

        builder.setPositiveButton("外部编码", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isExternalInputMode = true;
                isExternalEncode = true;
                tvMode.setText("外部编码");
                initSDK();
            }
        });
        builder.setCancelable(false);

        builder.show();
    }

    private void initSDK()
    {
        tvState.setText("初始化中..");
        ///初始化SDK相关设置
        //设置api为外部输入音视频的模式
        if( isExternalInputMode ){
            ///bugly处理
            CrashReport.setAppVersion(this, "out."+api.getSdkInfo());
            //如果是外部输入，这句话要放在init之前调用
            api.setExternalInputMode( true );
        }
        else{
            ///bugly处理
            CrashReport.setAppVersion(this, "in."+api.getSdkInfo());
            //do nothing, 默认内部采集
        }

        //Demo设置开启日志打印
        api.setLogLevel(YouMeConst.YOUME_LOG_LEVEL.LOG_INFO, YouMeConst.YOUME_LOG_LEVEL.LOG_INFO);
        //设置自定义Log路径
        //api.setUserLogPath("/sdcard/YouMe/ym_userlog.txt");
        //设置回调监听对象,需要implements YouMeCallBackInterface
        api.SetCallback(this);
        api.setRecvCustomDataCallback(this);

        //设置测试服还是正式服
        NativeEngine.setServerMode(NativeEngine.SERVER_MODE_FIXED_IP_MCU);
        NativeEngine.setServerIpPort("39.106.60.66", 6006);

        //调用初始化
        api.init(CommonDefines.appKey, CommonDefines.appSecret, YouMeConst.YOUME_RTC_SERVER_REGION.RTC_CN_SERVER, "");
    }

    private void initRender( int index, @IdRes int layoutId, @IdRes int viewId ){
        if( index < 0 || index > arrRenderViews.length || arrRenderViews[index] != null){
            return ;
        }

        PercentFrameLayout layout = (PercentFrameLayout)this.findViewById( layoutId );
        layout.setPosition(0,0,100,100);
        SurfaceViewRenderer renderView  = (SurfaceViewRenderer) this.findViewById( viewId );
        renderView.init(EglBase.createContext(api.sharedEGLContext()), null);
        renderView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        renderView.setMirror(false);
        renderView.setVisibility(View.VISIBLE);
        renderView.setRenderBackgroundColor( 255,255,255, 255 );

        arrRenderViews[index] = renderView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        resetAllRender();
    }

    /// 以下为外部采集模式和内部采集模式不同的操作
    private void startMic()
    {
        if( isExternalInputMode ){
            //初始化外部录音模块，并开始录音

            AudioRecorderSample.initRecorder(VideoCapturerActivity.this);
            AudioRecorderSample.startRecorder();

        }
        else{
            api.setMicrophoneMute( false );
        }
    }

    private void stopMic()
    {
        if( isExternalInputMode ){
            AudioRecorderSample.stopRecorder();
        }
        else{
            api.setMicrophoneMute( true );
        }

    }

    private void switchCamera()
    {
        if (cameraFront.isDoubleStreamingModel) {
            Toast.makeText(VideoCapturerActivity.this, "双流模式下不支持切换", Toast.LENGTH_SHORT).show();
            return;
        }


        if( isExternalInputMode ){
            currentSingleCamera.closeCamera();
            currentSingleCamera = currentSingleCamera == cameraFront ? cameraBackground : cameraFront;
            currentSingleCamera.openCamera(currentSingleCamera == cameraFront );
        }
        else{
            api.switchCamera();
        }

    }

    private void startCamera()
    {
        if( isExternalInputMode )
        {
            //Demo初始化摄像头
            CameraMgrSample.init(this.getApplicationContext());
            //设置外部采集模块参数，并启动
            cameraFront.setPreViewSize(_videoWidth, _videoHeight);
            cameraBackground.setPreViewSize(_videoWidth, _videoHeight);

            cameraFront.setPreViewFps(_fps);
            cameraBackground.setPreViewFps(_fps);

            int result = cameraFront.openCamera(true);
            Log.d(TAG, "open camera result:"+result);
        }
        else {
            //设置内部采集参数，并启动摄像头
            api.setVideoLocalResolution(_videoWidth, _videoHeight );
            api.startCapturer();
        }
    }

    private void stopCamera()
    {
        if( isExternalInputMode ){
            //停止外部采集视频
            cameraBackground.closeCamera();
            cameraFront.closeCamera();
            //通知SDK已经停止视频输入
            api.stopInputVideoFrame();
        }
        else {
            api.stopCapturer();
        }
    }

    private void resetAllRender()
    {
        for( RenderInfo  info: renderInfoMap.values() ){
            VideoRenderer.getInstance().deleteRender( info.userId);

            SurfaceViewRenderer renderView = getRenderViewByIndex( info.rendViewIndex );

            if( renderView != null ){
                renderView.clearImage();
            }
        }

        renderInfoMap.clear();
        userList.clear();
        mixUserId = "";
        for( int i = 0 ; i < m_UserViewIndexEn.length; i++ ) {
            m_UserViewIndexEn[i] = 0;
        }

        //清理自己/合流的视频显示
        {
            if (local_render_id != -1) {
                VideoRenderer.getInstance().deleteRender(local_user_id);
            }

            SurfaceViewRenderer renderView = getRenderViewByIndex(3);
            if (renderView != null) {
                renderView.clearImage();
            }
        }

    }

    private void resetStatus()
    {
        strAvTip = "";
        avTips.setText( strAvTip );
        api.setCaptureFrontCameraEnable( true );
        stopCamera();
        isCameraOn = false;
        btn_camera_onoff.setText("打开摄像头");
        stopMic();
        isMicOpen = false;
        btn_open_mic.setText("打开麦克风");
    }

    protected  void onPause()
    {
        super.onPause();
        //切入后台的时候考虑暂停
        api.pauseChannel();
    }

    protected  void onResume()
    {
        super.onResume();

        //切回前台的时候恢复
        api.resumeChannel();
    }

    private SurfaceViewRenderer getRenderViewByIndex( int index ){
        if( index < 0 || index > arrRenderViews.length ){
            return null;
        }
        else{
            return arrRenderViews[index];
        }
    }

    private String mixUserId = "";
    private void updateNewView(final String newUserId, final int index) {

        SurfaceViewRenderer renderView = getRenderViewByIndex( index );
        if( renderView == null ){
            return;
        }
        VideoRenderer.getInstance().addRender(newUserId, renderView);
        //记录渲染相关信息
        RenderInfo info = new RenderInfo();
        info.userId = newUserId;
        info.rendViewIndex = index;
        renderInfoMap.put(newUserId, info );
        m_UserViewIndexEn[index] = 1;
        //3是展示自己（内部采集）或者合流画面（外部采集），没有mask相关功能，退出房间也不清空
        if( index == 3){
            return ;
        }

        //设置点击触发mask功能
        renderView.setOnClickListener(new View.OnClickListener() {
            boolean mask = false;
            final String m_tempUserId = newUserId;
            @Override
            public void onClick(View view) {
                if (m_UserViewIndexEn[index] == 1) {
                    if (!mask) {
                        api.maskVideoByUserId(m_tempUserId, true); // 1是屏蔽
                        mask = true;
                    } else {
                        api.maskVideoByUserId(m_tempUserId, false); // 2是恢复
                        mask = false;
                    }
                }
            }
        });

        if(mixUserId.isEmpty()) {
            mixUserId = newUserId;
            api.addMixOverlayVideo(mixUserId ,20,40,1,120,160);
        }
    }

    /// 回调数据
    @Override
    public void onEvent (int event, int error, String room, Object param) {
        ///里面会更新界面，所以要在主线程处理
        Log.i(TAG, "event:" + CommonDefines.CallEventToString(event) + ", error:" + error + ", room:" + room + ",param:" + param);
        Message msg = new Message();
        Bundle extraData = new Bundle();
        extraData.putString("channelId", room);
        msg.what = event;
        msg.arg1 = error;
        msg.obj = param;
        msg.setData(extraData);
       sampleEventHandler.sendMessage(msg);
    }

	@Override
    public  void onRequestRestAPI(int requestID , int iErrorCode , String strQuery, String strResult) {

    }

    @Override
    public void onMemberChange(String channelID, MemberChange[] arrChanges, boolean isUpdate) {
        Log.i(TAG, "onMemberChange:" + channelID + ",isUpdate:" + isUpdate);
        memberChange = arrChanges;
        int i;
        for(i=0; i<memberChange.length; i++) {
            if (memberChange[i].userID == local_user_id)
            {
                continue;
            }

            int validIndex = -1;
            if( !renderInfoMap.containsKey( memberChange[i].userID ) ) {
                //找一个空闲的viewIndex
                for (int j = 0; j < 3; j++) {
                    if (m_UserViewIndexEn[j] == 0) {
                        validIndex = j;
                        break;
                    }
                }

                if (validIndex != -1) {//demo只支持接收3路远端数据，这个判断避免崩溃
                    updateNewView(memberChange[i].userID, validIndex);
                }
            }
        }
    }

	@Override
	public  void onBroadcast(int bc , String room, String param1, String param2, String content){
			
	}

    @Override
    public  void onAVStatistic( int avType,  String userID, int value )
    {
        if( avType == 2  )
        {
            Log.d(TAG, "onAVStatistic: video code:"+ (value * 8 / 1000) );
        }
        Message msg = new Message();
        Bundle extraData = new Bundle();
        extraData.putString("userID", userID);
        msg.what = 10000;
        msg.arg1 = avType;
        msg.arg2 = value;
        msg.obj = userID;
        msg.setData(extraData);
        sampleEventHandler.sendMessage(msg);

    }

    @Override
    public void onTranslateTextComplete(int errorcode, int requestID, String text, int srcLangCode, int destLangCode)
    {
        Log.d( TAG, "onTranslateTextComplete:" + errorcode + ", id:" + requestID +" text:" + text + " from " + srcLangCode +" to " + destLangCode );
    }

    private void onInitOk(){
      Log.d(TAG, "初始化成功");
      tvState.setText("初始化成功");
      inited = true;
      InitedUI();
    }

    private void onJoinOK(String roomId){
      joindedUI();
      currentRoomID = roomId;
      if( !isJoinedRoom ){
        Log.d(TAG, "进频道成功");
        tvState.setText("进频道成功");
        //api.setHeadsetMonitorOn(true);
        //进频道成功后可以设置视频回调
        api.SetVideoCallback();

        //设置混流回调
        //api.setVideoFrameCallback(new videoDataCallback());
        //设置合流画面尺寸
        api.setMixVideoSize(_videoWidth,_videoHeight);
        //设置自己在合流画面中的尺寸
        //String userId, int x, int y, int z, int width, int height
        api.addMixOverlayVideo(local_user_id,0,0,0,_videoWidth,_videoHeight);

        //设置远端语音音量回调
        api.setFarendVoiceLevelCallback( 10 );

        //这时候允许打开摄像头进行采集
        btn_camera_onoff.setActivated(true);

        //开启扬声器
        api.setSpeakerMute(false);

            isJoinedRoom = true;
            //远端视频渲染的view
            initRender(0, R.id.capturer_video_layout, R.id.capturer_video_view);
            initRender(1, R.id.remote_video_layout_one, R.id.remote_video_view_one);
            initRender(2, R.id.remote_video_layout_two, R.id.remote_video_view_two);
            //合流视频渲染的view
            initRender(3, R.id.remote_video_layout_three, R.id.remote_video_view_three);
            VideoRenderer.getInstance().setLocalUserId(local_user_id);
            updateNewView(local_user_id, 3);
            //下面这句自动测试才用
            //btn_camera_onoff.performClick();

            // 测试华为传输承载点对点传输信令和自定义消息
            boolean send_side = true;
            if (send_side) {
                final byte sendMediaBuffer[] = "custom data 0x112233...".getBytes();
                final String sendSignalBuffer = "signal data hello world";
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            api.getChannelUserList(currentRoomID, -1, false);
                            if (memberChange == null) {
                                try {
                                    Log.d(TAG, "bruce >>> have not received member change message");
                                    sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }

                            int i;
                            for (i = 0; i < memberChange.length; i++) {
                                if (!local_user_id.equals(memberChange[i].userID)) {
                                    Log.d(TAG, "bruce >>> send signal message and custom message to:" + memberChange[i].userID);
                                    api.sendMessageToUser(currentRoomID, sendSignalBuffer, memberChange[i].userID);
                                    api.inputCustomDataToUser(sendMediaBuffer, sendMediaBuffer.length, System.currentTimeMillis(), memberChange[i].userID);
                                    break;
                                }
                            }

                            try {
                                sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                thread.start();
            } else {
                final byte sendMediaBuffer[] = "only sending custom message...".getBytes();
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            api.getChannelUserList(currentRoomID, -1, false);
                            if (memberChange == null) {
                                try {
                                    Log.d(TAG, "bruce >>> have not received member change message");
                                    sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }

                            int i;
                            for (i = 0; i < memberChange.length; i++) {
                                if (!local_user_id.equals(memberChange[i].userID)) {
                                    Log.d(TAG, "bruce >>> send custom message to:" + memberChange[i].userID);
                                    api.inputCustomDataToUser(sendMediaBuffer, sendMediaBuffer.length, System.currentTimeMillis(), memberChange[i].userID);
                                    break;
                                }
                            }

                            try {
                                sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                thread.start();
            }
        }
    }

    private void onLeavedlAll(){
      leavedUI();
      resetAllRender();
      api.removeMixAllOverlayVideo();
      VideoRenderer.getInstance().deleteAllRender();
      isJoinedRoom = false;
    }

    private void onVideoOn(String userId){
      if( userId.equals(local_user_id )){
        updateNewView(local_user_id, 3 );
      }
      else
      {
        int validIndex = -1;
        if( !renderInfoMap.containsKey( userId ) ) {
          //找一个空闲的viewIndex
          for (int i = 0; i < 3; i++) {
            if (m_UserViewIndexEn[i] == 0) {
              validIndex = i;
              break;
            }
          }

          if (validIndex != -1) {//demo只支持接收3路远端数据，这个判断避免崩溃
            updateNewView(userId, validIndex);
          }
        }
      }
    }


    private void onVideoShutdown(String userId){

        RenderInfo info =  renderInfoMap.get(userId);
        if( info != null ) {
            //清除视图当前的画面,涉及界面，要回到主线程处理
            final SurfaceViewRenderer renderView = getRenderViewByIndex(info.rendViewIndex);
            if (renderView != null) {
                renderView.post(new Runnable() {
                    @Override
                    public void run() {
                        renderView.clearImage();
                    }
                });
            }
            m_UserViewIndexEn[info.rendViewIndex] = 0;
            VideoRenderer.getInstance().deleteRender(info.userId);
            renderInfoMap.remove(userId);
            userList.remove(userId);
            if (mixUserId.equals(userId)){
                //删除合流中的画面
                api.removeMixOverlayVideo(userId);
                mixUserId = "";
            }
        }
    }

    public String getLocalUserID() {
        return local_user_id;
    }

    private class MyHandler extends Handler {
        private final WeakReference<VideoCapturerActivity> mActivity;

    private MyHandler(VideoCapturerActivity activity) {
      mActivity = new WeakReference<VideoCapturerActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      VideoCapturerActivity activity = mActivity.get();
      if (activity != null) {
        super.handleMessage(msg);
        String userId;
        switch (msg.what) {
          case YouMeConst.YouMeEvent.YOUME_EVENT_INIT_OK:
            Log.d(TAG, "初始化成功");

            //直接写成自动帮你执行点击加入频道逻辑
            activity.onInitOk();
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_JOIN_OK:
            String roomId = msg.getData().getString("channelId");
            activity.onJoinOK(roomId);

            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_LEAVED_ALL:
            activity.onLeavedlAll();
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_ON:
            String newUserId = String.valueOf(msg.obj);
            activity.onVideoOn(newUserId);
            break;
        //   case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_OFF:
        //     userId = String.valueOf(msg.obj);
        //     Log.d(TAG, "下线的user ID=" + userId);
        //     break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_MASK_VIDEO_BY_OTHER_USER:
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_RESUME_VIDEO_BY_OTHER_USER:
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_MASK_VIDEO_FOR_USER:
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_RESUME_VIDEO_FOR_USER:
            break;
        //   case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_CAMERA_PAUSE:
        //     break;
        //   case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_CAMERA_RESUME:
        //     break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_SHUT_DOWN:
            userId = String.valueOf(msg.obj);
            activity.onVideoShutdown(userId);
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_INPUT_START:
            userId = String.valueOf(msg.obj);
            activity.userList.add(userId);
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_INPUT_STOP:
            userId = String.valueOf(msg.obj);
            activity.userList.remove(userId);
            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_QUERY_USERS_VIDEO_INFO:

            break;
          case YouMeConst.YouMeEvent.YOUME_EVENT_SET_USERS_VIDEO_INFO:

            break;
          case 10000: //avStatistic回调
            userId = String.valueOf( msg.obj );

            ///一秒以内的都当是一批来的数据，纯属为了显示
            long curtime = System.currentTimeMillis();
            if( curtime - activity.avTime >= 1000 )
            {
              activity.strAvTip = "";
            }

                        activity.avTime = curtime;
                        activity.strAvTip = activity.strAvTip + msg.arg1 + "," + userId + "," + msg.arg2 + "\n";
                        activity.avTips.setText(activity.strAvTip);
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_FAREND_VOICE_LEVEL:
                        ///这里应该区分userId
                        int level = msg.arg1;
                        String tempLevel;
                        userId = String.valueOf(msg.obj);
                        if (level < 1) {
                            tempLevel = "0";
                        } else if (level < 2) {
                            tempLevel = "00";
                        } else if (level < 3) {
                            tempLevel = "000";
                        } else if (level < 4) {
                            tempLevel = "0000";
                        } else if (level < 5) {
                            tempLevel = "00000";
                        } else if (level < 6) {
                            tempLevel = "000000";
                        } else if (level < 7) {
                            tempLevel = "0000000";
                        } else if (level < 8) {
                            tempLevel = "00000000";
                        } else if (level < 9) {
                            tempLevel = "000000000";
                        } else if (level < 10) {
                            tempLevel = "0000000000";
                        } else {
                            tempLevel = "00000000000";
                        }
                        activity.farendLevel = tempLevel;
                        activity.tvState.setText(("the sdkInfo:" + api.getSdkInfo() + "\n远端音量(" + userId + "): " + tempLevel));
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_SEND_MESSAGE_RESULT:
                        Log.d(TAG, "bruce >>> send signal message successful");
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_MESSAGE_NOTIFY:
                        Log.d(TAG, "bruce >>> received signal message:\"" + msg.obj + "\"");
                        break;
                    default:
                        Log.w(TAG, "handleMessage: unkonw message type");
                        break;
                }
            }
        }
    }

    /// btn 响应
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camera_switch:
                switchCamera();
                break;
            case R.id.btn_open_mic:
            {
                if( isMicOpen )
                {
                    btn_open_mic.setText( "打开麦克风" );
                    stopMic();
                    isMicOpen = false;
                }
                else{
                    btn_open_mic.setText( "关闭麦克风" );
                    startMic();
                    isMicOpen = true;
                }
            }
            break;
            case R.id.btn_tcpMode:
            {
                _bUseTcpMode = !_bUseTcpMode;
                api.setTCPMode( _bUseTcpMode );
                if( _bUseTcpMode )
                {
                    btn_tcp_mode.setText("Tcp Mode");
                }
                else{
                    btn_tcp_mode.setText("Udp Mode");
                }

            }
            break;
            case R.id.btn_playBGM:
            {
                _bPlayBGM = !_bPlayBGM;
                if ( _bPlayBGM ) {
                    api.playBackgroundMusic("/sdcard/backmusic/test.mp3", true);
                    btn_play_bgm.setText("stop BGM");
                } else {
                    api.stopBackgroundMusic();
                    btn_play_bgm.setText("play BGM");
                }
            }
            break;
        }
    }

    /// seekBar 回调
    // 数值改变
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        beautifyLevel = (float)progress / 100;
        api.setBeautyLevel( beautifyLevel  );
    }

    // 开始拖动
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    // 停止拖动
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }



    /**
     * 监听Back键按下事件,方法1:
     * 注意:
     * super.onBackPressed()会自动调用finish()方法,关闭
     * 当前Activity.
     * 若要屏蔽Back键盘,注释该行代码即可
     */
    @Override
    public void onBackPressed() {
        System.exit(0);
    }

    //点击设置按钮响应
    public void onSetClick(View v){
        Intent intent = new Intent();
        intent.setClass(VideoCapturerActivity.this, videoSet.class);

        startActivity(intent);
    }

    //大小流切换
    public void onVideoSwitchClick(View v){

        video_id = video_id == 1 ? 0 : 1;
        String []userArray= new String[userList.size()];
        int [] resolutionArray = new int[userList.size()];
        for (int i = 0; i < userList.size(); ++i)
        {
            userArray[i] = userList.get(i);
            resolutionArray[i] = video_id;
        }
        api.setUsersVideoInfo(userArray, resolutionArray);
    }


    // 开启双流推送
    public void onDoubleStream(View v) {
        if (isJoinedRoom ) {
            // 修改双端采样
            Button btnDoubleStream = (Button)findViewById(R.id.btn_doubleCamera);

            if (btnDoubleStream.getText().equals("开启双流")) {
                cameraFront.openCamera(true);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cameraBackground.openCamera(false);

                cameraFront.isDoubleStreamingModel = true;
                cameraBackground.isDoubleStreamingModel = true;

                VideoProducer.getInstance().init(cameraFront.preViewWidth,cameraFront.preViewHeight, cameraBackground.orientation);
                //设置视频无渲染帧超时等待时间，单位毫秒
                api.setVideoNoFrameTimeout(5000);

                btnDoubleStream.setText("关闭双流");
            } else {
                cameraBackground.closeCamera();
                cameraFront.closeCamera();
                cameraFront.isDoubleStreamingModel = false;
                cameraBackground.isDoubleStreamingModel = false;

                currentSingleCamera.openCamera(currentSingleCamera.equals(cameraFront) );

                VideoProducer.getInstance().release();
                btnDoubleStream.setText("开启双流");
            }



        } else {
            Toast.makeText(VideoCapturerActivity.this, "请先加入频道并打开摄像头", Toast.LENGTH_SHORT).show();
        }
    }


    //点击加入频道按钮响应
    public void onJoinClick(View v){
        if( !inited ){
            Toast.makeText(VideoCapturerActivity.this, "请先加入房间并打开摄像头", Toast.LENGTH_SHORT).show();
            return ;
        }

        if( api.isInChannel(currentRoomID) ){
            api.leaveChannelAll() ;
            btn_join.setEnabled(false);
            btn_camera_onoff.setEnabled(false);
            resetStatus();
            btn_join.setText("加入频道");
        }
        else
        {
            //加入频道前进行video设置
            if( isExternalInputMode ){
                //外部采集在打开摄像头或者麦克风的时候设置
                api.setExternalInputSampleRate(YouMeConst.YOUME_SAMPLE_RATE.SAMPLE_RATE_44, YouMeConst.YOUME_SAMPLE_RATE.SAMPLE_RATE_44);
                api.setVideoFps(_fps);
            }
            else{
                api.setVideoFps(_fps);
            }

            api.setVideoNetResolution(_videoWidth,_videoHeight);
            api.setAVStatisticInterval(_reportInterval);
            api.setVideoCodeBitrate(_maxBitRate, _minBitRate );
            api.setFarendVoiceLevelCallback(_farendLevel);
            api.setVideoHardwareCodeEnable(_bHWEnable);

            if(_bHighAudio){
                api.setAudioQuality(1);
            }else {
                api.setAudioQuality(0);
            }

            if( _bFixQuality )
            {
                api.setVBR(true);
            }
            else{
                api.setVBR(false);
            }

            api.openBeautify( _bBeautify );
            if( _bBeautify ){
                api.setBeautyLevel( beautifyLevel );
                seekbar_beautify.setEnabled( true );
            }
            else{
                seekbar_beautify.setEnabled( false );
            }

            int sampleRate = 44100;
            int channels = 1;
            api.setPcmCallbackEnable(mOnYouMePcm, YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Remote |
                    YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Record |
                    YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Mix, true, sampleRate, channels);

            //joingUI();
            // 禁用内部输入
            api.setMicrophoneMute( true );
            //进入频道
            local_user_id =  mUserIDEditText.getText().toString();
            currentRoomID = mRoomIDEditText.getText().toString();

            // 加入房间之前开始鉴权，客户端向sdk传入token和timestamp
            long startTime = System.currentTimeMillis() / 1000;
            String targetString = String.format(CommonDefines.appKey + CommonDefines.appKey + currentRoomID + local_user_id + Long.toString(startTime));
            String testLowerCaseToken = ShaOneEncrypt.encryptToSHA(targetString);
//            Log.d(TAG, "initSDK: bruce >>> targetString:" + targetString);
//            Log.d(TAG, "initSDK: bruce >>> testLowerCaseToken:" + testLowerCaseToken);
            api.setTokenV3(testLowerCaseToken, startTime);

            int ret = api.joinChannelSingleModeWithAppKey(local_user_id, currentRoomID, YouMeConst.YouMeUserRole.YOUME_USER_HOST, CommonDefines.appJoinKey, true);
            if (ret != 0) {
                String tip = "进频道失败,错误码:" + ret;
                tvState.setText(tip);
               return;
            }
            joingUI();
            api.setAutoSendStatus(true);

//            api.setPcmCallbackEnable(mOnYouMePcm, YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Remote |
//                    YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Record |
//                    YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Mix, true );

            btn_join.setText("离开频道");
            video_id = 0;


        }
    }

    private YouMeCallBackInterfacePcm mOnYouMePcm = new YouMeCallBackInterfacePcm() {
        private int mPcmFrameCount = 0;

        private boolean mNeedDump = true;
        private FileOutputStream fileRemote;
        private FileOutputStream  fileRecord;
        private FileOutputStream  fileMix;

        public FileOutputStream createFileWriter( String fileName )
        {
//            String filename = getApplicationContext().getExternalCacheDir().getAbsolutePath()  + File.separator + fileName;
            String filename = "/sdcard/Download" + File.separator + fileName;
            try {
                File file = new File(filename);
                if (!file.exists()) {
                    boolean res = file.createNewFile();
                }
                FileOutputStream outFile  = new FileOutputStream( filename );
                return outFile;
            }
            catch( Exception e ) {
                Log.d( CommonDefines.LOG_TAG, "create File stream fail:"+ e.getMessage() );
            }
            return null;
        }

        public void dumpPcm( FileOutputStream outFile, byte[] data )
        {
            if( outFile != null )
            {
                try
                {
                    outFile.write( data );
                }
                catch( Exception e )
                {
                    if ((mPcmFrameCount % 500) == 0) {
                        Log.i(CommonDefines.LOG_TAG, "Pcm write Exception:" + e.getMessage() );
                    }
                }
            }
        }

        @Override
        public void onPcmDataRemote(int channelNum, int samplingRateHz, int bytesPerSample, byte[] data) {
            if( mNeedDump )
            {
                if( fileRemote == null )
                {
                    fileRemote = createFileWriter( "dump_remote.pcm");
                }
                dumpPcm( fileRemote, data  );
            }


            if ((mPcmFrameCount % 500) == 0) {
                Log.i(CommonDefines.LOG_TAG, "Remote: ++PCM callback, channelNum:" + channelNum + ", sf:" + samplingRateHz + ", bytesPerSample:" + bytesPerSample
                        + ", data_size:" + data.length);
            }
            mPcmFrameCount++;
        }

        @Override
        public void onPcmDataRecord(int channelNum, int samplingRateHz, int bytesPerSample, byte[] data) {
            if( mNeedDump )
            {
                if( fileRecord == null )
                {
                    fileRecord = createFileWriter( "dump_record.pcm");
                }
                dumpPcm( fileRecord, data  );
            }

            if ((mPcmFrameCount % 500) == 0) {
                Log.i(CommonDefines.LOG_TAG, "Record:++PCM callback, channelNum:" + channelNum + ", sf:" + samplingRateHz + ", bytesPerSample:" + bytesPerSample
                        + ", data_size:" + data.length);
            }
            mPcmFrameCount++;
        }

        @Override
        public void onPcmDataMix(int channelNum, int samplingRateHz, int bytesPerSample, byte[] data) {
            if( mNeedDump )
            {
                if( fileMix == null )
                {
                    fileMix = createFileWriter( "dump_mix.pcm");
                }
                dumpPcm( fileMix, data  );
            }

            if ((mPcmFrameCount % 500) == 0) {
                Log.i(CommonDefines.LOG_TAG, "Mix:++PCM callback, channelNum:" + channelNum + ", sf:" + samplingRateHz + ", bytesPerSample:" + bytesPerSample
                        + ", data_size:" + data.length);
            }
            mPcmFrameCount++;
        }
    };

    @Override
    public void onRecvCustomData(byte[] data, long timestamp) {
        String mediaData = new String(data);
        Log.d(TAG, "bruce >>> received custom message:\"" + mediaData + "\"");
    }

    /*

    ///合流画面渲染，内部采集模式下，没有合流，所以不需要
    public class videoDataCallback implements VideoMgr.VideoFrameCallback {
        @Override
        public void onVideoFrameCallback(String userId, byte[] data, int len, int width, int height, int fmt, long timestamp) {
            //Log.i(TAG, "onVideoFrameCallback. data len:"+len+" fmt: " + fmt + " timestamp:" + timestamp);
        }


        @Override
        public void onVideoFrameMixed(byte[] data, int len, int width, int height, int fmt, long timestamp) {
            //Log.i(TAG, "onVideoFrameMixedCallback. data len:"+len+" fmt: " + fmt + " timestamp:" + timestamp);
            //合流视频用index3的renderView
            SurfaceViewRenderer view = getRenderViewByIndex( 3 );
            if(view != null) {
                int[] yuvStrides = new int[]{width, width / 2, width / 2};
                int yLen = width * height;
                int uLen = width * height / 4;
                int vLen = width * height / 4;
                byte[] yPlane = new byte[yLen];
                byte[] uPlane = new byte[uLen];
                byte[] vPlane = new byte[vLen];
                System.arraycopy(data, 0, yPlane, 0, yLen);
                System.arraycopy(data, yLen, uPlane, 0, uLen);
                System.arraycopy(data, yLen + uLen, vPlane, 0, vLen);
                ByteBuffer[] yuvPlanes = new ByteBuffer[]{ByteBuffer.wrap(yPlane), ByteBuffer.wrap(uPlane), ByteBuffer.wrap(vPlane)};
                VideoBaseRenderer.I420Frame frame = new VideoBaseRenderer.I420Frame(width, height,  0, yuvStrides, yuvPlanes);
                view.renderFrame(frame);
            } else {
                Log.e("VideoRenderer", "mixed SurfaceView is null");
            }
        }

        @Override
        public void onVideoFrameCallbackGLES(String userId, int type, int texture, float[] matrix, int width, int height, long timestamp){

        }

        @Override
        public void onVideoFrameMixedGLES(int type, int texture, float[] matrix, int width, int height, long timestamp){
            SurfaceViewRenderer view = getRenderViewByIndex( 3 );
            if(view != null) {
                VideoBaseRenderer.I420Frame frame = new VideoBaseRenderer.I420Frame(width, height, 0, texture, matrix, type==1);
                frame.timestamp = timestamp;
                view.renderFrame(frame);
            } else {
                Log.e("VideoRenderer", "mixed SurfaceView is null");
            }
        }


    }
  */


}



