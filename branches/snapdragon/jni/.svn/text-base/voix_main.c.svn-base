#include "voix.h"


/* Global variables */

pthread_mutex_t mutty;
int device_type = DEVICE_UNKNOWN;

/* Static variables */

static JavaVM *gvm;
static jobject giface;


static jint get_patch_info(JNIEnv* env, jobject obj) {
    int i, fd, info;
	fd = open("/dev/msm_pcm_in",O_RDONLY);
	if(fd < 0) return 0;
	i = ioctl(fd, AUDIO_GET_DEV_DRV_VER, &info, sizeof(info)) == 0;
	close(fd);
    return i ? info : 0;
}

static int get_device_type() {

    struct stat st;
    unsigned int info;
    int fd;

	if(device_type != DEVICE_UNKNOWN) return device_type;

	if(stat("/dev/voc_tx_record",&st) == 0 || stat("/dev/vocpcm2",&st) == 0) {
		device_type = DEVICE_MSM7K;
		return device_type;
	}

        fd = open("/dev/msm_pcm_in",O_RDONLY);
        if(fd < 0) return DEVICE_UNKNOWN;

	/* new msm8k drivers: all DEVICE_SNAPDRAGON3 or _newer_ DEVICE_SNAPDRAGON1/2 devices have this ioctl  */
	if(ioctl(fd, AUDIO_GET_DEV_DRV_VER, &info, sizeof(info)) == 0) {
		device_type = (info >> 16);
		goto dev_found;
	}

	/* old msm8k drivers: all DEVICE_SNAPDRAGON2 devices have this call, all DEVICE_SNAPDRAGON1 haven't */
	if(ioctl(fd,AUDIO_GET_VOICE_STATE, &info, sizeof(info)) == 0) {
		device_type = DEVICE_SNAPDRAGON2;
		goto dev_found;	
	}

	/* last attempt: SNAPDRAGON1 & SNAPDRAGON2 use different names for amr driver (msm_amr_in vs. msm_amrnb_in) */
	device_type = (stat("/dev/msm_amrnb_in", &st) == 0) ? DEVICE_SNAPDRAGON2 : DEVICE_SNAPDRAGON1;

    dev_found:
	close(fd);
    return device_type;	
}

static jint java_get_device_type(JNIEnv* env, jobject obj) {
    return (jint) get_device_type(); 
}

static int start_record(JNIEnv* env, jobject obj, jstring jfolder, jstring jfile, jint codec, jint boost_up, jint boost_dn) {

    switch(get_device_type())	{
	case DEVICE_MSM7K:
		return start_record_msm7k(env,obj,jfolder,jfile,codec,boost_up,boost_dn);
	case DEVICE_SNAPDRAGON1:
	case DEVICE_SNAPDRAGON2:
	case DEVICE_SNAPDRAGON3:
		return start_record_msm8k(env,obj,jfolder,jfile,codec);
	default:
		return 0;
    }	
}

static void stop_record(JNIEnv* env, jobject obj, jint context) {
    if(device_type == DEVICE_MSM7K) stop_record_msm7k(env,obj,context);	
    else stop_record_msm8k(env,obj,context);	
}

static void kill_record(JNIEnv* env, jobject obj, jint context) {
    if(device_type == DEVICE_MSM7K) kill_record_msm7k(env,obj,context);
    else kill_record_msm8k(env,obj,context);
}

static void call_java_callback(const char *function, int n, ...) {

    jclass cls;
    jmethodID mid = 0;
    JNIEnv *env;
    va_list va;
    int context, error;

        if((*gvm)->GetEnv(gvm, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
            if((*gvm)->AttachCurrentThread(gvm, &env, NULL) != JNI_OK) {
                log_err("AttachCurrentThread failed");
                return;
            }
        }
        cls = (*env)->GetObjectClass(env,giface);
        if(!cls) {
            log_err("GetObjectClass failed");
            (*gvm)->DetachCurrentThread(gvm);
            return;
        }

	if(n == 0)  mid = (*env)->GetStaticMethodID(env, cls, function, "()V");
	else if(n == 1) mid = (*env)->GetStaticMethodID(env, cls, function, "(I)V");
	else if(n == 2) mid = (*env)->GetStaticMethodID(env, cls, function, "(II)V");
	
        if(mid == NULL) {
            log_err("cannot find java callback to call");
            (*gvm)->DetachCurrentThread(gvm);
            return;
        }

 	if(n == 0) (*env)->CallStaticVoidMethod(env,cls,mid);
	else {
	    va_start(va,n);
	    context = va_arg(va, int);
	    if(n == 1) (*env)->CallStaticVoidMethod(env,cls,mid,context);
	    else {
		error = va_arg(va, int);
		(*env)->CallStaticVoidMethod(env,cls,mid,context,error);
	    }
	    va_end(va);
	}

        (*gvm)->DetachCurrentThread(gvm);
}

void recording_started(int ctx) {
   call_java_callback("onStartRecording",1,ctx);
   log_dbg("java onStartRecording notified");
}

void recording_complete(int ctx) {
   call_java_callback("onRecordingComplete",1,ctx);
   log_dbg("java onRecordingComplete notified");
}

void recording_error(int ctx, int error) {
   call_java_callback("onErrorRecording",2,ctx,error);
   log_dbg("java onErrorRecording notified");
}

void encoding_started(int ctx) {
   call_java_callback("onStartEncoding",1,ctx);
   log_dbg("java onStartEncoding notified");
}

void encoding_complete(int ctx) {
   call_java_callback("onEncodingComplete",1,ctx);
   log_dbg("java onEncodingComplete notified");
}

void encoding_error(int ctx, int error) {
   call_java_callback("onErrorEncoding",2,ctx,error);
   log_dbg("java onErrorEncoding notified");
}



void answer_call(JNIEnv* env, jobject obj, jstring jfile, jstring ofile, jint jbd) {
}

static const char *classPathName = "com/voix/RVoixSrv";


static JNINativeMethod methods[] = {
  { "startRecord", "(Ljava/lang/String;Ljava/lang/String;III)I", (void *) start_record },
  { "stopRecord", "(I)V", (void *) stop_record },
  { "killRecord", "(I)V", (void *) kill_record },
  { "getDeviceType", "()I",  (void *) java_get_device_type },
#ifdef USING_LAME
  { "convertToMp3", "(Ljava/lang/String;Ljava/lang/String;)I", (void *) convert_to_mp3 },
#endif
  { "getKernelPatchInfo", "()I", (void *) get_patch_info },
  { "answerCall", "(Ljava/lang/String;Ljava/lang/String;I)V", (void *) answer_call }
};

jint JNI_OnLoad(JavaVM* vm, void* reserved) {

    jclass clazz = NULL;
    JNIEnv* env = NULL;
    jmethodID constr = NULL;
    jobject obj = NULL;

	log_info("JNI_OnLoad");

	gvm = vm;
	if((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
	    log_err("GetEnv FAILED"); return -1;
	}

	clazz = (*env)->FindClass(env,classPathName);
	if(!clazz) {
	    log_err("Failed to find class"); return -1;
	}

	constr = (*env)->GetMethodID(env, clazz, "<init>", "()V");
	if(!constr) {
	    log_err("Failed to get constructor"); return -1;
	}

	obj = (*env)->NewObject(env, clazz, constr);
	if(!obj) {
	    log_err("Failed to create interface object"); return -1;
	}
	giface = (*env)->NewGlobalRef(env,obj);

	if((*env)->RegisterNatives(env, clazz, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
	    log_err("Registration failed"); return -1;
	}
	pthread_mutex_init(&mutty,0);

    return JNI_VERSION_1_4;
}

