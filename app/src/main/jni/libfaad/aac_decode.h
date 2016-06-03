//
// Created by Lenovo on 2016/6/1.
//

#ifndef AACDECODE_AAC_DECODE_C_H
#define AACDECODE_AAC_DECODE_C_H

int aac_decode(int argc, char *argv[]);
unsigned char* decode_aac_byte_stream(unsigned char* aac_buffer, int buffer_size, int* pcm_size);

#endif //AACDECODE_AAC_DECODE_C_H
