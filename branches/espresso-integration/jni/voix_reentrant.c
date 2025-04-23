
#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/cdefs.h>
#include <sys/ioctl.h>
#include <jni.h>
#include <android/log.h>

#ifdef USING_LAME
#include "lame/lame.h"
#endif

/* From:  arch/arm/mach-msm/qdsp5_comp/vocpcm.c */
#define VOCPCM_IOCTL_MAGIC 'v'
#define VOCPCM_REGISTER_CLIENT          _IOW(VOCPCM_IOCTL_MAGIC, 0, unsigned)
#define VOCPCM_UNREGISTER_CLIENT        _IOW(VOCPCM_IOCTL_MAGIC, 1, unsigned)
#define FRAME_NUM       (12)
#define FRAME_SIZE      (160)
#define BUFFER_NUM      (2)
#define BUFFER_SIZE     (FRAME_NUM * FRAME_SIZE)

#define READ_SIZE	(BUFFER_SIZE*2)		/* buffers of uint16_t's */
#define OUT_DIR "/sdcard/voix"
#define AA_FILE "/sdcard/voix/myvoice"
#define RSZ		(16*1024)

#define log_info(...)	__android_log_print(ANDROID_LOG_INFO,"libvoix",__VA_ARGS__)
#define log_err(...) 	__android_log_print(ANDROID_LOG_ERROR,"libvoix",__VA_ARGS__)

static JavaVM *gvm; 
static jobject giface;

void *record(void *);
void *encode(void *);

typedef struct _rec_ctx_t {
    int alive;
    int uplink;	
    int ismp3;
    pthread_t rec1,rec2;
    char cur_file[256];
    struct _rec_ctx_t *x;
} rec_ctx;


int start_record(JNIEnv* env, jobject obj, jstring jfile, jint encoding) {

    pthread_attr_t attr;
    const char *file = 0;
    struct stat st;    
    rec_ctx *ctx;

	log_info("start_record");

	if(!jfile) return 1;

	if(stat(OUT_DIR,&st) < 0) {
	    if(mkdir(OUT_DIR,0777) != 0) {
		log_err("cannot create output directory " OUT_DIR ".");
		return 0;
	    } 	
	} else if(!S_ISDIR(st.st_mode)) {
	    log_err(OUT_DIR " is not a directory.");	     	
	    return 0;	
	}

	file = (*env)->GetStringUTFChars(env,jfile,NULL);
	if(!file || !*file) {
	   (*env)->ReleaseStringUTFChars(env,jfile,file);  return 0;
	}

	ctx = (rec_ctx *) malloc(sizeof(rec_ctx));
	ctx->x = (rec_ctx *) malloc(sizeof(rec_ctx));

	ctx->alive = 1;
	ctx->ismp3 = encoding;
	strcpy(ctx->cur_file,file);
        (*env)->ReleaseStringUTFChars(env,jfile,file);
	ctx->uplink = 0;
	memcpy(ctx->x,ctx,sizeof(rec_ctx));
	ctx->x->uplink = 1;

	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	/* records incoming sound data */
	pthread_create(&ctx->rec1,&attr,record,(void *) ctx);

	/* records outgoing sound data */
	pthread_create(&ctx->rec2,&attr,record,(void *) ctx->x);

	pthread_attr_destroy(&attr);

	log_info("started record threads");

    return (int) ctx;
}

void stop_record(JNIEnv* env, jobject obj, jint x) {

    pthread_t k;
    rec_ctx *ctx = (rec_ctx *) x;
 
	log_info("stop_record");
	ctx->alive = 0;	
	ctx->x->alive = 0;	
	pthread_create(&k,0,encode,(void *)ctx);	
}

