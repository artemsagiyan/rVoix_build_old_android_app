#include <stdio.h>
#include <time.h>
#include <unistd.h>
#include <pthread.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/cdefs.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <errno.h>
#include <jni.h>
#include <android/log.h>

#ifdef USING_LAME
#include "lame/lame.h"
#endif

/* 
  Device types for each generation of Quallcom devices.
  The number is the one assigned by QC to the first device of each generation.
 */
#define DEVICE_UNKNOWN		0
#define DEVICE_MSM7K		7200	/* HTC Magic, ... */
#define DEVICE_SNAPDRAGON1	8250	/* Nexus One, ... */
#define DEVICE_SNAPDRAGON2	8255	/* HTC Desire HD, ... */
#define DEVICE_SNAPDRAGON3	8260	/* HTC Sensation, ... */

#define CODEC_WAV	0
#define CODEC_MP3	1
#define CODEC_AMR	2	

#define BUFFER_SIZE	(1024*4)
#define READ_SIZE	(BUFFER_SIZE*2)		/* buffers of uint16_t's */
#define AMR_FRMSZ       36
#define AMR_READ_SIZE   (20*AMR_FRMSZ)
#define AMR_RSZ         (200*AMR_FRMSZ)
#define RSZ             (16*1024)



/* Device driver ioctls: old devices */

#define VOCPCM_IOCTL_MAGIC 'v'
#define VOCPCM_REGISTER_CLIENT          _IOW(VOCPCM_IOCTL_MAGIC, 0, unsigned)
#define VOCPCM_UNREGISTER_CLIENT        _IOW(VOCPCM_IOCTL_MAGIC, 1, unsigned)

/* Device driver ioctls: new devices */

#define AUDIO_IOCTL_MAGIC 'a'
#define AUDIO_START        _IOW(AUDIO_IOCTL_MAGIC, 0, unsigned)
#define AUDIO_STOP         _IOW(AUDIO_IOCTL_MAGIC, 1, unsigned)
#define AUDIO_FLUSH        _IOW(AUDIO_IOCTL_MAGIC, 2, unsigned)
#define AUDIO_GET_CONFIG   _IOR(AUDIO_IOCTL_MAGIC, 3, unsigned)
#define AUDIO_SET_CONFIG   _IOW(AUDIO_IOCTL_MAGIC, 4, unsigned)
#define AUDIO_SET_INCALL   _IOW(AUDIO_IOCTL_MAGIC, 19, struct msm_voicerec_mode)

#define AUDIO_SET_VOLUME     _IOW(AUDIO_IOCTL_MAGIC, 10, unsigned)
#define AUDIO_SWITCH_DEVICE  _IOW(AUDIO_IOCTL_MAGIC, 32, unsigned)
#define AUDIO_SET_MUTE       _IOW(AUDIO_IOCTL_MAGIC, 33, unsigned)
#define AUDIO_START_VOICE    _IOW(AUDIO_IOCTL_MAGIC, 35, unsigned)
#define AUDIO_STOP_VOICE     _IOW(AUDIO_IOCTL_MAGIC, 36, unsigned)

/*
AUDIO_GET_VOICE_STATE -- РјРѕР№ РЅРѕРІС‹Р№ ioctl, РєРѕС‚РѕСЂС‹Р№ СЏ РІСЃС‚Р°РІРёР» РІ HD. Р’ РѕР±С‹С‡РЅРѕРј Desire РµРіРѕ РЅРµС‚.
Рў.Рѕ. РІСЃРµ СѓРїСЂРѕС‰Р°РµС‚СЃСЏ: РёР· java РјРѕР¶РЅРѕ СѓР·РЅР°С‚СЊ, С‡С‚Рѕ Р·Р° СѓСЃС‚СЂРѕР№СЃС‚РІРѕ, РІС‹Р·РѕРІРѕРј С‚Р°РєРѕР№ С„СѓРЅРєС†РёРё (РёР· onCreate РѕС‚РєСѓРґР°-РЅРёР±СѓРґСЊ).
РџРѕСЃР»Рµ _СЌС‚РѕРіРѕ_, РµСЃР»Рё HD, С‚Рѕ _РЅРµ РІС‹РїРѕР»РЅСЏС‚СЊ_ С‚Р°Рј РєРѕРґ СЃ РѕР¶РёРґР°РЅРёРµРј СѓСЃС‚Р°РЅРѕРІР»РµРЅРёСЏ СЃРѕРµРґРёРЅРµРЅРёСЏ РїРѕ СЃРѕРѕР±С‰РµРЅРёСЋ РёР· Р»РѕРіР° СЂР°РґРёРѕ, СЃСЂР°Р·Сѓ РЅР°С‡РёРЅР°С‚СЊ Р·Р°РїРёСЃСЊ.
РџСЂРёРІСЏР·Р°Р» РІРЅРёР·Сѓ Рє public static native bool isDesireHD();
*/

#define AUDIO_GET_VOICE_STATE   _IOR(AUDIO_IOCTL_MAGIC, 55, unsigned)
#define AUDIO_GET_DEV_DRV_VER   _IOR(AUDIO_IOCTL_MAGIC, 56, unsigned)

#define VOICE_STATE_INVALID     0x0
#define VOICE_STATE_INCALL      0x1
#define VOICE_STATE_OFFCALL     0x2
#define AUDIO_FLAG_INCALL_MIXED 0x2


/* Common macros */

#define log_info(...)	__android_log_print(ANDROID_LOG_INFO, "libvoix",__VA_ARGS__)
#define log_err(...) 	__android_log_print(ANDROID_LOG_ERROR,"libvoix",__VA_ARGS__)

#ifdef DEBUG
#define log_dbg(...)	__android_log_print(ANDROID_LOG_DEBUG,"libvoix",__VA_ARGS__)
#else
#define log_dbg(...)
#endif


/* Functions exported by voix_msm7k.c and voix_msm8k.c */
extern int start_record_msm7k(JNIEnv* env, jobject obj, jstring jfolder, jstring jfile, jint codec, jint boost_up, jint boost_dn);
extern int start_record_msm8k(JNIEnv* env, jobject obj, jstring jfolder, jstring jfile, jint codec);

extern void stop_record_msm8k(JNIEnv* env, jobject obj, jint context);
extern void stop_record_msm7k(JNIEnv* env, jobject obj, jint context);

extern void kill_record_msm7k(JNIEnv* env, jobject obj, jint context);
extern void kill_record_msm8k(JNIEnv* env, jobject obj, jint context);


/* Global variables from voix_main.c */
extern pthread_mutex_t mutty;
extern int device_type;


/* Functions exported by voix_main.c */
extern void recording_started(int ctx);
extern void recording_complete(int ctx);
extern void recording_error(int ctx, int error);
extern void encoding_started(int ctx);
extern void encoding_complete(int ctx);
extern void encoding_error(int ctx, int error);
#ifdef CR_CODE
extern void trial_period_ended();
extern int is_trial_period_ended();
#endif

/* Functions exported by voix_enc.c */
extern int wav_encode(int fd_in, int fd_out);
extern int wav_encode_stereo(int fd_in1, int fd_in2, int fd_out, int boost_up, int boost_dn);
extern int amr_encode(int fd_in, int fd_out);

#ifdef USING_LAME
extern int mp3_encode(int fd_in, int fd_out);
extern int mp3_encode_stereo(int fd_in1, int fd_in2, int fd_out, int boost_up, int boost_dn);
extern int convert_to_mp3(JNIEnv* env, jobject obj, jstring ifile, jstring ofile);
#endif


