package im.youme.video.sample2;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.youme.voiceengine.YouMeConst;

///参数设置界面
public class VideoSettings extends AppCompatActivity {
    private EditText mServerIp;
    private EditText mServerPort;
    private EditText mVideoWidth;
    private EditText mVideoHeight;
    private EditText mShareWidth;
    private EditText mShareHeight;
    private EditText mVideoFPS;
    private EditText mReportInterval;
    private EditText mMaxBitRate;
    private EditText mMinBitRate;
    private EditText mFarendLevel;

    private EditText mLocalIP;
    private EditText mLocalPort;
    private EditText mRemoteIP;
    private EditText mRemotePort;

    private EditText sdkIP;
    private EditText sdkPort;
    private EditText redirectIP;
    private EditText redirectPort;

    private Switch mQualitySwitch;
    private Switch mbHWEnableSwitch;
//    private Switch mBeautifySwitch;
    private Switch mTCPSwitch;
    private Switch mLandscapeSwitch;
    private Switch mTestmodeSwitch;
    private Switch mVBRSwitch;
    private Switch mTestP2P;
    private Switch mMinDelay;//低延迟模式开关
    private Switch mSecondeStream;

    private Spinner mVideoCodecSpinner;


    public static int getValue( String str , int defaultValue )
    {
        int value = defaultValue;
        try{
            value = Integer.parseInt( str );
        }
        catch ( Exception e  )
        {

        }

        return value;
    }
    private void initComponent(){
        SharedPreferences sharedPreferences = getSharedPreferences("demo_settings", Context.MODE_PRIVATE);
        mServerIp = (EditText)findViewById(R.id.editText_serverip);
        mServerPort = (EditText)findViewById(R.id.editText_serverport);
        mVideoWidth = (EditText)findViewById(R.id.editText_videoWidth);
        mVideoHeight = (EditText)findViewById(R.id.editText_videoHeight);
        mVideoFPS =  (EditText)findViewById(R.id.editText_videoFps);
        mReportInterval = (EditText)findViewById(R.id.editText_reportInterval);
        mMaxBitRate = (EditText)findViewById(R.id.editText_maxBitRate);
        mMinBitRate = (EditText)findViewById( R.id.editText_minBitRate);
        mFarendLevel = (EditText)findViewById(R.id.editText_farendLevel);

        mLocalIP=(EditText)findViewById(R.id.editTextLocalIP);
        mLocalPort=(EditText)findViewById(R.id.editTextLocalPort);
        mRemoteIP=(EditText)findViewById(R.id.editTextRemoteIP);
        mRemotePort=(EditText)findViewById(R.id.editTextRemotePort);

        mShareWidth=(EditText)findViewById(R.id.editText_shareWidth);
        mShareHeight=(EditText)findViewById(R.id.editText_shareHeight);

        mQualitySwitch = (Switch)findViewById(R.id.switch_videoQuality);
        mbHWEnableSwitch = (Switch)findViewById(R.id.switch_bHWEnable);
//        mBeautifySwitch =  (Switch)findViewById( R.id.switch_beautify );
        mTCPSwitch =  (Switch)findViewById( R.id.switch_tcp );
        mLandscapeSwitch =  (Switch)findViewById( R.id.switch_landscape );
        mTestmodeSwitch = (Switch)findViewById( R.id.switch_testmode );
        mVBRSwitch = (Switch)findViewById( R.id.switch_vbr );
        mMinDelay = (Switch)findViewById(R.id.switch_min_delay);
        mTestP2P=(Switch)findViewById(R.id.switch_testP2P);
        mSecondeStream =(Switch)findViewById(R.id.switch_test_seconde_stream);

        sdkIP = (EditText)findViewById(R.id.sdkIP);
        redirectIP = (EditText)findViewById(R.id.redirectIP);

        mVideoCodecSpinner = (Spinner)findViewById( R.id.videocodec_spinner );


        mServerIp.setText(VideoCapturerActivity._serverIp);
        mServerPort.setText(Integer.toString(VideoCapturerActivity._serverPort));
        mVideoWidth.setText(Integer.toString(VideoCapturerActivity._videoWidth));
        mVideoHeight.setText(Integer.toString(VideoCapturerActivity._videoHeight));
        mVideoFPS.setText(Integer.toString(VideoCapturerActivity._fps));
        mReportInterval.setText(Integer.toString(VideoCapturerActivity._reportInterval));
        mMaxBitRate.setText(Integer.toString(VideoCapturerActivity._maxBitRate));
        mMinBitRate.setText(Integer.toString(VideoCapturerActivity._minBitRate));
        mFarendLevel.setText(Integer.toString(VideoCapturerActivity._farendLevel));
        mShareWidth.setText(Integer.toString(VideoCapturerActivity.mVideoShareWidth));
        mShareHeight.setText(Integer.toString(VideoCapturerActivity.mVideoShareHeight));
        mMinDelay.setChecked(VideoCapturerActivity._minDelay);
        mSecondeStream.setChecked(VideoCapturerActivity.openSecondStream);


        //api14以下调用setChecked有问题？call requires api14
        mQualitySwitch.setChecked(VideoCapturerActivity._bHighAudio);
        mbHWEnableSwitch.setChecked(VideoCapturerActivity._bHWEnable);
//        mBeautifySwitch.setChecked( VideoCapturerActivity._bBeautify );
        mTCPSwitch.setChecked( VideoCapturerActivity._bTcp );
        mLandscapeSwitch.setChecked( VideoCapturerActivity._bLandscape );
        mTestmodeSwitch.setChecked(VideoCapturerActivity._bTestmode);
        mVBRSwitch.setChecked(VideoCapturerActivity._bVBR);

        mLocalIP.setText(sharedPreferences.getString("_LocalIP","192.168.0.1"));
        mLocalPort.setText(sharedPreferences.getInt("_LocalPort",50000) + "");
        mRemoteIP.setText(sharedPreferences.getString("_RemoteIP","192.168.0.1"));
        mRemotePort.setText(sharedPreferences.getInt("_RemotePort",50000) +"");
        mTestP2P.setChecked(sharedPreferences.getBoolean("_bTestP2P",false));

        sdkIP.setText(sharedPreferences.getString("_sdkIP",""));
        redirectIP.setText(sharedPreferences.getString("_redirectIP",""));

        mVideoCodecSpinner.setSelection( getPosByVideoCodec(VideoCapturerActivity.mVideoCodec) );

    }