void *record(void *init) {
    
    rec_ctx *ctx = (rec_ctx *) init;
    char file[256], *buff;
    int fd, fd_out;

	log_info("in %s thread", ctx->uplink ? "uplink" : "downlink");

	fd = open(ctx->uplink ? "/dev/voc_tx_record" : "/dev/voc_rx_record", O_RDWR);
	if(fd < 0)  {
            fd = open(ctx->uplink ? "/dev/vocpcm2" : "/dev/vocpcm0", O_RDWR);
	    if(fd < 0) {
         	log_err("cannot open %s driver", ctx->uplink ? "uplink" : "downlink" ); return 0;
	    }
	}

	/* positive return values are ok for this ioctl */
	if(ioctl(fd,VOCPCM_REGISTER_CLIENT,0) < 0) {
	    close(fd);	
            log_err("cannot register rpc client"); return 0;
	}

	sprintf(file, OUT_DIR "/%s-%s", ctx->cur_file, ctx->uplink ? "up" : "dn");	
	fd_out = open(file,O_CREAT|O_TRUNC|O_WRONLY);

	if(fd_out < 0) {
	    ioctl(fd,VOCPCM_UNREGISTER_CLIENT,0); 
	    close(fd);
	    log_err("cannot open output file"); return 0;
	}

	buff = (char *) malloc(READ_SIZE);
	if(!buff) {
	    ioctl(fd,VOCPCM_UNREGISTER_CLIENT,0); 
	    close(fd);  
	    close(fd_out); 
	    unlink(file);		
	    log_err("out of memory"); return 0;
	}

	while(ctx->alive) {
	    int i = read(fd,buff,READ_SIZE);
	    if(i < 0) {
		log_err("read error in %s thread: %d returned", ctx->uplink ? "uplink" : "downlink",i);
		 /* break; */
	    } else if(i <= READ_SIZE) write(fd_out,buff,i);
	}

	ioctl(fd,VOCPCM_UNREGISTER_CLIENT,0); 
	close(fd); 
	close(fd_out);
	free(buff);

    return 0;
}

static struct {
        uint32_t riff_id;
        uint32_t riff_sz;
        uint32_t riff_fmt;
        uint32_t fmt_id;
        uint32_t fmt_sz;
        uint16_t audio_format;
        uint16_t num_channels;
        uint32_t sample_rate;
        uint32_t byte_rate;       /* sample_rate * num_channels * bps / 8 */
        uint16_t block_align;     /* num_channels * bps / 8 */
        uint16_t bits_per_sample;
        uint32_t data_id;
        uint32_t data_sz;
} wavhdr = { 0x46464952, 0, 0x45564157, 0x20746d66, 16, 1, 2, 8000, 32000, 4, 16, 0x61746164, 0};

#ifdef USING_LAME
void lame_error_handler(const char *format, va_list ap) {
    __android_log_vprint(ANDROID_LOG_INFO,"libvoix",format,ap);
}
#endif

void *encode(void *init) {
   
    char file[256];
    int  fd1 = -1, fd2 = -1, fd_out = -1;
    uint32_t i, k;
    int16_t *b0 = 0, *b1 = 0, *b2 = 0;
    rec_ctx *ctx = (rec_ctx *) init;	

#ifdef USING_LAME
    lame_global_flags *gfp;
#endif
	log_info("in encode thread");

	pthread_join(ctx->rec1,0);
	pthread_join(ctx->rec2,0);

	log_info("recorder threads complete, encoding");

	sprintf(file, OUT_DIR "/%s-up",ctx->cur_file);
	fd1 = open(file,O_RDONLY);
	sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file);
	fd2 = open(file,O_RDONLY);
	if(fd1 < 0 || fd2 < 0) {
	     if(fd1 >= 0) { 
		close(fd1); sprintf(file, OUT_DIR "/%s-up",ctx->cur_file); unlink(file);
	     }	
	     if(fd2 >= 0) {
		close(fd2); sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file); unlink(file);
	     }	
	     log_err("encode: input file not found");
	     return 0;		
	}

#ifdef USING_LAME
     if(ctx->ismp3) 
        sprintf(file, OUT_DIR "/%s.mp3",ctx->cur_file);
     else 
