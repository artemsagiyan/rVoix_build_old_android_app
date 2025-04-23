
#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/cdefs.h>
#include <sys/ioctl.h>
#include <sys/time.h>
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
#define RSZ		(16*1024)

#define log_info(...)	__android_log_print(ANDROID_LOG_INFO, "com.voix",__VA_ARGS__)
#define log_err(...) 	__android_log_print(ANDROID_LOG_ERROR,"com.voix",__VA_ARGS__)

#ifdef DEBUG
#define log_dbg(...)	__android_log_print(ANDROID_LOG_DEBUG,"com.voix",__VA_ARGS__)
#else
#define log_dbg(...)
#endif


static int alive = 0;
static int volatile aa_record_started = 0;
static int boost_up = 0;
static int boost_dn = 0;
static char cur_file[256];
static pthread_t rec1, rec2;

static int volatile fdx[2] = {-1, -1}; 	/* device handles */
static int volatile fdo[2] = {-1, -1}; 	/* output file handles */

static JavaVM *gvm; 
static jobject giface;

void *record(void *);
void *encode(void *);
void *encode_incoming(void *);
int last_codec;
static pthread_mutex_t mutty;
void stop_record(JNIEnv* env, jobject obj, jint codec);

int start_record(JNIEnv* env, jobject obj, jstring jfile, jint jbu, jint jbd) {

    pthread_attr_t attr;
    const char *file = 0;
    struct stat st;    
  
	log_info("start_record");

	if(alive) {
		stop_record(env, obj, last_codec);	
	}
	if(!jfile) return 1;

	if(stat(OUT_DIR,&st) < 0) {
	    if(mkdir(OUT_DIR,0777) != 0) {
		log_err("cannot create output directory " OUT_DIR ".");
		return 1;
	    } 	
	} else if(!S_ISDIR(st.st_mode)) {
	    log_err(OUT_DIR " is not a directory.");	     	
	    return 1;	
	}

	file = (*env)->GetStringUTFChars(env,jfile,NULL);
	if(!file || !*file) {
	    (*env)->ReleaseStringUTFChars(env,jfile,file); 
	    log_err("bad string from jni");
	    return 1;
	}
	strcpy(cur_file,file);
        (*env)->ReleaseStringUTFChars(env,jfile,file);
	pthread_mutex_lock(&mutty);
	alive = 1;
	pthread_mutex_unlock(&mutty);
	boost_up = jbu;
	boost_dn = jbd;

	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	/* records incoming sound data */
	pthread_create(&rec1,&attr,record,(void *) 0);

	/* records outgoing sound data */
	pthread_create(&rec2,&attr,record,(void *) 1);

	pthread_attr_destroy(&attr);

	log_info("started record threads");

    return 0;
}


void stop_record(JNIEnv* env, jobject obj, jint codec) {

    pthread_t k;

	log_info("stop_record");
	last_codec = codec;
	pthread_mutex_lock(&mutty);
	alive = 0;	
	if(codec >= 2) {
	   if(aa_record_started) pthread_create(&k,0,encode_incoming,(void *)codec);
	} else pthread_create(&k,0,encode,(void *)codec);	
	pthread_mutex_unlock(&mutty);
	log_info("codec=%d aa_rec=%d fd=%d",codec,aa_record_started,fdx[0]);
	if(codec >= 2 && (aa_record_started || fdx[0] >= 0)) {
	    log_info("waiting for safe stop");
	    do {
		usleep(100000);		
	    } while(aa_record_started || fdx[0] >= 0);		
	    log_info("safely stopped");	
	}
}


