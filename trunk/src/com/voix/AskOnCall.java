package com.voix;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LightingColorFilter;
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
import android.widget.ImageView;
import android.widget.TextView;

public class AskOnCall extends Activity {

	// Interaction with the service: we call it
	public IRVoix srv = null;
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
        	 Log.dbg("recordingComplete()");
        	 hdl.post(new Runnable() {
        		 public void run() {
        			 if(StartStop == null) return;
        			 StartStop.setText(R.string.SStart);
        			 StartStop.setEnabled(true);
        			 StartStop.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFF00FF00));
        		 }
        	 });
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
        	 // Log.dbg("aarecordingStarted()");
         }
	};
	Handler hdl = new Handler();
	// call type
	public boolean outgoing = true;
	// need to start/stop recording during the lifetime
	public boolean incall = false;
	// phone number or contact's name
	private String phone = null;
	// our main layout to be inflated from xml
	private View layout = null;
	private WindowManager wm = null;
	private KeyguardManager km = null;
	private KeyguardLock kl = null;
	private AlertDialog dialog = null;
	// in case we'll ever need to restart ourselves
	private Intent inintent;
	private Button StartStop = null;
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bitmap bm = null;
		
		inintent = getIntent();
		outgoing = inintent.getBooleanExtra("outgoing",true);
		incall = inintent.getBooleanExtra("incall",false);
		phone = inintent.getStringExtra("phone");

		Log.dbg("onCreate()");
	    
		Intent intie = new Intent();
        intie.setClassName("com.voix", "com.voix.RVoixSrv");
        if(!bindService(intie, connection,0)) {
        	Log.err("cannot bind service");
        	finish();
        	return;
        }
	
        if(incall || !outgoing) {
			km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
			if(km == null) {
				Log.err("cannot obtain keyguard manager");
			} else {
				kl = km.newKeyguardLock("voix");
				kl.disableKeyguard();
			}	
		}
			
       
        Contix ctx = Contix.getContix();
        ctx.setContentResolver(getContentResolver());
		
		if((new File("/sdcard/voix/images/"+phone+".png")).exists()) {
			Log.dbg("found contact image");
			bm = BitmapFactory.decodeFile("/sdcard/voix/images/"+phone+".png");
		} else bm = ctx.getBitmap(phone, this); 
		
		layout = getLayoutInflater().inflate(incall ? R.layout.rec : (outgoing ? R.layout.outcall : R.layout.incall),	
				(ViewGroup) findViewById(R.id.LayoutRoot));

		TextView txt = (TextView) layout.findViewById(R.id.TextView);
		if(incall) txt.setText(phone);
		else txt.setText(getString(R.string.AskRecCall) + " "+ phone +"?");
		ImageView image = (ImageView) layout.findViewById(R.id.ImageView);
		if(bm == null) image.setImageResource(R.drawable.android_normal);
		else image.setImageBitmap(bm);
		
		/*	
	  	AlertDialog.Builder builder  = new AlertDialog.Builder(this);
	  	builder.setView(layout);
	  	dialog = builder.create();
	  	dialog.setCancelable(false);
	  	
	  	Window ww = dialog.getWindow();
	  	ww.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
		      	WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	 	ww.setType(WindowManager.LayoutParams.TYPE_PHONE);
	  	
	  	dialog.show();
      	*/
	  	
		try {
			wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			LayoutParams lp = new LayoutParams(dm.widthPixels,dm.heightPixels);
			lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
			if(incall) lp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
			wm.addView(layout, lp);
		} catch (Exception e) {
			Log.err("exception while trying to setup window");
			e.printStackTrace();
		}
		
	  	if(!incall) {

	  		Button Ok = (Button) layout.findViewById(R.id.ButtonYes);
			Button No = (Button) layout.findViewById(R.id.ButtonNo);

			Ok.setOnClickListener(new OnClickListener() {
	  			public void onClick(View arg0) {
	  				try {
	  					Log.dbg("forcing record");
	  					srv.force_record(true);
	  					if(!outgoing) srv.call_telephony("answerRingingCall");
	  				} catch (Exception e) {
	  					e.printStackTrace();
	  				}
	  				finish();
	  			}
	  		});
	  		No.setOnClickListener(new OnClickListener() {
	  			public void onClick(View arg0) {
	  				try {
	  					Log.dbg("forcing no record");
	  					srv.force_record(false);
	  					if(!outgoing) srv.call_telephony("answerRingingCall");
	  				} catch (Exception e) {
	  					e.printStackTrace();
	  				}
	  				finish();
	  			}
	  		});
	  	} else {
	  		StartStop = (Button) layout.findViewById(R.id.ButtonYes);
  		  	StartStop.getBackground().setColorFilter(new LightingColorFilter(0xFF404040, 0xFF00FF00));
	  		StartStop.setOnClickListener(new OnClickListener(){
  				public void onClick(View arg0) {
  					try {
  						if(!srv.is_recording()) {
  							Log.dbg("trying to start recording");
  							if(srv.start_rec())	{
  								StartStop.setText(R.string.SStop);
  								StartStop.getBackground().setColorFilter(new LightingColorFilter(0xFF404040, 0xFFFF0000));
  							}
  						} else {
  							Log.dbg("trying to stop recording");
  							srv.stop_rec();
  							StartStop.setEnabled(false);
  							StartStop.getBackground().setColorFilter(new LightingColorFilter(0xFF808080, 0xFFFF0000));
  						}
  					} catch (Exception e) {
  						e.printStackTrace();
  					}
  				}
  			});
	  	}
  		if(incall || !outgoing) {
  		  	Button Hup = (Button) layout.findViewById(R.id.ButtonHup);
  		  	Hup.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
  		  	Hup.setOnClickListener(new OnClickListener(){
  				public void onClick(View arg0) {
  					try {
  						srv.call_telephony("endCall");
  					} catch (Exception e) {
  						Log.err("exception in onClick (Hup)");
  						e.printStackTrace();
  					}
  					finish();
  				}
  			});
  		}
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


/*		
String ss = intie.getDataString();  
	if(ss != null && ss.startsWith("tel:")) { // for PRIVILEDGED
	outgoing = true;
	phone = ss.substring(4);
} else {
	outgoing = intie.getBooleanExtra("outgoing",true);
	phone = intie.getStringExtra("phone");
} */
