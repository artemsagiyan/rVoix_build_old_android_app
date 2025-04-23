
package com.voix;

import java.io.DataOutputStream;
import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootRec extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String s = intent.getAction();
			if(s.equals("android.intent.action.BOOT_COMPLETED")) {
				if(!checkSetDevicePermissions()) return;
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				if(settings.getBoolean("bootup", false) == true) {
					Intent intie = new Intent().setClassName("com.voix", "com.voix.RVoixSrv");
		        	context.startService(intie);
				}
			} else if(s.equals("android.intent.action.CALL_PRIVILEGED")) {
				String ss = intent.getDataString();  
				boolean outgoing = true;
				String phone;
				if(ss != null && ss.startsWith("tel:")) { // for PRIVILEDGED
					outgoing = true;
					phone = ss.substring(4);
				} else {
					outgoing = intent.getBooleanExtra("outgoing",true);
					phone = intent.getStringExtra("phone");
				}
				Log.msg("CALL_PRIVILEGED: " + phone + ", outgoing=" + outgoing);
			}
	}
	
    public static final boolean checkSetDevicePermissions() {
        java.lang.Process process = null;
        DataOutputStream os = null;
	    File f1 = new File("/dev/voc_tx_record");
        File f2 = new File("/dev/vocpcm2");
        File mod = new File("/system/lib/modules/vocpcm.ko");
        boolean olddevs = false;
        		try {
                        if(f1.exists()) {	
                        	if(f1.canRead() && f1.canWrite()) return true;
                        } else if(f2.exists()){
                        	if(f2.canRead() && f2.canWrite()) return true;
                        	olddevs = true;
                        } else if(mod.exists()) {
                        	Log.msg("vocpcm module found, trying to load it");
                        	process = Runtime.getRuntime().exec("su");
                        	os = new DataOutputStream(process.getOutputStream());
                        	os.writeBytes("insmod /system/lib/modules/vocpcm.ko\n"); os.flush();
                        	os.writeBytes("exit\n"); os.flush();
                        	process.waitFor();
                        	process.destroy(); process = null;
                        	os.close(); os = null;
                        	f1 = new File("/dev/voc_rx_record");
                        	if(!f1.exists()) {
                        		Log.err("module failed to create devices, exiting");
                        		return false;
                        	}
                        } else return false;
                        process = Runtime.getRuntime().exec("su");
                        os = new DataOutputStream(process.getOutputStream());
                        os.flush();
                        if(olddevs) {	
                        	os.writeBytes("chmod 0666 /dev/vocpcm0\n"); os.flush();
                        	os.writeBytes("chmod 0666 /dev/vocpcm2\n"); os.flush();
                        	os.writeBytes("chmod 0666 /dev/vocpcm3\n"); os.flush();
                        } else {
                        	os.writeBytes("chmod 0666 /dev/voc_tx_record\n"); os.flush();
                        	os.writeBytes("chmod 0666 /dev/voc_rx_record\n"); os.flush();
                        	os.writeBytes("chmod 0666 /dev/voc_tx_playback\n"); os.flush();
                        }
                        os.writeBytes("exit\n"); os.flush();
                        process.waitFor();
                } catch (Exception e) {
                		Log.err("exception in checkSetDevicePermissions()");
                		e.printStackTrace();
                		return false;
                } finally {
                        try {
                            if(os != null) os.close();
                            if(process != null) process.destroy();
                        } catch (Exception e) { 
                        	Log.err("exception when exiting checkSetDevicePermissions()");
                        	e.printStackTrace();
                        }
                }
                if((f1.exists() && f1.canRead() && f1.canWrite()) || (f2.exists() && f2.canRead() && f2.canWrite())) return true;
                return false;
    }
}
