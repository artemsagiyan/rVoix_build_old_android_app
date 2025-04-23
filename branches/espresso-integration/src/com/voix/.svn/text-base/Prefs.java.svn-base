package com.voix;

import java.util.List;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class Prefs extends PreferenceActivity 
	implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	private boolean prefs_changed;
	private String fdir;
	
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
		
		String []q = {"boost_up", "boost_dn"};
		for(String s : q) {
			m = (ListPreference) screen.findPreference(s);
			m.setSummary(getString(R.string.SCurVal) + " " + settings.getString(s, "0"));
		}
			
		String []y = {"max_files", "max_storage", "max_time"};
		for(String s : y) {
			EditTextPreference e = (EditTextPreference) screen.findPreference(s);
			e.setSummary(getString(R.string.SCurVal) + " " + settings.getString(s, "")); 
		}

	       screen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		Log.dbg("onCreate(): exit");
        
	}

    private int str2int(String s) { // won't use Integer.parseInt
		if(s == null) return 0;
		if(s.compareTo("1")==0) return 1;
		else if(s.compareTo("2")==0) return 2;
		else if(s.compareTo("3")==0) return 3;
		else if(s.compareTo("4")==0) return 4;
		else if(s.compareTo("5")==0) return 5;
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
			   prefs_changed = true;
			   java.lang.System.gc();
			   setResult(1);
		   }   
	}
}