void *record(void *init) {

    char file[256], *buff;
    int fd, fd_out, isup = (int) init;
    int err_count;
	log_info("entering %s thread", isup? "uplink" : "downlink");

	if(!isup)  {
	    pthread_mutex_lock(&mutty);
	    if(!alive) {
	    	pthread_mutex_unlock(&mutty);
		log_info("was already stopped, exiting");
		return 0;
	    }	 	
	}
	fd = open(isup ? "/dev/voc_tx_record" : "/dev/voc_rx_record", O_RDWR);
	if(fd < 0)  {
            fd = open(isup ? "/dev/vocpcm2" : "/dev/vocpcm0", O_RDWR);
	    if(fd < 0) {
	    	if(!isup) pthread_mutex_unlock(&mutty);
         	log_err("cannot open %s driver",isup? "uplink" : "downlink" ); return 0;
	    }
	}

	/* positive return values are ok for this ioctl */
	if(ioctl(fd,VOCPCM_REGISTER_CLIENT,0) < 0) {
	    close(fd);	
	    if(!isup) pthread_mutex_unlock(&mutty);
            log_err("cannot register rpc client"); return 0;
	}

	sprintf(file, OUT_DIR "/%s-%s", cur_file, isup ? "up" : "dn");	
	fd_out = open(file,O_CREAT|O_TRUNC|O_WRONLY);

	if(fd_out < 0) {
	    ioctl(fd,VOCPCM_UNREGISTER_CLIENT,0); 
	    close(fd);
	    if(!isup) pthread_mutex_unlock(&mutty);
	    log_err("cannot open output file \'%s\'",file); return 0;
	}
	if(!isup) {
	    aa_record_started = 1;	
	    pthread_mutex_unlock(&mutty);
	}

	fdx[isup] = fd;
	fdo[isup] = fd_out;

	buff = (char *) malloc(READ_SIZE);
	if(!buff) {
	    ioctl(fd,VOCPCM_UNREGISTER_CLIENT,0); 
	    close(fd);  
	    close(fd_out); 
	    unlink(file);		
	    log_err("out of memory"); return 0;
	}
	
#define MAX_ERR_COUNT 3 
	err_count = 0;

	while(alive) {
	    int i = read(fd,buff,READ_SIZE);
	    if(i < 0) {
		err_count++;
		log_info("read error %i in %s thread", i, isup ? "uplink":"downlink");
		if(err_count == MAX_ERR_COUNT) {
		   log_err("max read err count in %s thread reached", isup ? "uplink":"downlink");	
		   break;
		}
		/* log_err("read error in %s thread: %d returned",isup? "uplink" : "downlink",i);
		   break; */
	    } else if(i <= READ_SIZE) {
		i = write(fd_out,buff,i);
		if(i < 0) {
		    log_info("write error in %s thread", isup ? "uplink":"downlink");	
		    break;	
		}
	    }	
	}
	log_info("exiting %s thread", isup ? "uplink":"downlink");
	/* fd and fd_out still open: fd_out to be closed in encode() and fd in closeup() */

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
    __android_log_vprint(ANDROID_LOG_INFO,"com.voix",format,ap);
}
#endif

static void recording_complete();
static void encoding_complete();


static inline int16_t boost_word(int16_t j, int boost) {
   int32_t i = j << boost;
    if(i > ((1<<15)-1)) i = ((1<<15)-1);
    else if(i < -(1<<15)) i = -(1<<15);
   return i;
}

static void boost_buff(int16_t *buff, size_t bufsz, int boost) {
   int i;
     for(i = 0; i < bufsz; i++) buff[i] = boost_word(buff[i],boost);
}

pthread_t clsup;

void *closeup(void *used) {
   int both = (int) used;
	log_info("entering closeup thread");
	ioctl(fdx[0],VOCPCM_UNREGISTER_CLIENT,0);
	close(fdx[0]);     fdx[0] = -1;
	if(both) {
	    ioctl(fdx[1],VOCPCM_UNREGISTER_CLIENT,0);
	    close(fdx[1]); fdx[1] = -1;	
	}
	pthread_join(rec1,0);
#if 0
	if(both) pthread_join(rec2,0);
#else
	pthread_join(rec2,0); /* for dummy_write thread */
#endif
	log_info("recorder thread%s joined", both ? "s": "");
        recording_complete();
  return 0;	
}

