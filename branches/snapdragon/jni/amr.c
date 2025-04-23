#include <stdio.h>
#include <string.h>
#include <inttypes.h>
#include <fcntl.h>

/* AMR frame type definitions */
typedef enum {
  AMRSUP_SPEECH_GOOD,          /* Good speech frame              */
  AMRSUP_SPEECH_DEGRADED,      /* Speech degraded                */
  AMRSUP_ONSET,                /* onset                          */
  AMRSUP_SPEECH_BAD,           /* Corrupt speech frame (bad CRC) */
  AMRSUP_SID_FIRST,            /* First silence descriptor       */
  AMRSUP_SID_UPDATE,           /* Comfort noise frame            */
  AMRSUP_SID_BAD,              /* Corrupt SID frame (bad CRC)    */
  AMRSUP_NO_DATA,              /* Nothing to transmit            */
  AMRSUP_SPEECH_LOST,          /* Lost speech in downlink        */
  AMRSUP_FRAME_TYPE_MAX
} amrsup_frame_type;

/* AMR frame mode (frame rate) definitions */
typedef enum {
  AMRSUP_MODE_0475,    /* 4.75 kbit /s */
  AMRSUP_MODE_0515,    /* 5.15 kbit /s */
  AMRSUP_MODE_0590,    /* 5.90 kbit /s */
  AMRSUP_MODE_0670,    /* 6.70 kbit /s */
  AMRSUP_MODE_0740,    /* 7.40 kbit /s */
  AMRSUP_MODE_0795,    /* 7.95 kbit /s */
  AMRSUP_MODE_1020,    /* 10.2 kbit /s */
  AMRSUP_MODE_1220,    /* 12.2 kbit /s */
  AMRSUP_MODE_MAX
} amrsup_mode_type;

/* The AMR classes
*/
typedef enum  {
  AMRSUP_CLASS_A,
  AMRSUP_CLASS_B,
  AMRSUP_CLASS_C
} amrsup_class_type;

/* The maximum number of bits in each class */
#define AMRSUP_CLASS_A_MAX 81
#define AMRSUP_CLASS_B_MAX 405
#define AMRSUP_CLASS_C_MAX 60

/* The size of the buffer required to hold the vocoder frame */
#define AMRSUP_VOC_FRAME_BYTES  \
  ((AMRSUP_CLASS_A_MAX + AMRSUP_CLASS_B_MAX + AMRSUP_CLASS_C_MAX + 7) / 8)

/* Size of each AMR class to hold one frame of AMR data */
#define AMRSUP_CLASS_A_BYTES ((AMRSUP_CLASS_A_MAX + 7) / 8)
#define AMRSUP_CLASS_B_BYTES ((AMRSUP_CLASS_B_MAX + 7) / 8)
#define AMRSUP_CLASS_C_BYTES ((AMRSUP_CLASS_C_MAX + 7) / 8)


/* Number of bytes for an AMR IF2 frame */
#define AMRSUP_IF2_FRAME_BYTES 32

/* Frame types for 4-bit frame type as in 3GPP TS 26.101 v3.2.0, Sec.4.1.1 */
typedef enum {
  AMRSUP_FRAME_TYPE_INDEX_0475    = 0,    /* 4.75 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_0515    = 1,    /* 5.15 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_0590    = 2,    /* 5.90 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_0670    = 3,    /* 6.70 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_0740    = 4,    /* 7.40 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_0795    = 5,    /* 7.95 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_1020    = 6,    /* 10.2 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_1220    = 7,    /* 12.2 kbit /s    */
  AMRSUP_FRAME_TYPE_INDEX_AMR_SID = 8,    /* SID frame       */
/* Frame types 9-11 are not supported */
  AMRSUP_FRAME_TYPE_INDEX_NO_DATA = 15,   /* No data         */
  AMRSUP_FRAME_TYPE_INDEX_MAX,
  AMRSUP_FRAME_TYPE_INDEX_UNDEF = AMRSUP_FRAME_TYPE_INDEX_MAX
} amrsup_frame_type_index_type;

#define AMRSUP_FRAME_TYPE_INDEX_MASK         0x0F /* All frame types */
#define AMRSUP_FRAME_TYPE_INDEX_SPEECH_MASK  0x07 /* Speech frame    */

typedef enum {
  AMRSUP_CODEC_AMR_NB,
  AMRSUP_CODEC_AMR_WB,
  AMRSUP_CODEC_MAX
} amrsup_codec_type;

/* IF1-encoded frame info */
typedef struct {
  amrsup_frame_type_index_type frame_type_index;
  unsigned char fqi;    /* frame quality indicator: TRUE: good frame, FALSE: bad */
  amrsup_codec_type amr_type;   /* AMR-NB or AMR-WB */
} amrsup_if1_frame_info_type;

