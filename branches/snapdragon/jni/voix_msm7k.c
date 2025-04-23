#include "voix.h"


/* Threads */ 	
static void *record(void *);
static void *encode(void *);
static void *closeup(void *);

typedef struct {
    int isup;		/* uplink or downlink */
    int codec;
    int fd_in;		/* device */
    int	fd_out;		/* file being recorded */
    char *cur_file;	/* its full path */
    pthread_t rec;	/* recording thread */
    char *buff;		/* read buffer */
    int alive;		/* context is recording*/ 	
    int boost;		/* sound boost for this channel */
} channel_ctx;

/* Different from that for msm8k */

typedef struct {
    channel_ctx *uplink;
    channel_ctx *downlink;
} rec_ctx; 

static int init_recording(rec_ctx *ctx);

/*  Must be called with mutex locked.
    Closing active device may take some time, 
    so it should generally be done from a separate thread. */

static void close_device(channel_ctx *ctx) {
    if(!ctx) return;
    if(ctx->fd_in >= 0) {
	ioctl(ctx->fd_in,VOCPCM_UNREGISTER_CLIENT,0);
	close(ctx->fd_in);  
	ctx->fd_in = -1;
    }
}

/*  Free context. If start_record() ever returns non-zero, 
    it *must* be freed via stop_record() or kill_record(). */

static void free_ctx(rec_ctx *ctx) {
    if(!ctx) return;	
    if(ctx->uplink) {	
	if(ctx->uplink->cur_file) free(ctx->uplink->cur_file);
	if(ctx->uplink->buff) free(ctx->uplink->buff);
	free(ctx->uplink);
    }	
    if(ctx->downlink) {
        if(ctx->downlink->cur_file) free(ctx->downlink->cur_file);
        if(ctx->downlink->buff) free(ctx->downlink->buff);
        free(ctx->downlink);
    }
    free(ctx);
}


int start_record_msm7k(JNIEnv* env, jobject obj, jstring jfolder, jstring jfile, jint codec, jint boost_up, jint boost_down) {

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
            case CODEC_WAV: 
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
	ctx->uplink = (channel_ctx *) malloc(sizeof(channel_ctx));
	ctx->downlink = (channel_ctx *) malloc(sizeof(channel_ctx));

	if(!ctx->uplink || ! ctx->downlink) {
	    log_err("no memory!");
	    goto fail;		
	}

	ctx->uplink->buff = (char *) malloc(READ_SIZE);
	ctx->uplink->cur_file = (char *) malloc(strlen(folder)+strlen(file)+16);
	ctx->downlink->buff = (char *) malloc(READ_SIZE);
	ctx->downlink->cur_file = (char *) malloc(strlen(folder)+strlen(file)+16);

        if(!ctx->uplink->buff || ! ctx->downlink->buff ||
		!ctx->uplink->cur_file || !ctx->downlink->cur_file) {
            log_err("no memory!");
            goto fail;
        }

	ctx->uplink->isup = 1;
	ctx->uplink->codec = codec;
	ctx->uplink->fd_in = -1;
	ctx->uplink->fd_out = -1; 
	sprintf(ctx->uplink->cur_file, "%s/%s-up", folder, file);
	ctx->uplink->alive = 0;
	ctx->uplink->boost = boost_up;

	ctx->downlink->isup = 0;
	ctx->downlink->codec = codec;
	ctx->downlink->fd_in = -1;
	ctx->downlink->fd_out = -1;
	sprintf(ctx->downlink->cur_file, "%s/%s-dn", folder, file);
	ctx->downlink->alive = 0;
	ctx->downlink->boost = boost_down;


	/* Open/configure the devices, and open output files*/	

	if(!init_recording(ctx)) goto fail;
	
	ctx->uplink->alive = 1;
	ctx->downlink->alive = 1;

	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	pthread_create(&ctx->uplink->rec,&attr,record,ctx->uplink);
	pthread_create(&ctx->downlink->rec,&attr,record,ctx->downlink);
	log_info("started recording threads");

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

    ctx->uplink->fd_in = open("/dev/voc_tx_record", O_RDWR); 	
    if(ctx->uplink->fd_in < 0) ctx->uplink->fd_in = open("/dev/vocpcm2", O_RDWR);
    if(ctx->uplink->fd_in < 0) { 
	log_err("cannot open uplink device");
	return 0;
    }	

    ctx->downlink->fd_in = open("/dev/voc_rx_record", O_RDWR);
    if(ctx->downlink->fd_in < 0) ctx->downlink->fd_in = open("/dev/vocpcm0", O_RDWR);
    if(ctx->downlink->fd_in < 0) { 
	log_err("cannot open downlink device");
	close(ctx->uplink->fd_in);
	return 0;
    } 	

    if(ioctl(ctx->uplink->fd_in,VOCPCM_REGISTER_CLIENT,0) < 0 || 
		ioctl(ctx->downlink->fd_in,VOCPCM_REGISTER_CLIENT,0) < 0) { 	
	log_err("cannot register rpc clients");
	close(ctx->uplink->fd_in);
	close(ctx->downlink->fd_in);
	return 0;	
    }	

    ctx->uplink->fd_out = open(ctx->uplink->cur_file,O_CREAT|O_TRUNC|O_WRONLY);
    ctx->downlink->fd_out = open(ctx->downlink->cur_file,O_CREAT|O_TRUNC|O_WRONLY);

    if(ctx->uplink->fd_out < 0 || ctx->uplink->fd_out < 0) {
	log_err("cannot open output files");
	if(ctx->uplink->fd_out >= 0) {
		close(ctx->uplink->fd_out);
		unlink(ctx->uplink->cur_file);	
	}
        if(ctx->downlink->fd_out >= 0) {
                close(ctx->downlink->fd_out);
                unlink(ctx->downlink->cur_file);
        }
        close(ctx->uplink->fd_in);
        close(ctx->downlink->fd_in);
        return 0;
    }	

   return 1;
}