void *encode(void *init) {
   
    char fn[256], file[256];
    int  fd1 = -1, fd2 = -1, fd_out = -1;
    uint32_t i, k;
    int16_t *b0 = 0, *b1 = 0, *b2 = 0;
#ifdef USING_LAME
    lame_global_flags *gfp;
    int  mp3 = ((int) init) & 1;	
#endif
    struct timeval start, stop, tm;
    off_t off1, off2, off_out;	
	
	log_info("entering encode thread");

	strcpy(fn,cur_file);

	close(fdo[0]);
	close(fdo[1]);
	pthread_create(&clsup,0,closeup,(void *) 1);

	gettimeofday(&start,0);

	if((int) init < 0) {
	     log_info("deleting at user request");	
	     sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
             sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();	
	     return 0;	
	}

	sprintf(file, OUT_DIR "/%s-up",fn);
	fd1 = open(file,O_RDONLY);
	sprintf(file, OUT_DIR "/%s-dn",fn);
	fd2 = open(file,O_RDONLY);
	if(fd1 < 0 || fd2 < 0) {
	     if(fd1 >= 0) { 
		close(fd1); sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
	     }
	     if(fd2 >= 0) {
		close(fd2); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     } 
	     log_err("encode: input file not found");
	     encoding_complete();
	     return 0;		
	}

#ifdef USING_LAME
     if(mp3) 
        sprintf(file, OUT_DIR "/%s.mp3",fn);
     else 
#endif
	sprintf(file, OUT_DIR "/%s.wav",fn);

	fd_out = open(file,O_CREAT|O_TRUNC|O_WRONLY);
	if(fd_out < 0) {
	     log_err("encode: cannot open output file %s",file);
             close(fd1); sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();	
	     return 0;		
	}

	i = (uint32_t) lseek(fd1,0,SEEK_END);
	k = (uint32_t) lseek(fd2,0,SEEK_END);

	log_dbg("%s-up: size=%d", fn, i);
	log_dbg("%s-dn: size=%d", fn, k);

	lseek(fd1,0,SEEK_SET);
	lseek(fd2,0,SEEK_SET);

	if(i != k) {
	     log_info("file sizes mismatch");	
	     if(i > k) i = k;	    		
	}
	i &= ~1;
	if(i == 0) {
	     log_info("zero size input file");
	     close(fd_out); unlink(file);
	     close(fd1); sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);		
	     close(fd2); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();	
	     return 0;			
	}

#ifdef USING_LAME
     if(mp3) {

	gfp = lame_init();
	lame_set_errorf(gfp,lame_error_handler);
	lame_set_debugf(gfp,lame_error_handler);
	lame_set_msgf(gfp,lame_error_handler);
	lame_set_num_channels(gfp,2);
	lame_set_in_samplerate(gfp,8000);
#if 0 
	lame_set_brate(gfp,64); /* compress 1:4 */
	lame_set_mode(gfp,0);	/* mode = stereo */
	lame_set_quality(gfp,2);   /* 2=high  5 = medium  7=low */ 
#else
	lame_set_quality(gfp,5);   /* 2=high  5 = medium  7=low */ 
	lame_set_mode(gfp,3);	/* mode = mono */
	if (lame_get_VBR(gfp) == vbr_off) lame_set_VBR(gfp, vbr_default);
        lame_set_VBR_quality(gfp, 7.0);
	lame_set_findReplayGain(gfp, 0);
	lame_set_bWriteVbrTag(gfp, 1);
	lame_set_out_samplerate(gfp,11025);
/*	lame_set_num_samples(gfp,i/2); */
#endif
	if(lame_init_params(gfp) < 0) {
	     log_err("encode: failed to init lame"); 	
             close(fd_out); unlink(file);
             close(fd1); sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();
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
             close(fd1); sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();	
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
		if(boost_up) boost_buff(b1,count/2,boost_up);
		if(boost_dn) boost_buff(b2,count/2,boost_dn);	
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
	i = lame_get_lametag_frame(gfp,(unsigned char *)b0,OCHSZ);
	if(i>0) write(fd_out,b0,i);
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
             close(fd1); sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
             close(fd2); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();
             return 0;
        }

        wavhdr.num_channels = 2;
        wavhdr.byte_rate = 32000;
        wavhdr.block_align = 4;
	wavhdr.data_sz = i*2;
	wavhdr.riff_sz = i*2 + 36; 

	write(fd_out,&wavhdr,sizeof(wavhdr));
	k = 0;		
	if(boost_up && boost_dn)
	   while(1) {
	    uint32_t j, count = (k + RSZ > i) ? i - k : RSZ;
		if(read(fd1,b1,count) < 0) break;
		if(read(fd2,b2,count) < 0) break;
		for(j = 0; j < count/2; j++) {
			b0[2*j] = boost_word(b1[j],boost_up);  
			b0[2*j+1] = boost_word(b2[j],boost_dn);
		}
		write(fd_out,b0,count*2);			
		k += count;
		if(k == i) break;
	   }
	else if(boost_up)
           while(1) {
            uint32_t j, count = (k + RSZ > i) ? i - k : RSZ;
                if(read(fd1,b1,count) < 0) break;
                if(read(fd2,b2,count) < 0) break;
                for(j = 0; j < count/2; j++) {
                        b0[2*j] = boost_word(b1[j],boost_up);
                        b0[2*j+1] = b2[j];
                }
                write(fd_out,b0,count*2);
                k += count;
                if(k == i) break;
           }
	else if(boost_dn)
           while(1) {
            uint32_t j, count = (k + RSZ > i) ? i - k : RSZ;
                if(read(fd1,b1,count) < 0) break;
                if(read(fd2,b2,count) < 0) break;
                for(j = 0; j < count/2; j++) {
                        b0[2*j] = b1[j];
                        b0[2*j+1] = boost_word(b2[j],boost_dn);
                }
                write(fd_out,b0,count*2);
                k += count;
                if(k == i) break;
           }
	else
           while(1) {
            uint32_t j, count = (k + RSZ > i) ? i - k : RSZ;
                if(read(fd1,b1,count) < 0) break;
                if(read(fd2,b2,count) < 0) break;
                for(j = 0; j < count/2; j++) {
                        b0[2*j] = b1[j];
                        b0[2*j+1] = b2[j];
                }
                write(fd_out,b0,count*2);
                k += count;
                if(k == i) break;
           }

#ifdef USING_LAME
    }
