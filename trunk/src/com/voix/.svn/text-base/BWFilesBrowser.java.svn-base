package com.voix;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;

public class BWFilesBrowser extends ListActivity implements FilenameFilter	{
	
	private int list_color = 0;
	private String start = null;
	private ArrayList<String> lines = new ArrayList<String>();
	public static final String[]st = {".wlist",".blist",".ilist",".olist", ".alist"};
	File [] files = null;

	@Override
	public boolean accept(File dir, String filename) {
		if(filename.startsWith(start)) return true;
		return false;		
	}
    
	@Override
	protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        list_color = getIntent().getIntExtra("list_color",0);
        Log.dbg("onCreate(): " + list_color);

        if(list_color < 0) {
        	String prefix = "/sdcard/voix/sounds";
        	files = (new File(prefix)).listFiles();
        	if(files != null && files.length> 0) {
        		int k = prefix.length()+1;
        		for(int i=0; i < files.length; i++) {
        			String ss = files[i].toString().substring(k); 
        			if(!lines.contains(ss)) lines.add(ss);
        		}
        	}
        	setTitle(getString(R.string.SAASndFile));
        } else { 
        	String prefix = "/sdcard/voix";
        	start =  st[list_color];
        	files = (new File(prefix)).listFiles(this);
        	if(files != null && files.length> 0) {
        		int k = prefix.length()+1+6;
        		for(int i=0; i < files.length; i++) {
        			String ss =files[i].toString().substring(k); 
        			if(!lines.contains(ss)) lines.add(ss);
        		}
        	} 
    	    String []x = getResources().getStringArray(R.array.ListNames);
    	    setTitle(x[list_color]);
        }
        setContentView(R.layout.list);
        if(list_color < 0) {
        	ListView flist = (ListView) findViewById(android.R.id.list);
        	flist.setOnItemLongClickListener(longClick);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.list_item_browser, R.id.label, lines);
        setListAdapter(adapter);
    }
	
	@Override
	public void onListItemClick(ListView p, View v, int pos, long id) { 
		if(pos < lines.size()) {
			String s = lines.get(pos);
	        if(list_color < 0) Prefs.fbPath = s;
	        BWList.fbPath = s;
	        setResult(1);
	        finish();
		}
	}
	AdapterView.OnItemLongClickListener longClick= new OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> a, View v, int i, long k) {
       	 	Log.dbg("long click detected");
        	int lpress = (int) k;
        	if(lpress >= lines.size()) return false;
        	String f = lines.get(lpress);
        	Intent intie = new Intent(Intent.ACTION_VIEW); 
        	intie.setDataAndType(Uri.fromFile(new File("/sdcard/voix/sounds/"+f)), "audio/wav");
        	Log.msg("start playing " + f);
        	startActivity(intie);
        	return true;
        }
   };
	
}
