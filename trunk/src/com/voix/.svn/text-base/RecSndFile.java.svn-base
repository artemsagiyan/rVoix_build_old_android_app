package com.voix;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

public class RecSndFile extends Activity 
	implements OnClickListener,	AudioRecord.OnRecordPositionUpdateListener	{
 
	private AudioRecord ar;
	private byte[] buffer;
	private boolean recording = false;
	private static final int BUFSZ = 8000*2*60; // 1 minute of data
	private static final int READSZ = 8000*2; 	// 1 second
	private int bytes_read = 0;
	Button rec, save;
	EditText edt;
	ProgressBar progress;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    setContentView(R.layout.record);

	    rec = ((Button) findViewById(R.id.ButtonRR));
	    rec.setOnClickListener(this);
	    
	    save = ((Button) findViewById(R.id.ButtonSave));
	    save.setOnClickListener(this);
	    save.setEnabled(false);
	    
	    edt = ((EditText) findViewById(R.id.EditText));
        
	    progress = (ProgressBar) findViewById(R.id.ProgressBar);
	    progress.setMax(60);
	    
	    File f = new File("/sdcard/voix/sounds");
        if(!f.exists()) {
        	f.mkdir();
        	if(!f.exists()) {
        		errMsg(R.string.SCantCreateSndDir);
        		finish();
        		return;
        	}
        }
	    buffer = new byte[BUFSZ];
	}
	@Override
	public void onDestroy() {
        super.onDestroy();
        Log.dbg("onDestroy() called");
        try {
        	if(ar != null) ar.release();
        } catch (Exception e) { /* */ }
	}
	@Override
	public void onMarkerReached(AudioRecord recorder) {
		// TODO Auto-generated method stub		// won't do.
	}
	@Override
	public void onPause() {
		super.onPause();
		if(recording) stopRecord();
	}
	@Override
	public void onResume() {
		super.onResume();
		if(bytes_read != 0) save.setEnabled(true);
		else save.setEnabled(false);
		rec.setText(R.string.StartRecord);
	}
	@Override
	public void onClick(View v) {
		try {
			if(v.equals(rec)) {	// Record button
				if(!recording) {
					if(bytes_read == 0) startRecord();
					else {
						new AlertDialog.Builder(this).setCancelable(false).setMessage(R.string.SFileNotSaved)
							.setPositiveButton(R.string.SYes, new DialogInterface.OnClickListener() { 
								public void onClick(DialogInterface dialog, int id)	{
									bytes_read = 0;
									startRecord();
								}
							})
							.setNegativeButton(R.string.SNo, new DialogInterface.OnClickListener() { 
									public void onClick(DialogInterface dialog, int id)	{/* */}	})
							.show();
					}
				} else 	stopRecord();
			} else  {			// Save button
				if(bytes_read != 0) {
					String s = edt.getText().toString().trim();
					if(s.length() >= 0) {
						if(!save_wav("/sdcard/voix/sounds/" + s + ".wav")) errMsg(R.string.SCantSave);
						bytes_read = 0;
						save.setEnabled(false);
					} else errMsg(R.string.SNoFileName);	
				} 
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.err("just an exception");
		}
	}
	@Override
	public void onPeriodicNotification(AudioRecord recorder) {
		if(!recording) return;
		try {
			if(recorder.read(buffer, bytes_read, READSZ) != READSZ) {
				boolean now_rec = true;
				synchronized(this) {
					now_rec = recording; 					
				}
				if(now_rec) {
					errMsg(R.string.RecErrorWrite);
					stopRecord();
				}
				return;
			}
			progress.incrementProgressBy(1);
			bytes_read += READSZ;
		} catch(Exception e) {
			e.printStackTrace();
			Log.err("Exception, exiting.");
			try {
				stopRecord();
			} catch(Exception e1) {	
				Log.err("Cannot stop recording: exiting."); 
			}
		}
	}
	private synchronized void startRecord() {
		try {
			rec.setEnabled(false);
			ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
	                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFSZ);
		    if(ar.getState() != AudioRecord.STATE_INITIALIZED) {
		    	errMsg(R.string.RecErrorInit);
		    	Log.err("cannot initialize audio track"); 
		    	rec.setEnabled(true);
		    	return;
		    }
		    ar.setRecordPositionUpdateListener(this);
		    if(ar.setPositionNotificationPeriod(READSZ/2) != AudioRecord.SUCCESS) {
		    	errMsg(R.string.RecErrorSetup);
		    	Log.err("cannot initialize audio track"); 
		    	rec.setEnabled(true);
		    	return;
		    }
		    bytes_read = 0;
		    ar.startRecording();
		    rec.setText(R.string.StopRecord);
		    save.setEnabled(false);
			progress.setProgress(0);
			ar.read(buffer, 0, READSZ);
			recording = true;
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {	
					Log.dbg("timer expired, stop recording.");
					stopRecord(); 
				}	
			},	59*1000);
		} catch (Exception e) {
			e.printStackTrace();
			errMsg(R.string.RecError);
		}
		rec.setEnabled(true);
	}
	private synchronized void stopRecord() {
		try {
			if(!recording) return;
			ar.stop();
			ar.release();
			ar = null;
			recording = false;
			ui_updater.sendEmptyMessage(0);
		} catch (Exception e) {
			e.printStackTrace();
			errMsg(R.string.RecError);
		}
	}
	private Handler ui_updater = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		    switch(msg.what) {
		    	case 0:
					rec.setText(R.string.StartRecord);
				    save.setEnabled(true);
				    progress.setProgress(0);
				    break;
				default:    
					break;
		    }	
		}
	};
	private void errMsg(int res) {
		new AlertDialog.Builder(this).setMessage(res).setCancelable(false).setPositiveButton("OK",
				new DialogInterface.OnClickListener() { 
						public void onClick(DialogInterface dialog, int id)	{/* */}
				}
			).show();
	}
	private boolean save_wav(String dst) {
		byte []header = { 0x52, 0x49, 0x46, 0x46, /* riff_sz */ 0, 0, 0, 0, 0x57, 0x41, 0x56, 0x45, 0x66, 0x6d, 0x74, 0x20, 16, 0, 0, 0,
				1, 0, 1, 0, 0x40, 0x1f, 0, 0, -128, 0x3e, 0, 0, 2, 0, 16, 0, 0x64, 0x61, 0x74, 0x61, /* data_sz */ 0, 0, 0, 0}; 
		try {
			int data_sz = bytes_read;
			if(data_sz == 0) return false;
			int riff_sz = data_sz + 36;
			header[4] =  (byte) (riff_sz & 0xff);
			header[5] =  (byte) ((riff_sz >> 8)  & 0xff);
			header[6] =  (byte) ((riff_sz >> 16) & 0xff);
			header[7] =  (byte) ((riff_sz >> 24) & 0xff);
			header[40] = (byte) (data_sz & 0xff);
			header[41] = (byte) ((data_sz >> 8)  & 0xff);
			header[42] = (byte) ((data_sz >> 16) & 0xff);
			header[43] = (byte) ((data_sz >> 24) & 0xff);
			FileOutputStream fout = new FileOutputStream(dst); 
			fout.write(header);
			fout.write(buffer,0,bytes_read);
			fout.close();
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
