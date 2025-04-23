
package com.voix;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

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
				if(!checkSetDevicePermissions()) {
					Log.err("Cannot set device permissions!");
					return;
				}
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				if(settings.getBoolean("bootup", false) == true) {
					Log.msg("Starting service");
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

	private static boolean sudo(ArrayList <String> cmds) {
		java.lang.Process process = null;
	    DataOutputStream os = null;
	    try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.flush();
            for(int i = 0; i < cmds.size(); i++) {
            	os.writeBytes(cmds.get(i));  
            	os.flush();
            }
            os.writeBytes("exit\n"); 
            os.flush(); 
            os.close(); 
            os = null;
            process.waitFor();
		} catch (Exception e) {
			Log.err("exception in sudo()");
			e.printStackTrace();
			return false;
		} finally {
            try {
                if(os != null) os.close();
            	if(process != null) process.destroy();
            } catch (Exception e) { 
            	Log.err("exception on exit of sudo()");
            	e.printStackTrace();
            }
		}
		return true;
	}
	
    public static final boolean checkSetDevicePermissions() {
        
	    final String std_devz[] = { "/dev/msm_pcm_in", "/dev/msm_amrnb_in", "/dev/msm_amr_in", "/dev/msm_audio_dev_ctrl" };
	    ArrayList <String> cmds = new ArrayList <String>();
	    File f = new File(std_devz[0]);
	    
	    		if(!f.exists()) return false;
	    		if(f.canRead()) return true;
	    		
	    		for(String s: std_devz) {
	    			File ff = new File(s);
	    			if(ff.exists()) {
	    				if(!ff.canRead()) cmds.add("chmod 0664 " + s + "\n");
	    			}
                }
	    		if(cmds.isEmpty()) {
	    			return true;
	    		}
	    		if(!sudo(cmds)) return false;
	    return f.canRead();
    }
    
    
    // Load a list of modules, possibly with arguments, and possibly with functions not exported by kernel.
    // If "args" or "funcs" is non-null, it's size must match that of "mods".
    // For each non-exported function "fn", "fn_addr=0x..." is added to module parameters.
    // "forced" will execute "modprobe -f modname ..." instead of "insmod /system/lib/modname.ko ..."
    
    public static final int NOERROR = 0;
    public static final int INV_ARG = 1;
    public static final int NO_MODULES_FOUND = 2;
    public static final int FUNC_NOT_FOUND = 3;
    public static final int LOAD_FAIL = 4;
    
	public static int tryLoadingModules(boolean forced, String[] mods, String[][] args, String[][] funcs) {
		
		if(mods == null || mods.length == 0) return INV_ARG;
		
	    int i, cnt = 0;
		boolean need_loading[] = new boolean[mods.length]; 
		File proc;
		String line = null;
		BufferedReader reader = null;
		String modz[] = new String[mods.length];
		String argz[] = new String[mods.length];

		ArrayList<String> funz  = new ArrayList<String>();
		ArrayList<String> addrz = new ArrayList<String>();
		ArrayList<String> cmds = new ArrayList<String>();
				 
		if(args != null && args.length != mods.length) return INV_ARG;
		if(funcs != null && funcs.length != mods.length) return INV_ARG;

		try {
			for(i = 0; i < mods.length; i++) {
				modz[i] = "/system/lib/modules/" + mods[i] + ".ko";
				argz[i] = "";
				if(args != null && args[i] != null) {
					for(String s: args[i]) 
						argz[i] += (" " + s);
				}
				if(funcs != null && funcs[i] != null) {
					for(String s: funcs[i]) 
						if(!funz.contains(s)) {
							funz.add(s);
							addrz.add("none");
						}
				}
			}

			// check if any modules are present
			for(i = 0; i < need_loading.length; i++) {
				need_loading[i] = (new File(modz[i])).exists();
				if (need_loading[i]) cnt++;
			}
			if(cnt == 0) {
				Log.dbg("No modules found");
				return NO_MODULES_FOUND;
			}
			
			// check if any modules are already loaded
			proc = new File("/proc/modules");
			reader = new BufferedReader(new FileReader(proc), 8192);
	 		while((line = reader.readLine()) != null) {
		 		for (i = 0; i < modz.length; i++) {
		 			if (line.startsWith(modz[i])) {
		 				Log.dbg(modz[i] + " already loaded, skipping");
		 				need_loading[i] = false;
		 				cnt--;
		 			}
		 		}
		 	}
	 		reader.close(); reader = null;
	 		if(cnt == 0) return NOERROR;

	 		// find all addresses of non-exported functions
	 		if(funcs != null) {
	 			proc = new File("/proc/kallsyms");
	 			reader = new BufferedReader(new FileReader(proc), 8192);
	 			while((line = reader.readLine()) != null) {
	 				String s[] = line.split(" ");
	 				if(s.length == 3 && funz.indexOf(s[2]) >= 0) {
	 					addrz.set(i, s[2] +"_addr=0x" + s[0]);
	 					Log.dbg("found " + s[2] + " at " + s[0]);
	 				}
	 			}
	 			reader.close(); reader = null;
	 		}
	 		// add addresses to module parameters
	 		for(i = 0; i < mods.length; i++) {
	 			if(need_loading[i] && funcs != null && funcs[i] != null) {
	 				for (String s : funcs[i]) {
	 					String addr = addrz.get(funz.indexOf(s));
	 					if(addr.equals("none")) {
	 						Log.err("Function " + s + " for " + mods[i] + " is not found in kernel.");
	 						return FUNC_NOT_FOUND;
	 					}
	 					argz[i] += (" " + addr);
	 				}
	 			}
	 		}
	 		// commands for sudo
	 		for(i = 0; i < modz.length; i++) {
	 			if (need_loading[i]) {
	 				String cmd;
	 				if(!forced) cmd = "insmod " + modz[i] + argz[i];
	 				else cmd = "modprobe -f " + mods[i] + argz[i];
	 				cmd += "\n";
	 				cmds.add(cmd);
	 			}
	 		}
			if(sudo(cmds)) return LOAD_FAIL;

		} catch(Exception e) { 
			Log.err("Exception while loading modules"); 
		} finally {
			try {
				if(reader != null) reader.close();
			} catch(Exception e) { 
				Log.err("Exception during cleanup"); 
			}
		}
		return NOERROR;
	}

}