/*  Normal termination of recording.
    Invokes encode()/closeup() threads. */

void stop_record_msm7k(JNIEnv* env, jobject obj, jint context) {

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
    Closes/deletes the recorded raw files without calling encode(), 
    and invokes closeup(). */

void kill_record_msm7k(JNIEnv* env, jobject obj, jint context) {
   
    pthread_t k;
    rec_ctx *ctx = (rec_ctx *) context;	

	pthread_mutex_lock(&mutty);

	log_info("kill_record");
	close(ctx->uplink->fd_out);
	close(ctx->downlink->fd_out);
	ctx->uplink->fd_out = -1;	
	ctx->downlink->fd_out = -1;	
	unlink(ctx->uplink->cur_file);
	unlink(ctx->downlink->cur_file);
	pthread_create(&k,0,closeup,ctx);

	pthread_mutex_unlock(&mutty);
}


/*  Main recording thread. */

static void *record(void *context) {

    int i, err_count = 0;
    channel_ctx *ctx = (channel_ctx *) context;
    int isup = ctx->isup;

	log_info("entering %s thread", isup ? "uplink": "downlink");
	

#define MAX_ERR_COUNT 3
	err_count = 0;

	while(ctx->alive) {
	    i = read(ctx->fd_in, ctx->buff, READ_SIZE);
	    if(i < 0) {
                if(ctx->fd_in == -1) break;
		err_count++;
 		log_info("read error %d in %s thread", i, isup? "uplink" : "downlink");
		if(err_count == MAX_ERR_COUNT) {
 		   log_err("max read err count in %s thread reached", isup? "uplink" : "downlink");
		   break;
		}
	    } else if(i <= READ_SIZE) {
		i = write(ctx->fd_out, ctx->buff,i);
		if(i < 0) {
		    if(ctx->fd_out == -1) break;	
		    log_info("write error in %s thread", isup? "uplink" : "downlink");
		    break;
		}
	    }
	}
	ctx->alive = 0;
	log_info("exiting %s thread", isup? "uplink" : "downlink");

    return 0;
}


/* Invoked by stop_record() [indirectly through encode()], and by kill_record().
   On exit from this function, context is no more valid */

static void *closeup(void *context) {

   rec_ctx *ctx = (rec_ctx *) context;	

	pthread_mutex_lock(&mutty);

	log_info("entered closeup thread");
        recording_complete((int) ctx);

	ctx->uplink->alive = 0;
	ctx->downlink->alive = 0;

	close_device(ctx->uplink);
	close_device(ctx->downlink);

	if(ctx->uplink->fd_out >= 0) close(ctx->uplink->fd_out);
	if(ctx->downlink->fd_out >= 0) close(ctx->downlink->fd_out);

	pthread_join(ctx->uplink->rec,0);
	pthread_join(ctx->downlink->rec,0);

	free_ctx(ctx);

	pthread_mutex_unlock(&mutty);

	log_info("closeup complete");

  return 0;
}


static void *encode(void *context) {

    char file_up[256], file_dn[256], file_out[256];
    int  fd1 = -1, fd2 = -1, fd_out = -1;
    uint32_t i, k;
    struct timeval start, stop, tm;
    off_t off1, off_out;
    pthread_t clsup;
    rec_ctx *ctx = (rec_ctx *) context;
    int java_ctx = (int) context;
    int codec, boost_up, boost_dn;
    
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

        log_info("entering encoding thread with codec %d", ctx->uplink->codec);

        strcpy(file_up, ctx->uplink->cur_file);
        strcpy(file_dn, ctx->downlink->cur_file);
        codec = ctx->uplink->codec;

	boost_up = ctx->uplink->boost;
	boost_dn = ctx->downlink->boost;

        /* Context becomes invalid after this call */
        pthread_create(&clsup,0,closeup,ctx);

        gettimeofday(&start,0);
        encoding_started(java_ctx);

        fd1 = open(file_up, O_RDONLY);
        fd2 = open(file_dn, O_RDONLY);

	if(fd1 < 0 || fd2 < 0) {
	     if(fd1 >= 0) {
	        log_err("no downlink input file");	
		close(fd1); unlink(file_up);
	     }	
	     if(fd2 >= 0) {
	        log_err("no uplink input file");	
		close(fd2); unlink(file_dn);
	     }	
             encoding_error(java_ctx,0);
             return 0;
	}
	strcpy(file_out, file_up);
	file_out[strlen(file_out)-3] = 0; 
	
        switch(codec) {
             case CODEC_WAV:
                    strcat(file_out,".wav"); break;
#ifdef USING_LAME
             case CODEC_MP3:
                    strcat(file_out,".mp3"); break;
#endif
             default:
                log_err("encode: unsupported codec %d", codec);
                close(fd1); unlink(file_up);
                close(fd2); unlink(file_dn);
                encoding_error(java_ctx,1);
                return 0;
        }

        fd_out = open(file_out,O_CREAT|O_TRUNC|O_WRONLY);
        if(fd_out < 0) {
             log_err("encode: cannot open output file %s",file_out);
             close(fd1); unlink(file_up);
             close(fd2); unlink(file_dn);
             encoding_error(java_ctx,2);
             return 0;
        }

        i = (uint32_t) lseek(fd1,0,SEEK_END);
        k = (uint32_t) lseek(fd2,0,SEEK_END);


        if(i == 0 || k == 0) {
 	     if(i == 0) log_err("zero size uplink input file");
 	     if(i == 0) log_err("zero size downlink input file");
             encoding_error(java_ctx,3);
             goto err_enc;
        }

	if(i != k) log_info("uplink/downlink size mismatch: %d/%d", i, k);

#ifdef USING_LAME
        if(codec == CODEC_MP3) {
            if(!mp3_encode_stereo(fd1, fd2, fd_out, boost_up, boost_dn)) {
                encoding_error(java_ctx,4);
                goto err_enc;
            }
        } else 
#endif
	if(codec == CODEC_WAV) {
            if(!wav_encode_stereo(fd1, fd2, fd_out, boost_up, boost_dn)) {
                encoding_error(java_ctx,5);
                goto err_enc;
            }
        }

        off1 = lseek(fd1, 0, SEEK_END);
	off1 += lseek(fd2, 0, SEEK_END);
        off_out = lseek(fd_out, 0, SEEK_END);
        close(fd1); 
	close(fd2);
	close(fd_out);

        gettimeofday(&stop,0);
        timersub(&stop,&start,&tm);
        log_info("encoding complete: %ld -> %ld in %ld sec", off1, off_out, tm.tv_sec);

        unlink(file_up);
        unlink(file_dn);
        encoding_complete(java_ctx);

    return 0;

    err_enc:
        close(fd1);
        close(fd2);
        close(fd_out);
        unlink(file_out);
        unlink(file_up);
        unlink(file_dn);

    return 0;
}




