//
// Created by Lenovo on 2016/6/19.
//

#ifndef AACDECODE_MP3_INFO_DECODER_H
#define AACDECODE_MP3_INFO_DECODER_H

typedef struct MP3Info {
    /** ID3v1信息 */
    unsigned char* title;
    unsigned char* artist;
    unsigned char* album;
    int year;
    unsigned char* comment;
    unsigned char* style;

    /** ID3v2信息 */

} mp3_info_t;

void print_mp3_info(const unsigned char* path);

#endif //AACDECODE_MP3_INFO_DECODER_H
