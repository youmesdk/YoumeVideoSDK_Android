package im.youme.talk.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

///参数设置界面
public class videoSet extends AppCompatActivity {

    private EditText mVideoWidth;
    private EditText mVideoHeight;
    private EditText mVideoFPS;
    private EditText mReportInterval;
    private EditText mMaxBitRate;
    private EditText mMinBitRate;
    private EditText mFarendLevel;
    private Switch mQualitySwitch;
    private Switch mbHWEnableSwitch;
    private Switch mBeautifySwitch;
    private Switch mFixQuality;

    private int getValue( String str , int defaultValue )
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
        mVideoWidth = (EditText)findViewById(R.id.editText_videoWidth);
        mVideoHeight = (EditText)findViewById(R.id.editText_videoHeight);
        mVideoFPS =  (EditText)findViewById( R.id.editText_videoFps );
        mReportInterval = (EditText)findViewById(R.id.editText_reportInterval);
        mMaxBitRate = (EditText)findViewById(R.id.editText_maxBitRate);
        mMinBitRate = (EditText)findViewById( R.id.editText_minBitRate);
        mFarendLevel = (EditText)findViewById(R.id.editText_farendLevel);
        mQualitySwitch = (Switch)findViewById(R.id.switch_audioQuality);
        mbHWEnableSwitch = (Switch)findViewById(R.id.switch_bHWEnable);
        mBeautifySwitch =  (Switch)findViewById( R.id.switch_beautify );
        mFixQuality = (Switch)findViewById( R.id.switch_videoQuality);


        mVideoWidth.setText(Integer.toString(VideoCapturerActivity._videoWidth));
        mVideoHeight.setText(Integer.toString(VideoCapturerActivity._videoHeight));
        mVideoFPS.setText(Integer.toString(VideoCapturerActivity._fps));
        mReportInterval.setText(Integer.toString(VideoCapturerActivity._reportInterval));
        mMaxBitRate.setText(Integer.toString(VideoCapturerActivity._maxBitRate));
        mMinBitRate.setText(Integer.toString(VideoCapturerActivity._minBitRate));
        mFarendLevel.setText(Integer.toString(VideoCapturerActivity._farendLevel));

        //api14以下调用setChecked有问题？call requires api14
        mQualitySwitch.setChecked(VideoCapturerActivity._bHighAudio);
        mbHWEnableSwitch.setChecked(VideoCapturerActivity._bHWEnable);
        mBeautifySwitch.setChecked( VideoCapturerActivity._bBeautify );
        mFixQuality.setChecked( VideoCapturerActivity._bFixQuality );

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_set);
        initComponent();
    }


    //点击确定按钮的响应
    public void onConfirmClick(View view){

        VideoCapturerActivity._videoWidth = getValue(mVideoWidth.getText().toString(), 240 );
        VideoCapturerActivity._videoHeight = getValue(mVideoHeight.getText().toString(), 320 );
        VideoCapturerActivity._fps = getValue(mVideoFPS.getText().toString(), 320 );

        VideoCapturerActivity._reportInterval = getValue(mReportInterval.getText().toString(), 5000 );
        VideoCapturerActivity._maxBitRate = getValue(mMaxBitRate.getText().toString(), 0 );
        VideoCapturerActivity._minBitRate = getValue(mMinBitRate.getText().toString(), 0 );
        VideoCapturerActivity._farendLevel = getValue(mFarendLevel.getText().toString(), 10 );
        VideoCapturerActivity._bHighAudio = mQualitySwitch.isChecked();
        VideoCapturerActivity._bHWEnable   =mbHWEnableSwitch.isChecked();
        VideoCapturerActivity._bBeautify = mBeautifySwitch.isChecked();
        VideoCapturerActivity._bFixQuality = mFixQuality.isChecked();

        videoSet.this.finish();
    }
}
