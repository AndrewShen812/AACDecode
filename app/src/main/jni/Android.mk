LOCAL_PATH := $(call my-dir)

FAAD2_TOP := $(LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_LDLIBS := -llog

include $(FAAD2_TOP)/libfaad/Android.mk