#endif
	off1 = lseek(fd1, 0, SEEK_END);
	off2 = lseek(fd2, 0, SEEK_END);
	off_out = lseek(fd_out, 0, SEEK_END);

	close(fd1); close(fd2); close(fd_out);
	free(b0); free(b1); free(b2);	
	
	gettimeofday(&stop,0);
        timersub(&stop,&start,&tm);
        log_info("encoding complete: %ld -> %ld in %ld sec", off1+off2, off_out, tm.tv_sec);

	sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
	sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	encoding_complete();
    return 0; 
}

/******   Answer the incoming call  ******/
static void *say_them(void *);
static void *dummy_write(void *p);

/***** Java callback to hangup. Not reached on Hero 
(the phone rather hangs itself or reboots) *****/
static void  call_java2hangup();
static void  aa_rec_started();

static int aa_file = -1;
static int aa_fd = -1;

void answer_call(JNIEnv* env, jobject obj, jstring jfile, jstring ofile, jint jbd) {

    pthread_t pt;
    const char *file = 0;
	
	log_info("in answer_call");
	aa_record_started = 0;
	aa_fd = -1;	

        file = (*env)->GetStringUTFChars(env,jfile,NULL);
        if(!file || !*file) {
            (*env)->ReleaseStringUTFChars(env,jfile,file);
            log_err("bad string from jni");
            return;
        }
	aa_file = open(file, O_RDONLY);
        if(aa_file < 0) {
	    (*env)->ReleaseStringUTFChars(env,jfile,file);
            log_err("cannot open auto answer sound file %s", file); 
	    return;
        }
	if(strlen(file)> 4 && strcmp(file+strlen(file)-4,".wav")==0)
	  lseek(aa_file,sizeof(wavhdr),SEEK_SET); /* don't bother checking wav header */
        (*env)->ReleaseStringUTFChars(env,jfile,file);


	file = (*env)->GetStringUTFChars(env,ofile,NULL);
	if(file && *file) strcpy(cur_file,file);		
	else *cur_file = 0;

	(*env)->ReleaseStringUTFChars(env,ofile,file);

	if(*cur_file) log_info("will record to %s after answering", cur_file);
	else log_info("will hang up after answering");

	boost_dn = jbd;	
    
	pthread_create(&pt,0,say_them,0);
}


