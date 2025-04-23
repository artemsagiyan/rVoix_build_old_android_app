package com.voix;

import java.io.File;
import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class Prefs extends PreferenceActivity 
	implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	private boolean prefs_changed;
	
	public static final int RR_REQ_CODE = 21;
	public static final int BW_REQ_CODE = 22;
	public static final int AA_REQ_CODE = 23;
	private String fdir;
	public 	static String fbPath = null;
	
	private final String []aa_files = {"un_file_a","cn_file_a","nc_file_a","ba_file_a","wa_file_a", 
			"un_file_r","cn_file_r","nc_file_r","wa_file_r"};
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
        Log.dbg("onCreate(): entry");
        prefs_changed = false;
        addPreferencesFromResource(R.xml.prefs);
        
        PreferenceScreen screen = getPreferenceScreen();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        fdir = getFilesDir().toString() + "/";
        
        ListPreference m = (ListPreference) screen.findPreference("format");
       	
        m.setSummary(str2int(settings.getString("format", "0")) == 0 ? "WAV" : "MP3");

		String []x = getResources().getStringArray(R.array.OutCallActions);
		m = (ListPreference) screen.findPreference("outgoing_calls");
		m.setSummary(x[str2int(settings.getString("outgoing_calls", "0"))]);

		String []z = {"unknown_numbers", "numbers_in_contacts", "numbers_not_in_contacts"};
		String []ia = getResources().getStringArray(R.array.InCallActions);
		for(String s : z) {
			m = (ListPreference) screen.findPreference(s);
			int i = str2int(settings.getString(s, "0"));
			if(i > 5) {	// backward compatibility
				i = 0;
				SharedPreferences.Editor e = settings.edit();
				e.putString(s, "0");
			}
			m.setSummary(ia[i]);
		}

		String []z1 = {"cn_aa_mode", "nc_aa_mode", "un_aa_mode"};
		String []ia1 = getResources().getStringArray(R.array.AAModes);
		for(String s : z1) {
			m = (ListPreference) screen.findPreference(s);
			int i = str2int(settings.getString(s, "0"));
			m.setSummary(ia1[i]);
		}
		
		String []q = {"boost_up", "boost_dn" };
		for(String s : q) {
			m = (ListPreference) screen.findPreference(s);
			m.setSummary(getString(R.string.SCurVal) + " " + settings.getString(s, "0"));
		}
			
		String []y = {"max_files", "max_storage", "max_time", "min_out_time", "nc_aa_delay", "cn_aa_delay", "un_aa_delay", "ex_aa_delay", "min_in_time"};
		for(String s : y) {
			EditTextPreference e = (EditTextPreference) screen.findPreference(s);
			e.setSummary(getString(R.string.SCurVal) + " " + settings.getString(s, "0")); 
		}

		String []iw = getResources().getStringArray(R.array.BlistActions);
		m = (ListPreference) screen.findPreference("bmode");
		int io = str2int(settings.getString("bmode", "0"));
		if(io > RVoixSrv.BMODE_LAST) io = RVoixSrv.BMODE_NONE;
		m.setSummary(iw[io]);
		
		String []aa = {"edit_wlist","edit_blist","edit_ielist","edit_oelist","edit_aelist"};
		int []col = {FContentList.WMODE,FContentList.BMODE,FContentList.IEMODE,FContentList.OEMODE,FContentList.AEMODE};
		for(int i = 0; i < aa.length; i++) {
			Preference p = screen.findPreference(aa[i]);
			p.setSummary(getString((new File(fdir+FContentList.LIST_FILES[i])).exists() 
		       		? R.string.SListActive : R.string.SListInactive));
			set_pref_handler(p, "com.voix.BWList", col[i], BW_REQ_CODE);
		}

		for(int i = 0; i < aa_files.length; i++) {
			Preference p = screen.findPreference(aa_files[i]);
			String s = settings.getString(aa_files[i], null);
			if(s == null) s = getString(R.string.SAANoFile);
			else if(!(new File(s)).exists()) { // user deleted the file
				settings.edit().remove(aa_files[i]).commit();
				s = getString(R.string.SAANoFile);
			} else s = s.substring(20); // skip initial "/sdcard/voix/sounds/"
			p.setSummary(s);
			set_pref_handler(p, "com.voix.BWFilesBrowser", -1, AA_REQ_CODE+i);
		}
       	
       	final Preference pp = screen.findPreference("delete_log");
       	final File ff = new File(RVoixSrv.ServiceLogger.logfile);
       	final Context ctx = this;
       	if(!ff.exists()) {
       		pp.setEnabled(false);
       		pp.setSummary(R.string.SNoLogFile);
       	} else pp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
       		@Override
       		public boolean onPreferenceClick(Preference preference) {
       			try {       			
       				ff.delete();
       				pp.setEnabled(false);
       				pp.setSummary(R.string.SNoLogFile);
       				Toast.makeText(ctx, R.string.SLogCleared, Toast.LENGTH_SHORT).show();
       			} catch (Exception e) {
       				e.printStackTrace();
       				Toast.makeText(ctx, R.string.SCantDeleteLogFile, Toast.LENGTH_SHORT).show();
       			}
       			return false;
       		}});
       	Preference p = screen.findPreference("new_rec");
       	set_pref_handler(p, "com.voix.RecSndFile", 0, RR_REQ_CODE);
	    
       	screen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		Log.dbg("onCreate(): exit");
       	
	}

	private void set_pref_handler(Preference p, String cls, int col, int req) {
       	final int color = col;
       	final int req_code = req;
       	final String cl = new String(cls);
		fbPath = null;
       	p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
       		@Override
       		public boolean onPreferenceClick(Preference preference) {
       			Intent intent = new Intent().setClassName("com.voix", cl);
       			intent.putExtra("list_color", color);
       			startActivityForResult(intent, req_code);
       			return false;
       	}});
	}
	
    private int str2int(String s) { // won't use Integer.parseInt
		if(s == null) return 0;
		if(s.compareTo("1")==0) return 1;
		else if(s.compareTo("2")==0) return 2;
		else if(s.compareTo("3")==0) return 3;
		else if(s.compareTo("4")==0) return 4;
		else if(s.compareTo("5")==0) return 5;
		else if(s.compareTo("6")==0) return 6;
		else if(s.compareTo("7")==0) return 7;
		return 0;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		prefs_changed = true;
		Preference pref = findPreference(key);
	    if (pref instanceof ListPreference) {
	        ListPreference listPref = (ListPreference) pref;
	        String s = ""+ listPref.getEntry();
	        if(key.equals("boost_up") || key.equals("boost_dn")) s = getString(R.string.SCurVal) + " " + s;
			pref.setSummary(s);
	    } else if (pref instanceof EditTextPreference) {
	    	EditTextPreference editPref = (EditTextPreference) pref;
		    pref.setSummary(getString(R.string.SCurVal) + " " + editPref.getText());
	    }	
	}
	@Override
	public void onDestroy() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		if(prefs_changed) {
			ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);  
			List<ActivityManager.RunningServiceInfo> ls = am.getRunningServices(1000);
			for(int i = 0; i < ls.size(); i++) {
				if(ls.get(i).process.compareTo("com.voix:remote")==0) {
					Intent intent = new Intent().setClassName("com.voix", "com.voix.RVoixSrv");
			        if(startService(intent)!= null) Log.msg("service restarted");
			        break;
				}
			}
		}
		super.onDestroy();
	}
	@Override
	protected void  onActivityResult(int requestCode, int resultCode, Intent  data) {
		   super.onActivityResult(requestCode, resultCode, data);
		   Log.dbg("onActivityResult() code="+requestCode+", result="+resultCode);
		   if(resultCode == 1) {
			   if(requestCode == BW_REQ_CODE) {
				   prefs_changed = true;
				   PreferenceScreen screen = getPreferenceScreen();
				   Preference p;
				   String[] lst = {"edit_wlist","edit_blist","edit_ielist","edit_oelist","edit_aelist" };
				   for(int i = 0; i < lst.length; i++) {
			   			p = screen.findPreference(lst[i]);
			   			p.setSummary(getString((new File(fdir+FContentList.LIST_FILES[i])).exists() 
						   ? R.string.SListActive : R.string.SListInactive));
			   		}   
			   } else if(requestCode >= AA_REQ_CODE) {
				   if(fbPath == null) return;
				   String fname = "/sdcard/voix/sounds/"+fbPath;
		           File f = new File(fname);
		           if(!f.exists() || !f.canRead()) return;
		           Log.dbg("user selected sound file: " + fname);
				   prefs_changed = true;
				   PreferenceScreen screen = getPreferenceScreen();
				   int i = requestCode - AA_REQ_CODE;
				   Preference p = screen.findPreference(aa_files[i]);
				   p.setSummary(fbPath);
				   SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				   SharedPreferences.Editor e = settings.edit();
				   e.putString(aa_files[i], fname);
				   e.commit();
			   }
			   java.lang.System.gc();
			   setResult(1);
		   }   
	}
}