#endif
	sprintf(file, OUT_DIR "/%s.wav",ctx->cur_file);

	fd_out = open(file,O_CREAT|O_TRUNC|O_WRONLY);
	if(fd_out < 0) {
	     log_err("encode: cannot open output file %s",file);
             close(fd1); sprintf(file, OUT_DIR "/%s-up",ctx->cur_file); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file); unlink(file);
	     return 0;		
	}

	i = (uint32_t) lseek(fd1,0,SEEK_END);
	k = (uint32_t) lseek(fd2,0,SEEK_END);
	lseek(fd1,0,SEEK_SET);
	lseek(fd2,0,SEEK_SET);

	if(i != k) {
	     log_info("file sizes mismatch");	
	     if(i > k) i = k;	    		
	}
	i &= ~1;
	if(i == 0) {
	     log_err("zero size input file");
	     close(fd_out); unlink(file);
	     close(fd1); sprintf(file, OUT_DIR "/%s-up",ctx->cur_file); unlink(file);		
	     close(fd2); sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file); unlink(file);
	     return 0;			
	}

#ifdef USING_LAME
     if(ctx->ismp3) {
	gfp = lame_init();
	lame_set_errorf(gfp,lame_error_handler);
	lame_set_debugf(gfp,lame_error_handler);
	lame_set_msgf(gfp,lame_error_handler);
	lame_set_num_channels(gfp,2);
	lame_set_in_samplerate(gfp,8000);
	lame_set_brate(gfp,64); /* compress 1:4 */
	lame_set_mode(gfp,0);	/* mode = stereo */
	lame_set_quality(gfp,2);   /* 2=high  5 = medium  7=low */ 
	if(lame_init_params(gfp) < 0) {
	     log_err("encode: failed to init lame"); 	
             close(fd_out); unlink(file);
             close(fd1); sprintf(file, OUT_DIR "/%s-up",ctx->cur_file); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file); unlink(file);
	     return 0;			
	}
#define CHSZ (512*1024)
#define OCHSZ (5*(CHSZ/8)+7200) 
	b0 = (int16_t *) malloc(OCHSZ);
        b1 = (int16_t *) malloc(CHSZ);
        b2 = (int16_t *) malloc(CHSZ);
        if(!b0 || !b1 || !b2) {
             log_err("encode: out of memory");
	     if(b0) free(b0);
             if(b1) free(b1);
             if(b2) free(b2);
             close(fd_out); unlink(file);
             close(fd1); sprintf(file, OUT_DIR "/%s-up",ctx->cur_file); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file); unlink(file);
             return 0;
        }
	k = 0;
	do {
	   int ret = 0;	
	   uint32_t j = 0, count = 0;
	   unsigned char *c1,*c2;	
	     for(c1 = (unsigned char*) b1, c2 = (unsigned char*) b2; count < CHSZ; c1 += ret, c2 += ret) {
		j = (k + RSZ > i) ? i - k : RSZ;
		ret = read(fd1,c1,j);
		if(ret < 0) break;
		ret = read(fd2,c2,j);
		if(ret < 0) break;
		k += ret; count += ret;
		if(k == i) break;
	     }
	     if(ret < 0) break;	
	     if(count) {
		ret = lame_encode_buffer(gfp,b1,b2,count/2,(unsigned char *)b0,OCHSZ);
		if(ret < 0) break;
		c1 = (unsigned char *) b0;
		count = ret;
		for(j = 0; j < count; j += RSZ, c1 += ret) {
		     ret = (j + RSZ > count) ? count - j : RSZ;
		     write(fd_out,c1,ret);
		}		
	     }				
	} while(k < i);

	i = (uint32_t) lame_encode_flush(gfp,(unsigned char *)b0,OCHSZ);
	if(i) write(fd_out,b0,i);
	lame_close(gfp);
     } else {		
#endif
        b0 = (int16_t *) malloc(RSZ*2);
        b1 = (int16_t *) malloc(RSZ);
        b2 = (int16_t *) malloc(RSZ);
        if(!b0 || !b1 || !b2) {
             log_err("encode: out of memory");
             if(b0) free(b0);
             if(b1) free(b1);
             if(b2) free(b2);
             close(fd_out); unlink(file);
             close(fd1); sprintf(file, OUT_DIR "/%s-up",ctx->cur_file); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file); unlink(file);
             return 0;
        }
	wavhdr.data_sz = i*2;
	wavhdr.riff_sz = i*2 + 36; 
	write(fd_out,&wavhdr,sizeof(wavhdr));
	k = 0;		
	while(1) {
	    uint32_t j, count = (k + RSZ > i) ? i - k : RSZ;
		if(read(fd1,b1,count) < 0) break;
		if(read(fd2,b2,count) < 0) break;
		for(j = 0; j < count/2; j++) {
		      b0[2*j] = b1[j];  b0[2*j+1] = b2[j];
		}			
		write(fd_out,b0,count*2);			
		k += count;
		if(k == i) break;
	}
