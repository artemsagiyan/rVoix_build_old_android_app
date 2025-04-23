package com.voix;

import java.util.List;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TattooHack extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	private class RThread extends Thread {
		@Override
		public void run() {
			while(serviceIsAlive()) {
				try {
					Thread.sleep(500);
					Log.msg("main service still active, waiting");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Intent intent = new Intent().setClassName("com.voix", "com.voix.RVoixSrv");
			if(startService(intent)== null) Log.err("service not started");
	        else Log.msg("Restarted RVoixSrv service, calling stopSelf()");
	        stopSelf();
		}
	}
	@Override
	public void onStart(Intent intent, int startId) {
		Log.dbg("onStart()");
		RThread rt = new RThread();
		rt.start();
		return;
	}
	private boolean serviceIsAlive() {
		ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);  
		List<ActivityManager.RunningServiceInfo> ls = am.getRunningServices(1000);
		for(int i = 0; i < ls.size(); i++) {
			if(ls.get(i).process.compareTo("com.voix:remote")==0) return true;
		}
		return false;
	}
	@Override
    public void onDestroy() {
		// Log.dbg("onDestroy()");
	}		
}
