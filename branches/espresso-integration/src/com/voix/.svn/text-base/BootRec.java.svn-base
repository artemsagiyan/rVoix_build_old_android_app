
package com.voix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootRec extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String s = intent.getAction();
			if(s.equals("android.intent.action.BOOT_COMPLETED")) {
				Intent intie = new Intent().setClassName("com.voix", "com.voix.RVoixSrv");
	        	context.startService(intie);
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
}
