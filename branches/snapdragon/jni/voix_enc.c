#include "voix.h"

/* The only function exported by amr.c */
extern void amr_transcode(unsigned char *src, unsigned char *dst);

struct {
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

uint8_t amr_hdr[6] = {0x23, 0x21, 0x41, 0x4d, 0x52, 0xa}; // #!AMR\n

#ifdef USING_LAME
static void lame_error_handler(const char *format, va_list ap) {
    __android_log_vprint(ANDROID_LOG_INFO,"libvoix",format,ap);
}
#endif


/********* Mono encodings **********/

int wav_encode(int fd_in, int fd_out) {

    uint32_t i, k;
    uint8_t *src = 0;

	src = (uint8_t *) malloc(RSZ);
        if(!src) {
        	log_err("encode: out of memory");
		return 0;
	}

        i = (uint32_t) lseek(fd_in,0,SEEK_END);
        lseek(fd_in,0,SEEK_SET);

	wavhdr.num_channels = 1;
	wavhdr.sample_rate = 8000;
	wavhdr.byte_rate = 16000;
	wavhdr.block_align = 2;
	wavhdr.data_sz = i;
	wavhdr.riff_sz = i + 36;
	write(fd_out,&wavhdr,sizeof(wavhdr));

	k = 0;

	while(1) {
	    uint32_t count = (k + RSZ > i) ? i - k : RSZ;
		if(read(fd_in,src,count) < 0) break;
		write(fd_out,src,count);
		k += count;
		if(k == i) break;
	}
	free(src);
    return 1;	
}

#ifdef USING_LAME
int mp3_encode(int fd_in, int fd_out) {

    lame_global_flags *gfp;
    uint32_t i, j, k, count;
    int16_t *b0 = 0, *b1 = 0;
    unsigned char *c;
    int ret;
	
	/* calculate data size*/
	k = (uint32_t) lseek(fd_in, 0, SEEK_CUR);
	i = (uint32_t) lseek(fd_in, 0, SEEK_END);
	lseek(fd_in, k, SEEK_SET);
	i -= k;
	k = 0;

	gfp = lame_init();
	lame_set_errorf(gfp,lame_error_handler);
	lame_set_debugf(gfp,lame_error_handler);
	lame_set_msgf(gfp,lame_error_handler);
	lame_set_num_channels(gfp,2);
	lame_set_in_samplerate(gfp,8000);
#if 0
	lame_set_brate(gfp,64); /* compress 1:4 */
	lame_set_mode(gfp,0);   /* mode = stereo */
	lame_set_quality(gfp,2);   /* 2=high  5 = medium  7=low */
#else
	lame_set_quality(gfp,5);   /* 2=high  5 = medium  7=low */
	lame_set_mode(gfp,3);   /* mode = mono */
	if(lame_get_VBR(gfp) == vbr_off) lame_set_VBR(gfp, vbr_default);
	lame_set_VBR_quality(gfp, 7.0);
	lame_set_findReplayGain(gfp, 0);
	lame_set_bWriteVbrTag(gfp, 1);
	lame_set_out_samplerate(gfp,11025);
	/*   lame_set_num_samples(gfp,i/2); */
#endif
	if(lame_init_params(gfp) < 0) {
	     log_err("encode: failed to init lame");
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
             return 0;
	}

	do {
	    ret = 0; j = 0; count = 0;
	    for(c = (unsigned char*) b1; count < CHSZ; c += ret) {
		j = (k + RSZ > i) ? i - k : RSZ;
		ret = read(fd_in,c,j);
		if(ret < 0) break;
		k += ret; count += ret;
		if(k == i) break;
	    }
	    if(ret < 0) break;
	    if(count) {
		ret = lame_encode_buffer(gfp,b1,b1,count/2,(unsigned char *)b0,OCHSZ);
		if(ret < 0) break;
		c = (unsigned char *) b0;
		count = ret;
		for(j = 0; j < count; j += RSZ, c += ret) {
		     ret = (j + RSZ > count) ? count - j : RSZ;
		     write(fd_out,c,ret);
		}
	    }
	} while(k < i);

	k = (uint32_t) lame_encode_flush(gfp,(unsigned char *)b0,OCHSZ);
	if(k) write(fd_out,b0,k);

	k = lame_get_lametag_frame(gfp,(unsigned char *)b0,OCHSZ);
	if(k > 0) write(fd_out,b0,k);

	lame_close(gfp);
	free(b0);
	free(b1);

     return 1;	
}
#endif

int amr_encode(int fd_in, int fd_out) {

     uint32_t i, j, k, n, count;
     uint8_t *src = 0, *dst = 0, *c_in, *c_out;

	src = (uint8_t *) malloc(AMR_RSZ*2);
	if(!src) {
		log_err("encode: out of memory");
		return 0;
	}
	dst = src + AMR_RSZ;

	write(fd_out,amr_hdr,sizeof(amr_hdr));

	i = (uint32_t) lseek(fd_in,0,SEEK_END);
	lseek(fd_in,0,SEEK_SET);
	k = 0;

	if(device_type == DEVICE_SNAPDRAGON1) {
	    while(1) {
		count = (k + 2*AMR_RSZ > i) ? i - k : 2*AMR_RSZ;
		if(read(fd_in,src,count) < 0) break;
		write(fd_out,src,count);
		k += count;
		if(k == i) break;
	    }
	} else {
	    while(1) {
		count = (k + AMR_RSZ > i) ? i - k : AMR_RSZ;
		c_in = src;
		c_out = dst;
		n = count/AMR_FRMSZ;
		if(read(fd_in,src,count) < 0) break;
		for(j = 0; j < n; c_in += AMR_FRMSZ, c_out += 32, j++) amr_transcode(c_in, c_out);
		if(write(fd_out,dst,32*n) <= 0) break;
		k += count;
		if(k + AMR_FRMSZ > i) break;
	    }
        }

	free(src);
     return 1;	
}


/********* Stereo encodings **********/

static inline int16_t boost_word(int16_t j, int boost) {
   int32_t i = j << boost;
    if(i > ((1<<15)-1)) i = ((1<<15)-1);
    else if(i < -(1<<15)) i = -(1<<15);
   return i;
}

#ifdef USING_LAME
static void boost_buff(int16_t *buff, size_t bufsz, int boost) {
   int i;
     for(i = 0; i < bufsz; i++) buff[i] = boost_word(buff[i],boost);
}
#endif

int wav_encode_stereo(int fd1, int fd2, int fd_out, int boost_up, int boost_dn) {

    uint32_t i, j, k, count;
    int16_t *b0 = 0, *b1 = 0, *b2 = 0;

	b0 = (int16_t *) malloc(RSZ*2);
	b1 = (int16_t *) malloc(RSZ);
	b2 = (int16_t *) malloc(RSZ);
	if(!b0 || !b1 || !b2) {
	     log_err("encode: out of memory");
	     if(b0) free(b0);
	     if(b1) free(b1);
	     if(b2) free(b2);
	     return 0;
	}

        i = (uint32_t) lseek(fd1,0,SEEK_END);
        lseek(fd1,0,SEEK_SET);
        k = (uint32_t) lseek(fd2,0,SEEK_END);
        lseek(fd2,0,SEEK_SET);

	if(k < i) i = k;

        wavhdr.num_channels = 2;
	wavhdr.sample_rate = 8000;
        wavhdr.byte_rate = 32000;
        wavhdr.block_align = 4;
	wavhdr.data_sz = i*2;
	wavhdr.riff_sz = i*2 + 36;
	write(fd_out,&wavhdr,sizeof(wavhdr));

	k = 0;
	if(boost_up && boost_dn)
	   while(1) {
		count = (k + RSZ > i) ? i - k : RSZ;
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
		count = (k + RSZ > i) ? i - k : RSZ;
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
		count = (k + RSZ > i) ? i - k : RSZ;
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
		count = (k + RSZ > i) ? i - k : RSZ;
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

	free(b0);
	free(b1);
	free(b2);

    return 1;
}

#ifdef USING_LAME
int mp3_encode_stereo(int fd1, int fd2, int fd_out, int boost_up, int boost_dn) {

    lame_global_flags *gfp;
    uint32_t i, j, k, count;
    int16_t *b0 = 0, *b1 = 0, *b2 = 0;
    unsigned char *c1, *c2;
    int ret;
 	

        /* calculate data size*/
        i = (uint32_t) lseek(fd1, 0, SEEK_END);
        k = (uint32_t) lseek(fd2, 0, SEEK_END);
        lseek(fd1, 0, SEEK_SET);
        lseek(fd2, 0, SEEK_SET);

	if(k < i) i = k;
        k = 0;

	gfp = lame_init();
	lame_set_errorf(gfp,lame_error_handler);
	lame_set_debugf(gfp,lame_error_handler);
	lame_set_msgf(gfp,lame_error_handler);
	lame_set_num_channels(gfp,2);
	lame_set_in_samplerate(gfp,8000);
	lame_set_quality(gfp,5);   /* 2=high  5 = medium  7=low */
	lame_set_mode(gfp,3);   /* mode = mono */
	if(lame_get_VBR(gfp) == vbr_off) lame_set_VBR(gfp, vbr_default);
	lame_set_VBR_quality(gfp, 7.0);
	lame_set_findReplayGain(gfp, 0);
	lame_set_bWriteVbrTag(gfp, 1);
	lame_set_out_samplerate(gfp,11025);

	if(lame_init_params(gfp) < 0) {
	     log_err("encode: failed to init lame");
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
             return 0;
	}

        do {
	   ret = 0; j = 0, count = 0;
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


	k = (uint32_t) lame_encode_flush(gfp,(unsigned char *)b0,OCHSZ);
	if(k) write(fd_out,b0,k);

	k = lame_get_lametag_frame(gfp,(unsigned char *)b0,OCHSZ);
	if(k > 0) write(fd_out,b0,k);

	lame_close(gfp);
	free(b0);
	free(b1);
	free(b2);

     return 1;	
}


int convert_to_mp3(JNIEnv* env, jobject obj, jstring ifile, jstring ofile) {

    const char *fi = 0, *fo = 0;
    int fd, fd_out;
			
	if(!ifile || !ofile) {
	     log_err("convert_to_mp3: invalid arguments");
	     return 0;
	}

	fi = (*env)->GetStringUTFChars(env,ifile,NULL);
	fo = (*env)->GetStringUTFChars(env,ofile,NULL);

	if(!fi || !fo || !*fi || !*fo) {
	    (*env)->ReleaseStringUTFChars(env,ifile,fi);
	    (*env)->ReleaseStringUTFChars(env,ofile,fo);
	     log_err("convert_to_mp3: invalid arguments");
	    return 0; 	
	}

        fd = open(fi, O_RDONLY);
        fd_out = open(fo,O_CREAT|O_TRUNC|O_WRONLY);
	
	if(fd < 0 || fd_out < 0) {
	     if(fd >= 0) {
		log_err("cannot open output file");
		close(fd);
	     }		
	     if(fd_out >= 0) {
		log_err("cannot open input file");
		close(fd_out);	
		unlink(fo);
	     }	
	     (*env)->ReleaseStringUTFChars(env,ifile,fi);
	     (*env)->ReleaseStringUTFChars(env,ofile,fo);
	     return 0;	

	}
	(*env)->ReleaseStringUTFChars(env,ifile,fi);
	(*env)->ReleaseStringUTFChars(env,ofile,fo);

        lseek(fd,sizeof(wavhdr),SEEK_SET);

     return mp3_encode(fd,fd_out);
}
#endif


