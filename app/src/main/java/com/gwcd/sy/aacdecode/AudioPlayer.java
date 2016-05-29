package com.gwcd.sy.aacdecode;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by Lenovo on 2016/5/26.
 */
public class AudioPlayer {

//                         0: 96000 Hz
//                         1: 88200 Hz
//                         2: 64000 Hz
//                         3: 48000 Hz
//                         4: 44100 Hz
//                         5: 32000 Hz
//                         6: 24000 Hz
//                         7: 22050 Hz
//                         8: 16000 Hz
//                         9: 12000 Hz
//                         10: 11025 Hz
//                         11: 8000 Hz
//                         12: 7350 Hz

    //                         channel_configuration: 表示声道数
//                         1: 1 channel: front-center
//                         2: 2 channels: front-left, front-right
//                         3: 3 channels: front-center, front-left, front-right
//                         4: 4 channels: front-center, front-left, front-right, back-center
//                         5: 5 channels: front-center, front-left, front-right, back-left, back-right
//                         6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
//                         7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
//                         8-15: Reserved
    public static final int DEF_SAMPLE_RATE = 44100;
    private final static String TAG = "AudioPlayer";
    private AudioParam mAudioParam;                         // 音频参数
    private byte[] mData;                               // 音频数据
    private AudioTrack mAudioTrack;                         // AudioTrack对象
    private boolean mBReady = false;                     // 播放源是否就绪
    private PlayAudioThread mPlayAudioThread;               // 播放线程
    private int mMinBufferSize;

    private static AudioPlayer mInstance;
    private boolean isDecFile = false;
    private Handler mHandler = null;
    public static final int MSG_DEC_AAC_FILE_START = 1;
    public static final int MSG_DEC_AAC_FILE_TIME = 2;
    public static final int MSG_PLAY_START = 3;
    public static final int MSG_PLAY_COMPLETE = 4;

    public static AudioPlayer newInstance(boolean isDecFile) {
        if (mInstance == null) {
            mInstance = new AudioPlayer();
        }
        mInstance.setDecFile(isDecFile);

        return mInstance;
    }

