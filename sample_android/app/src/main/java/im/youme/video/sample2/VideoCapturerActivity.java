package im.youme.video.sample2;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.youme.voiceengine.CameraMgr;
import com.youme.voiceengine.MemberChange;
import com.youme.voiceengine.NativeEngine;
import com.youme.voiceengine.ScreenRecorder;
import com.youme.voiceengine.ScreenRecorderService;
import com.youme.voiceengine.VideoRenderer;
import com.youme.voiceengine.YouMeCallBackInterface;
import com.youme.voiceengine.YouMeCallBackInterfacePcm;
import com.youme.voiceengine.YouMeConst;
import com.youme.voiceengine.YouMeCustomDataCallbackInterface;
import com.youme.voiceengine.YouMeVideoPreDecodeCallbackInterface;
import com.youme.voiceengine.api;
import com.youme.voiceengine.video.EglBase;
import com.youme.voiceengine.video.RendererCommon;
import com.youme.voiceengine.video.SurfaceViewRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import im.youme.video.externalSample.RTCService;
import im.youme.video.externalSample.ScreenRecorder2;
import im.youme.video.externalSample.ScreenRecorder3;
import im.youme.video.videoRender.PercentFrameLayout;

import com.tencent.bugly.crashreport.CrashReport;


public class VideoCapturerActivity extends Activity implements YouMeCallBackInterface, View.OnClickListener, YouMeVideoPreDecodeCallbackInterface, View.OnTouchListener, YouMeCustomDataCallbackInterface {

    public class RenderInfo {
        public String userId;
        public int rendViewIndex;
        public boolean RenderStatus;
        public int userIndex;
    }

    private static String TAG = "VideoCapturerActivity";

    private static Boolean DEBUG = false;
    public static Boolean openSecondStream = false;

    ///声明video设置块相关静态变量
    public static String _serverIp = "0.0.0.0";
    public static int _serverPort = 0;
    public static int _videoWidth = 480;
    public static int _videoHeight = 640;
    public static int _maxBitRate = 0;
    public static int _minBitRate = 0;
    public static int _reportInterval = 3000;
    public static int _farendLevel = 10;
    public static String _LocalIP="192.168.0.1";
    public static int _LocalPort=50000;
    public static String _RemoteIP="192.168.0.1";
    public static int _RemotePort=50000;
    public static boolean _bHighAudio = false;
    public static boolean _bHWEnable = true;
    public static boolean _bBeautify = false;
    public static boolean _bTcp = false;
    public static boolean _bLandscape = false;
    public static boolean _bVBR = true;
    public static boolean _minDelay = false; //低延迟模式
    public static boolean _bTestmode = false;
    public static boolean _bTestP2P=false;
    public static int _fps = 24;
    public static String sdkIP = "";
    public static int sdkPort = 8012;
    public static String redirectIP = "";
    public static int redirectPort = 5574;


    public boolean inited = false;
    private ViewGroup mViewGroup;
    private ImageButton micBtn;
    private ImageButton speakerBtn;

    private SurfaceViewRenderer[] arrRenderViews = null;
    private static boolean lastTestmode = false;

    private MyHandler youmeVideoEventHandler;

    private TextView avTips = null;
    private Map<String, RenderInfo> renderInfoMap = null;
    private EditText cameraFPSEditText;
    private EditText shareFPSEditText;
    private EditText cameraWidthEditText;
    private EditText cameraHeightEditText;
    private EditText shareWidthEditText;
    private EditText shareHeightEditText;

    private int[] m_UserViewIndexEn = {0, 0, 0, 0,0,0,0};

    String local_user_id = null;
    int local_render_id = -1;
    int mUserCount = 0;
    boolean isP2P = false;
    boolean useUDPToSendCustomMessage = true;

    private boolean isJoinedRoom = false;
    private float beautifyLevel = 0.5f;

    private String currentRoomID = "";
    private int mFullScreenIndex = -1;
    private boolean isOpenCamera = false;
    private boolean needResumeCamera = false;
    private boolean micLock = false;
    private boolean isOpenShare = false;
    private boolean isOpenScreenrecord = false;
    private boolean isPreviewMirror = true;

    public static int mVideoShareWidth = 1080;
    public static int mVideoShareHeight = 1920;

    public static int mVideoCodec = 0;

    private Intent forgroundIntent;

    /**
     * 接受Video视图选择的服务器
     */
    static int RTC_XX_SERVER = 0;
    /**
     * 该状态是用来判断当前活跃的
     */
    private boolean activity;

    //private int[] mAVStatistic = new int[20];
    private HashMap<String, int[]> avStaticMap =  new HashMap<>();
    private Map<String, String> userLeaveMap = new HashMap<>();
    private Map<String, String> maskUsersMap = new HashMap<>();; //被屏蔽的用户列表缓存

    private float mScaleFactor = 0.0f;
    private float mLastZoomFactor = 1.0f;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;
    private View mFocusView;
    private Timer resetTimer;
    private Button mButton;

    public static boolean bTokenV3 = false;

    private static FileOutputStream mPreDecodeFos = null;
    private static int mPreDecodeCounter = 0;
    OrientationEventListener mOrientationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        RTCService.contentTitle = "YoumeVideoDemo";
        RTCService.contentText = "运行中...";
        SharedPreferences sharedPreferences = getSharedPreferences("demo_settings", Context
                .MODE_PRIVATE);
        _LocalIP = (sharedPreferences.getString("_LocalIP","192.168.0.1"));
        _LocalPort = (sharedPreferences.getInt("_LocalPort",50000));
        _RemoteIP = (sharedPreferences.getString("_RemoteIP","192.168.0.1"));
        _RemotePort = (sharedPreferences.getInt("_RemotePort",50000));
        _bTestP2P = (sharedPreferences.getBoolean("_bTestP2P",false));


        setContentView(R.layout.activity_video_capturer);
        initCtrls();

        //取得启动该Activity的Intent对象
        Intent intent1 = getIntent();
        local_user_id = intent1.getStringExtra("userid");
        currentRoomID = intent1.getStringExtra("roomid");

        int area = intent1.getIntExtra("Area", 0);

        if (RTC_XX_SERVER != area || _bTestmode != lastTestmode) {
            inited = false;
            api.unInit();
        }
        RTC_XX_SERVER = area;//YouMeConst.YOUME_RTC_SERVER_REGION.RTC_DEFAULT_SERVER;

        renderInfoMap = new HashMap<>();
        ///初始化界面相关数据
        arrRenderViews = new SurfaceViewRenderer[7];
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //应用运行时，保持屏幕高亮，不锁屏

        youmeVideoEventHandler = new MyHandler(this);
        avTips = (TextView) findViewById(R.id.avtip);

