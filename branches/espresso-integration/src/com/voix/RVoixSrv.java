package com.voix;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;

public class RVoixSrv extends Service {
    
    static {
		System.loadLibrary("lame");
		System.loadLibrary("voix");
	}
	public static native int startRecord(String file, int bu, int bd);
	public static native void stopRecord(int encoding_mode);
	public static native void answerCall();

	// NB: The first two are also hard-coded in voix.c (passed in encoding_mode to startRecord) 
	public static final int MODE_RECORD_WAV = 0;
	public static final int MODE_RECORD_MP3 = 1;
	private int srv_mode = MODE_RECORD_WAV;
	private int boost_up = 0;
	private int boost_dn = 0;
	
	// NB: These are also hard-coded in prefs.xml through @array/CallValues
	public static final int OUTGOING_REC_ASK_INCALL = 5;
	public static final int INCOMING_ACT_ASK_INCALL = 5;
	
	private int unk_proc = INCOMING_ACT_ASK_INCALL;
	private int cont_proc = INCOMING_ACT_ASK_INCALL;
	private int ncont_proc = INCOMING_ACT_ASK_INCALL;
	private int out_proc = OUTGOING_REC_ASK_INCALL;
		
	private long start_time = 0;
	private int wait_confirm_result = 0; 
	
	// If this is a foreground service, we also provide a status bar icon.
	// Without it, the service seems to be killed for no reason after a lengthy period.
	private boolean foreground = true;
	
	private CallReceiver cr = new CallReceiver();
	private OutNumReceiver onr = new OutNumReceiver();
	
	private static Telephony telephony = null;
	private static PowerManager.WakeLock wakeLock = null;
	private static AudioManager aman = null;
	
	private static final int NOTIFY_ID = 1;
	private Contix ctx;
	
	private ArrayList <String> iilist = null;
	private ArrayList <String> oilist = null;
	private ArrayList <String> get_array(int mode, char type) {
		FContentList fc = new FContentList(FContentList.LIST_FILES[mode],this);
		fc.read();
		return fc.get_array(type);
	}
	
	
	@Override
   	public void onCreate() {
        	Log.msg("onCreate(): entry");
			super.onCreate();
			
			mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			try {
			    mStartForeground = getClass().getMethod("startForeground",	new Class[] { int.class, Notification.class});
			    mStopForeground = getClass().getMethod("stopForeground", new Class[] { boolean.class});
			} catch (NoSuchMethodException e) {
	        // Running on an older platform.
			    Log.dbg("old platform detected");
				mStartForeground = mStopForeground = null;
			}
			    
			if(telephony == null) telephony = new Telephony();
			this.registerReceiver(cr, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
			this.registerReceiver(onr, new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL));
            if(wakeLock == null) {
                PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
                wakeLock.setReferenceCounted(false);
            }
            ctx = Contix.getContix();
            ctx.setContentResolver(getContentResolver());
            if(aman == null)  aman = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            Log.dbg("onCreate(): exit");
	}
	