static void *say_them(void *p) {

    char *buff;

	log_info("in say_them() thread");

	aa_fd = open("/dev/voc_tx_playback", O_RDWR);
        if(aa_fd < 0)  {
            aa_fd = open("/dev/vocpcm3",O_RDWR);
            if(aa_fd < 0) {
		close(aa_file); aa_file = -1;
                log_err("cannot open playback driver"); 
		return 0;
            }
        }
        if(ioctl(aa_fd,VOCPCM_REGISTER_CLIENT,0) < 0) {
            close(aa_fd); close(aa_file); aa_file = -1;
            log_err("cannot register rpc client for playback"); 
	    return 0;
        }

	buff = (char *) malloc(BUFFER_SIZE);
	memset(buff,0,BUFFER_SIZE);

	write(aa_fd,buff,BUFFER_SIZE);
	write(aa_fd,buff,BUFFER_SIZE);

        log_info("answering...");
	alive = 1;

	while(alive) {	
	    int i,m;
	    i  = read(aa_file,buff,BUFFER_SIZE);
	    if(i <= 0) break;
	    m = write(aa_fd,buff,i);
	    if(m < 0)  {
		log_info("playback: i'm probably closed from outside");
		pthread_mutex_lock(&mutty);	
		alive = 0;
		pthread_mutex_unlock(&mutty);	
		break;
	    }	
	}

	close(aa_file);
	aa_file = -1;
#if 0
        close(aa_fd);
	aa_fd = -1;
#endif
	free(buff);
	if(!alive || *cur_file == 0) {
	    ioctl(aa_fd,VOCPCM_UNREGISTER_CLIENT,0);
	    close(aa_fd);
	    aa_fd = -1;		
            log_info("now hanging up in java");
	    call_java2hangup();
	} else {
	 pthread_attr_t attr;
	    pthread_attr_init(&attr);
            pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
        /* records incoming sound data */
            pthread_create(&rec1,&attr,record,(void *) 0);
#if 1
            pthread_create(&rec2,&attr,dummy_write,(void *) 0);
#endif
	    aa_rec_started();	
	}
    return 0;	
}

static void *dummy_write(void *p) {

   char *buff;	
	buff = (char *) malloc(BUFFER_SIZE);
        memset(buff,0,BUFFER_SIZE);
	log_info("in dummy_write thread");
	if(aa_fd < 0) return 0;
	while(alive) {	
	   if(write(aa_fd,buff,BUFFER_SIZE) < 0) break;	
	}
	ioctl(aa_fd,VOCPCM_UNREGISTER_CLIENT,0);
	close(aa_fd);
	aa_fd = -1;
	free(buff);
	log_info("exiting dummy_write thread");
   return 0;
}

void *encode_incoming(void *init) {
   
    char fn[256], file[256];
    int  fd1 = -1, fd_out = -1;
    uint32_t i, k;
    int16_t *b0 = 0, *b1 = 0;
#ifdef USING_LAME
    lame_global_flags *gfp;
    int  mp3 = ((int) init) & 1;	
#endif
    struct timeval start, stop, tm;
    off_t off1, off_out;	
	
	log_info("entering encode_incoming thread");
	strcpy(fn,cur_file);

	close(fdo[0]);
	pthread_create(&clsup,0,closeup,(void *) 0);

	gettimeofday(&start,0);

	sprintf(file, OUT_DIR "/%s-dn",fn);
	fd1 = open(file,O_RDONLY);
	if(fd1 < 0) {
	     sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     log_err("encode: input file not found");
	     encoding_complete();
	     return 0;		
	}
#ifdef USING_LAME
     if(mp3) 
        sprintf(file, OUT_DIR "/%s.mp3",fn);
     else 
#endif
	sprintf(file, OUT_DIR "/%s.wav",fn);
	fd_out = open(file,O_CREAT|O_TRUNC|O_WRONLY);
	if(fd_out < 0) {
	     log_err("encode: cannot open output file %s",file);
             close(fd1); sprintf(file, OUT_DIR "/%s-up",fn); unlink(file);
	     encoding_complete(); 
	     return 0;		
	}
	i = (uint32_t) lseek(fd1,0,SEEK_END);
	log_dbg("%s-dn: size=%d", fn, i);
	lseek(fd1,0,SEEK_SET);
	if(i == 0) {
	     log_info("zero size input file");
	     close(fd_out); unlink(file);
	     close(fd1); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);		
	     encoding_complete();
	     return 0;			
	}