        mFocusView = findViewById(R.id.camera_focus);
        mGestureDetector = new GestureDetector(this, simpleOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(this, scaleGestureListener);

        CameraMgr.setCameraAutoFocusCallBack(cameraFocusCallback);
        //定时重置avstatic 统计信息
        resetTimer = new Timer();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<String, int[]> entry : avStaticMap.entrySet()){
                    String userid = entry.getKey();
                    int[] mAVStatistic = entry.getValue();
                    for (int i = 0; i < 15; i++) {
                        mAVStatistic[i] = 0;
                    }
                }
            }
        }, 10000, 10000);

        initSDK();

        addOrientationListener();

        /*远程控制演示*/
        mViewGroup = (ViewGroup) findViewById(R.id.activity_video_capturer);
        mButton = (Button) findViewById(R.id.id_text);
        mButton.setOnTouchListener(this);

        //配置共享流的分辨率为屏幕的1/4
//        DisplayMetrics metric = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getRealMetrics(metric);
//        mVideoShareWidth = metric.widthPixels/2; // 宽度（PX）
//        mVideoShareHeight = metric.heightPixels/2; // 高度（PX）
    }

    private OrientationReciver mOrientationReciver;
    private void addOrientationListener() {
        removeOrientationListener();

        mOrientationReciver = new OrientationReciver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        registerReceiver(mOrientationReciver,intentFilter);
    }

    private void removeOrientationListener()
    {
        if(mOrientationReciver !=null)
            unregisterReceiver(mOrientationReciver);
    }

    private class OrientationReciver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            int rotation = VideoCapturerActivity.this.getWindowManager().getDefaultDisplay().getRotation() * 90;
            Log.i("Orientation", "onReceive Orientation: " + rotation);
            if(rotation == 90 || rotation == 270){
                ScreenRecorder.orientationChange(rotation,mVideoShareHeight, mVideoShareWidth);
            }else{
                ScreenRecorder.orientationChange(rotation,mVideoShareWidth, mVideoShareHeight);
            }
        }
    }

    private int xDelta;
    private int yDelta;
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        final int x = (int) event.getRawX();
        final int y = (int) event.getRawY();
        Log.d(TAG, "onTouch: x= " + x + "y=" + y);
        moveControl(event.getAction() & MotionEvent.ACTION_MASK, x,y, false);
        return true;
    }

    public void moveControl(int event,int x,int y, boolean fromRemote)
    {
        String message = "";
        switch (event) {
            case MotionEvent.ACTION_DOWN:
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mButton
                        .getLayoutParams();
                if(!fromRemote) {
                    xDelta = x - params.leftMargin;
                    yDelta = y - params.topMargin;
                    message = event + "|" + params.leftMargin + "|" + params.topMargin;
                }else{
                    //对齐远端点击时的位置
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mButton
                            .getLayoutParams();
                    layoutParams.leftMargin = x;
                    layoutParams.topMargin = y;
                    mButton.setLayoutParams(layoutParams);
                }

                Log.d(TAG, "ACTION_DOWN: xDelta= " + xDelta + "yDelta=" + yDelta);
                break;
            case MotionEvent.ACTION_MOVE:
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mButton
                        .getLayoutParams();
                int xDistance = x - xDelta;
                int yDistance = y - yDelta;
                Log.d(TAG, "ACTION_MOVE: xDistance= " + xDistance + "yDistance=" + yDistance);
                layoutParams.leftMargin = xDistance;
                layoutParams.topMargin = yDistance;
                mButton.setLayoutParams(layoutParams);
                message = event + "|" + x + "|" + y;
                break;
        }
        if(!fromRemote) {
            if (useUDPToSendCustomMessage) {
                api.inputCustomData(message.getBytes(), message.getBytes().length, System.currentTimeMillis());
            } else {
                api.sendMessage(currentRoomID, message);
            }
        }
        mViewGroup.invalidate();
    }

    @Override
    public void onClick(View v) {

    }


    private void initSDK() {
        ///初始化SDK相关设置

        if (_bTestmode) {
            NativeEngine.setServerMode(1);
        } else {
            NativeEngine.setServerMode(0);
        }
        String modeTips = "[正常模式]";
        if(!sdkIP.equals("") && _bTestmode){
            if(!sdkIP.equals(_serverIp)){
                api.unInit();
            }
            _serverIp = sdkIP;
            _serverPort = sdkPort;
            Log.d(TAG, "sdk serverip: " + _serverIp + ", port: " + _serverPort);
            NativeEngine.setServerMode(NativeEngine.SERVER_MODE_FIXED_IP_VALIDATE);
            NativeEngine.setServerIpPort(_serverIp, _serverPort);
            modeTips = "[sdk:"+_serverIp+"]";
        }else if(!redirectIP.equals("") && _bTestmode){
            if(!redirectIP.equals(_serverIp)){
                api.unInit();
            }
            _serverIp = redirectIP;
            _serverPort = redirectPort;
            Log.d(TAG, "redirect serverip: " + _serverIp + ", port: " + _serverPort);
            NativeEngine.setServerMode(NativeEngine.SERVER_MODE_FIXED_IP_REDIRECT);
            NativeEngine.setServerIpPort(_serverIp, _serverPort);
            modeTips = "[rdt:"+_serverIp+"]";
        }

        lastTestmode = _bTestmode;
        CrashReport.setAppVersion(this, "in." + api.getSdkInfo());
        api.setLogLevel(YouMeConst.YOUME_LOG_LEVEL.LOG_INFO, YouMeConst.YOUME_LOG_LEVEL.LOG_INFO);
        api.SetCallback(this);

        if(bTokenV3) {
            //设置测试服还是正式服
            NativeEngine.setServerMode(NativeEngine.SERVER_MODE_FIXED_IP_MCU);
            NativeEngine.setServerIpPort("39.106.60.66", 6006);
        }


        ToastMessage.showToast(this,"安全校验中: "+modeTips,1000);
        // 调用初始化
        int code = api.init(CommonDefines.appKey, CommonDefines.appSecret, RTC_XX_SERVER, "");
        // 设置本段登录的 userid
        VideoRendererSample.getInstance().setLocalUserId(local_user_id);
        // 设置视频数据接收回调接收对象
        api.setVideoFrameCallback(VideoRendererSample.getInstance());

        if (code == YouMeConst.YouMeErrorCode.YOUME_ERROR_WRONG_STATE) {
            //已经初始化过了，就不等初始化回调了，直接进频道就行
            autoJoinClick();
            inited = true;
        }

        Log.i("区域", "" + RTC_XX_SERVER);
    }


    private void initRender(int index, @IdRes int layoutId, @IdRes int viewId) {
        if (index < 0 || index > arrRenderViews.length || arrRenderViews[index] != null) {
            return;
        }
        SurfaceViewRenderer renderView = (SurfaceViewRenderer) this.findViewById(viewId);
        if (index != 6) {
            PercentFrameLayout layout = (PercentFrameLayout) this.findViewById(layoutId);
            layout.setPosition(0, 0, 100, 100);
        } else {
            renderView.setVisibility(View.INVISIBLE);
        }
        renderView.init(EglBase.createContext(api.sharedEGLContext()), null);
        renderView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        renderView.setMirror(false);
        renderView.setRenderBackgroundColor(0,0,0,0);
        arrRenderViews[index] = renderView;
    }

    private void releaseRender() {
        for (int i = 0; i < arrRenderViews.length; i++) {
            if (arrRenderViews[i] != null) {
                arrRenderViews[i].release();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetTimer.cancel();
        if(forgroundIntent != null){
            try {
                this.stopService(forgroundIntent);
            }catch (Throwable e){
                e.printStackTrace();
            }
        }
        removeOrientationListener();
        releaseRender();
        ScreenRecorder.stopScreenRecorder();
        VideoRendererSample.getInstance().deleteAllRender();
    }

    private void startMic() {
        api.setMicrophoneMute(false);
    }

    private void stopMic() {
        api.setMicrophoneMute(true);
    }

    void setZoomFactor(float factor) {

        mLastZoomFactor = factor;
    }

    private void startCamera() {
        api.startCapturer();
        isOpenCamera = true;
    }

    private void stopCamera() {
        api.stopCapturer();
        isOpenCamera = false;
    }

    private void switchCamera() {
        api.switchCamera();
    }

    private void switchRotation() {
        String UserId = IndexToUserId(mFullScreenIndex);
        if (UserId != null) {
            VideoRendererSample.getInstance().switchRotation(UserId);
        }
    }

    private void resetAllRender() {
        for (RenderInfo info : renderInfoMap.values()) {
            VideoRenderer.getInstance().deleteRender(info.userId);

            SurfaceViewRenderer renderView = getRenderViewByIndex(info.rendViewIndex);

            if (renderView != null) {
                renderView.clearImage();
            }
        }

        renderInfoMap.clear();
        for (int i = 0; i < m_UserViewIndexEn.length; i++) {
            m_UserViewIndexEn[i] = 0;
        }

        //清理自己/合流的视频显示
        {
            if (local_render_id != -1) {
                VideoRenderer.getInstance().deleteRender(local_user_id);
            }

            SurfaceViewRenderer renderView = getRenderViewByIndex(6);
            if (renderView != null) {
                renderView.clearImage();
            }
        }

    }

    private void resetStatus() {
//    api.setCaptureFrontCameraEnable(true);
        stopCamera();
        startCamera();
    }

    private boolean RTCServiceStarted = false;
    protected void onPause() {
        // 放到后台时完全暂停: api.pauseChannel();
        if (!activity) {
            api.setVideoFrameCallback(null);
            needResumeCamera = isOpenCamera;
            //stopCamera();
            activity = true;
        }
//        if(api.isInChannel() && !RTCServiceStarted)
//        {
//            try{
//                RTCService.mContext = getApplicationContext();
//                RTCService.mActivity = this;
//                if(RTCService.mContext !=null && RTCService.mActivity!=null) {
//                    forgroundIntent = new Intent(this, RTCService.class);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        this.startForegroundService(forgroundIntent);
//                    } else {
//                        this.startService(forgroundIntent);
//                    }
//                    RTCServiceStarted = true;
//                }
//            }catch (Throwable e){
//                RTCServiceStarted = false;
//                e.printStackTrace();
//            }
//        }
//    Object[] obj = renderInfoMap.values().toArray(new Object[renderInfoMap.values().size()]);
//    for (int i= obj.length - 1 ;i > -1; i--) {
//        shutDOWN(((RenderInfo)obj[i]).userId);
//    }
        super.onPause();
    }

    protected void onResume() {
        //设置横屏
        if (_bLandscape && getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "rotation:" + this.getWindowManager().getDefaultDisplay().getRotation());
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            Log.d(TAG, "rotation end:" + this.getWindowManager().getDefaultDisplay().getRotation());
            api.setScreenRotation(this.getWindowManager().getDefaultDisplay().getRotation());
        }

        super.onResume();

        //切回前台的时候恢复
        //   api.resumeChannel();
        if (activity) {
            if (needResumeCamera) startCamera();
            needResumeCamera = false;
            api.setVideoFrameCallback(VideoRendererSample.getInstance());
            activity = false;
        }

//        if(forgroundIntent != null){
//            RTCServiceStarted = false;
//            try {
//                this.stopService(forgroundIntent);
//            }catch (Throwable e){
//                e.printStackTrace();
//            }
//        }

//    Object[] obj = renderInfoMap.values().toArray(new Object[renderInfoMap.values().size()]);
//    for (int i= obj.length - 1 ;i > -1; i--) {
//        shutDOWN(((RenderInfo)obj[i]).userId);
//    }
    }

    private SurfaceViewRenderer getRenderViewByIndex(int index) {
        if (index < 0 || index > arrRenderViews.length) {
            return null;
        } else {
            return arrRenderViews[index];
        }
    }


    private void updateNewView(final String newUserId, final int index) {
        final SurfaceViewRenderer renderView = getRenderViewByIndex(index);
        if (renderView == null) {
            return;
        }

        renderView.setVisibility(View.VISIBLE);
        if (newUserId != local_user_id) {
            renderView.setZOrderOnTop(true);
        }
        //renderView.clearImage();

        VideoRendererSample.getInstance().addRender(newUserId, renderView);
        RenderInfo info = new RenderInfo();
        info.userId = newUserId;
        info.rendViewIndex = index;
        info.RenderStatus = true;
        info.userIndex = ++mUserCount;
        renderInfoMap.put(newUserId, info);
        m_UserViewIndexEn[index] = 1;

    }


    /// 回调数据
    @Override
    public void onEvent(int event, int error, String room, Object param) {
        ///里面会更新界面，所以要在主线程处理
        Log.i(TAG, "event:" + CommonDefines.CallEventToString(event) + ", error:" + error + ", room:" + room + ",param:" + param);
        Message msg = new Message();
        Bundle extraData = new Bundle();
        extraData.putString("channelId", room);
        msg.what = event;
        msg.arg1 = error;
        msg.obj = param;
        msg.setData(extraData);

        youmeVideoEventHandler.sendMessage(msg);
    }

    @Override
    public void onRequestRestAPI(int requestID, int iErrorCode, String strQuery, String strResult) {

    }

    @Override
    public void onMemberChange(String channelID, final MemberChange[] arrChanges, boolean isUpdate) {
        /**
         * 离开频道时移除该对象的聊天画面
         */
        Log.i(TAG, "onMemberChange:" + channelID + ",isUpdate:" + isUpdate);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了
                for (int i = 0; i < arrChanges.length; i++) {
                    if (arrChanges[i].isJoin == false) {
                        userLeaveMap.put(String.valueOf(arrChanges[i].userID),"");
                        String leaveUserId = String.valueOf(arrChanges[i].userID);
                        shutDOWN(leaveUserId);
                        shutDOWN(leaveUserId+"_share");
                        if(avStaticMap.get(leaveUserId) != null){
                            avStaticMap.remove(leaveUserId);
                        }
                    }else{
                        if(userLeaveMap.containsKey(String.valueOf(arrChanges[i].userID))){
                            userLeaveMap.remove(String.valueOf(arrChanges[i].userID));
                        }
                        String []userArray= new String[1];
                        int [] resolutionArray = new int[1];

                        userArray[0] = String.valueOf(arrChanges[i].userID);;
                        resolutionArray[0] = 1; //0 为高清流， 1 为低清流
                        // 调用批量订阅接口
                        VideoCapturerActivity.this.setRecvStream(userArray, resolutionArray);
                    }
                }
            }
        });
    }

    @Override
    public void onBroadcast(int bc, String room, String param1, String param2, String content) {

    }

    @Override
    public void onAVStatistic(final int avType, final String strUserID, final int value) {

        if (youmeVideoEventHandler != null) {
            youmeVideoEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(avStaticMap.get(strUserID) == null){
                        avStaticMap.put(strUserID,new int[30]);
                    }
                    int[] tmpAVStatistic = avStaticMap.get(strUserID);
                    tmpAVStatistic[avType] = value;
                    String tips = "";
                    for (Map.Entry<String, int[]> entry : avStaticMap.entrySet()){
                        String userid = entry.getKey();
                        int[] mAVStatistic = entry.getValue();
                        String direction = userid.endsWith(local_user_id) ?"Up":"Down";
                        tips += (userid.endsWith(local_user_id) ? userid+"(self)":userid) +"\n" +
                                "FPS: " + (mAVStatistic[3]) + "\n" +
                                "shareFPS: " + (mAVStatistic[15]) + "\n" +
                                "audio"+direction+": " + mAVStatistic[1] * 8 / 1000 + "kbps\n" +
                                "video"+direction+": " + mAVStatistic[2] * 8 / 1000 + "kbps\n" +
                                "UpLoss: " + mAVStatistic[6] + "‰ \n" +
                                "DownLoss: " + mAVStatistic[7] + "‰ \n" +
                                (!userid.endsWith(local_user_id) ? "rtt: " + mAVStatistic[9] + "ms \n" : "") +
                                (userid.endsWith(local_user_id) ? "" : "block: " + mAVStatistic[10]+" \n")+
                                (userid.endsWith(local_user_id) ? "DownBW: " + mAVStatistic[13] * 8 / 1000 + "kbps\n" :"" )+
                                (userid.endsWith(local_user_id) ? "connect:" + (isP2P? "p2p" : "server") + "\n" : "");
                    }
                    avTips.setText(tips);

                }
            });
        }

    }

    @Override
    public  void onAVStatisticNew( int avType,  String userID, int value, String param ){

    }
    
    @Override
    public void onTranslateTextComplete(int errorcode, int requestID, String text, int srcLangCode, int destLangCode)
    {

    }

    @Override
    public void onVideoPreDecode(String userId, byte[] data, int dataSizeInByte, long timestamp, int type) {
//        Log.i(TAG, "onVideoPreDecode:" + userId + ", type:" + type + ", size:" + dataSizeInByte + ", ts:" + timestamp);
        if (DEBUG) {
            if (mPreDecodeFos != null) {
                try {
                    mPreDecodeFos.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void joinOK(String roomid) {
        currentRoomID = roomid;
        if (!isJoinedRoom) {
            Log.d(TAG, "进频道成功");
            //api.maskVideoByUserId(userid,block);
            //进频道成功后可以设置视频回调
            api.SetVideoCallback();
            //设置远端语音音量回调
            api.setFarendVoiceLevelCallback(10);
            //开启扬声器
            api.setSpeakerMute(false);

            isJoinedRoom = true;
            //远端视频渲染的view
            initRender(0, R.id.PercentFrameLayout0, R.id.SurfaceViewRenderer0);
            initRender(1, R.id.PercentFrameLayout1, R.id.SurfaceViewRenderer1);
            initRender(2, R.id.PercentFrameLayout2, R.id.SurfaceViewRenderer2);
            //本地视频
            initRender(6, R.id.remote_video_view_twelve1, R.id.remote_video_view_twelve1);

            initRender(3, R.id.PercentFrameLayout3, R.id.SurfaceViewRenderer3);
            initRender(4, R.id.PercentFrameLayout4, R.id.SurfaceViewRenderer4);
            initRender(5, R.id.PercentFrameLayout5, R.id.SurfaceViewRenderer5);
            VideoRendererSample.getInstance().setLocalUserId(local_user_id);
            updateNewView(local_user_id, 6);
            autoOpenStartCamera();
            mFullScreenIndex = 6;
        }
    }

    public void nextViewShow() {

        int validIndex = -1;
        int minIndex = 999999;
        String userId = "";

        for (int i = 0; i < 6; i++) {
            if (m_UserViewIndexEn[i] == 0) {
                validIndex = i;
                break;
            }
        }
        if (validIndex == -1)
            return;

        for (RenderInfo vaule : renderInfoMap.values()) {
            if (!vaule.RenderStatus && vaule.userIndex <= minIndex) {
                minIndex = vaule.userIndex;
                userId = vaule.userId;
            }
        }

        if (minIndex != 999999) {
            renderInfoMap.remove(userId);
            updateNewView(userId, validIndex);
        }

    }

    public void shutDOWN(String userId) {

        RenderInfo info = renderInfoMap.get(userId);
        if (info == null)
            return;

        if (info.RenderStatus) {
            if (mFullScreenIndex == info.rendViewIndex) {
                switchFullScreen(6);
            }
            final SurfaceViewRenderer renderView = getRenderViewByIndex(info.rendViewIndex);
            renderView.clearImage();
            renderView.setVisibility(View.INVISIBLE);
            VideoRendererSample.getInstance().deleteRender(userId);
            api.deleteRenderByUserID(userId);//不移除可能收不到VIDEO_ON事件
            m_UserViewIndexEn[info.rendViewIndex] = 0;
            nextViewShow();
        }
        renderInfoMap.remove(userId);

        //有人退出，接触屏蔽，触发重新分配
        for (Map.Entry<String, String> entry : maskUsersMap.entrySet()){
            api.deleteRenderByUserID(entry.getKey());//不移除可能收不到VIDEO_ON事件
            api.maskVideoByUserId(entry.getKey(), false);
        }
        maskUsersMap.clear();

    }


    public void videoON(String userId) {
        Log.d(TAG, "新加的user ID=" + userId);
        // 默认请求小流
        onVideoSwitch(userId, 1);

        int validIndex = -1;
        if (renderInfoMap.containsKey(userId))
            return;
        if (userId.equals(local_user_id)) {
            updateNewView(local_user_id, 6);
            return;
        }
        for (int i = 0; i < 6; i++) {
            if (m_UserViewIndexEn[i] == 0) {
                validIndex = i;
                break;
            }
        }
        if (validIndex != -1) {
            api.maskVideoByUserId(userId, false);
            updateNewView(userId, validIndex);
        } else {
            if(userId.indexOf("_share")<0) {
                //显示不下，就屏蔽了
                api.maskVideoByUserId(userId, true);
                maskUsersMap.put(userId,userId);
            }
        }

    }

    @Override
    public void onRecvCustomData(byte[] bytes, long l) {
        //模拟
        Message msg = new Message();
        Bundle extraData = new Bundle();
        extraData.putString("channelId", "");
        msg.what = 999999; //模拟成event事件
        msg.arg1 = 0;
        msg.obj = new String(bytes);
        msg.setData(extraData);

        youmeVideoEventHandler.sendMessage(msg);
    }


    private static class MyHandler extends Handler {

        private final WeakReference<VideoCapturerActivity> mActivity;

        public MyHandler(VideoCapturerActivity activity) {
            mActivity = new WeakReference<VideoCapturerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoCapturerActivity activity = mActivity.get();
            if (activity != null) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case YouMeConst.YouMeEvent.YOUME_EVENT_INIT_OK:
                        Log.d(TAG, "初始化成功");
                        ToastMessage.showToast(mActivity.get(),"初始化成功",1000);
                        activity.autoJoinClick();
                        activity.inited = true;
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_INIT_FAILED:
                        Log.d(TAG, "初始化失败");
                        ToastMessage.showToast(mActivity.get(),"初始化失败，重试",3000);
                        activity.inited = false;
                        mActivity.get().initSDK();
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_JOIN_OK:
                        ToastMessage.showToast(mActivity.get(),"已经进入通话频道",1000);
                        String roomId = msg.getData().getString("channelId");
                        activity.joinOK(roomId);
                        TimerTask task = new TimerTask() {
                            public void run() {
                                // 停止播放铃声
                                //BackgroundMusic.getInstance(null).stopBackgroundMusic();
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(task, 1000);
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_JOIN_FAILED:
                        ToastMessage.showToast(mActivity.get(),"已经进入通话频道失败:"+msg.arg1,1500);
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_LEAVED_ALL:
                        api.removeMixAllOverlayVideo();
                        activity.leavedUI();
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_INPUT_START:
                        Log.d(TAG, "YOUME_EVENT_OTHERS_VIDEO_INPUT_START");
                        String userID =  String.valueOf(msg.obj);
                        String[] userArray = {userID};
                        int[] resolutionArray = {1};
                        // 用户开启了视频输入设备，默认订阅他的低清视频流
                        activity.setRecvStream(userArray, resolutionArray);
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_ON: {
                        Log.d(TAG, "YOUME_EVENT_OTHERS_VIDEO_ON:" + msg.obj);
                        String newUserId1 = String.valueOf(msg.obj);
                        if(newUserId1.indexOf("_share")>-1){
                            newUserId1 = newUserId1.substring(0,newUserId1.length()-6);
                        }
                        if (activity.userLeaveMap.containsKey(newUserId1)) {
                            api.deleteRenderByUserID(String.valueOf(msg.obj));//不移除可能收不到VIDEO_ON事件
                            Log.d(TAG, "YOUME_EVENT_OTHERS_VIDEO_ON not found:" + newUserId1);
                        }else{
                            activity.videoON(String.valueOf(msg.obj));
                        }
                    }
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_INPUT_STOP:
                        Log.d(TAG, "YOUME_EVENT_OTHERS_VIDEO_INPUT_STOP");
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_OTHERS_VIDEO_SHUT_DOWN:
                        Log.d(TAG, "YOUME_EVENT_OTHERS_VIDEO_SHUT_DOWN");
                        //超过时间收不到新的画面，就先移除掉
                        String leaveUserId = String.valueOf(msg.obj);
                        activity.shutDOWN(leaveUserId);
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_LOCAL_MIC_OFF:
                        if( msg.arg1 == 0) activity.micBtn.setSelected(true); // 自己关闭麦克风
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_LOCAL_MIC_ON:
                        if( msg.arg1 == 0) activity.micBtn.setSelected(false); // 自己打开麦克风
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_MIC_CTR_OFF:
                        activity.micLock = true;
                        activity.micBtn.setSelected(true); // 主持人关闭我的麦克风
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_MIC_CTR_ON:
                        activity.micLock = false;
                        activity.micBtn.setSelected(false); // 主持人打开麦我的克风
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_MASK_VIDEO_BY_OTHER_USER:
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_RESUME_VIDEO_BY_OTHER_USER:
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_MASK_VIDEO_FOR_USER:
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_RESUME_VIDEO_FOR_USER:
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_QUERY_USERS_VIDEO_INFO:
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_SET_USERS_VIDEO_INFO:

                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_VIDEO_ENCODE_PARAM_REPORT:
                        Log.d(TAG, "YOUME_EVENT_VIDEO_ENCODE_PARAM_REPORT");
                        String param = String.valueOf(msg.obj);
                        Log.d(TAG, "param:" + param);
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_RTP_ROUTE_P2P:
                        // p2p通路检测ok
                        activity.isP2P = true;
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_RTP_ROUTE_SEREVER:
                        // p2p通路检测失败，根据业务需求是否退出房间 或者切换server转发
                        activity.isP2P = false;
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_RTP_ROUTE_CHANGE_TO_SERVER:
                        // p2p传输过程中连接异常，切换server转发
                        activity.isP2P = false;
                        break;
                    case 10000: //avStatistic回调
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_FAREND_VOICE_LEVEL:

                        break;
                    case 999999:
                    case YouMeConst.YouMeEvent.YOUME_EVENT_MESSAGE_NOTIFY:
                        // 用于演示远程信令控制
                        String message = String.valueOf(msg.obj);
                        Log.d("MESSAGE",message);
                        String[] x = message.split("\\|");
                        if(x.length ==3){
                            Log.d("MESSAGE","start moveControl");
                            mActivity.get().moveControl(Integer.valueOf(x[0]).intValue(),Integer.valueOf(x[1]).intValue(),Integer.valueOf(x[2]).intValue(),true);
                        }
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_LOCAL_SHARE_INPUT_START:
                        Button shareBtn1 = (Button)mActivity.get().findViewById(R.id.vt_btn_share);
                        shareBtn1.setText("停止共享");
                        mActivity.get().isOpenShare = true;
                        break;
                    case YouMeConst.YouMeEvent.YOUME_EVENT_LOCAL_SHARE_INPUT_STOP:
                        Button shareBtn2 = (Button)mActivity.get().findViewById(R.id.vt_btn_share);
                        shareBtn2.setText("开始共享");
                        mActivity.get().isOpenShare = false;
                        break;
                }
            }
        }
    }

    public void leaveChannel() {
        api.removeMixAllOverlayVideo();
        api.leaveChannelAll();

        if (DEBUG) {
            if (mPreDecodeFos != null) {
                try {
                    mPreDecodeFos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        finish();
        Intent intent = new Intent();
        intent.setClass(VideoCapturerActivity.this, Video.class);
    }

    public void leavedUI() {
        finish();
        Intent intent = new Intent();
        intent.setClass(VideoCapturerActivity.this, Video.class);
    }

    @Override
    public void onBackPressed() {
        //BackgroundMusic.getInstance(this).stopBackgroundMusic();
        leaveChannel();
    }


    /**
     * 自动进入频道方法   在初始化后面使用
     */
    private void autoJoinClick() {
        // 录屏模块初始化
        ScreenRecorder.init(this);
        ScreenRecorder.setResolution(mVideoShareWidth, mVideoShareHeight);
        ScreenRecorder.setFps(_fps);
        
        //每次进入房间都重新走一遍流程，防止上次退出房间超时，导致后面进入相同房间时接口未调用
        api.setTCPMode(_bTcp);
        if (_fps >= 24) {
            api.setVideoPreviewFps(_fps);
        } else {
            api.setVideoPreviewFps(24);
        }

        //加入频道前进行video设置
        api.setVideoFps(_fps);
        api.setVideoFpsForSecond(12);
        //设置本地采集分辨率
        api.setVideoLocalResolution(_videoWidth, _videoHeight);
        //调用这个方法来设置视屏的分辨率
        api.setVideoNetResolution(_videoWidth, _videoHeight);

        //NativeEngine.setVideoEncodeParamCallbackEnable(true);

        int child_width = 160;//_videoWidth/2;
        int child_height = 224;//_videoHeight/2;

        /*
        if (child_width * child_height <= 240*320) {
            child_width = 240;
            child_height = 320;
        } else if (child_width * child_height >= 960 * 540){
            child_width = _videoWidth/4;
            child_height = _videoHeight/4;
        }
        */

        //设置视频小流分辨率
        if(openSecondStream) {
            api.setVideoNetResolutionForSecond(child_width, child_height);
        }
        api.setVideoFpsForShare(_fps);
        api.setVideoNetResolutionForShare(mVideoShareWidth, mVideoShareHeight);

        //调用这个方法来设置时间间隔
        api.setAVStatisticInterval(_reportInterval);
        //设置视频编码比特率
        api.setVideoCodeBitrate(_maxBitRate, _minBitRate);
        api.setVideoCodeBitrateForShare(_maxBitRate,_minBitRate);
        //设置远端语音水平回调
        api.setFarendVoiceLevelCallback(_farendLevel);
        //设置视屏是软编还是硬编
        api.setVideoHardwareCodeEnable(_bHWEnable);
        //同步状态给其他人
        api.setAutoSendStatus(true);
        // 设置视频无帧渲染的等待超时时间，超过这个时间会给上层回调YOUME_EVENT_OTHERS_VIDEO_SHUT_DOWN, 单位ms
        api.setVideoNoFrameTimeout(5000);

        api.setVideoCodecType( mVideoCodec );

        if (DEBUG) {
            api.setVideoPreDecodeCallbackEnable(this, true);
            if (mPreDecodeFos == null) {
                String path = String.format(getExternalFilesDir("").toString() + "/predecode_dump_%d.h264", mPreDecodeCounter++);
                File file = new File(path);
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    mPreDecodeFos = new FileOutputStream(file); //建立一个可以存取字节的文件
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //api.setVideoFrameRawCbEnabled(true);
        if (_bHighAudio) {
            api.setAudioQuality(1);//高音质，48k，流量要求高
        }
        api.setVBR(_bVBR);

        /* 特殊音频设备适配
          AudioManager mAudioManager =(AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
          mAudioManager.setParameters("mic_group= handhold-amic");
        */
        //api.setReleaseMicWhenMute(true);

        int sampleRate = 44100;
        int channels = 1;
        api.setPcmCallbackEnable(mOnYouMePcm, YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Remote |
                YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Record |
                YouMeConst.YouMePcmCallBackFlag.PcmCallbackFlag_Mix, true , 48000, 1);

        if(_bTestP2P){
            api.setLocalConnectionInfo(_LocalIP, _LocalPort, _RemoteIP, _RemotePort);
            api.setRouteChangeFlag(true);
        } else {
            api.clearLocalConnectionInfo();
        }

        api.setRecvCustomDataCallback(this);

        if(bTokenV3) {
            // 加入房间之前开始鉴权，客户端向sdk传入token和timestamp
            long startTime = System.currentTimeMillis() / 1000;
            String targetString = String.format(CommonDefines.appKey + CommonDefines.appKey + currentRoomID + local_user_id + Long.toString(startTime));
            String testLowerCaseToken = ShaOneEncrypt.encryptToSHA(targetString);
            Log.d(TAG, "initSDK: bruce >>> targetString:" + targetString);
            Log.d(TAG, "initSDK: bruce >>> testLowerCaseToken:" + testLowerCaseToken);
            api.setTokenV3(testLowerCaseToken, startTime);
        }
        ToastMessage.showToast(this,"进入通话频道中...",1000);
        api.setVideoSmooth(_minDelay ? 0 : 1);//低延迟模式不开启nack重传平滑
        api.joinChannelSingleMode(local_user_id, currentRoomID, YouMeConst.YouMeUserRole.YOUME_USER_HOST, openSecondStream ? false : true);
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

            String filename = getExternalFilesDir("").toString() + File.separator + fileName;
            try
            {
                File file= new File( filename );
                if( !file.exists() )
                {
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
//            if( mNeedDump )
//            {
//                if( fileRemote == null )
//                {
//                    fileRemote = createFileWriter( "dump_remote.pcm");
//                }
//                dumpPcm( fileRemote, data  );
//            }
//
//
//            if ((mPcmFrameCount % 500) == 0) {
//                Log.i(CommonDefines.LOG_TAG, "Remote: ++PCM callback, channelNum:" + channelNum + ", sf:" + samplingRateHz + ", bytesPerSample:" + bytesPerSample
//                        + ", data_size:" + data.length);
//            }
//            mPcmFrameCount++;
        }

        @Override
        public void onPcmDataRecord(int channelNum, int samplingRateHz, int bytesPerSample, byte[] data) {
//            if( mNeedDump )
//            {
//                if( fileRecord == null )
//                {
//                    fileRecord = createFileWriter( "dump_record.pcm");
//                }
//                dumpPcm( fileRecord, data  );
//            }
//
//            if ((mPcmFrameCount % 500) == 0) {
//                Log.i(CommonDefines.LOG_TAG, "Record:++PCM callback, channelNum:" + channelNum + ", sf:" + samplingRateHz + ", bytesPerSample:" + bytesPerSample
//                        + ", data_size:" + data.length);
//            }
//            mPcmFrameCount++;
        }

        @Override
        public void onPcmDataMix(int channelNum, int samplingRateHz, int bytesPerSample, byte[] data) {
//            if( mNeedDump )
//            {
//                if( fileMix == null )
//                {
//                    fileMix = createFileWriter( "dump_mix.pcm");
//                }
//                dumpPcm( fileMix, data  );
//            }
//
//            if ((mPcmFrameCount % 500) == 0) {
//                Log.i(CommonDefines.LOG_TAG, "Mix:++PCM callback, channelNum:" + channelNum + ", sf:" + samplingRateHz + ", bytesPerSample:" + bytesPerSample
//                        + ", data_size:" + data.length);
//            }
//            mPcmFrameCount++;
        }
    };


    /**
     * 自动打开摄像头
     */
    private void autoOpenStartCamera() {

        //开启摄像头
        startCamera();

        //设置视频无渲染帧超时等待时间，单位毫秒
        api.setVideoNoFrameTimeout(5000);


    }

    private void initCtrls() {
        ButtonClickListener clickListener = new ButtonClickListener();
        ImageButton cameraBtn = (ImageButton) findViewById(R.id.vt_btn_camera);
        cameraBtn.setOnClickListener(clickListener);
        micBtn = (ImageButton) findViewById(R.id.vt_btn_mic);
        micBtn.setSelected(true);
        micBtn.setOnClickListener(clickListener);
        speakerBtn = (ImageButton) findViewById(R.id.vt_btn_speaker);
        speakerBtn.setOnClickListener(clickListener);
        cameraFPSEditText = (EditText) findViewById(R.id.fps_edittext);
        shareFPSEditText  = (EditText) findViewById(R.id.fps_share_edittext);
        cameraWidthEditText  = (EditText) findViewById(R.id.w_edittext);
        cameraHeightEditText  = (EditText) findViewById(R.id.h_edittext);
        shareWidthEditText =  (EditText) findViewById(R.id.w_share_edittext);
        shareHeightEditText  = (EditText) findViewById(R.id.h_share_edittext);

        cameraFPSEditText.setText(Integer.toString(_fps));
        shareFPSEditText.setText(Integer.toString(_fps));
        cameraWidthEditText.setText(Integer.toString(_videoWidth));
        cameraHeightEditText.setText(Integer.toString(_videoHeight));
        shareWidthEditText.setText(Integer.toString(mVideoShareWidth));
        shareHeightEditText.setText(Integer.toString(mVideoShareHeight));

        ImageButton closeBtn = (ImageButton) findViewById(R.id.vt_btn_close);
        closeBtn.setOnClickListener(clickListener);
        ImageButton switchCameraBtn = (ImageButton) findViewById(R.id.vt_btn_switch_camera);
        switchCameraBtn.setOnClickListener(clickListener);
        ImageButton swtichRotation = (ImageButton) findViewById(R.id.vt_btn_Render_Rotation);
        swtichRotation.setOnClickListener(clickListener);

        ImageButton previewMirror = (ImageButton) findViewById(R.id.vt_btn_preview_mirror);
        previewMirror.setOnClickListener(clickListener);

        Button shareBtn = (Button)findViewById(R.id.vt_btn_share);
        shareBtn.setOnClickListener(clickListener);
        shareBtn.setText("开始共享");

        Button screenrecordBtn = (Button)findViewById(R.id.vt_btn_screenrecord);
        screenrecordBtn.setOnClickListener(clickListener);
        screenrecordBtn.setText("开始录制");

        //clickListener.onClick(this.findViewById(R.id.vt_btn_mic));
        ((Button)findViewById(R.id.fps_setbutton)).setOnClickListener(clickListener);
        ((Button)findViewById(R.id.fps_share_setbutton)).setOnClickListener(clickListener);
        ((Button)findViewById(R.id.res_setbutton)).setOnClickListener(clickListener);
        ((Button)findViewById(R.id.res_share_setbutton)).setOnClickListener(clickListener);
    }

    //设置按钮监听
    private class ButtonClickListener implements View.OnClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            Log.i("按钮", "" + v.getId());
            switch (v.getId()) {
                case R.id.vt_btn_camera: {
                    v.setSelected(!v.isSelected());
                    boolean disableCamera = v.isSelected();
                    if (!disableCamera) {
                        startCamera();
                    } else {
                        stopCamera();
                    }

                }
                break;

                case R.id.vt_btn_mic: {
                    if(!micLock) {
//                    v.setSelected(!v.isSelected());
                        boolean disableMic = !v.isSelected();
                        if (disableMic) {
                            //关闭麦克风
                            stopMic();
                        } else {
                            //打开麦克风
                            startMic();
                        }
                    }
                }
                break;

                case R.id.vt_btn_speaker: {
                    v.setSelected(!v.isSelected());
                    boolean disableSpeaker = v.isSelected();
                    if (disableSpeaker) {
                        //关闭声音
                        api.setSpeakerMute(true);
                    } else {
                        //打开声音
                        api.setSpeakerMute(false);
                    }


                }
                break;

                case R.id.vt_btn_close: {
                    leaveChannel();

                }
                break;

                case R.id.vt_btn_switch_camera: {
                    switchCamera();

                }
                break;

                case R.id.vt_btn_Render_Rotation: {
                    switchRotation();
                }
                break;

                case R.id.vt_btn_preview_mirror: {
                    if (isPreviewMirror) {
                        api.setlocalVideoPreviewMirror(false);
                    } else {
                        api.setlocalVideoPreviewMirror(true);
                    }
                    isPreviewMirror = !isPreviewMirror;
                }
                break;

                case R.id.vt_btn_share: {
                    if(!isOpenShare) {
                        ScreenRecorder.startScreenRecorder();
//                        ((Button)v).setText("停止共享");
                    }else {
                        ScreenRecorder.stopScreenRecorder();
//                        ((Button)v).setText("开始共享");
                    }
                }
                break;
                case R.id.vt_btn_screenrecord: {
                    if(!isOpenScreenrecord) {
                        String path = String.format(getExternalFilesDir("").toString() + "/screen_dump.mp4");
                        ScreenRecorder.startScreenLocalSave(path);
                        ((Button)v).setText("停止录制");
                    }else {
                        ScreenRecorder.stopScreenLocalSave();
                        ((Button)v).setText("开始录制");
                    }
                    isOpenScreenrecord = !isOpenScreenrecord;
                }
                break;
                case R.id.fps_setbutton: {
                    api.setVideoFps( VideoSettings.getValue(VideoCapturerActivity.this.cameraFPSEditText.getText().toString().trim(),15));
                }
                break;
                case R.id.fps_share_setbutton: {
                    api.setVideoFpsForShare( VideoSettings.getValue(VideoCapturerActivity.this.shareFPSEditText.getText().toString().trim(),15));
                }
                break;
                case R.id.res_setbutton: {
                    api.setVideoNetResolution(
                            VideoSettings.getValue(VideoCapturerActivity.this.cameraWidthEditText.getText().toString().trim(),480),
                            VideoSettings.getValue(VideoCapturerActivity.this.cameraHeightEditText.getText().toString().trim(),640)
                    );
                }
                break;
                case R.id.res_share_setbutton: {
                    api.setVideoNetResolution(
                            VideoSettings.getValue(VideoCapturerActivity.this.shareWidthEditText.getText().toString().trim(),720),
                            VideoSettings.getValue(VideoCapturerActivity.this.shareHeightEditText.getText().toString().trim(),1280)
                    );
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ScreenRecorder.SCREEN_RECODER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= 29) {
                    ScreenRecorder.setScreenRecordNotification("YoumeVideoDemo", "正在共享屏幕内容");
                }

                ScreenRecorder.onActivityResult(requestCode, resultCode, data);

            }else {
                isOpenShare = false;
                isOpenScreenrecord = false;
            }
        }
    }

    private String IndexToUserId(int index) {
        for (RenderInfo info : renderInfoMap.values()) {
            if (info.rendViewIndex == index) {
                return info.userId;
            }
        }
        return null;
    }

    //大小流切换
    public void onVideoSwitch(String userId, int videoId){

        String []userArray= new String[1];
        int [] resolutionArray = new int[1];

        userArray[0] = userId;
        resolutionArray[0] = videoId;

        this.setRecvStream(userArray, resolutionArray);
    }

    private void setRecvStream(String[] userIds, int[] streamIds)
    {
        if(openSecondStream) {
            api.setUsersVideoInfo(userIds, streamIds);
        }
    }

    private void switchFullScreen(int index) {
        int tempIndex = mFullScreenIndex;
        if (mFullScreenIndex != -1) {
            String fullUserId = IndexToUserId(mFullScreenIndex);
            if (fullUserId != null) {
                VideoRendererSample.getInstance().deleteRender(fullUserId);
                VideoRendererSample.getInstance().deleteRender(local_user_id);
                VideoRendererSample.getInstance().addRender(fullUserId, getRenderViewByIndex(mFullScreenIndex));
                VideoRendererSample.getInstance().addRender(local_user_id, getRenderViewByIndex(6));

                // 请求小流
                onVideoSwitch(fullUserId, 1);
            }
            mFullScreenIndex = -1;
        }

        if (index != 6 && index != tempIndex) {
            String userId = IndexToUserId(index);
            if (userId != null) {
                VideoRendererSample.getInstance().deleteRender(local_user_id);
                VideoRendererSample.getInstance().deleteRender(userId);
                VideoRendererSample.getInstance().addRender(userId, getRenderViewByIndex(6));
                VideoRendererSample.getInstance().addRender(local_user_id, getRenderViewByIndex(index));
                mFullScreenIndex = index;

                // 请求大流
                onVideoSwitch(userId, 0);
            }
        }

    }

    public void onVideoViewClick(View v) {

        switch (v.getId()) {
            case R.id.SurfaceViewRenderer0:
                switchFullScreen(0);
                break;
            case R.id.SurfaceViewRenderer1:
                switchFullScreen(1);
                break;
            case R.id.SurfaceViewRenderer2:
                switchFullScreen(2);
                break;
            case R.id.SurfaceViewRenderer3:
                switchFullScreen(3);
                break;
            case R.id.SurfaceViewRenderer4:
                switchFullScreen(4);
                break;
            case R.id.SurfaceViewRenderer5:
                switchFullScreen(5);
                break;
            case R.id.remote_video_view_twelve1:
                switchFullScreen(6);
                break;
        }
    }


    //重写onTouchEvent方法 获取手势
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //识别手势
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    //操作类
    public ScaleGestureDetector.OnScaleGestureListener scaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (detector.getCurrentSpan() > mScaleFactor) {
                mLastZoomFactor += 0.3f;
            } else {
                mLastZoomFactor -= 0.3f;
            }
            if (api.isCameraZoomSupported() && mLastZoomFactor >= 1.0f) {
                //Log.i(TAG, "zoom scale:"+mLastZoomFactor);
                api.setCameraZoomFactor(mLastZoomFactor);
            }
            mScaleFactor = detector.getCurrentSpan();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mScaleFactor = detector.getCurrentSpan();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mScaleFactor = detector.getCurrentSpan();
        }
    };


    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                    api.isCameraFocusPositionInPreviewSupported()) {
                mFocusView.removeCallbacks(timeoutRunnable);
                mFocusView.postDelayed(timeoutRunnable, 1500);
                mFocusView.setVisibility(View.VISIBLE);

                RelativeLayout.LayoutParams focusParams = (RelativeLayout.LayoutParams) mFocusView.getLayoutParams();
                focusParams.leftMargin = (int) e.getX() - focusParams.width / 2;
                focusParams.topMargin = (int) e.getY() - focusParams.height / 2;
                mFocusView.setLayoutParams(focusParams);

                ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFocusView, "scaleX", 1, 0.5f);
                scaleX.setDuration(300);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFocusView, "scaleY", 1, 0.5f);
                scaleY.setDuration(300);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(mFocusView, "alpha", 1f, 0.3f, 1f, 0.3f, 1f, 0.3f, 1f);
                alpha.setDuration(600);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(scaleX).with(scaleY).before(alpha);
                animatorSet.start();
                mFocusView.setTag(cameraFocusCallback);


                WindowManager wm1 = getWindowManager();
                int width = wm1.getDefaultDisplay().getWidth();
                int height = wm1.getDefaultDisplay().getHeight();
                float x = e.getX() / width;
                float y = 1 - e.getY() / height;
                api.setCameraFocusPositionInPreview(x, y);
                //Log.i(TAG, "focus x:"+ x + " y:"+y);
            }

            return true;
        }

        /**
         * 前置摄像头可能不会回调对焦成功，因此需要手动隐藏对焦框
         */
        private Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (mFocusView.getVisibility() == View.VISIBLE) {
                    mFocusView.setVisibility(View.INVISIBLE);
                }
            }
        };


    };

    Camera.AutoFocusCallback cameraFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(final boolean success, Camera camera) {
            Log.d(TAG, "auto focus result: " + success);
            if (mFocusView.getTag() == this && mFocusView.getVisibility() == View.VISIBLE) {
                mFocusView.setVisibility(View.INVISIBLE);
            }
        }
    };
}