    protected  int getVideoCodecByPos( int pos )
    {
        switch ( pos )
        {
            case  0 :
                return YouMeConst.YMVideoCodecType.YOUME_VIDEO_CODEC_H264;
            case 1:
                return YouMeConst.YMVideoCodecType.YOUME_VIDEO_CODEC_VP8;

                default:
                    return YouMeConst.YMVideoCodecType.YOUME_VIDEO_CODEC_H264;
        }
    }

    protected  int getPosByVideoCodec( int videoCodec )
    {
        switch ( videoCodec )
        {
            case YouMeConst.YMVideoCodecType.YOUME_VIDEO_CODEC_H264:
                return 0;
            case  YouMeConst.YMVideoCodecType.YOUME_VIDEO_CODEC_VP8:
                return 1;
            default:
                return 0;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_set);
        initComponent();
    }


    //点击确定按钮的响应
    public void onConfirmClick(View view){
        SharedPreferences sharedPreferences = getSharedPreferences("demo_settings", Context
                .MODE_PRIVATE);
        //VideoCapturerActivity._serverIp = mServerIp.getText().toString();
        VideoCapturerActivity._serverPort = getValue(mServerPort.getText().toString(), 0 );

        VideoCapturerActivity._videoWidth = getValue(mVideoWidth.getText().toString(), 240 );
        VideoCapturerActivity._videoHeight = getValue(mVideoHeight.getText().toString(), 320 );
        VideoCapturerActivity._fps = getValue(mVideoFPS.getText().toString(), 15 );
        VideoCapturerActivity._reportInterval = getValue(mReportInterval.getText().toString(), 5000 );
        VideoCapturerActivity._maxBitRate = getValue(mMaxBitRate.getText().toString(), 0 );
        VideoCapturerActivity._minBitRate = getValue(mMinBitRate.getText().toString(), 0 );
        VideoCapturerActivity._farendLevel = getValue(mFarendLevel.getText().toString(), 10 );
        VideoCapturerActivity._bHighAudio = mQualitySwitch.isChecked();
        VideoCapturerActivity._bHWEnable  = mbHWEnableSwitch.isChecked();
//        VideoCapturerActivity._bBeautify = mBeautifySwitch.isChecked();
        VideoCapturerActivity._bTcp  = mTCPSwitch.isChecked();
        VideoCapturerActivity._bLandscape = mLandscapeSwitch.isChecked();
        VideoCapturerActivity._bTestmode = mTestmodeSwitch.isChecked();
        VideoCapturerActivity._bVBR = mVBRSwitch.isChecked();
        VideoCapturerActivity._minDelay = mMinDelay.isChecked();
        VideoCapturerActivity._bTestP2P=mTestP2P.isChecked();

        VideoCapturerActivity._LocalIP=mLocalIP.getText().toString();
        VideoCapturerActivity._LocalPort=getValue(mLocalPort.getText().toString(),50000);
        VideoCapturerActivity._RemoteIP=mRemoteIP.getText().toString();
        VideoCapturerActivity._RemotePort=getValue(mRemotePort.getText().toString(),50000);
        VideoCapturerActivity.mVideoShareWidth = getValue(mShareWidth.getText().toString(),720);
        VideoCapturerActivity.mVideoShareHeight = getValue(mShareHeight.getText().toString(),1280);
        VideoCapturerActivity.sdkIP = sdkIP.getText().toString().trim();
        VideoCapturerActivity.redirectIP = redirectIP.getText().toString().trim();
        VideoCapturerActivity.openSecondStream = mSecondeStream.isChecked();

        VideoCapturerActivity.mVideoCodec = getVideoCodecByPos( mVideoCodecSpinner.getSelectedItemPosition() );

        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString("_LocalIP", VideoCapturerActivity._LocalIP);
        edit.putInt("_LocalPort",VideoCapturerActivity._LocalPort );
        edit.putString("_RemoteIP", VideoCapturerActivity._RemoteIP);
        edit.putInt("_RemotePort",VideoCapturerActivity._RemotePort );
        edit.putBoolean("_bTestP2P",VideoCapturerActivity._bTestP2P );
        edit.putString("_sdkIP", VideoCapturerActivity.sdkIP);
        edit.putString("_redirectIP", VideoCapturerActivity.redirectIP);
        edit.commit();

        VideoSettings.this.finish();
    }
}
