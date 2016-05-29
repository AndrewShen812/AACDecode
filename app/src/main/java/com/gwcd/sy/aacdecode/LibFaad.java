/**
 * 项目名称：AACDecode
 * 创建日期：2016年05月27日
 * Copyright 2016 GALAXYWIND Network Systems Co.,Ltd.All rights reserved.
 */
package com.gwcd.sy.aacdecode;

import android.os.Handler;
import android.os.Message;

/**
 * 类描述：<br>
 * 创建者：shenyong<br>
 * 创建时间：2016/5/27<br>
 * 修改记录：<br>
 */
public class LibFaad {

    static {
        System.loadLibrary("faad");
    }

//    public static Handler mAACDecHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//        }
//    };

    public static void DecCallback() {

    }

    public static native int openFaad();
    public static native int initFaad(byte[] buff, int buff_size, int samplerate, int channel);
    public static native byte[] decodeAAC(byte[] aac_buff, int aac_size);
    public static native int decodeAACFile(String aac_file, String pcm_file);
    public static native void colseFaad();
}
