#include <jni.h>

#ifndef AACDECODE_COM_GWCD_INDIACAR_UTILS_AUDIODECODER_H
#define AACDECODE_COM_GWCD_INDIACAR_UTILS_AUDIODECODER_H

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    open
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_open
  (JNIEnv *, jclass);

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    init
 * Signature: ([BIII)I
 */
JNIEXPORT jint JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_init
  (JNIEnv *, jclass, jbyteArray, jint, jint, jint);

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    decodeAAC
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_decodeAAC
  (JNIEnv *, jclass, jbyteArray, jint);

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    decodeAACFile
 * Signature: (Ljava/lang/String;Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_decodeAACFile
  (JNIEnv *, jclass, jstring, jstring, jint, jint);

/*
 * Class:     com_gwcd_indiacar_utils_AudioDecoder
 * Method:    colse
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_gwcd_indiacar_utils_AudioDecoder_colse
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif

#endif //AACDECODE_COM_GWCD_INDIACAR_UTILS_AUDIODECODER_H
