package com.voix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OutRec extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Intent i = new Intent(context, RVoixSrv.class);
		i.setAction(Intent.ACTION_NEW_OUTGOING_CALL);
		i.putExtra(Intent.EXTRA_PHONE_NUMBER, intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
		context.startService(i);
	}
}
