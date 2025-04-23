
package com.voix;
import com.voix.IRVoixCallback;

interface IRVoix {
	void force_record(in boolean r);
	void call_telephony(String s);
	boolean is_recording();
	boolean start_rec();
	boolean stop_rec();
	void wait_confirm(int r);
	void cancel_autoanswer();
	void registerCallback(IRVoixCallback c);
    void unregisterCallback(IRVoixCallback c);
}
