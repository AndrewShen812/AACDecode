/**
 * 项目名称：AACDecode
 * 创建日期：2016年06月01日
 * Copyright 2016 GALAXYWIND Network Systems Co.,Ltd.All rights reserved.
 */
package com.gwcd.indiacar.utils;

/**
 * 类描述：<br>
 * 创建者：shenyong<br>
 * 创建时间：2016/6/1<br>
 * 修改记录：<br>
 */
public class AudioDecoder {

    static {
        System.loadLibrary("faad");
    }

    public static native int open();
    public static native int init(byte[] buff, int buff_size, int samplerate, int channel);
    public static native byte[] decodeAAC(byte[] aac_buff, int aac_size);
    public static native int decodeAACFile(String aac_file, String pcm_file, int samplerate, int channel);
    public static native int decodeAACFile2(String aac_file, String pcm_file, int samplerate, int channel);
    public static native void colse();

}
