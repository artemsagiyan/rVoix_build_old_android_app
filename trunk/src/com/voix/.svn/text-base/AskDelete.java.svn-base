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
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

public class AskDelete extends Activity {
    
	public IRVoix srv = null;
	private AlertDialog dialog = null;
	private View layout = null;
	private KeyguardManager km = null;
	private KeyguardLock kl = null;
	private WindowManager wm = null;
	
	ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName cn,IBinder obj) {
            try {    
        		srv = IRVoix.Stub.asInterface(obj);
                if(srv == null) {
                	Log.err("cannot obtain service interface");
                	finish();
                }
            } catch (Exception e){
            	Log.err("exception in onServiceConnected()");
            	e.printStackTrace();
            }
        }
        public void onServiceDisconnected(ComponentName cn) {
        		srv = null;
        }
	};
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.msg("onCreate(): entry");
		
		Intent intie = new Intent();
        intie.setClassName("com.voix", "com.voix.RVoixSrv");
        if(!bindService(intie, connection,0)) {
        	Log.err("cannot bind service");
        	finish();
        	return;
        }
        try {
        	km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE); 
        	kl = km.newKeyguardLock("voix");
        	kl.disableKeyguard();
        } catch(Exception e) {
        	Log.err("exception while disabling keyguard manager");
        	e.printStackTrace();
        }

    	layout = getLayoutInflater().inflate(R.layout.askdel, (ViewGroup) findViewById(R.id.LayoutRoot));
    	
        try {
        	wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			LayoutParams lp = new LayoutParams(dm.widthPixels,dm.heightPixels);
			lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
			lp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
			wm.addView(layout, lp);
		} catch (Exception e) {
			Log.err("exception while trying to setup window");
			e.printStackTrace();
		}		

	  	Button Ok = (Button) layout.findViewById(R.id.ButtonYes);
		Button No = (Button) layout.findViewById(R.id.ButtonNo);

		Ok.setOnClickListener(new OnClickListener() {
  			public void onClick(View arg0) {
  				try {
  					Log.dbg("confirming delete");
  					srv.wait_confirm(2);
  				} catch (Exception e) {
  					Log.err("exception on click yes");
  					e.printStackTrace();
  				}
  				finish();
  			}
  		});
  		No.setOnClickListener(new OnClickListener() {
  			public void onClick(View arg0) {
  				try {
  					Log.dbg("not confirming delete");
  					srv.wait_confirm(1);
  				} catch (Exception e) {
  					Log.err("exception on click no");
  					e.printStackTrace();
  				}
  				finish();
  			}
  		});
//  		dialog.show();
	}
	@Override
	public void onDestroy() {
        super.onDestroy();
     	Log.dbg("onDestroy(): entry");
        if(dialog != null)dialog.dismiss();
        Log.msg("onDestroy() called");
       	if(kl != null) kl.reenableKeyguard();
        if(wm != null) wm.removeView(layout);
        if(connection != null) unbindService(connection);
        Log.dbg("onDestroy(): exit");
    }
	
}
