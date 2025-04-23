package com.voix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import android.content.Context;

public class FContentList extends ArrayList <String> {

	private static final long serialVersionUID = 1L;

	public static final String[] LIST_FILES = {
		"ilist", "olist"
	};
	
	public static final String[] LIST_FILES_EXT_PREFIXES = {
		"/sdcard/voix/.ilist", "/sdcard/voix/.olist"
	};
	
	public static final int IEMODE = 0;
	public static final int OEMODE = 1;
	
	public static final char TYPE_NONE = 0;
	public static final char TYPE_I = 'i';	// in-call record
	
	public static final String FAVS_FILE = "/sdcard/voix/.favourites";
	
	public boolean dirty = false;
	private String file;
	private Context ctx;
	boolean external;
	
	FContentList(String fname, Context context) {
		file = new String(fname);
		external = (file.charAt(0) == '/');
		ctx = context;
	}

	synchronized private void read_internal() {
		try {
			clear();
			File f = new File(ctx.getFilesDir() + "/" + file);
			if(!f.exists()) {
				Log.msg(file + " list does not exist");
				return;
			}
			FileInputStream reader = ctx.openFileInput(file);
			int len = (int) f.length();
			byte[] buffer = new byte[len];
			reader.read(buffer);
			reader.close();
			String s = new String(buffer);
			String[] ss = s.split("\n");
			for(int i = 0; i < ss.length; i++) {
        		 String sss = ss[i].trim();
        		 if(sss.length()> 0) add(sss);
        	}
			Log.dbg("read_internal(): read "+ size() + " entries from " + file);
        } catch (Exception e){ 
    		Log.err("exception in read_internal()");
        	e.printStackTrace(); 
        }
	}

	synchronized private void write_internal()  {
        try {
   		 	Log.dbg("write_internal(): writing "+size()+ " entries to " + file);
   			ctx.deleteFile(file);
   			if(size()==0) return;
   		 	FileOutputStream writer = ctx.openFileOutput(file, Context.MODE_PRIVATE);
        	int k = size();
        	for(int i = 0; i < k; i++)  {
        		String s = get(i);
        		writer.write(s.getBytes());
        		writer.write('\n');
        	}
        	writer.close();
        	dirty = false;
        } catch (Exception e){ 
        	Log.err("exception in write()");
        	e.printStackTrace(); 
        }
	}
	
	synchronized private void read_external() {
		clear();
		File f = new File(file);
		if(f.exists() && f.canRead()) {
			try {
       		 	BufferedReader reader = new BufferedReader(new FileReader(f), 8192);
       		 	String line = null;
       		 	while((line = reader.readLine()) != null) {
       		 		String s = line.trim();
       		 		if(s.length()> 0) add(s);
       		 	}
       		 	Log.dbg("read_external(): read "+size()+ " entries from " +file);
       		 	reader.close();
			} catch (Exception e){ 
				Log.err("exception in read_external()");
				e.printStackTrace(); 
			}
        } else Log.dbg(file + " does not exist or is unreadable");
	}

	synchronized private void write_external()  {
		File f = new File(file);
		if(f.exists()) {
        	if(!f.delete()) {
        		Log.err("cannot delete " + file);
        		return;
        	}
        }
        try {
   		 	Log.dbg("write_external(): writing "+size()+ " entries to " + file);
        	trim_content();
        	BufferedWriter writer = new BufferedWriter(new FileWriter(f, false), 8192);
        	int k = size();
        	for(int i = 0; i < k; i++)  {
        		String s = get(i);
        		writer.write(s);
        		writer.newLine();
        	}
        	writer.flush();
        	writer.close();
        	dirty = false;
        } catch (Exception e){ 
        	Log.err("exception in write_external()");
        	e.printStackTrace(); 
        }
	}
	
	public void trim_content() {
		// remove duplicates
		HashSet<String> set = new HashSet<String>((ArrayList <String>)clone());
		clear();
		addAll(set);
		// remove non-existing elements
		if(file.compareTo(FAVS_FILE) == 0) {
			for (Iterator<String> iter = iterator(); iter.hasNext(); ) {
				String s = iter.next();
				if(!(new File(s).exists())) {
					Log.dbg(s+" does not exist, removing entry");
					iter.remove(); 
				}
			}
		}
		if(size() > 0) {
			ArrayList <String>a = (ArrayList <String>)clone();
			Collections.sort(a);
			clear();
			addAll(a);
		}
	}

	public void read() {
		if(external) read_external();
		else read_internal();
	}
	public void write() {
		if(external) write_external();
		else write_internal();
	}

	public ArrayList <String> get_array(char type) {
		ArrayList <String> a = new ArrayList <String>();
		for (Iterator<String> iter = iterator(); iter.hasNext(); ) {
			String s = iter.next();
			if(type == TYPE_NONE) {
				a.add(new String(s));
				continue;
			}
			String []ss = s.split("\t");
		}
		Log.dbg("get_array() returning " + a.size());
		return a;
	}
}