	@Override
    public void onDestroy() {
			Log.msg("onDestroy()");
        	this.unregisterReceiver(cr);
        	this.unregisterReceiver(onr);
        	if(foreground) stopForegroundCompat(NOTIFY_ID);
        	if(wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        	super.onDestroy();
    }

	private boolean first_start = true;
	static RVoixSrv ziz;	// dirty hack to simplify jni stuff
	@Override
    public void onStart(Intent intent, int startId) {
			Log.dbg("onStart(): entry");
			if(foreground) stopForegroundCompat(NOTIFY_ID);
			getPrefs(); ziz = this;
			if(foreground) goForeground();
			iilist = get_array(FContentList.IEMODE,FContentList.TYPE_I);
			oilist = get_array(FContentList.OEMODE,FContentList.TYPE_I);
			Log.dbg("onStart(): exit");
			first_start = false;
	}

	public void goForeground() {
			String s = getString(first_start ? R.string.SStarted : R.string.SReStarted);
			Notification notification = new Notification(R.drawable.stat_sample, s,	System.currentTimeMillis());
			Intent notificationIntent = new Intent(this, Browser.class);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			Context context = getApplicationContext();
			notification.setLatestEventInfo(context, "Call Recorder", s, contentIntent);
			startForegroundCompat(NOTIFY_ID,notification);
	}
	
	private enum CType { UNKNOWN, OUTGOING_CONT, OUTGOING_NCONT, 
					INCOMING_NONUMBER, INCOMING_CONT, INCOMING_NCONT  }

	private CType call_type = CType.UNKNOWN;
	private int call_proc = INCOMING_ACT_ASK_INCALL;
	
	// phone number, or contact name (if present)
	private String out_number = null;
	private String inc_number = null;
	
	public static boolean recording  = false;
	public static boolean encoding = false;
	
	// number of recording for *INCALL modes
	private int chunk = 0;
	private boolean ask_in_progress = false;
	private boolean ask_incall_started = false;	
		
	private void set_defaults() {
		call_type = CType.UNKNOWN;
   		call_proc = INCOMING_ACT_ASK_INCALL;
   		out_number = null;
   		inc_number = null;
   		chunk = 0;
   		ask_in_progress = false;
   		ask_incall_started = false;
	}

	public void startAskActivity(Context context, boolean out, boolean incall) {
		Intent intie = new Intent();
 	   	intie.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 	   	intie.setClassName("com.voix", "com.voix.AskOnCall");
 	   	intie.putExtra("outgoing", out);
 	   	intie.putExtra("incall", incall);
 	   	intie.putExtra("phone", out ? out_number : inc_number);
 	   	Log.msg("asking whether to record " + (out ? out_number : inc_number));
        Log.dbg("starting AskOnCall activity");
 	   	context.startActivity(intie);
	}
	
	// Interaction with AskActivity: we call it
    private static final RemoteCallbackList<IRVoixCallback> cBacks = new RemoteCallbackList<IRVoixCallback>();

    public enum CBack { ENC_CMPL, REC_CMPL, DONE, REC_START  }
    
    private static void callAskActivity(CBack cb) {
        final int k = cBacks.beginBroadcast();
            Log.dbg("callAskActivity(): entry");
        	for (int i=0; i < k; i++) {
            	try {
            		switch(cb) {
            			case ENC_CMPL:	cBacks.getBroadcastItem(i).encodingComplete(); break;
            			case REC_CMPL:	cBacks.getBroadcastItem(i).recordingComplete(); break;
            			case REC_START:	cBacks.getBroadcastItem(i).recordingStarted(); break;
            			case DONE:	cBacks.getBroadcastItem(i).goodbye(); break;	
            		}
            	} catch (RemoteException e) {
                    Log.err("Remote exception!");
            		e.printStackTrace(); 
            		break;
            	}
            }
        	cBacks.finishBroadcast();
        	Log.dbg("callAskActivity(): exit");
    }

    private static void broadcastAboutToRecord(boolean is_outgoing, boolean is_incall, String p) {
        final int k = cBacks.beginBroadcast();
            Log.dbg("callAskActivity(): entry");
        	for (int i=0; i < k; i++) {
            	try {
            		cBacks.getBroadcastItem(i).recordingAboutToStart(is_outgoing, is_incall, p);
            	} catch (RemoteException e) {
                    Log.err("Remote exception!");
            		e.printStackTrace(); 
            		break;
            	}
            }
        	cBacks.finishBroadcast();
        	Log.dbg("callAskActivity(): exit");
    }
    
	// Interaction with AskActivity: it calls us

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
   
    public final IRVoix.Stub binder = new IRVoix.Stub() {
	    public void force_record(boolean rec) { 
	    	if(call_type == CType.UNKNOWN) { // must be already on-hook
	    		set_defaults();
	    		return;
	    	}
	    	if(rec) {
	    		if(call_type == CType.OUTGOING_CONT 
	    			|| call_type == CType.OUTGOING_NCONT) start_time = System.currentTimeMillis();
	    		cr.lastFile = makeFilename();
		    	if(startRecord(cr.lastFile, boost_up, boost_dn) != 0) {
		    		Log.err("startRecord failed"); return;
		    	}
		    	recording = true;
		    	callAskActivity(CBack.REC_START);
		    	Log.msg("started recording (at user request) to " + cr.lastFile);
	   			if(!wakeLock.isHeld()) wakeLock.acquire();
	    	} else {
	    		Log.msg("skipped recording (at user request)");
	    		set_defaults();
	    	}
	    	ask_in_progress = false;
	    }
	    public void call_telephony(String s) {
	    	Log.dbg("trying to invoke telephony interface: " + s);
	    	telephony.invoke(s);
	    }
	    public boolean is_recording() {
	    	return recording;
	    }
	    public boolean start_rec() {
	    	if(recording) {
	    		Log.err("start_rec() called while recording!");
	    		return false;
	    	}
	    	Log.dbg("start_rec(): entry");
	    	String ss = makeFilename();
	    	if(startRecord(ss, boost_up, boost_dn) != 0) {
	    		Log.err("startRecord failed"); return false;
	    	}
	    	recording = true;
	    	callAskActivity(CBack.REC_START);
	    	Log.msg("started recording (at user request) to " + ss);
   			if(!wakeLock.isHeld()) wakeLock.acquire();
   			Log.dbg("start_rec(): exit");
   			return true;
	    }
	    public boolean stop_rec() {
	    	if(!recording) {
	    		Log.err("stop_rec() called while not recording!");
	    		return false;
	    	}
	    	Log.dbg("stop_rec(): entry");
	    	stopRecord(srv_mode); chunk++;
	    	Log.dbg("stop_rec(): exit");
	    	return true;
	    }
	    public void wait_confirm(int confirm) {
	    	Log.msg("wait_confirm: " + confirm);
	    	wait_confirm_result = confirm;
	    }
	    public void registerCallback(IRVoixCallback cb)   { if(cb != null) cBacks.register(cb); }
        public void unregisterCallback(IRVoixCallback cb) { if(cb != null) cBacks.unregister(cb); }
	};
	
	// Interaction with native code: it calls us. 
	public static void onCallAnswered() {
		telephony.invoke("endCall");
	}
	public static void onRecordingComplete() {
		Log.dbg("onRecordingComplete()");
		if(wakeLock.isHeld()) wakeLock.release();
		callAskActivity(CBack.REC_CMPL);	// re-route to our activity
		recording = false;
	}

	public static void onEncodingComplete() {
		Log.dbg("onEncodingComplete()");
		if(wakeLock.isHeld()) wakeLock.release();
		callAskActivity(CBack.ENC_CMPL);	// re-route to our activity
		encoding = false;
		ziz.cleanUp();
		java.lang.System.gc();
	}
	
	// NB: chunk # is updated in stop_rec()
	private String makeFilename() {
		Log.dbg("makeFilename(): entry");
		String file = DateFormat.format("MM-dd-kkmm", new Date()).toString();
   		if(call_type == CType.OUTGOING_CONT || call_type == CType.OUTGOING_NCONT) {
   			if(out_number != null) {
   				String s = new String(out_number);
   				if(s.contains("*")) s = out_number.replace('*', '#');
   				file = "O-" + file + (s.charAt(0) == '+' ? "" : "-") + s;
   			}
   		} else if(inc_number != null){
   			String s = new String(inc_number);
			if(s.contains("*")) s = inc_number.replace('*', '#'); 
   			file = "I-" + file + (s.charAt(0) == '+' ? "" : "-") + s;
   		}
		if(ask_incall_started) file = file + "-" + chunk; 
		Log.dbg("makeFilename(): exit");
		return file;
	}
	
	
	// Receiver of outgoing calls
	private class OutNumReceiver extends BroadcastReceiver {
		private void check_overrides(String s) {
			if(oilist.contains(s)) {
			   call_proc = OUTGOING_REC_ASK_INCALL;
			   Log.dbg("override: phone found in always incall list");
			}
		}
			
		@Override
		public void onReceive(Context context, Intent intent) {
			   String s = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			   Log.dbg("onReceive(): " + s);
			   call_proc = out_proc;
			   if(s != null) {
				   String name = ctx.findInContacts(s);
				   if(name == null) {
					   call_type = CType.OUTGOING_NCONT;
					   out_number = new String(s);
				   } else {
					   call_type = CType.OUTGOING_CONT;
					   out_number = new String(name);
				   }
				   check_overrides(s);
			   } else {	// is it possible?
				   out_number = "unknown";
				   call_type = CType.OUTGOING_NCONT;
			   }
			   Log.msg("OutNumReceiver: new outgoing call " + out_number);
		}
	}
	
	// Receiver of phone state changes
	private class CallReceiver extends BroadcastReceiver {
		private String lastFile = null;
		@Override
		public void onReceive(Context context, Intent intent) {
    	   	   String s = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
               Log.msg("onReceive(), state = " + s);
               
               if(s.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
           	   	
            	    if(recording) return;
            	   	s =  intent.getStringExtra("incoming_number");
            	   	if(s != null) {
            	   		String name = ctx.findInContacts(s);
            	   		if(name != null) {
            	   			call_type = CType.INCOMING_CONT;
            	   			call_proc = cont_proc;
            	   			inc_number = new String(name);
            	   		} else {
            	   			call_type = CType.INCOMING_NCONT;
            	   			call_proc = ncont_proc;
            	   			inc_number = new String(s);
            	   		}
            	   		Log.dbg("default call_proc = " + call_proc );
            	   		if(iilist.contains(s)) {
            	   			call_proc = INCOMING_ACT_ASK_INCALL;
            	   			Log.dbg("phone found in always ask in-call list");
            	   		}
            	   		Log.dbg("effective call_proc after lists = " + call_proc );
            	   	} else {
            	   		inc_number = "unknown";
            	   		call_type = CType.INCOMING_NONUMBER;
            	   		call_proc = unk_proc;
            	   	}
            	   	
        	   		Log.msg("CallReceiver: new incoming call " + inc_number);
               } else if(s.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            	   	
            	   	start_time = 0;
            	   	
        	   		switch(call_type) {
    	   			case UNKNOWN:
        	   			case OUTGOING_CONT:
        	   			case OUTGOING_NCONT:
//        	   				startAskActivity(context, true, true);
        	   				broadcastAboutToRecord(true, true, out_number);
        	   				ask_incall_started = true;
            	   			return;
        	   			case INCOMING_NONUMBER:
        	   			case INCOMING_CONT:
        	   			case INCOMING_NCONT:
	        	   			if(call_proc == INCOMING_ACT_ASK_INCALL) {
//	        	   				startAskActivity(context, false, true);
	        	   				broadcastAboutToRecord(false, true, inc_number);
	        	   				ask_incall_started = true;
	        	   				return;
        	   				}
        	   				set_defaults();
        	   				return;
        	   		}
        	   		lastFile = makeFilename();
        	   		if(startRecord(lastFile, boost_up, boost_dn) != 0) {
        	   			Log.err("startRecord failed");
        	   			set_defaults();
        	   		} else {
        	   			recording = true;
        	   			Log.msg("started recording to " + lastFile);
        	   			if(!wakeLock.isHeld()) wakeLock.acquire();
        	   			callAskActivity(CBack.REC_START);
        	   		}
       	    		// set_defaults();
       	    		
               } else if(s.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            	   	
       	    		if(ask_incall_started || ask_in_progress) {
       	    			Log.msg("stopping AskActivity");
       	    			callAskActivity(CBack.DONE);
       	    		}
            	    if(recording) {
       	    			encoding = true;
       	    			stopRecord(srv_mode);
       	    		}
   	    			start_time = 0;
       	    		set_defaults();
               } 
		}
	}
	
	
	// Helper class to call internal telephony interface
	private class Telephony {
		private Object tel_mgr;
		Telephony() {
			try {
				Class<?> sm = Class.forName("android.os.ServiceManager");
				Method getSvc = sm.getMethod("getService", String.class);
				IBinder oBender = (IBinder) getSvc.invoke(sm, Context.TELEPHONY_SERVICE);
				Class<?> stub = Class.forName("com.android.internal.telephony.ITelephony$Stub");
				Method asIf = stub.getMethod("asInterface", IBinder.class);
				tel_mgr = asIf.invoke(stub, oBender);
			} catch (Exception e) { 
				Log.err("exception while trying to get ITelephony interface");
				e.printStackTrace();
			}
		}
		public void invoke(String function) {
			try {
				tel_mgr.getClass().getMethod(function,(Class [])null).invoke(tel_mgr);
			} catch (Exception e) {	
				Log.err("exception in invoke()");
				e.printStackTrace();
			}
		}
	}
	
	
	private void getPrefs(){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		foreground = settings.getBoolean("foreground", true);
		
		srv_mode = Integer.parseInt(settings.getString("format", "0"));
	
		try {
			boost_up = Integer.parseInt(settings.getString("boost_up", "0"));
			boost_dn = Integer.parseInt(settings.getString("boost_dn", "0"));
			out_proc = 5;
			unk_proc = 5;
			cont_proc = 5;
			ncont_proc = 5;
		} catch (Exception e) {
			Log.err("Error parsing preference values");
			e.printStackTrace();
		}
	}
	
	class Fxx implements Comparable<Fxx>  {
		public String fname;
		long last_mod;
		long length;
		@Override
		public int compareTo(Fxx another) {
			if(this.last_mod > another.last_mod) return 1;
			else if(this.last_mod < another.last_mod) return -1;
			else return 0;
//			return (int) (this.last_mod - another.last_mod);
		}
	}

	private NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
	    // If we have the new startForeground API, then use it.
	    if (mStartForeground != null) {
	        mStartForegroundArgs[0] = Integer.valueOf(id);
	        mStartForegroundArgs[1] = notification;
	        try {
	            mStartForeground.invoke(this, mStartForegroundArgs);
	        } catch (Exception e) {
	        	Log.err("exception in startForeground");
	        	e.printStackTrace();
	        }
	        return;
	    }
	    // Fall back on the old API.
	    setForeground(true);
	    mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
	    // If we have the new stopForeground API, then use it.
	    if (mStopForeground != null) {
	        mStopForegroundArgs[0] = Boolean.TRUE;
	        try {
	            mStopForeground.invoke(this, mStopForegroundArgs);
	        } catch (Exception e) {
	        	Log.err("exception in stopForeground");
	        	e.printStackTrace();
	        }
	        return;
	    }
	    // Fall back on the old API.  Note to cancel BEFORE changing the
	    // foreground state, since we could be killed at that point.
	    mNM.cancel(id);
	    setForeground(false);
	}
	
