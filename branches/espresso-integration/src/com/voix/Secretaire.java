package com.voix;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.view.animation.AnimationUtils;

public class Secretaire extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        Log.dbg("onCreate()");

        if(!BootRec.checkSetDevicePermissions()) {
         	(new AlertDialog.Builder(this))
 			.setCancelable(false)
 			.setMessage(getString(R.string.Nodev))
 			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dlg, int id) {
                    finish();
                    Uri u = Uri.fromParts("package", getPackageName(), null);
                    Intent i = new Intent("android.intent.action.DELETE",u);
                    startActivity(i);
               }})
            .create().show();
        }  
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
        File f = new File("/sdcard/voix");
        if(!f.exists()) {
        	Log.dbg("trying to create /sdcard/voix");
        	f.mkdir();
        	if(f.exists()) Log.dbg("created /sdcard/voix");
        	else {
        		Log.err("could not create /sdcard/voix");
        		finish();
        	}
        }
        setContentView(R.layout.main);
      
        ((Button) findViewById(R.id.ButtonRR)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				arg0.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.blink));
				Intent intent = new Intent().setClassName("com.voix", "com.voix.RVoixSrv");
				if(startService(intent)== null) Log.err("service not started");
		        else Log.msg("started service");
		        Log.dbg("exiting");
		        finish();
			}
        });

        ((Button) findViewById(R.id.ButtonQuit)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				arg0.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.blink));
				Intent intent = new Intent().setClassName("com.voix", "com.voix.RVoixSrv");
		        if(stopService(intent)==false) Log.msg("service not stopped");
		        else Log.msg("service stopped");
		        Log.dbg("exiting");
		        finish();
			}
        });
                
        ((Button) findViewById(R.id.ButtonSetup)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				arg0.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.blink));
				Intent intie = new Intent();
		        intie.setClassName("com.voix", "com.voix.Prefs");
		        Log.dbg("starting setup activity");
		        startActivityForResult(intie, 1);
			}
        });
        
        ((Button) findViewById(R.id.ButtonRec)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				arg0.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.blink));
				Intent intie = new Intent();
		        intie.setClassName("com.voix", "com.voix.Browser");
		        Log.dbg("starting browser activity");
		        startActivity(intie);
			}
        });
        
        final Context ctx = Secretaire.this;
        ((Button) findViewById(R.id.ButtonAbout)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				arg0.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.blink)); 
				(new AlertDialog.Builder(ctx))
                  .setCancelable(false)
                  .setMessage(R.string.SBuild)
                  .setTitle(R.string.SAbout)
				  .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                	  public void onClick(DialogInterface dlg, int id) {
                		  dlg.cancel();
                	  }})
                  .create().show();
			}
        });

        ((Button) findViewById(R.id.ButtonVLog)).setOnClickListener(new OnClickListener() {
        	public void onClick(View arg0) {
				arg0.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),  R.anim.blink));
				File ff = new File(RVoixSrv.ServiceLogger.logfile);
				if(ff.exists()) {
					Intent intie = new Intent();
					intie.setAction(Intent.ACTION_VIEW);
					intie.setDataAndType(Uri.fromFile(ff), "text/plain");
					startActivity(intie);
				} else Toast.makeText(ctx, R.string.SNoLogFile, Toast.LENGTH_SHORT).show();
			}
        });
        // test_if_running();
    } 
    public boolean test_if_running() {
        ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> ls = am.getRunningAppProcesses();
        for(int i = 0; i < ls.size(); i++) {
        	ActivityManager.RunningAppProcessInfo rip = ls.get(i); 
        	if(rip.processName.equals("com.voix:remote")) {
        		Log.msg("Service running: pid=" + rip.pid + ", importance=" + rip.importance);
        		return true;
        	}
        }
        return false;
    }
    
 }