#define AUDFADEC_AMR_FRAME_TYPE_MASK     0x78
#define AUDFADEC_AMR_FRAME_TYPE_SHIFT    3
#define AUDFADEC_AMR_FRAME_QUALITY_MASK  0x04

#define AMR_CLASS_A_BITS_BAD   0

#define AMR_CLASS_A_BITS_SID  39

#define AMR_CLASS_A_BITS_122  81
#define AMR_CLASS_B_BITS_122 103
#define AMR_CLASS_C_BITS_122  60

typedef struct {
  int   len_a;
  unsigned short *class_a;
  int   len_b;
  unsigned short *class_b;
  int   len_c;
  unsigned short *class_c;
} amrsup_frame_order_type;

const unsigned short amrsup_bit_order_122_a[AMR_CLASS_A_BITS_122] = {
     0,   1,   2,   3,   4,   5,   6,   7,   8,   9,
    10,  11,  12,  13,  14,  23,  15,  16,  17,  18,
    19,  20,  21,  22,  24,  25,  26,  27,  28,  38,
   141,  39, 142,  40, 143,  41, 144,  42, 145,  43,
   146,  44, 147,  45, 148,  46, 149,  47,  97, 150,
   200,  48,  98, 151, 201,  49,  99, 152, 202,  86,
   136, 189, 239,  87, 137, 190, 240,  88, 138, 191,
   241,  91, 194,  92, 195,  93, 196,  94, 197,  95,
   198
};

const unsigned short amrsup_bit_order_122_b[AMR_CLASS_B_BITS_122] = {
   /**/  29,  30,  31,  32,  33,  34,  35,  50, 100,
   153, 203,  89, 139, 192, 242,  51, 101, 154, 204,
    55, 105, 158, 208,  90, 140, 193, 243,  59, 109,
   162, 212,  63, 113, 166, 216,  67, 117, 170, 220,
    36,  37,  54,  53,  52,  58,  57,  56,  62,  61,
    60,  66,  65,  64,  70,  69,  68, 104, 103, 102,
   108, 107, 106, 112, 111, 110, 116, 115, 114, 120,
   119, 118, 157, 156, 155, 161, 160, 159, 165, 164,
   163, 169, 168, 167, 173, 172, 171, 207, 206, 205,
   211, 210, 209, 215, 214, 213, 219, 218, 217, 223,
   222, 221,  73,  72
};


const unsigned short amrsup_bit_order_122_c[AMR_CLASS_C_BITS_122] = {
   /* ------------- */  71,  76,  75,  74,  79,  78,
    77,  82,  81,  80,  85,  84,  83, 123, 122, 121,
   126, 125, 124, 129, 128, 127, 132, 131, 130, 135,
   134, 133, 176, 175, 174, 179, 178, 177, 182, 181,
   180, 185, 184, 183, 188, 187, 186, 226, 225, 224,
   229, 228, 227, 232, 231, 230, 235, 234, 233, 238,
   237, 236,  96, 199
};


const amrsup_frame_order_type amrsup_122_framing = {
  AMR_CLASS_A_BITS_122,
  (unsigned short *) amrsup_bit_order_122_a,
  AMR_CLASS_B_BITS_122,
  (unsigned short *) amrsup_bit_order_122_b,
  AMR_CLASS_C_BITS_122,
  (unsigned short *) amrsup_bit_order_122_c
};


static int amrsup_frame_len_bits(amrsup_frame_type frame_type,
  amrsup_mode_type amr_mode)
{
  int frame_len=0;

  switch (frame_type)
  {
    case AMRSUP_SPEECH_GOOD :
    case AMRSUP_SPEECH_DEGRADED :
    case AMRSUP_ONSET :
    case AMRSUP_SPEECH_BAD :
      if (amr_mode >= AMRSUP_MODE_MAX)
      {
        frame_len = 0;
      }
      else
      {
        frame_len = amrsup_122_framing.len_a
                    + amrsup_122_framing.len_b
                    + amrsup_122_framing.len_c;
      }
      break;

    case AMRSUP_SID_FIRST :
    case AMRSUP_SID_UPDATE :
    case AMRSUP_SID_BAD :
      frame_len = AMR_CLASS_A_BITS_SID;
      break;

    case AMRSUP_NO_DATA :
    case AMRSUP_SPEECH_LOST :
    default :
      frame_len = 0;
  }

  return frame_len;
}

static int amrsup_frame_len(amrsup_frame_type frame_type, amrsup_mode_type amr_mode)
{
  int frame_len = amrsup_frame_len_bits(frame_type, amr_mode);

  frame_len = (frame_len + 7) / 8;
  return frame_len;
}

