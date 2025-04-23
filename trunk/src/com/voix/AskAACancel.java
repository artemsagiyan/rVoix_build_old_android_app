package com.voix;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

public class AskAACancel extends Activity {

	private String phone; 
	public IRVoix srv = null;
	private View layout = null;
	private WindowManager wm = null;
	private KeyguardManager km = null;
	private KeyguardLock kl = null;
	private AlertDialog dialog = null;
	private Handler hdl = new Handler();
	private TextView txt;
    ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName cn,IBinder obj) {
            try {    
        		srv = IRVoix.Stub.asInterface(obj);
                if(srv == null) {
                	Log.err("cannot obtain service interface");
                	finish();
                }
                srv.registerCallback(cBack);
            } catch (Exception e){
            	Log.err("exception in onServiceConnected()");
            	e.printStackTrace();
            }
        }
        public void onServiceDisconnected(ComponentName cn) {
        		srv = null;
        }
	};
	// Interaction with the service: it calls us
	private IRVoixCallback cBack = new IRVoixCallback.Stub() {
         public void goodbye() {
        	 Log.dbg("goodbye()");
        	 finish();
         }
         public void recordingComplete() {
        	 // Log.dbg("recordingComplete()");
         }
         public void encodingComplete() {
        	 // Log.dbg("encodingComplete()");
         }
         public void recordingAboutToStart(boolean out, boolean inc, String p) {
        	 // Log.dbg("recordingAboutToStart()" + out + ", " + inc + ", " + p);
         }
         public void recordingStarted() {
        	 // Log.dbg("recordingStarted()");
         }
         public void aarecordingStarted() {
        	 Log.dbg("recording started event");
        	 hdl.post(new Runnable() {
        		 public void run() {
        			 txt.setText(getString(R.string.NowRecording) + " "+ phone);
        		 }
        	 });
         }
	};
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		Intent intie = getIntent();
		phone = intie.getStringExtra("phone");
		Log.dbg("onCreate()");
	    
		intie = new Intent();
        intie.setClassName("com.voix", "com.voix.RVoixSrv");
        if(!bindService(intie, connection,0)) {
        	Log.err("cannot bind service");
        	finish();
        	return;
        }
	
		km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		if(km == null) {
			Log.err("cannot obtain keyguard manager");
		} else {
			kl = km.newKeyguardLock("voix");
			kl.disableKeyguard();
		}
    
		layout = getLayoutInflater().inflate(R.layout.aa_cancel, (ViewGroup) findViewById(R.id.LayoutRoot));

		txt = (TextView) layout.findViewById(R.id.TextView);
		txt.setText(getString(R.string.NowAnswering) + " "+ phone);

		try {
			wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			LayoutParams lp = new LayoutParams(dm.widthPixels,dm.heightPixels);
			lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
			wm.addView(layout, lp);
		} catch (Exception e) {
			Log.err("exception while trying to setup window");
			e.printStackTrace();
		}
		Button Ok = (Button) layout.findViewById(R.id.ButtonYes);
		Ok.setOnClickListener(new OnClickListener() {
  			public void onClick(View arg0) {
  				try {
  					Log.dbg("cancelling autoanswer");
  					srv.cancel_autoanswer();
  				} catch (Exception e) {
  					e.printStackTrace();
  				}
  				finish();
  			}
  		});
    }
	@Override
	public void onDestroy() {
        super.onDestroy();
        Log.dbg("onDestroy()");
        if(dialog != null)dialog.dismiss();
        if(wm != null) wm.removeView(layout);
     	if(kl != null) kl.reenableKeyguard();
        if(srv != null) {
            try {
            	srv.unregisterCallback(cBack);
            } catch (Exception e) { 
            	Log.err("exceptinon while trying to unregister");
            	e.printStackTrace();
            }
        }
        if(connection != null) unbindService(connection);
    }

}
