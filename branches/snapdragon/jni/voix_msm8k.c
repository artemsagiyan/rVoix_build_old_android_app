#include "voix.h"


/* Threads */ 	
static void *record(void *);
static void *encode(void *);
static void *closeup(void *);


/* Structure declarations */

/* Different from that for msm7k */
typedef struct {
    int codec;
    int fd_in;		/* device */
    int	fd_out;		/* file being recorded */
    char *cur_file;	/* its full path */
    pthread_t rec;	/* recording thread */
    char *buff;		/* read buffer */
    int bsz;		/* its size */	
    int alive;		/* context is recording*/ 	
} rec_ctx;


struct msm_audio_config {
        uint32_t buffer_size;
        uint32_t buffer_count;
        uint32_t channel_count;
        uint32_t sample_rate;
        uint32_t type;
        uint32_t unused[3];
};

struct msm_audio_amrnb_enc_config_v2 {
        uint32_t band_mode;
        uint32_t dtx_enable;
        uint32_t frame_format;
};

struct msm_voicerec_mode {
        uint32_t rec_mode;
};


static int init_recording(rec_ctx *ctx);

/*  Must be called with mutex locked.
    Closing active device may take some time, 
    so it should generally be done from a separate thread. */

static void close_device(rec_ctx *ctx) {
    if(!ctx) return;
    if(ctx->fd_in >= 0) {
	ioctl(ctx->fd_in,AUDIO_STOP,0);
	ioctl(ctx->fd_in,AUDIO_FLUSH,0);
	close(ctx->fd_in);  
	ctx->fd_in = -1;
    }
}

/*  Free context. If start_record() ever returns non-zero, 
    it *must* be freed via stop_record() or kill_record(). */

static void free_ctx(rec_ctx *ctx) {
    if(!ctx) return;	
    if(ctx->cur_file) free(ctx->cur_file);
    if(ctx->buff) free(ctx->buff);
    free(ctx);
}


int start_record_msm8k(JNIEnv* env, jobject obj, jstring jfolder, jstring jfile, jint codec) {

    pthread_attr_t attr;
    const char *folder = 0;
    const char *file = 0;
    struct stat st;
    rec_ctx *ctx = 0;

	log_info("start_record");

#ifdef CR_CODE
	if(is_trial_period_ended()) {
	    trial_period_ended();
	    return 0;
	}
#endif

	if(!jfile || !jfolder) {
	    log_err("invalid parameters from jni");	
	    return 0;
	}
	
	switch(codec) {
	    case CODEC_WAV: case CODEC_AMR:
#ifdef USING_LAME
	    case CODEC_MP3:
#endif
 		break;
	    default:
		log_err("invalid codec");
		return 0;	
	}

	pthread_mutex_lock(&mutty);

	folder = (*env)->GetStringUTFChars(env,jfolder,NULL);
	file = (*env)->GetStringUTFChars(env,jfile,NULL);

	if(!folder || !*folder || !file || !*file) {
	    log_err("invalid parameters from jni");
	    goto fail;
	}

	if(stat(folder,&st) < 0) {
	    if(mkdir(folder,0777) != 0) {
		log_err("cannot create output directory");
		goto fail;
	    }
	} else if(!S_ISDIR(st.st_mode)) {
	    log_err("%s is not a directory.", folder);
	    goto fail;
	}

	ctx = (rec_ctx *) malloc(sizeof(rec_ctx));

	if(!ctx) {
	    log_err("no memory!");
	    goto fail;
	}

	ctx->alive = 0;
	ctx->codec = codec;
	ctx->fd_in = -1;
	ctx->fd_out = -1; 
	ctx->cur_file = (char *) malloc(strlen(folder)+strlen(file)+2);
	ctx->bsz = (codec == CODEC_AMR ? AMR_READ_SIZE : READ_SIZE);
        ctx->buff = (char *) malloc(ctx->bsz);

	if(!ctx->cur_file || !ctx->buff) {
            log_err("no memory!");
	    goto fail;	
        }

	sprintf(ctx->cur_file, "%s/%s", folder, file);

	/* Open/configure the device, and open output file */	

	if(!init_recording(ctx)) goto fail;
	
	ctx->alive = 1;

	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	pthread_create(&ctx->rec,&attr,record,ctx);
	log_info("started recording thread");

	pthread_attr_destroy(&attr);
	(*env)->ReleaseStringUTFChars(env,jfolder,folder);
	(*env)->ReleaseStringUTFChars(env,jfile,file);

	pthread_mutex_unlock(&mutty);

	return (int) ctx;

    fail:

	(*env)->ReleaseStringUTFChars(env,jfolder,folder);
	(*env)->ReleaseStringUTFChars(env,jfile,file);
	free_ctx(ctx);

	pthread_mutex_unlock(&mutty);

	return 0;
}


/*  Called from start_record() only. 
    Device is closed on error (if it was opened). */

