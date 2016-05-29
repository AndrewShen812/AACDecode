package com.gwcd.sy.aacdecode;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private AudioPlayer mPlayer;

    private TextView mTvDesc;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AudioPlayer.MSG_DEC_AAC_FILE_START:
                    mTvDesc.setText("开始解码文件...");
                    break;
                case AudioPlayer.MSG_DEC_AAC_FILE_TIME:
                    mTvDesc.append("\n解码文件完成，用时：" + msg.arg1 + "ms");
                    break;
                case AudioPlayer.MSG_PLAY_START:
                    mTvDesc.append("\n开始播放...");
                    break;
                case AudioPlayer.MSG_PLAY_COMPLETE:
                    mTvDesc.append("\n播放完成，用时：" + msg.arg1 + "ms");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvDesc = (TextView) findViewById(R.id.tv_main_desc);
    }

    public void onClickDecAAC(View view) {
        mPlayer = AudioPlayer.newInstance(false);
        mPlayer.setHandler(mHandler);
        mPlayer.startPlay();
        mTvDesc.setText("");
    }

    public void onClickDecFile(View view) {
        mPlayer = AudioPlayer.newInstance(true);
        mPlayer.setHandler(mHandler);
        mPlayer.startPlay();
        mTvDesc.setText("");
    }

    public void onClickStop(View view) {
        if (mPlayer != null) {
            mPlayer.stopPlay();
            mPlayer = null;
        }
        mTvDesc.setText("停止");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.stopPlay();
            mPlayer = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 1 && keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