static void amrsup_tx_order(unsigned char *dst_frame,  int *dst_bit_index,
  unsigned char *src,  int num_bits, const unsigned short *order)
{
  unsigned long dst_mask = 0x00000080 >> ((*dst_bit_index) & 0x7);
  unsigned char *dst = &dst_frame[((unsigned int) *dst_bit_index) >> 3];
  unsigned long src_bit, src_mask;

  /* Prepare to process all bits
  */
  *dst_bit_index += num_bits;
  num_bits++;

  while(--num_bits) {
    /* Get the location of the bit in the input buffer */
    src_bit  = (unsigned long ) *order++;
    src_mask = 0x00000080 >> (src_bit & 0x7);

    /* Set the value of the output bit equal to the input bit */
    if (src[src_bit >> 3] & src_mask) {
      *dst |= (unsigned char ) dst_mask;
    }

    /* Set the destination bit mask and increment pointer if necessary */
    dst_mask >>= 1;
    if (dst_mask == 0) {
      dst_mask = 0x00000080;
      dst++;
    }
  }
} /* amrsup_tx_order */

static int amrsup_if1_framing(unsigned char *vocoder_packet,
  amrsup_frame_type frame_type, amrsup_mode_type amr_mode,
  unsigned char *if1_frame, amrsup_if1_frame_info_type *if1_frame_info)
{
  amrsup_frame_order_type *ordering_table;
  int frame_len = 0;
  int i;

#if 0
  if(amr_mode >= AMRSUP_MODE_MAX)
  {
    return 0;
  }
#endif

  /* Initialize IF1 frame data and info */
  if1_frame_info->fqi = 1;

  if1_frame_info->amr_type = AMRSUP_CODEC_AMR_NB;

  memset(if1_frame, 0,
           amrsup_frame_len(AMRSUP_SPEECH_GOOD, AMRSUP_MODE_1220));


  switch (frame_type)
  {
    case AMRSUP_SID_BAD:
      if1_frame_info->fqi = 0;
      /* fall thru */

    case AMRSUP_SID_FIRST:
    case AMRSUP_SID_UPDATE:
      /* Set frame type index */
      if1_frame_info->frame_type_index = AMRSUP_FRAME_TYPE_INDEX_AMR_SID;
      /* ===== Encoding SID frame ===== */
      /* copy the sid frame to class_a data */
      for (i=0; i<5; i++)
      {
        if1_frame[i] = vocoder_packet[i];
      }

      /* Set the SID type : SID_FIRST: Bit 35 = 0, SID_UPDATE : Bit 35 = 1 */
      if (frame_type == AMRSUP_SID_FIRST)
      {
        if1_frame[4] &= ~0x10;
      }

      if (frame_type == AMRSUP_SID_UPDATE)
      {
        if1_frame[4] |= 0x10;
      }
      else
      {
      /* Set the mode (Bit 36 - 38 = amr_mode with bits swapped)
      */
      if1_frame[4] |= (((unsigned char)amr_mode << 3) & 0x08)
        | (((unsigned char)amr_mode << 1) & 0x04) | (((unsigned char)amr_mode >> 1) & 0x02);

      frame_len = AMR_CLASS_A_BITS_SID;
      }

      break;

    case AMRSUP_SPEECH_BAD:
      if1_frame_info->fqi = 0;
      /* fall thru */

    case AMRSUP_SPEECH_GOOD:
      /* Set frame type index */

        if1_frame_info->frame_type_index
        = (amrsup_frame_type_index_type)(amr_mode);
      /* ===== Encoding Speech frame ===== */
      /* Clear num bits in frame */
      frame_len = 0;

      /* Select ordering table */
      ordering_table =
      (amrsup_frame_order_type*)&amrsup_122_framing;

      amrsup_tx_order(
        if1_frame,
        &frame_len,
        vocoder_packet,
        ordering_table->len_a,
        ordering_table->class_a
      );

      amrsup_tx_order(
        if1_frame,
        &frame_len,
        vocoder_packet,
        ordering_table->len_b,
        ordering_table->class_b
      );

      amrsup_tx_order(
        if1_frame,
        &frame_len,
        vocoder_packet,
        ordering_table->len_c,
        ordering_table->class_c
      );

      /* frame_len already updated with correct number of bits */
      break;

    default:
      /* fall thru */

    case AMRSUP_NO_DATA:
      /* Set frame type index */
      if1_frame_info->frame_type_index = AMRSUP_FRAME_TYPE_INDEX_NO_DATA;

      frame_len = 0;

      break;
  }  /* end switch */

  /* convert bit length to byte length */
  frame_len = (frame_len + 7) / 8;

  return frame_len;
}


void amr_transcode(unsigned char *src, unsigned char *dst)
{
   amrsup_frame_type frame_type_in = (amrsup_frame_type) *(src++);
   amrsup_mode_type frame_rate_in = (amrsup_mode_type) *(src++);
   amrsup_if1_frame_info_type frame_info_out;
   unsigned char frameheader;

   amrsup_if1_framing(src, frame_type_in, frame_rate_in, dst+1, &frame_info_out);
   frameheader = (frame_info_out.frame_type_index << 3) + (frame_info_out.fqi << 2);
   *dst = frameheader;

   return;
}