static int init_recording(rec_ctx *ctx) {

    struct msm_voicerec_mode vrm;
    struct msm_audio_config cfg;

	switch(ctx->codec) {
	    case CODEC_WAV:
#ifdef USING_LAME
	    case CODEC_MP3:
#endif
		    ctx->fd_in = open("/dev/qc_pcm_in",O_RDONLY);
		    if(ctx->fd_in < 0) ctx->fd_in = open("/dev/msm_pcm_in",O_RDONLY);
		break;
	    case CODEC_AMR:
		    ctx->fd_in = open("/dev/msm_amr_in",O_RDONLY);
		    if(ctx->fd_in < 0) ctx->fd_in = open("/dev/msm_amrnb_in",O_RDONLY);
		break;
	    default:
		return 0;
	}
	if(ctx->fd_in < 0)  {
	    log_err("cannot open device"); 
	    return 0;
	}
	if(ctx->codec != CODEC_AMR) {
	    memset(&cfg,0,sizeof cfg);
	    cfg.buffer_size = BUFFER_SIZE;
	    cfg.buffer_count  = 2;
	    cfg.channel_count = 1;
	    cfg.sample_rate = 8000;
	    cfg.type = 0;
	    if(ioctl(ctx->fd_in, AUDIO_SET_CONFIG, &cfg, sizeof cfg) != 0) {
		log_err("cannot configure audio driver: msm_pcm_in");
		close_device(ctx);	
		return 0;
	    }
	}

	vrm.rec_mode = AUDIO_FLAG_INCALL_MIXED;

	if(ioctl(ctx->fd_in, AUDIO_SET_INCALL, &vrm, sizeof vrm) < 0) {
	    log_err("cannot set mixed incall mode");
#if 0
	    /* disallow devices which don't support incall recording */
	    close_device(ctx); 
	    return 0;
#endif
	} else log_info("mixed incall mode set");

	ctx->fd_out = open(ctx->cur_file,O_CREAT|O_TRUNC|O_WRONLY);

	if(ctx->fd_out < 0) {
	    log_err("cannot open output file \'%s\'", ctx->cur_file); 
	    close_device(ctx);	
	    return 0;
	}

   return 1;

}


/*  Normal termination of recording.
    Invokes encode()/closeup() threads. */

