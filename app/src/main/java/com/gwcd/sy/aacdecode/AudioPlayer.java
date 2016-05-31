package com.gwcd.sy.aacdecode;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
    private AudioTrack mAudioTrack;                         // AudioTrack对象
    private boolean mBReady = false;                     // 播放源是否就绪
    private PlayAudioThread mPlayAudioThread;               // 播放线程
    private int mMinBufferSize;

    private static AudioPlayer mInstance;
    private Handler mHandler = null;
    public static final int MSG_DEC_AAC_FILE_START = 1;
    public static final int MSG_DEC_AAC_FILE_TIME = 2;
    public static final int MSG_PLAY_START = 3;
    public static final int MSG_PLAY_COMPLETE = 4;
    public static final int MSG_DEC_ERROR = 5;

    private BlockingQueue<QueueData> mQueue;
    private DecodeThread mDecodeThread;
    private String mAacPath;
    private String mPcmPath;
    private boolean isDecFile = false;
    private boolean isPlayFile = false;
    private int mSampleRate = DEF_SAMPLE_RATE;

    private static class QueueData {
        public int seqId;
        public boolean isEnding;
        public byte[] pcmBuff;
        public int buffLen;
    }

    public static AudioPlayer newInstance() {
        if (mInstance == null) {
            mInstance = new AudioPlayer();
        }

        return mInstance;
    }

    private AudioPlayer() {
        mAudioParam = new AudioParam();
        try {
            mQueue = new ArrayBlockingQueue<QueueData>(100);
//            createAudioTrack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDecFile(boolean isDecFile) {
        this.isDecFile = isDecFile;
        this.isPlayFile = false;
    }

    public void setPlayFile(boolean isPlayFile) {
        this.isPlayFile = isPlayFile;
        this.isDecFile = false;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public void setAacPath(String path) {
        mAacPath = path;
    }

    public void setPcmPath(String path) {
        mPcmPath = path;
    }

    public void setSampleRate(int sampleRate) {
        this.mSampleRate = sampleRate;
    }

    private void createAudioTrack() throws Exception {
        // 获得构建对象的最小缓冲区大小
        mMinBufferSize = AudioTrack.getMinBufferSize(mSampleRate,
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
                mSampleRate,
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
        mQueue.clear();
        if (mDecodeThread == null) {
            mDecodeThread = new DecodeThread();
            mDecodeThread.start();
        }
        if (mPlayAudioThread == null) {
            mPlayAudioThread = new PlayAudioThread();
            mPlayAudioThread.start();
        }
    }

    public void stopPlay() {
        if (mDecodeThread != null) {
            mDecodeThread.interrupt();
            mDecodeThread = null;
        }
        if (mPlayAudioThread != null) {
            mPlayAudioThread.interrupt();
            mPlayAudioThread = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    /**
     *  播放音频线程
     */
    class PlayAudioThread extends Thread {
        @Override
        public void run() {
            try {
                createAudioTrack();
                mAudioTrack.play();
                long start = System.currentTimeMillis();
                while (true) {
                    QueueData qdata = mQueue.take();
                    if (qdata != null && qdata.buffLen > 0) {
                        if (qdata.isEnding) {
                            break;
                        } else if (qdata.buffLen > 0) {
                            Log.d(TAG, "queue seqId:" + qdata.seqId);
                            long startWrite = System.currentTimeMillis();
                            mAudioTrack.write(qdata.pcmBuff, 0, qdata.buffLen);
                            Log.d(TAG, "mAudioTrack.write time:" + (System.currentTimeMillis() - startWrite));
                        }
                    }
                }
                mAudioTrack.stop();
                long playEnd = System.currentTimeMillis();
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_PLAY_COMPLETE;
                    msg.arg1 = (int) (playEnd - start);
                    mHandler.sendMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "PlayAudioThread complete...");
        }
    }

    /**
     * 解码线程
     */
    class DecodeThread extends Thread {
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
//                String aacPath = "/storage/emulated/0/aac-pcm/霍元甲.m4a";
//                String pcmPath = "/storage/emulated/0/aac-pcm/霍元甲.pcm";
                FileInputStream fis = null;
                if (isDecFile) {
                    File pcmFile = new File(mPcmPath);
                    if (!pcmFile.exists()) {
                        pcmFile.createNewFile();
                    }
                    long decFileStart = System.currentTimeMillis();
                    if (mHandler != null) {
                        mHandler.sendEmptyMessage(MSG_DEC_AAC_FILE_START);
                    }
                    int decFileErr = LibFaad.decodeAACFile(mAacPath, mPcmPath);
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
                    fis = new FileInputStream(mPcmPath);
                } else if (isPlayFile) {
                    fis = new FileInputStream(mPcmPath);
                } else{
                    fis = new FileInputStream(mAacPath);
                    LibFaad.openFaad();
                }

                int readCnt = -1;
                Log.d(TAG, "file size:" + fis.available());
                boolean initSuccess = false;
                byte[] file_buff = new byte[mMinBufferSize];
                int msgId = 0;
                while ((readCnt = fis.read(file_buff, 0, file_buff.length)) > 0) {
                    if (isDecFile || isPlayFile) {
                        QueueData qdata = new QueueData();
                        qdata.seqId = msgId;
                        qdata.pcmBuff = file_buff;
                        qdata.buffLen = readCnt;
                        qdata.isEnding = false;
                        Log.d(TAG, "queue length:" + mQueue.size());
                        mQueue.put(qdata);
                        file_buff = new byte[mMinBufferSize];
                        msgId++;
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
                            QueueData qdata = new QueueData();
                            qdata.seqId = msgId;
                            qdata.pcmBuff = pcmBuff;
                            qdata.buffLen = pcmBuff.length;
                            qdata.isEnding = false;
                            mQueue.put(qdata);
                            msgId++;
                        }
                    }
                }
                fis.close();
                if (!isDecFile) {
                    LibFaad.colseFaad();
                }
                /** 添加结束包到队列 */
                QueueData qdata = new QueueData();
                qdata.pcmBuff = new byte[0];
                qdata.buffLen = qdata.pcmBuff.length;
                qdata.isEnding = true;
                mQueue.put(qdata);
            } catch (Exception e) {
                mHandler.sendEmptyMessage(MSG_DEC_ERROR);
                e.printStackTrace();
            }
        }
    }
}