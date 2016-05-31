package com.gwcd.sy.aacdecode;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private AudioPlayer mPlayer;

    private TextView mTvDesc;

    private TextView mTvSampleRate;

    private EditText mEtPath;

    private PopupMenu mPopupMenu;

    private Menu mMenu;

    private int mSampleRate = 44100;

    private int[] mSampRate = new int[]{96000,
            88200,
            64000,
            48000,
            44100,
            32000,
            24000,
            22050,
            16000,
            12000,
            11025,
            8000,
            7350};

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
                case AudioPlayer.MSG_DEC_ERROR:
                    mTvDesc.append("解码/播放出错，请查看打印输出");
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEtPath = (EditText) findViewById(R.id.et_main_path);
        mTvSampleRate = (TextView) findViewById(R.id.tv_main_samplerate);
        mTvDesc = (TextView) findViewById(R.id.tv_main_desc);
        mEtPath.setText(SharedPrefUtils.newInstance(this).getLastInput());
        mPopupMenu = new PopupMenu(this, findViewById(R.id.btn_main_samplerate));
        mMenu = mPopupMenu.getMenu();
        getMenuInflater().inflate(R.menu.menu_sample_rate, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(this);

        mPlayer = AudioPlayer.newInstance();
    }

    public void onClickDecAAC(View view) {
        String path = mEtPath.getText().toString().trim();
        if (TextUtils.isEmpty(path) || !path.startsWith("/storage/emulated/0/aac-pcm/")) {
            Toast.makeText(MainActivity.this, "路径不对", Toast.LENGTH_SHORT).show();
            return;
        }
        setInputCache();
        mPlayer.setDecFile(false);
        mPlayer.setPlayFile(false);
        mPlayer.setAacPath(path);
        mPlayer.setHandler(mHandler);
        mPlayer.startPlay();
        mTvDesc.setText("");
    }

    public void onClickDecFile(View view) {
        String path = mEtPath.getText().toString().trim();
        if (TextUtils.isEmpty(path) || !path.startsWith("/storage/emulated/0/aac-pcm/")) {
            Toast.makeText(MainActivity.this, "路径不对", Toast.LENGTH_SHORT).show();
            return;
        }
        setInputCache();
        mPlayer.setDecFile(true);
        mPlayer.setAacPath(path);
        mPlayer.setPcmPath(path.substring(0, path.lastIndexOf(".")) + ".pcm");
        mPlayer.setHandler(mHandler);
        mPlayer.startPlay();
        mTvDesc.setText("");
    }

    public void onClickPlayFile(View view) {
        String path = mEtPath.getText().toString().trim();
        if (TextUtils.isEmpty(path) || !path.startsWith("/storage/emulated/0/aac-pcm/")) {
            Toast.makeText(MainActivity.this, "路径不对", Toast.LENGTH_SHORT).show();
            return;
        }
        setInputCache();
        mPlayer.setPlayFile(true);
        mPlayer.setHandler(mHandler);
        mPlayer.setPcmPath(path);
        mPlayer.startPlay();
        mTvDesc.setText("");
    }

    public void onClickStop(View view) {
        if (mPlayer != null) {
            mPlayer.stopPlay();
        }
        mTvDesc.setText("停止");
    }

    private void setInputCache() {
        String path = mEtPath.getText().toString().trim();
        if (!TextUtils.isEmpty(path) && path.startsWith("/storage/emulated/0/aac-pcm/")) {
            SharedPrefUtils.newInstance(this).setLastInput(path);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setInputCache();
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

    public void onClickSampleRate(View view) {
        mPopupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sample_rate_96000:
                mSampleRate = 96000;
                break;
            case R.id.sample_rate_88200:
                mSampleRate = 88200;
                break;
            case R.id.sample_rate_64000:
                mSampleRate = 64000;
                break;
            case R.id.sample_rate_48000:
                mSampleRate = 48000;
                break;
            case R.id.sample_rate_44100:
                mSampleRate = 44100;
                break;
            case R.id.sample_rate_32000:
                mSampleRate = 32000;
                break;
            case R.id.sample_rate_24000:
                mSampleRate = 24000;
                break;
            case R.id.sample_rate_22050:
                mSampleRate = 22050;
                break;
            case R.id.sample_rate_16000:
                mSampleRate = 16000;
                break;
            case R.id.sample_rate_12000:
                mSampleRate = 12000;
                break;
            case R.id.sample_rate_11025:
                mSampleRate = 11025;
                break;
            case R.id.sample_rate_8000:
                mSampleRate = 8000;
                break;
            case R.id.sample_rate_7350:
                mSampleRate = 7350;
                break;
        }
        mPlayer.setSampleRate(mSampleRate);
        mTvSampleRate.setText(mSampleRate + "");
        return true;
    }
}