#ifdef USING_LAME
     if(mp3) {

	gfp = lame_init();
	lame_set_errorf(gfp,lame_error_handler);
	lame_set_debugf(gfp,lame_error_handler);
	lame_set_msgf(gfp,lame_error_handler);
	lame_set_num_channels(gfp,2);
	lame_set_in_samplerate(gfp,8000);
#if 0 
	lame_set_brate(gfp,64); /* compress 1:4 */
	lame_set_mode(gfp,0);	/* mode = stereo */
	lame_set_quality(gfp,2);   /* 2=high  5 = medium  7=low */ 
#else
	lame_set_quality(gfp,5);   /* 2=high  5 = medium  7=low */ 
	lame_set_mode(gfp,3);	/* mode = mono */
	if(lame_get_VBR(gfp) == vbr_off) lame_set_VBR(gfp, vbr_default);
        lame_set_VBR_quality(gfp, 7.0);
	lame_set_findReplayGain(gfp, 0);
	lame_set_bWriteVbrTag(gfp, 1);
	lame_set_out_samplerate(gfp,11025);
/*	lame_set_num_samples(gfp,i/2); */
#endif
	if(lame_init_params(gfp) < 0) {
	     log_err("encode: failed to init lame"); 	
             close(fd_out); unlink(file);
             close(fd1); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();
	     return 0;			
	}
#define CHSZ (512*1024)
#define OCHSZ (5*(CHSZ/8)+7200) 
	b0 = (int16_t *) malloc(OCHSZ);
        b1 = (int16_t *) malloc(CHSZ);
        if(!b0 || !b1) {
             log_err("encode: out of memory");
	     if(b0) free(b0);
             if(b1) free(b1);
             close(fd_out); unlink(file);
             close(fd1); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();
             return 0;
        }
	aa_record_started = 0;
	k = 0;
	do {
	   int ret = 0;	
	   uint32_t j = 0, count = 0;
	   unsigned char *c1;	
	     for(c1 = (unsigned char*) b1; count < CHSZ; c1 += ret) {
		j = (k + RSZ > i) ? i - k : RSZ;
		ret = read(fd1,c1,j);
		if(ret < 0) break;
		k += ret; count += ret;
		if(k == i) break;
	     }
	     if(ret < 0) break;	
	     if(count) {
		if(boost_dn) boost_buff(b1,count/2,boost_up);
		ret = lame_encode_buffer(gfp,b1,b1,count/2,(unsigned char *)b0,OCHSZ);
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
	i = lame_get_lametag_frame(gfp,(unsigned char *)b0,OCHSZ);
	if(i>0) write(fd_out,b0,i);
	lame_close(gfp);
     } else {		
#endif
        b1 = (int16_t *) malloc(RSZ);
        if(!b1) {
             log_err("encode: out of memory");
             close(fd_out); unlink(file);
             close(fd1); sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	     encoding_complete();
             return 0;
        }
	aa_record_started = 0;
	wavhdr.num_channels = 1;
	wavhdr.byte_rate = 16000;
	wavhdr.block_align = 2;
	wavhdr.data_sz = i;
	wavhdr.riff_sz = i + 36; 
	write(fd_out,&wavhdr,sizeof(wavhdr));
	k = 0;		
	while(1) {
            uint32_t count = (k + RSZ > i) ? i - k : RSZ;
                if(read(fd1,b1,count) < 0) break;
		if(boost_dn) boost_buff(b1,count/2,boost_dn);
                write(fd_out,b1,count);
                k += count;
                if(k == i) break;
        }
#ifdef USING_LAME
    }
#endif
	off1 = lseek(fd1, 0, SEEK_END);
	off_out = lseek(fd_out, 0, SEEK_END);
	close(fd1); close(fd_out);
	if(b0) free(b0); if(b1) free(b1);;	
	gettimeofday(&stop,0);
        timersub(&stop,&start,&tm);
        log_info("encode_incoming complete: %ld -> %ld in %ld sec", off1, off_out, tm.tv_sec);
	sprintf(file, OUT_DIR "/%s-dn",fn); unlink(file);
	encoding_complete();
    return 0; 
}

static void call_java_callback(const char *function) {

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
        mid = (*env)->GetStaticMethodID(env, cls, function, "()V");
        if(mid == NULL) {
            log_err("cannot find java callback to call");
            return;
        }
        (*env)->CallStaticVoidMethod(env,cls,mid);
        (*gvm)->DetachCurrentThread(gvm);
}

static void call_java2hangup() {
   call_java_callback("onCallAnswered");
   log_dbg("java onCallAnswered notified");
}

static void recording_complete() {
   call_java_callback("onRecordingComplete");	
   log_dbg("java onRecordingComplete notified");
}

static void encoding_complete() {
   call_java_callback("onEncodingComplete");
   log_dbg("java onEncodingComplete notified");
}

static void aa_rec_started() {
   call_java_callback("onAutoanswerRecordingStarted");
   log_dbg("java onAutoanswerRecordingStarted notified");
}


/********************************************
*********************************************
*********************************************
*********************************************/

static const char *classPathName = "com/voix/RVoixSrv";


static JNINativeMethod methods[] = {
  { "startRecord", "(Ljava/lang/String;II)I", (void *) start_record },
  { "stopRecord", "(I)V", (void *) stop_record },
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



