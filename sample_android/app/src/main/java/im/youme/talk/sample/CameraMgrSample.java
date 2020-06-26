package im.youme.talk.sample;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.GLES11Ext;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.Manifest;
import android.view.Surface;
import android.view.ViewDebug;

import com.youme.mixers.GLESVideoMixer;
import com.youme.mixers.VideoMixerHelper;
import com.youme.voiceengine.YouMeConst;
import com.youme.voiceengine.api;
import com.youme.voiceengine.video.GlUtil;

import im.youme.talk.streaming.core.VideoProducer;


@SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class CameraMgrSample {
    static String tag =  CameraMgrSample.class.getSimpleName();

    private static final boolean DEBUG = true;

    private final static int DEFAULE_WIDTH = 640;
    private final static int DEFAULE_HEIGHT = 480;
    private final static int DEFAULE_FPS = 15;

    private SurfaceView svCamera = null;
    private int  mTextureID = 0;
    private SurfaceTexture mSurfaceTexture = null;
    private Camera camera = null;
    Camera.Parameters camPara = null;
    private String camWhiteBalance;
    private String camFocusMode;

    private byte mBuffer[];
    private static boolean isFrontCamera = false;
    public int orientation = 90;
    private static int screenOrientation = 0;
    private static CameraMgrSample instance = new CameraMgrSample();
    private static Context context = null;

    public boolean isDoubleStreamingModel;
    public CameraMgrSample (){}
//    public static CameraMgrSample getInstance() {
//        return instance;
//    }

    private int mFps = 15;
    private int mFrameInterval = 1000/mFps;
    private int keyFrameInterval = 2 * mFps;
    private int videoBitrate = 800 *1000;

    private int videoWidth = 640;
    private int videoHeight = 480;
    public int preViewWidth = 640;
    public int preViewHeight = 480;

    private boolean needLoseFrame = false;
    private long  lastFrameTime = 0;
    private int  frameCount = 0;
    private long totalTime = 0;
    private int cameraId = 0;
    private boolean mAutoFocusLocked = false;
    private boolean mIsSupportAutoFocus = false;
    private boolean mIsSupportAutoFocusContinuousPicture = false;

    private ArrayList<EncodeThread> streamEncodeThreads = new ArrayList<EncodeThread>(2);
    //camera 采集视频cache，用于encode处理
    public static ArrayBlockingQueue<byte[]> YUVQueueMain = new ArrayBlockingQueue<byte[]>(15);
    public static ArrayBlockingQueue<byte[]> YUVQueueMinor = new ArrayBlockingQueue<byte[]>(15);
    private int mainStreamIndex = -1;
    private int minorStreamIndex = -1;

    public CameraMgrSample(SurfaceView svCamera) {
        this.svCamera = svCamera;
        //this.svCamera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //this.svCamera.getHolder().addCallback(new YMSurfaceHolderCallback());
    }

    public static void init(Context ctx) {
        context = ctx;
        if( context instanceof Activity )
        {
            switch (((Activity)context).getWindowManager().getDefaultDisplay().getRotation())  {
                case Surface.ROTATION_0:
                    screenOrientation = 0;
                    break;
                case Surface.ROTATION_90:
                    screenOrientation = 90;
                    break;
                case Surface.ROTATION_180:
                    screenOrientation = 180;
                    break;
                case Surface.ROTATION_270:
                    screenOrientation = 270;
                    break;
            }
        }
        else
        {
            screenOrientation = 0;
        }
    }

    public void setPreViewFps(int fps) {
        mFps = fps;
        mFrameInterval = 1000/mFps;
    }

    public void setVideoSize(int width, int height) {
        videoWidth = width;
        videoHeight = height;
    }

    public void setVideoBitrate(int bitrate) {
        videoBitrate = bitrate;
    }

    public int openCamera( boolean isFront) {
        if(null != camera) {
            closeCamera();
        }

        cameraId = 0;
        int cameraNum = Camera.getNumberOfCameras();
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < cameraNum; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if((isFront) && (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)) {
                cameraId = i;
                orientation = (360 - cameraInfo.orientation + 360 - screenOrientation) % 360;
                orientation = (360 - orientation) % 360;  // compensate the mirror
                Log.d(tag, "i:" + i + "orientation:" + orientation + "screenOrientation:" + screenOrientation);
                break;
            } else if((!isFront) && (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK)) {
                cameraId = i;
                orientation = (cameraInfo.orientation + 360 - screenOrientation) % 360;
                Log.d(tag, "ii:" + i + "orientation:" + orientation + "screenOrientation:" + screenOrientation);
                break;
            }
        }

        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            camera = null;
            return -1;
        }