	private void cleanUp() {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		if(tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) return;

		int max_files = 0; 
		long max_storage = 0, max_time = 0;
		
		Log.msg("trying to clean up");
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		try {
			max_files = Integer.parseInt(settings.getString("max_files", null));
			max_storage= Integer.parseInt(settings.getString("max_storage", null));
			max_storage *= (1024*1024); // megabytes -> bytes
			max_time = Integer.parseInt(settings.getString("max_time", null));
			max_time *= 1000*60*60*24;	// days -> milliseconds
		} catch (Exception e) {
			Log.err("Error parsing cleanup preference values");
			e.printStackTrace();
			return;
		}
		
		if(max_files == 0 && max_storage == 0 && max_time == 0) {
			Log.msg("no limits set, bailing out");
			return;
		}
		
		// read favourites

	    FContentList favs = new FContentList(FContentList.FAVS_FILE, this);
	    favs.read();
		
		// Log.msg("total favs: " + favs.size());
		
		// get all files
		
		ArrayList<Fxx> files = new ArrayList<Fxx>();
			
		try {
			File[] filez = (new File(com.voix.Browser.voixdir)).listFiles();
			for(int i = 0; i < filez.length; i++) {
				String s = filez[i].toString();
				String q = s.substring(com.voix.Browser.dirlen);
				if((s.endsWith(".wav") || s.endsWith(".mp3")) && (q.startsWith("I-") || q.startsWith("O-"))) {
					if(favs.contains(s)) {
				//		Log.msg(s + " in favourites, skipped.");
						continue;
					}
					Fxx x = new Fxx();
					x.fname = s;
					x.last_mod = filez[i].lastModified();
					x.length = filez[i].length();
					files.add(x);
				}
			}
		} catch (Exception e) {
			Log.err("something nasty happened");
			e.printStackTrace();
		}
		
		if(files.size() == 0) {
			Log.msg("no files: cleanup complete");
			return;
		}
		
		Collections.sort(files);
		
	//	for(int i = 0; i < files.size(); i++) {
	//		Log.msg("file: " + files.get(i).fname + ", last_mod=" + files.get(i).last_mod);
	//	}
		
		////////////////////// Real job begins.
		//////////////////////

	//	Log.msg("step 1: " + files.size() + " files to process");
		if(max_files != 0 && max_files < files.size()) {
			int nr = 0;
			while(max_files < files.size()) {
				(new File(files.get(0).fname)).delete();
				files.remove(0);
				nr++;
			}
			if(nr != 0) Log.msg("cleanUp: removed " + nr + " files (max files number exceeded)");
		}

		if(files.size() == 0) {
			Log.msg("no more files: cleanup complete");
			return;
		}

	//	Log.msg("step 2: " + files.size() + " files to process");
		if(max_storage != 0) {
			int nr = 0;
			long sz = 0;
			for(int i = 0; i < files.size(); i++) sz += files.get(i).length;
			for(int i = 0; max_storage < sz && files.size() != 0; i++) {
				sz -= files.get(0).length;
				(new File(files.get(0).fname)).delete();
				files.remove(0);
				nr++;
			}
			if(nr != 0) Log.msg("cleanUp: removed " + nr + " files (max storage size exceeded)");
		}

		if(files.size() == 0) {
			Log.msg("no more files: cleanup complete");
			return;
		}
		
	//	Log.msg("step 3: " + files.size() + " files to process");
		if(max_time != 0) {
			int nr = 0;
			long now = System.currentTimeMillis();
			while(files.size() != 0) {
				if(now - files.get(0).last_mod < max_time) break; // files are time ordered
				(new File(files.get(0).fname)).delete();
				Log.msg("removing: " + files.get(0).fname + " " + (now - files.get(0).last_mod) + " < " + max_time);
				files.remove(0);
				nr++;
			}
			if(nr != 0) Log.msg("cleanUp: removed " + nr + " files (max storage duration exceeded)");
		}
		Log.msg("cleanup complete: " + files.size() + " non-favourites remain");
	}
}
