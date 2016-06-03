#include <stdio.h>
#include <stdlib.h>
#include "com_gwcd_indiacar_utils_AudioDecoder.h"
#include "neaacdec.h"
#include "AndroidLog.h"
#include "aac_decode.h"

#define FRAME_MAX_LEN   1024*5
#define BUFFER_MAX_LEN  1024*1024
#define UP_SAMPLE_RATE  1   // 是否提高采样率

int get_one_ADTS_frame(unsigned char *buffer, size_t buf_size, unsigned char *data, size_t *data_size);

static NeAACDecHandle decHandle;

static int jSampleRate = 44100;
static int jChannel = 1;

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    open
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_open
        (JNIEnv *env, jclass cls) {
    decHandle = NeAACDecOpen();
    #if UP_SAMPLE_RATE == 1
        //防止采样频率加倍
        NeAACDecConfigurationPtr conf = NeAACDecGetCurrentConfiguration(decHandle);
        conf->dontUpSampleImplicitSBR = 1;
        NeAACDecSetConfiguration(decHandle, conf);
    #endif
}

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    init
 * Signature: ([BIII)I
 */
JNIEXPORT jint JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_init
        (JNIEnv *env, jclass cls, jbyteArray aacArr, jint aacLen, jint samplerate, jint channel) {
    //unsigned long aac_size = (*env)->GetArrayLength(evn, aacArr);
    unsigned char *aac_data = (unsigned char *) ((*env)->GetByteArrayElements(env, aacArr, NULL));
    unsigned char channels = (unsigned char)channel;
    unsigned long sample_rate = samplerate;
    long err = NeAACDecInit(decHandle, aac_data, aacLen, &sample_rate, &channels);
    LOGW("aac channels:%d", channels);
    LOGW("aac samplerate:%ld", sample_rate);
    if (err != 0) {
        LOGW("init decoder fail!");
        return -1;
    } else {
        jSampleRate = samplerate;
        jChannel = channel;
        LOGI("init decoder success!");
        return 0;
    }
}

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    decodeAAC
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_decodeAAC
        (JNIEnv *env, jclass cls, jbyteArray aacArr, jint aac_size) {
    unsigned char *buffer = (unsigned char *) ((*env)->GetByteArrayElements(env, aacArr, NULL));
    static unsigned char frame[FRAME_MAX_LEN];

    size_t data_size = aac_size;
    size_t size = 0;

    NeAACDecFrameInfo frame_info;
    unsigned char* input_data = buffer;
    unsigned char* pcm_data = NULL;
    jbyteArray pcm_array = (*env)->NewByteArray(env, 0);
    if(get_one_ADTS_frame(buffer, data_size, frame, &size) < 0)
    {
        LOGE("get no frame");
        return pcm_array;
    }

    int pcm_total_size = 0;
    int pcm_frame_size = 0;
    unsigned char* pcm_buffer = (unsigned char*) malloc(1024 * 512);
    while(get_one_ADTS_frame(input_data, data_size, frame, &size) == 0)
    {
        //decode ADTS frame
        pcm_data = (unsigned char*)NeAACDecDecode(decHandle, &frame_info, frame, size);

        if(frame_info.error > 0)
        {
            LOGD("error:%d, message:%s", frame_info.error, NeAACDecGetErrorMessage(frame_info.error));
        }
        else if(pcm_data && frame_info.samples > 0)
        {
            LOGD("frame info: bytesconsumed %ld, channels %d, header_type %d object_type %d, samples %ld, samplerate %ld\n",
                 frame_info.bytesconsumed,
                 frame_info.channels, frame_info.header_type,
                 frame_info.object_type, frame_info.samples,
                 frame_info.samplerate);

            LOGD("jChannel:%d", jChannel);
            if (jChannel == 1) {
                /*从双声道的数据中提取单通道
                 *参考：http://blog.csdn.net/yuan1125/article/details/50668412
                 */
                int i, j;
                for(i=0,j=0; i<4096 && j<2048; i+=4, j+=2)
                {
                    pcm_buffer[pcm_total_size + j] = pcm_data[i];
                    pcm_buffer[pcm_total_size + j + 1] = pcm_data[i + 1];
                }
                pcm_frame_size = frame_info.samples;
            } else {
                /*
                int i, j;
                for (i=0; i<4096; i++) {
                    pcm_buffer[pcm_total_size + i] = pcm_data[i];
                }*/
                pcm_frame_size = frame_info.samples * frame_info.channels;
                memcpy(pcm_buffer + pcm_total_size, pcm_data, pcm_frame_size);
            }
            pcm_total_size += pcm_frame_size;
        }
        data_size -= size;
        input_data += size;
    }
    LOGD("pcm_total_size:%d", pcm_total_size);
    pcm_array = (*env)->NewByteArray(env, pcm_total_size);
    (*env)->SetByteArrayRegion(env, pcm_array, 0, pcm_total_size, (jbyte *)pcm_buffer);

    pcm_data = NULL;
    input_data = NULL;
    buffer = NULL;
    free(pcm_buffer);
    pcm_buffer = NULL;
    return pcm_array;
}

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    decodeAACFile
 * Signature: (Ljava/lang/String;Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_decodeAACFile
  (JNIEnv *env, jclass cls, jstring aac_file, jstring pcm_file, jint jsampleRate, jint jchannels)
{
    static unsigned char frame[FRAME_MAX_LEN];
    static unsigned char frame_mono[FRAME_MAX_LEN];
    static unsigned char buffer[BUFFER_MAX_LEN] = {0};

    const char* src_file = (*env)->GetStringUTFChars(env, aac_file, 0);
    const char* dst_file = (*env)->GetStringUTFChars(env, pcm_file, 0);
    LOGD("src_file:%s", src_file);
    LOGD("dst_file:%s", dst_file);
    FILE* ifile = NULL;
    FILE* ofile = NULL;

    unsigned long samplerate;
    unsigned char channels;
    NeAACDecHandle decoder = 0;

    size_t data_size = 0;
    size_t size = 0;

    NeAACDecFrameInfo frame_info;
    unsigned char* input_data = buffer;
    unsigned char* pcm_data = NULL;

    ifile = fopen(src_file, "rb");
    ofile = fopen(dst_file, "wb");
    if(!ifile || !ofile)
    {
        LOGE("fail to open source or destination file");
        return -1;
    }

    data_size = fread(buffer, 1, BUFFER_MAX_LEN, ifile);
    LOGD("data_size:%d", data_size);
    //open decoder
    decoder = NeAACDecOpen();
    if(get_one_ADTS_frame(buffer, data_size, frame, &size) < 0)
    {
        LOGE("get no frame");
        return -1;
    }

    #if UP_SAMPLE_RATE == 1
        /* 防止采样频率加倍
         * 参考：http://blog.csdn.net/yuan1125/article/details/50668412
         */
        LOGE("dontUpSampleImplicitSBR");
        NeAACDecConfigurationPtr conf = NeAACDecGetCurrentConfiguration(decoder);
        conf->dontUpSampleImplicitSBR = 1;
        NeAACDecSetConfiguration(decoder, conf);
    #endif

    //initialize decoder
    NeAACDecInit(decoder, frame, size, &samplerate, &channels);
    LOGD("samplerate %ld, channels %d\n", samplerate, channels);
    int write_size = 0;
    while(get_one_ADTS_frame(input_data, data_size, frame, &size) == 0)
    {
        //decode ADTS frame
        pcm_data = (unsigned char*)NeAACDecDecode(decoder, &frame_info, frame, size);

        if(frame_info.error > 0)
        {
            LOGD("error:%s", NeAACDecGetErrorMessage(frame_info.error));
        }
        else if(pcm_data && frame_info.samples > 0)
        {
            LOGD("frame info: bytesconsumed %ld, channels %d, header_type %d object_type %d, samples %ld, samplerate %ld\n",
                 frame_info.bytesconsumed,
                 frame_info.channels, frame_info.header_type,
                 frame_info.object_type, frame_info.samples,
                 frame_info.samplerate);

            if (jchannels == 1) {
                /*从双声道的数据中提取单通道
                 *参考：http://blog.csdn.net/yuan1125/article/details/50668412
                 */
                int i, j;
                for(i=0,j=0; i<4096 && j<2048; i+=4, j+=2)
                {
                    frame_mono[j]=pcm_data[i];
                    frame_mono[j+1]=pcm_data[i+1];
                }
                write_size += frame_info.samples;
                fwrite(frame_mono, 1, frame_info.samples, ofile);      //单通道
            } else {
                fwrite(pcm_data, 1, frame_info.samples * frame_info.channels, ofile);      //多通道
            }
            fflush(ofile);
        }
        data_size -= size;
        input_data += size;
    }
    LOGD("write_size:%d", write_size);
    NeAACDecClose(decoder);

    fclose(ifile);
    fclose(ofile);

    return 0;
}

//URL长度
#define MAX_URL_LENGTH 2000

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    decodeAAC2
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_decodeAAC2
    (JNIEnv *env, jclass cls, jbyteArray aac_array, jint aac_size)
{
    unsigned char *aac_buffer = (unsigned char *) ((*env)->GetByteArrayElements(env, aac_array, NULL));
    int pcm_size = 0;
    unsigned char * pcm_data = decode_aac_byte_stream(aac_buffer, aac_size, &pcm_size);
    if (!pcm_data && pcm_size > 0) {
        jbyteArray pcm_array = (*env)->NewByteArray(env, pcm_size);
        (*env)->SetByteArrayRegion(env, pcm_array, 0, pcm_size, (jbyte *)pcm_data);

        return pcm_array;
    }

    return (*env)->NewByteArray(env, 0);
}

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    decodeAACFile2
 * Signature: (Ljava/lang/String;Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_decodeAACFile2
  (JNIEnv *env, jclass cls, jstring aac_file, jstring pcm_file, jint jsampleRate, jint jchannels)
{
    const char* src_file = (*env)->GetStringUTFChars(env, aac_file, 0);
    const char* dst_file = (*env)->GetStringUTFChars(env, pcm_file, 0);
    LOGD("src_file:%s", src_file);
    LOGD("dst_file:%s", dst_file);
    // 检查文件路径
	if(strcmp(src_file,"") == 0 || strcmp(dst_file,"") == 0){
	    LOGE("文件路径错误！");
		return;
	}

	int argc=6;
	char **argv=(char **)malloc(MAX_URL_LENGTH);
	argv[0]=(char *)malloc(MAX_URL_LENGTH);
	argv[1]=(char *)malloc(MAX_URL_LENGTH);
	argv[2]=(char *)malloc(MAX_URL_LENGTH);
	argv[3]=(char *)malloc(MAX_URL_LENGTH);
	argv[4]=(char *)malloc(MAX_URL_LENGTH);
	argv[5]=(char *)malloc(MAX_URL_LENGTH);

	strcpy(argv[0],"dummy");

	strcpy(argv[1],"-f");
	// 输出格式：有WAV和RAW PCM两种，这里默认为RAW PCM
	//strcpy(argv[2],"1"); // WAV格式
    strcpy(argv[2],"2"); // RAW PCM格式

	strcpy(argv[3],"-b");
	/* 采样率：默认16bit
	1、16 bit pcm data
	2、24 bit pcm data
	3、32 bit pcm data
	4、32 bit floating data
	5、64 bit floating data
	*/
	strcpy(argv[4],"1");

	// 文件路径
	strcpy(argv[5],src_file);

	// 解码文件和打印输出
	int dec_ret = aac_decode(argc, argv);

	// 释放内存
	free(argv[0]);
	free(argv[1]);
	free(argv[2]);
	free(argv[3]);
	free(argv[4]);
	free(argv[5]);
	free(argv);

	return dec_ret;
}

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    colse
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_colse
        (JNIEnv *env, jclass cls)
{
    NeAACDecClose(decHandle);
}

int get_one_ADTS_frame(unsigned char *buffer, size_t buf_size, unsigned char *data, size_t *data_size)
{
    size_t size = 0;

    if (!buffer || !data || !data_size) {
        return -1;
    }

    while (1) {
        if (buf_size < 7) {
            return -1;
        }
        if ((buffer[0] == 0xff) && ((buffer[1] & 0xf0) == 0xf0)) {
            size |= ((buffer[3] & 0x03) << 11);     //high 2 bit
            size |= buffer[4] << 3;                //middle 8 bit
            size |= ((buffer[5] & 0xe0) >> 5);        //low 3bit
            break;
        }
        --buf_size;
        ++buffer;
    }

    if (buf_size < size) {
        return -1;
    }

    memcpy(data, buffer, size);
    *data_size = size;

    return 0;
}