//        dumpCameraInfo(camera, cameraId);

        try {
            camPara = camera.getParameters();
        } catch(Exception e) {
            e.printStackTrace();
            camera = null;
            return -2;
        }

       Camera.Size size = getCloselyPreSize(videoWidth, videoHeight, camPara.getSupportedPreviewSizes(), false);
        if( size == null  ){
            camPara.setPreviewSize(preViewWidth, preViewHeight);

//            Log.d(tag, "could not  getCloselyPreSize ");
//            return -3;
        }
        else
        {
            camPara.setPreviewSize(size.width, size.height);
            preViewWidth = size.width;
            preViewHeight = size.height;
        }

       // Log.d(tag, "width = " + size.width + ", height = " + size.height + "; w = " + DEFAULE_WIDTH + ", h = " + DEFAULE_HEIGHT + ", fps = " + DEFAULE_FPS);

        //p.setPreviewSize(352, 288);
        camPara.setPreviewFormat(ImageFormat.NV21);
        //camPara.setPreviewFormat(ImageFormat.YV12);
        List<int[]> fpsRangeList = camPara.getSupportedPreviewFpsRange();

        ///先设置一下，有些机器上设置帧率会失败，所以其他参数先设置吧
        try {
            camera.setParameters(camPara);
        } catch(Exception e) {
            e.printStackTrace();
        }

        //设置自动对焦
        List<String> focusModes = camPara.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mIsSupportAutoFocusContinuousPicture = true;
            camPara.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 自动连续对焦
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mIsSupportAutoFocus = true;
            camPara.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);// 自动对焦
        } else {
            mIsSupportAutoFocusContinuousPicture = false;
            mIsSupportAutoFocus = false;
        }

        camPara.setPreviewFpsRange(mFps*1000, mFps*1000);
        Log.i(tag, "minfps = " + fpsRangeList.get(0)[0]+" maxfps = "+fpsRangeList.get(0)[1]);
        //camera.setDisplayOrientation(90);
        //mCamera.setPreviewCallback(new H264Encoder(352, 288));
        camWhiteBalance = camPara.getWhiteBalance();
        camFocusMode = camPara.getFocusMode();
        Log.d(tag, "white balance = " + camWhiteBalance + ", focus mode = " + camFocusMode);

        try {
            camera.setParameters(camPara);
        } catch(Exception e) {
            needLoseFrame = true;
            setDefPreViewFps();
            e.printStackTrace();
        }
        int mFrameWidth = camPara.getPreviewSize().width;
        int mFrameHeigth = camPara.getPreviewSize().height;
        int frameSize = mFrameWidth * mFrameHeigth;
        frameSize = frameSize * ImageFormat.getBitsPerPixel(camPara.getPreviewFormat())/8;
        mBuffer = new byte[frameSize];
        camera.addCallbackBuffer(mBuffer);
        camera.setPreviewCallback(youmePreviewCallback);

        if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) && (null == svCamera)) {
            GLESVideoMixer.SurfaceContext surfaceContext = VideoMixerHelper.getCameraSurfaceContext();
            if(surfaceContext != null) {
                mTextureID = surfaceContext.textureId;
                mSurfaceTexture = surfaceContext.surfaceTexture;
                mSurfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);

                Log.d("", "mSurfaceTexture id:"+mTextureID);
            }
            else
                {
                mTextureID = GlUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                mSurfaceTexture = new SurfaceTexture(mTextureID);
            }
            try {
                camera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                if(null == svCamera) {
                    camera.setPreviewDisplay(null);
                } else {
                    camera.setPreviewDisplay(svCamera.getHolder());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            camera.startPreview();
            if (mIsSupportAutoFocusContinuousPicture) {
                camera.cancelAutoFocus();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mainStreamIndex = startVideoStreamEncode(videoWidth, videoHeight, 0);
//        minorStreamIndex = startVideoStreamEncode(videoWidth/2, videoHeight/2, 1);

        totalTime = 0;
        frameCount = 0;
        isFrontCamera = isFront;

        return 0;
    }

    public void setDefPreViewFps() {
        try {
            if (camera == null) {
                return;
            }
            List<int[]> fpsRangeList = camPara.getSupportedPreviewFpsRange();
            Camera.Parameters p = camera.getParameters();
            p.setPreviewFpsRange(fpsRangeList.get(0)[0], fpsRangeList.get(0)[1] );
            camera.setParameters(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int closeCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;

            if (minorStreamIndex >= 0) {
                Log.d("bruce", "closeCamera: minorStreamIndex:" + minorStreamIndex);
                stopStream(minorStreamIndex);
            }

            if (mainStreamIndex >= 0) {
                Log.d("bruce", "closeCamera: mainStreamIndex:" + mainStreamIndex);
                stopStream(mainStreamIndex);
            }
        }
        return 0;
    }

    public void setSvCamera(SurfaceView svCamera) {
        this.svCamera = svCamera;
        //this.svCamera.getHolder().addCallback(new YMSurfaceHolderCallback());
        //this.svCamera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    Camera.PreviewCallback youmePreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            int height = preViewHeight;
            int width = preViewWidth;
            int rotation = orientation;
            int mirror ;
            if (isFrontCamera) {
                mirror = YouMeConst.YouMeVideoMirrorMode.YOUME_VIDEO_MIRROR_MODE_NEAR;
            }
            else{
                mirror = YouMeConst.YouMeVideoMirrorMode.YOUME_VIDEO_MIRROR_MODE_DISABLED;
            }
            if (isDoubleStreamingModel) {
                VideoProducer.getInstance().pushFrame(data, cameraId);
            } else {
//                Log.i(tag, "inputVideoFrame youmePreviewCallback 1. data len:"+data.length+" fmt: " + YouMeConst.YOUME_VIDEO_FMT.VIDEO_FMT_NV21 + " timestamp:" + System.currentTimeMillis());
//                api.inputVideoFrame(data, data.length, width, height, YouMeConst.YOUME_VIDEO_FMT.VIDEO_FMT_NV21, rotation, mirror, System.currentTimeMillis(), 0);
                putYUVData(0, data);
                byte[] tempData = NV21_minor_scale(data, preViewWidth, preViewHeight);
                putYUVData(1, tempData);
            }
            if(camera != null) {
                camera.addCallbackBuffer(data);
            }
        }
    };

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            long timestamp = System.currentTimeMillis();
            int mirror ;
            if (isFrontCamera) {
                mirror = YouMeConst.YouMeVideoMirrorMode.YOUME_VIDEO_MIRROR_MODE_FAR;
            }
            else{
                mirror = YouMeConst.YouMeVideoMirrorMode.YOUME_VIDEO_MIRROR_MODE_DISABLED;
            }
//            Log.i(tag, "inputVideoFrame texture 1. " + "timestamp: "+ timestamp);
//            api.inputVideoFrameGLES(mTextureID, null, preViewWidth, preViewHeight, YouMeConst.YOUME_VIDEO_FMT.VIDEO_FMT_TEXTURE_OES, 90, mirror, timestamp);

        }
    };


    private byte[] switchYV21UV(byte[] data, int width, int height) {
        int len = data.length;
        byte[] dataRet = new byte[width * height * 3 / 2];

        int YSize = width * height;
        int UVSize = YSize / 4;

        System.arraycopy(data, 0, dataRet, 0, YSize);
        System.arraycopy(data, YSize + UVSize, dataRet, YSize, UVSize);
        System.arraycopy(data, YSize, dataRet, YSize + UVSize, UVSize);

        return dataRet;
    }

    public void setPreViewSize(int width, int height)
    {
        videoWidth = width;
        videoHeight = height;
    }