#ifdef USING_LAME
    }
#endif
	close(fd1); close(fd2); close(fd_out);
	free(b0); free(b1); free(b2);	
	log_info("encoding complete");

	sprintf(file, OUT_DIR "/%s-up",ctx->cur_file); unlink(file);
	sprintf(file, OUT_DIR "/%s-dn",ctx->cur_file); unlink(file);

	free(ctx->x);
	free(ctx);

    return 0; 
}

/******   Answer the incoming call  ******/
void *say_them(void *);

/***** Java callback to hangup. Not reached on Hero 
(the phone rather hangs itself or reboots) *****/
void  call_java2hangup();

void answer_call(JNIEnv* env, jobject obj) {
    pthread_t pt;
	log_info("in answer_call");
	pthread_create(&pt,0,say_them,0);
}

void *say_them(void *p) {

    int  fd, aa_file;
    char *buff;

	log_info("in say_them() thread");

	fd = open("/dev/voc_tx_playback", O_RDWR);
        if(fd < 0)  {
            fd = open("/dev/vocpcm3",O_RDWR);
            if(fd < 0) {
                log_err("cannot open playback driver"); return 0;
            }
        }
        if(ioctl(fd,VOCPCM_REGISTER_CLIENT,0) < 0) {
            close(fd);
            log_err("cannot register rpc client for playback"); return 0;
        }
	aa_file = open(AA_FILE, O_RDONLY);
	if(aa_file < 0) {
            ioctl(fd,VOCPCM_UNREGISTER_CLIENT,0);
            close(fd);
            log_err("cannot open auto answer sound file " AA_FILE); return 0;
	}

	buff = (char *) malloc(BUFFER_SIZE);
	memset(buff,0,BUFFER_SIZE);

	write(fd,buff,BUFFER_SIZE);
	write(fd,buff,BUFFER_SIZE);

        log_info("answering...");

	while(1) {	
	    int i;
	    i  = read(aa_file,buff,BUFFER_SIZE);
	    if(i <= 0) break;
	    i = write(fd,buff,i);
	    if(i <= 0) break;
	}

	ioctl(fd,VOCPCM_UNREGISTER_CLIENT,0);
        close(fd);
	close(aa_file);
	free(buff);
        log_err("now hanging up in java");
	call_java2hangup();

    return 0;	
}

void call_java2hangup() {

    jclass cls;
    jmethodID mid;
    JNIEnv *env;

	if((*gvm)->GetEnv(gvm, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
	    if((*gvm)->AttachCurrentThread(gvm, &env, NULL) != JNI_OK) {
		log_err("AttachCurrentThread failed"); 
		return;
            }
        }
	cls = (*env)->GetObjectClass(env,giface);
	if(!cls) {
	    log_err("GetObjectClass failed"); 
	    return;
	}
	mid = (*env)->GetStaticMethodID(env, cls, "onCallAnswered", "()V");
	if(mid == NULL) {
	    log_err("cannot find java callback to call"); 
	    return;	
	}
        (*env)->CallStaticVoidMethod(env,cls,mid);
        (*gvm)->DetachCurrentThread(gvm);
	log_info("java notified");
}


/********************************************
*********************************************
*********************************************
*********************************************/

static const char *classPathName = "com/voix/RVoixSrv";


static JNINativeMethod methods[] = {
  { "startRecord", "(Ljava/lang/String;)I", (void *) start_record },
  { "stopRecord", "(I)V", (void *) stop_record },
  { "answerCall", "()V", (void *) answer_call }
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

    return JNI_VERSION_1_4;
}




