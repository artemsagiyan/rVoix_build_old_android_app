# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ifndef NOLAME
USING_LAME := 1
endif

LOCAL_PATH := $(call my-dir)

ifdef USING_LAME
include $(CLEAR_VARS)
LOCAL_MODULE    := lame
LOCAL_SRC_FILES := lame/VbrTag.c \
        lame/bitstream.c \
        lame/encoder.c \
        lame/fft.c \
        lame/gain_analysis.c \
        lame/id3tag.c \
        lame/lame.c \
        lame/mpglib_interface.c \
        lame/newmdct.c \
        lame/presets.c \
        lame/psymodel.c \
        lame/quantize.c \
        lame/quantize_pvt.c \
        lame/reservoir.c \
        lame/set_get.c \
        lame/tables.c \
        lame/takehiro.c \
        lame/util.c \
        lame/vbrquantize.c \
        lame/version.c  
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
endif

include $(CLEAR_VARS)
LOCAL_MODULE    := voix 
LOCAL_SRC_FILES := voix.c 
LOCAL_LDLIBS := -llog

ifdef USING_LAME
LOCAL_CFLAGS += -O2 -Wall -DUSING_LAME
LOCAL_SHARED_LIBRARIES := lame
else
LOCAL_CFLAGS += -O2 -Wall
endif

include $(BUILD_SHARED_LIBRARY)