//    public static void setCaptureSize(int width, int height)
//    {
//        getInstance().setPreViewSize(width, height);
//    }
//
//    public static void setFps(int fps)
//    {
//        getInstance().setPreViewFps(fps);
//    }
//
//    public static int startCapture() {
//        Log.e(tag, "start capture is called");
//        isFrontCamera = true;
//       return getInstance().openCamera(isFrontCamera);
//    }
//
//    public static void stopCapture() {
//        Log.e(tag, "stop capture is called.");
//        getInstance().closeCamera();
//    }
//
//    public static void switchCamera() {
//        Log.e(tag, "switchCamera is called.");
//        isFrontCamera = !isFrontCamera;
//        getInstance().closeCamera();
//        getInstance().openCamera(isFrontCamera);
//    }

    private static class PermissionCheckThread extends Thread {
        @Override
        public void run() {
            try {
                Log.i(tag, "PermissionCheck starts...");
                Context mContext = context;
                while(!Thread.interrupted()) {
                    Thread.sleep(1000);
                    Log.i(tag, "PermissionCheck starts...running");
                    if ((mContext != null) && (mContext instanceof Activity)) {
                        int cameraPermission = ContextCompat.checkSelfPermission((Activity)mContext, Manifest.permission.CAMERA);
                        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                            // Once the permission is granted, reset the microphone to take effect.
                            break;
                        }
                        int audioPermission = ContextCompat.checkSelfPermission((Activity)mContext, Manifest.permission.RECORD_AUDIO);
                        if (audioPermission == PackageManager.PERMISSION_GRANTED) {
                            // Once the permission is granted, reset the microphone to take effect.
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Log.i(tag, "PermissionCheck interrupted");
            }catch (Throwable e) {
                Log.e(tag, "PermissionCheck caught a throwable:" + e.getMessage());

            }
            Log.i(tag, "PermissionCheck exit");
        }
    }
    private static PermissionCheckThread mPermissionCheckThread = null;

    public static boolean startRequestPermissionForApi23() {
        boolean isApiLevel23 = false;
        Context mContext = context;//AppPara.getContext();
        try {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (mContext != null)
                    && (mContext instanceof Activity) && (mContext.getApplicationInfo().targetSdkVersion >= 23)) {

                isApiLevel23 = true;
                int cameraPermission = ContextCompat.checkSelfPermission((Activity)mContext, Manifest.permission.CAMERA);
                if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                    Log.e(tag, "Request for camera permission");
                    ActivityCompat.requestPermissions((Activity)mContext,
                            new String[]{Manifest.permission.CAMERA},
                            1);
                    // Start a thread to check if the permission is granted. Once it's granted, reset the microphone to apply it.
                    if (mPermissionCheckThread != null) {
                        mPermissionCheckThread.interrupt();
                        mPermissionCheckThread.join(2000);
                    }
                    mPermissionCheckThread = new PermissionCheckThread();
                    if (mPermissionCheckThread != null) {
                        mPermissionCheckThread.start();
                    }
                } else {
                    Log.i(tag, "Already got camera permission");
                }
            }
        } catch (Throwable e) {
            Log.e(tag, "Exception for startRequirePermiForApi23");
            e.printStackTrace();
        }

        return isApiLevel23;
    }

    public static void stopRequestPermissionForApi23() {
        try {
            if (mPermissionCheckThread != null) {
                mPermissionCheckThread.interrupt();
                mPermissionCheckThread.join(2000);
                mPermissionCheckThread = null;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param surfaceWidth           需要被进行对比的原宽
     * @param surfaceHeight          需要被进行对比的原高
     * @param preSizeList            需要对比的预览尺寸列表
     * @return  得到与原宽高比例最接近的尺寸
     */
    protected Camera.Size getCloselyPreSize(int surfaceWidth, int surfaceHeight,
                                            List<Camera.Size> preSizeList, boolean mIsPortrait) {

        int ReqTmpWidth;
        int ReqTmpHeight;
        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
        switch(orientation) {
            case 90:
            case 270:
                ReqTmpWidth = surfaceHeight;
                ReqTmpHeight = surfaceWidth;
                break;
            default:
                ReqTmpWidth = surfaceWidth;
                ReqTmpHeight = surfaceHeight;
                break;
        }

        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        float wRatio = 1.0f;
        float hRatio = 1.0f;
        List<Camera.Size> tempList = new ArrayList<Camera.Size>();
        for(Camera.Size size : preSizeList){
            wRatio = (((float) size.width) / ReqTmpWidth);
            hRatio = (((float) size.height) / ReqTmpHeight);
            if((wRatio >= 1.0) && (hRatio >= 1.0)) {
                tempList.add(size);
            }
        }

        int pixelCount = 0;
        Camera.Size retSize = null;
        for(Camera.Size size : tempList) {
            if(0 == pixelCount) {
                pixelCount = size.width * size.height;
                retSize = size;
            } else {
                if((size.width * size.height) < pixelCount) {
                    pixelCount = size.width * size.height;
                    retSize = size;
                }
            }
        }

        // 得到与传入的宽高比最接近的size
//        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
//        float curRatio, deltaRatio;
//        float deltaRatioMin = Float.MAX_VALUE;
//        Camera.Size retSize = null;
//        for (Camera.Size size : preSizeList) {
//            curRatio = ((float) size.width) / size.height;
//            deltaRatio = Math.abs(reqRatio - curRatio);
//            if (deltaRatio < deltaRatioMin) {
//                deltaRatioMin = deltaRatio;
//                retSize = size;
//            }
//        }

        if( retSize != null ){
            Log.i(tag, "w:"+retSize.width+" h:"+retSize.height);
        }

        return retSize;
    }

    public int startVideoStreamEncode(int width, int height, int streamId) {
        EncodeThread streamEncodeThread = new EncodeThread(width, height, streamId);
        streamEncodeThread.start();
        streamEncodeThreads.add(streamEncodeThread);
        Log.d("bruce", "startVideoStreamEncode: streamEncodeThread:" + streamEncodeThread);
        Log.d("bruce", "startVideoStreamEncode: streamEncodeThreads.size():" + streamEncodeThreads.size());
        return streamEncodeThreads.size() - 1;
    }

    public void stopStream(int streamIndex) {
        if (streamIndex > streamEncodeThreads.size()-1) {
            return;
        }

        EncodeThread encodeThread = streamEncodeThreads.get(streamIndex);
        if (encodeThread != null) {
            Log.d("bruce", "stopStream: encodeThread:" + encodeThread);
            encodeThread.setEnable(false);
            streamEncodeThreads.remove(streamIndex);
        }
    }

    public MediaCodec createMediaCodec(int width, int height, int streamId) {
        MediaCodec mediaCodec = null;

        // 默认设置大流码率，针对小流，码率取其一半
        int tempBitrate = videoBitrate;
        if (1 == streamId) {
            tempBitrate = videoBitrate/2;
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, tempBitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); //关键帧时间间隔，单位s

        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mediaCodec;
    }

    public void closeMediaCodec(MediaCodec mediaCodec) {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
    }

    public FileOutputStream createOutputStream(int width, int height) {
        FileOutputStream fileOutputStream = null;
        try {
            String fileName = "h264_" + String.valueOf(width) + "*" + String.valueOf(height);
            File saveFile = new File("/sdcard/Download/", fileName);
            fileOutputStream = new FileOutputStream(saveFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return fileOutputStream;
    }

    public void writeDataToFile(FileOutputStream fileOutputStream, byte[] data) {
        if (data == null || fileOutputStream == null) {
            return;
        }

        try {
            fileOutputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeOutputStream(FileOutputStream fileOutputStream) {
        if (fileOutputStream == null) {
            return;
        }

        try {
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getYUVQueueSize(int streamId) {
        if (0 == streamId) {
            return YUVQueueMain.size();
        } else {
            return YUVQueueMinor.size();
        }
    }

    private byte[] getYUVQueueElem(int streamId) {
        if (0 == streamId) {
            return YUVQueueMain.poll();
        } else {
            return YUVQueueMinor.poll();
        }
    }

    public void putYUVData(int streamId, byte[] buffer) {
        if (0 == streamId) {
            if (YUVQueueMain.size() >= 10) {
                YUVQueueMain.poll();
            }
            YUVQueueMain.add(buffer);
        } else {
            if (YUVQueueMinor.size() >= 10) {
                YUVQueueMinor.poll();
            }
            YUVQueueMinor.add(buffer);
        }

    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFps;
    }

    // simple scale to minor stream
    private byte[] NV21_minor_scale(byte[] input, int inputWidth, int inputHeight) {
        if (null == input) {
            return null;
        }
        int i = 0, j = 0;
        int inputFrameYSize = inputWidth * inputHeight;

        int outputWidth = inputWidth/2;
        int outputHeight = inputHeight/2;
        int outputFrameYSize = outputWidth * outputHeight;
        int outputFrameSize = outputWidth * outputHeight * 3 / 2;

        byte[] outputFrame = new byte[outputFrameSize];

        // scale Y
        for (i = 0; i < outputHeight; i++) {
            for (j = 0; j < outputWidth; j++) {
                outputFrame[i * outputWidth + j] = input[2 * i * outputWidth + 2*j];
            }
        }

        //scale UV
        for (i = 0; i < outputWidth; i++) {
            for (j = 0; j < outputHeight/2; j+=2) {
                outputFrame[outputFrameYSize + i * outputHeight/2 + j] = input[inputFrameYSize + i * outputHeight + 2*j];
                outputFrame[outputFrameYSize + i * outputHeight/2 + j + 1] = input[inputFrameYSize + i * outputHeight + 2*j + 1];
            }
        }

        return outputFrame;
    }

    private void NV21ToNV12(byte[] nv21,byte[] nv12,int width,int height){
        if(nv21 == null || nv12 == null) {
            return;
        }

        int framesize = width*height;
        int i = 0,j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for(i = 0; i < framesize; i++){
            nv12[i] = nv21[i];
        }

        for (j = 0; j < framesize/2; j+=2)
        {
            nv12[framesize + j] = nv21[j+framesize + 1];
            nv12[framesize + j + 1] = nv21[j+framesize];
        }
    }

    private byte[] NV21_rotate_to_270(byte[] nv21_data, int width, int height)
    {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;
        byte[] nv21_rotated = new byte[buffser_size];
        int i = 0;

        // Rotate the Y luma
        for (int x = width - 1; x >= 0; x--)
        {
            int offset = 0;
            for (int y = 0; y < height; y++)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset += width;
            }
        }

        // Rotate the U and V color components
        i = y_size;
        for (int x = width - 1; x > 0; x = x - 2)
        {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++)
            {
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i++;
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset += width;
            }
        }
        return nv21_rotated;
    }

    class EncodeThread extends Thread {
        private int width;
        private int height;
        private int streamId;

        private byte[] inputRaw = null;
        private byte[] inputRotate = null;
        private byte[] inputYuv420sp = null;

        private long pts = 0;
        private long generateIndex = 0;
        private int TIMEOUT_USEC = 0;

        private ByteBuffer[] inputBuffers;
        private ByteBuffer[] outputBuffers;
        ByteBuffer inputBuffer;
        ByteBuffer outputBuffer;
        int inputBufferIndex;
        int outputBufferIndex;
        byte[] outData = null;
        boolean isEnable = true;

        MediaCodec.BufferInfo bufferInfo;

        EncodeThread(int width, int height, int streamId) {
            this.width = width;
            this.height = height;
            this.streamId = streamId;
        }

        public void setEnable(boolean enable) {
            isEnable = enable;
        }

        @Override
        public void run () {
            MediaCodec mediaCodec = createMediaCodec(width, height, streamId);
            // debug: dump文件测试
            FileOutputStream fileOutputStream = createOutputStream(width, height);
            // 暂时存储sps/pps帧数据
            byte[] configbyte = null;

            while(isEnable) {
                if (getYUVQueueSize(streamId) > 0) {
                    inputRaw = getYUVQueueElem(streamId);
                    inputYuv420sp = new byte[width*height*3/2];

                    inputRotate = NV21_rotate_to_270(inputRaw, height, width);
                    NV21ToNV12(inputRotate, inputYuv420sp, width, height);

                    inputRaw = null;
                    inputRotate = null;
                }

                if (inputYuv420sp != null) {
                    try {
                        inputBuffers = mediaCodec.getInputBuffers();
                        outputBuffers = mediaCodec.getOutputBuffers();
                        inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            pts = computePresentationTime(generateIndex);
                            inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(inputYuv420sp);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, inputYuv420sp.length, pts, 0);
                            generateIndex += 1;
                        }

                        bufferInfo = new MediaCodec.BufferInfo();
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

                        while (outputBufferIndex >= 0) {
                            outputBuffer = outputBuffers[outputBufferIndex];
                            outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);

                            // debug: dump文件测试
                            if (DEBUG) {
                                writeDataToFile(fileOutputStream, outData);
                            }

                            long timestamp = System.currentTimeMillis();
                            if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 2){
                                Log.i(tag,  "Config frame generated. Offset: " + bufferInfo.offset +
                                        ". Size: " + bufferInfo.size);
                                int sps_position = 0;
                                int pps_position = 0;
                                for (int i = 0; i < bufferInfo.size; i++) {
                                    if (outData[i] == 0 && outData[i+1] == 0 && outData[i+2] == 0 && outData[i+3] == 1) {
                                        if ((outData[i+4] & 0x1F) == 0x7) {
                                            sps_position = i+4;
                                        } else if ((outData[i+4] & 0x1F) == 0x8) {
                                            pps_position = i+4;
                                        }
                                    }
                                }
                                int sps_len = pps_position - sps_position - 4;
                                int pps_len = bufferInfo.size - pps_position ;

//                                startcode + stap-a nalutype + spslen + spsbody + ppslen + ppsbody
//                                int configsize = 4 + 1 + 2 + sps_len + 2 + pps_len;
                                if (configbyte != null) {
                                    configbyte = null;
                                }

                                configbyte = outData;
                                outData = null;

                            }else if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) == 1){

//                                Log.i(tag, "inputVideoFrame frame key 1. stream: "+ streamId +", sps: " + configbyte.length + ", timestamp: " + timestamp);
//                                Log.i(tag, "inputVideoFrame frame key 2. stream: "+ streamId +", IDR: " + bufferInfo.size + ", timestamp: " + timestamp);

                                // sps+pps
                                api.inputVideoFrameEncrypt(configbyte, configbyte.length, width, height, YouMeConst.YOUME_VIDEO_FMT.VIDEO_FMT_H264, 0, 0,  timestamp, streamId);
                                // key frame
                                api.inputVideoFrameEncrypt(outData, bufferInfo.size, width, height, YouMeConst.YOUME_VIDEO_FMT.VIDEO_FMT_H264, 0, 0,  timestamp, streamId);
                                Thread.sleep(mFrameInterval);
                            } else {
//                                Log.i(tag, "inputVideoFrame frame normal 3. stream: "+ streamId +",  P frame: " + bufferInfo.size + ", timestamp: " + timestamp);
                                api.inputVideoFrameEncrypt(outData, bufferInfo.size, width, height, YouMeConst.YOUME_VIDEO_FMT.VIDEO_FMT_H264, 0, 0,  timestamp, streamId);
                                Thread.sleep(mFrameInterval);
                            }

                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    inputYuv420sp = null;
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            closeMediaCodec(mediaCodec);
            closeOutputStream(fileOutputStream);
        }
    }
}