    private AudioPlayer() {
        mAudioParam = new AudioParam();
//        try {
//            createAudioTrack();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public void setDecFile(boolean isDecFile) {
        this.isDecFile = isDecFile;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public void setDataSource(byte[] data) {
        mData = data;
    }

    public void setDataSource(File aacFile) {

    }

    private void createAudioTrack() throws Exception {
        // 获得构建对象的最小缓冲区大小
        mMinBufferSize = AudioTrack.getMinBufferSize(DEF_SAMPLE_RATE,
                // 获得构建对象的最小缓冲区大小
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
//        int minSize = 32* 1024;
//        if (mMinBufferSize < minSize)
//            mMinBufferSize = minSize;
        mMinBufferSize *= 2;
        Log.d(TAG, "mMinBufferSize = " + mMinBufferSize);
//               STREAM_ALARM：警告声
//               STREAM_MUSCI：音乐声，例如music等
//               STREAM_RING：铃声
//               STREAM_SYSTEM：系统声音
//               STREAM_VOCIE_CALL：电话声音
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                DEF_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mMinBufferSize,
                AudioTrack.MODE_STREAM);
//              AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
//              STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。
//              这个和我们在socket中发送数据一样，应用层从某个地方获取数据，例如通过编解码得到PCM数据，然后write到audiotrack。
//              这种方式的坏处就是总是在JAVA层和Native层交互，效率损失较大。
//              而STATIC的意思是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
//              后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
//              这种方法对于铃声等内存占用较小，延时要求较高的声音来说很适用。
    }

    public void startPlay() {
        stopPlay();
        if (mPlayAudioThread == null) {
            mPlayAudioThread = new PlayAudioThread();
            mPlayAudioThread.start();
        }
    }

    public void stopPlay() {
        if (mPlayAudioThread != null) {
            mPlayAudioThread.interrupt();
            mPlayAudioThread = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    /*
     *  播放音频的线程
     */
    class PlayAudioThread extends Thread {
        @Override
        public void run() {
            try {
                /**
                 * 北京北京8k16bits单声道.pcm
                 冰雨片段8k16bit单声道.pcm
                 冰雨片段32k16bit单声道.pcm
                 冰雨片段48k16bit单声道.pcm
                 浪花一朵朵片段8k16bit单声道.pcm
                 浪花一朵朵片段32k16bit单声道.pcm
                 浪花一朵朵片段48k16bit单声道.pcm
                 */
//                String path = "/storage/emulated/0/pcm/冰雨片段48k16bit单声道.pcm";
                String aacPath = "/storage/emulated/0/test.aac";
                String pcmPath = "/storage/emulated/0/test.pcm";
                FileInputStream fis = new FileInputStream(aacPath);
                File pcmFile = new File(pcmPath);
                if (!pcmFile.exists()) {
                    pcmFile.createNewFile();
                }
                if (isDecFile) {
                    long decFileStart = System.currentTimeMillis();
                    if (mHandler != null) {
                        mHandler.sendEmptyMessage(MSG_DEC_AAC_FILE_START);
                    }
                    int decFileErr = LibFaad.decodeAACFile(aacPath, pcmPath);
                    long decFileEnd = System.currentTimeMillis();
                    Log.d(TAG, "dec file time:" + (decFileEnd - decFileStart));
                    if (mHandler != null) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = MSG_DEC_AAC_FILE_TIME;
                        msg.arg1 = (int) (decFileEnd - decFileStart);
                        mHandler.sendMessage(msg);
                    }
                    if (decFileErr != 0) {
                        Log.d(TAG, "decodeAACFile failed.");
                        return;
                    }
                    fis = new FileInputStream(pcmPath);
                } else {
                    LibFaad.openFaad();
                }

                int readCnt = -1;
                int readOffset = 0;
                Log.d(TAG, "file size:" + fis.available());
                boolean initSuccess = false;
                createAudioTrack();
                mAudioTrack.play();
                byte[] file_buff = new byte[mMinBufferSize];
                long start = System.currentTimeMillis();
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(MSG_PLAY_START);
                }
                while ((readCnt = fis.read(file_buff, 0, file_buff.length)) > 0) {
                    if (isDecFile) {
                        mAudioTrack.write(file_buff, 0, readCnt);
                    } else {
                        if (!initSuccess) {
                            int ret = LibFaad.initFaad(file_buff, readCnt, 44100, 1);
                            initSuccess = ret == 0 ? true : false;
                        }
                        /** AAC解码 */
                        long decstart = System.currentTimeMillis();
                        byte[] pcmBuff = LibFaad.decodeAAC(file_buff, readCnt);
                        Log.d(TAG, "dec time:" + (System.currentTimeMillis() - decstart));
                        if (pcmBuff.length > 0) {
                            long writestart = System.currentTimeMillis();
                            mAudioTrack.write(pcmBuff, 0, pcmBuff.length);
                            Log.d(TAG, "write time:" + (System.currentTimeMillis() - writestart));
                        }
                        Log.d(TAG, "pcmBuff.length:" + pcmBuff.length);
                    }
                    readOffset += readCnt;
                    Log.d(TAG, "readOffset:" + readOffset);
                }
                fis.close();
                if (!isDecFile) {
                    LibFaad.colseFaad();
                }
                mAudioTrack.stop();
                long playEnd = System.currentTimeMillis();
                Log.d(TAG, "play time:" + (playEnd - start));
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_PLAY_COMPLETE;
                    msg.arg1 = (int) (playEnd - start);
                    mHandler.sendMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "onPlayComplete...");
            }
            Log.d(TAG, "PlayAudioThread complete...");
        }
    }
}