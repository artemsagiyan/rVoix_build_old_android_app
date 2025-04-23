package com.voix;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BWFilesBrowser extends ListActivity implements FilenameFilter	{
	
	private int list_color = 0;
	private String start = null;
	private ArrayList<String> lines = new ArrayList<String>();
	public static final String prefix = "/sdcard/voix";
	public static final String[]st = {".wlist",".blist",".ilist",".olist"};
	File [] files = null;

	@Override
	public boolean accept(File dir, String filename) {
		if(filename.startsWith(start)) return true;
		return false;		
	}
    
	@Override
	protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Intent intie = getIntent();
        list_color = intie.getIntExtra("list_color",0);
        
        Log.msg("onCreate(): " + list_color);
        start =  st[list_color];
		files = (new File(prefix)).listFiles(this);
        if(files.length> 0) {
			int k = prefix.length()+1+6;
			for(int i=0; i < files.length; i++) {
				String ss =files[i].toString().substring(k); 
				if(!lines.contains(ss)) lines.add(ss);
			}
		} 
        setContentView(R.layout.list);
        
	    String []x = getResources().getStringArray(R.array.ListNames);
	    setTitle(x[list_color]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.list_item_browser, R.id.label, lines);
        setListAdapter(adapter);
    }
	
	@Override
	public void onListItemClick(ListView p, View v, int pos, long id) { 
		if(pos < lines.size()) {
			String s = lines.get(pos);
			Log.msg("New BWlist selected: " + s + ", pos=" +pos+ ", id=" + id);
	        BWList.fbPath = s;
	  		setResult(1);
	        finish();
		}
	}
}
