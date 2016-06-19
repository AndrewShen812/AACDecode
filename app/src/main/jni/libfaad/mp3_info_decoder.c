//
// Created by Lenovo on 2016/6/19.
//
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "AndroidLog.h"
#include "mp3_info_decoder.h"

void print_ID3v1_info(FILE* mp3_file);
void print_ID3v2_info(FILE* mp3_file);

void print_mp3_info(const unsigned char* path)
{
    FILE* mp3_file = NULL;
    unsigned char v1_tag[128], v2_tag[10];
    mp3_file = fopen(path, "rb");
    if (!mp3_file) {
        LOGE("open mp3 file error.");
        return;
    }

    memset(v1_tag, 0, 128);
    memset(v2_tag, 0, 10);
    fseek(mp3_file, 0, SEEK_END);
    long file_len = ftell(mp3_file);
    fseek(mp3_file, 0, SEEK_SET);
    /** 读取ID3v2 TAG标识 */
    fread(v2_tag, 1, 10, mp3_file);
    /** 读取ID3v1 TAG标识 */
    fseek(mp3_file, file_len - 128, SEEK_SET);
    fread(v1_tag, 1, 128, mp3_file);
    if (memcmp(v2_tag, "ID3", 3) == 0) {
        LOGD("发现ID3v2信息");
        print_ID3v2_info(mp3_file);
    } else if (memcmp(v1_tag, "TAG", 3) == 0) {
        LOGD("发现ID3v1信息");
        print_ID3v1_info(mp3_file);
    } else {
        LOGE("没有发现ID3信息");
    }

    fclose(mp3_file);
    mp3_file = NULL;
}

/**
 * ID3V1标签结构:
 * 字段       长度（byte）    说明
 * Header    3              内容总是“TAG”
 * Title     30             歌曲的标题
 * Artist    30             歌手
 * Album     30             歌曲的专辑
 * Year      4              年份
 * Comment   28             注释
 * Reserve   1              保留字段
 * Track     1              歌曲在专辑中的位置
 * Genre     1              歌曲风格索引值
 */
void print_ID3v1_info(FILE* mp3_file)
{
    if (!mp3_file) {
        return;
    }
    unsigned char info_buff[128];
    unsigned char title[30];
    unsigned char artist[30];
    unsigned char album[30];
    unsigned char year[4];
    unsigned char comment[28];
    unsigned char track;
    unsigned char genre;
    memset(info_buff, 0, 128);
    memset(title, 0, 30);
    memset(artist, 0, 30);
    memset(album, 0, 30);
    memset(year, 0, 4);
    memset(comment, 0, 28);
    fseek(mp3_file, -128, SEEK_END);
    fread(info_buff, 1, 128, mp3_file);
    if (memcmp(info_buff, "TAG", 3) != 0) {
        LOGE("没有ID3v1信息");
        return;
    }
    memcpy(title, info_buff+3, 30);
    memcpy(artist, info_buff+33, 30);
    memcpy(album, info_buff+63, 30);
    memcpy(year, info_buff+93, 4);
    memcpy(comment, info_buff+97, 28);
    memcpy(&track, info_buff+126, 1);
    memcpy(&genre, info_buff+127, 1);
    int iYear = year[0] << 24 || year[1] << 16 || year[2] << 8 || year[3];
    LOGD("title:%s", title);
    LOGD("artist:%s", artist);
    LOGD("album:%s", album);
    LOGD("year:%d", iYear);
    LOGD("comment:%s", comment);
    LOGD("track:%d", (int)track);
    LOGD("genre:%d", (int)genre);
}

void print_ID3v2_info(FILE* mp3_file)
{
    if (!mp3_file) {
        return;
    }
    unsigned char v2_tag[10];
    memset(v2_tag, 0, 10);
    fseek(mp3_file, 0, SEEK_SET);
    fread(v2_tag, 1, 10, mp3_file);
    if (memcmp(v2_tag, "ID3", 3) != 0) {
        LOGE("没有ID3v2信息");
        return;
    }
    /** 
        * 版本号，一个字节;ID3V2.3就记录03,ID3V2.4就记录04
    */
    LOGD("版本号：%d", (int)v2_tag[3]);
    /**
     * 副版本号，一个字节;此版本记录为00 
     */
    LOGD("副版本号：%d", (int)v2_tag[4]);
    /** 
     * 标志字节一般为0，定义如下：abc00000 
     * a --表示是否使用不同步(一般不设置) 
     * b --表示是否有扩展头部，一般没有(至少Winamp没有记录)，所以一般也不设置   
     * c --表示是否为测试标签(99.99%的标签都不是测试用的啦，所以一般也不设置)    */
    LOGD("Flags：%d", (int)v2_tag[5]);
    /** 
         * 标签大小，包括标签帧和扩展标签头。（不包括标签头的10个字节）
     * 一共四个字节，但每个字节只用7位，最高位不使用恒为0。所以格式如下   
     * 0xxxxxxx 0xxxxxxx0xxxxxxx0xxxxxxx 
         * 计算大小时要将0去掉，得到一个28位的二进制数，就是标签大小，计算公式如下：*/
    int ID3v2_size = v2_tag[6]*0x200000 + v2_tag[7]*0x4000 + v2_tag[8]*0x80 + v2_tag[9];
    LOGD("ID3v2_size：%d", ID3v2_size);
    unsigned char tmp[4];
    unsigned char fid[5];
    int read_cnt = 0;
    int frame_size = 0;
    do {
        memset(tmp, 0, 4);
        memset(fid, 0, 5);
        fread(fid, 1, 4, mp3_file);
        fid[4] = '\0';
        LOGD("frame ID：%s", fid);
        fread(tmp, 1, 4, mp3_file);
        frame_size =  tmp[0] * 0x1000000
                      + tmp[1] * 0x10000
                      + tmp[2] * 0x100
                      + tmp[3];
        LOGD("frame size：%d", frame_size);
        // 跳过Flags
        fread(tmp, 1, 2, mp3_file);
        fseek(mp3_file, 2, SEEK_CUR);
        /** TIT2帧数据的第一个字节标识编码方式
         * 0代表字符使用ISO-8859-1编码方式；
         * 1代表字符使用UTF-16编码方式；
         * 2代表字符使用 UTF-16BE编码方式；
         * 3代表字符使用UTF-8编码方式。
         */
//        memset(tmp, 0, 4);
//        fread(tmp, 1, 1, mp3_file);
//        LOGD("编码方式：%d", (int)tmp[0]);
        unsigned char frame_desc[frame_size + 1];
        memset(frame_desc, 0, frame_size + 1);
        //fread(frame_desc, 1, frame_size - 1, mp3_file);
        fread(frame_desc, 1, frame_size, mp3_file);
        int i;/*
        for(i=0; i<=frame_size; i++) {
            LOGD("%c", frame_desc[i]);
        }*/
        frame_desc[frame_size + 1] = '\0';
        LOGD("%s：%s", fid, frame_desc);
        read_cnt += frame_size;
    } while(read_cnt < ID3v2_size);
}