void stop_record_msm8k(JNIEnv* env, jobject obj, jint context) {

    pthread_t k;
    rec_ctx *ctx = (rec_ctx *) context;

	pthread_mutex_lock(&mutty);

	log_info("stop_record");

#ifdef CR_CODE
	if(!ctx || is_trial_period_ended()) {
	    trial_period_ended();
#else
	if(!ctx) {
#endif
	    pthread_mutex_unlock(&mutty);
            return;
	}
	pthread_create(&k,0,encode,ctx);

	pthread_mutex_unlock(&mutty);
}


/*  Forced termination of recording.
    Closes/deletes the recorded raw file without calling encode(), 
    and invokes closeup(). */

void kill_record_msm8k(JNIEnv* env, jobject obj, jint context) {
   
    pthread_t k;
    rec_ctx *ctx = (rec_ctx *) context;	

	pthread_mutex_lock(&mutty);

	log_info("kill_record");
	close(ctx->fd_out);
	ctx->fd_out = -1;	
	unlink(ctx->cur_file);
	pthread_create(&k,0,closeup,ctx);

	pthread_mutex_unlock(&mutty);
}


/*  Main recording thread. */

static void *record(void *context) {

    int err_count = 0;
    int i, voice_state;
    int started = -1;
    rec_ctx *ctx = (rec_ctx *) context;

	log_info("entering recording thread with codec %d", ctx->codec);
	
	i = ioctl(ctx->fd_in,AUDIO_GET_VOICE_STATE, &voice_state, sizeof voice_state);
	if(i < 0) {
		log_info("AUDIO_GET_VOICE_STATE not supported, new_logic");
		while(started < 0) {
			usleep(250*1000);
			started = ioctl(ctx->fd_in, AUDIO_START, 0);
			log_info("new_logic: started 0 %d", started);
		}
	}
	else while(voice_state != VOICE_STATE_INCALL) {
		usleep(250*1000);
		if(ioctl(ctx->fd_in,AUDIO_GET_VOICE_STATE, &voice_state, sizeof voice_state) < 0) {
			break;
		}
		if(!ctx->alive) {
			return 0;
		}
	}

	if(started = -1 && ioctl(ctx->fd_in, AUDIO_START, 0) < 0) {

	    log_info("notifying java about failure");
	    
	    /*  Context STILL NEEDS to be freed through
		stop_record() -> encode() -> closeup() */	

	    recording_error((int) ctx,0);

	    close_device(ctx);
	    close(ctx->fd_out);
            unlink(ctx->cur_file);
	    ctx->alive = 0;

	    log_err("cannot start recording");

	    return 0;
	} else {
	    log_info("notifying java about success");
	    recording_started((int) ctx);
	}

#define MAX_ERR_COUNT 3
	err_count = 0;

	while(ctx->alive) {
	    i = read(ctx->fd_in, ctx->buff, ctx->bsz);
	    if(i < 0) {
                if(ctx->fd_in == -1) break;
		err_count++;
		log_info("read error %d in recording thread", errno);
		if(err_count == MAX_ERR_COUNT) {
		   log_err("max read err count in recording thread reached");
		   break;
		}
		/* log_err("read error in %s thread: %d returned",isup? "uplink" : "downlink",i);
		   break; */
	    } else if(i <= READ_SIZE) {
		i = write(ctx->fd_out, ctx->buff,i);
		if(i < 0) {
		    if(ctx->fd_out == -1) break;	
		    log_info("write error in recording thread");
		    break;
		}
	    }
	}

	ctx->alive = 0;

	log_info("exiting recording thread");

	/* fd_in and fd_out still open: fd_out to be closed in encode() and fd_in in closeup() */

    return 0;
}


/* Invoked by stop_record() [indirectly through encode()], and by kill_record().
   On exit from this function, context is no more valid */

static void *closeup(void *context) {

   rec_ctx *ctx = (rec_ctx *) context;	

	pthread_mutex_lock(&mutty);

	log_info("entered closeup thread");
        recording_complete((int) ctx);
	ctx->alive = 0;
	close_device(ctx);
	if(ctx->fd_out >= 0) close(ctx->fd_out);
	pthread_join(ctx->rec,0);
	free_ctx(ctx);

	pthread_mutex_unlock(&mutty);

	log_info("closeup complete");

  return 0;
}


static void *encode(void *context) {

    char file_in[256], file_out[256];
    int  fd = -1, fd_out = -1;
    uint32_t i;
    struct timeval start, stop, tm;
    off_t off1, off_out;
    pthread_t clsup;
    rec_ctx *ctx = (rec_ctx *) context;
    int java_ctx = (int) context;
    int codec;
     	
#ifdef CR_CODE
	if(is_trial_period_ended()) {
	     trial_period_ended();
             return 0;
	}
#endif
	if(!ctx) {
	     log_err("null context in encode()");	
	     return 0;
	}

        log_info("entering encoding thread with codec %d", ctx->codec);

        strcpy(file_in,ctx->cur_file);
	codec = ctx->codec;

	/* Context becomes invalid after this call */
        pthread_create(&clsup,0,closeup,ctx);

        gettimeofday(&start,0);
	encoding_started(java_ctx);

        fd = open(file_in, O_RDONLY);
        if(fd < 0) {
	     log_info("no input file");
             encoding_error(java_ctx,0);
             return 0;
        }

        switch(codec) {
	     case CODEC_WAV:
		    sprintf(file_out,"%s.wav",file_in); break;
	     case CODEC_AMR:
		    sprintf(file_out,"%s.amr",file_in); break;
#ifdef USING_LAME
	     case CODEC_MP3:
		    sprintf(file_out,"%s.mp3",file_in); break;
#endif
	     default:
		log_err("encode: unsupported codec %d", codec);
		close(fd); unlink(file_in);
		encoding_error(java_ctx,1);
		return 0;
	}

        fd_out = open(file_out,O_CREAT|O_TRUNC|O_WRONLY);
        if(fd_out < 0) {
             log_err("encode: cannot open output file %s",file_out);
             close(fd); unlink(file_in);
             encoding_error(java_ctx,2);
             return 0;
        }

        i = (uint32_t) lseek(fd,0,SEEK_END);
        log_dbg("%s: size=%d", file_in, i);
        lseek(fd,0,SEEK_SET);

        if(i == 0) {
	     log_info("zero size input file");
	     encoding_error(java_ctx,3);
             goto err_enc;
        }

	if(codec == CODEC_WAV) {
	    if(!wav_encode(fd, fd_out)) {
		encoding_error(java_ctx,5);
		goto err_enc;
	    }
#ifdef USING_LAME
	} else if(codec == CODEC_MP3) {
	    if(!mp3_encode(fd, fd_out)) {
                encoding_error(java_ctx,4);
		goto err_enc;
	    }	
#endif
	} else if(codec == CODEC_AMR) {
	    if(!amr_encode(fd, fd_out)) {
		encoding_error(java_ctx,6);
		goto err_enc;
	    }
	}

	off1 = lseek(fd, 0, SEEK_END);
	off_out = lseek(fd_out, 0, SEEK_END);
	close(fd); close(fd_out);

	gettimeofday(&stop,0);
	timersub(&stop,&start,&tm);
	log_info("encoding complete: %ld -> %ld in %ld sec", off1, off_out, tm.tv_sec);

	unlink(file_in);
	encoding_complete(java_ctx);

    return 0;

    err_enc: 
	close(fd_out);
	unlink(file_out);
	close(fd);
	unlink(file_in);
    return 0;	
}



