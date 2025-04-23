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
		"wlist", "blist", "ilist", "olist", "alist"
	};
	
	public static final String[] LIST_FILES_EXT_PREFIXES = {
		"/sdcard/voix/.wlist", "/sdcard/voix/.blist", "/sdcard/voix/.ilist", "/sdcard/voix/.olist", "/sdcard/voix/.alist"
	};
	
	public static final int WMODE = 0;
	public static final int BMODE = 1;
	public static final int IEMODE = 2;
	public static final int OEMODE = 3;
	public static final int AEMODE = 4;
	
	public static final char TYPE_NONE = 0;
	public static final char TYPE_H = 'h';	// hang up
	public static final char TYPE_M = 'm';	// mute
	public static final char TYPE_R = 'r';	// record
	public static final char TYPE_A = 'a';	// ask
	public static final char TYPE_N = 'n';	// don't record
	public static final char TYPE_I = 'i';	// in-call record
	public static final char TYPE_Q = 'q';	// auto answer
	public static final char TYPE_X = 'x';	// auto answer+record
	public static final char TYPE_Z = 'z';	// any type
	public static final char TYPE_Q2 = 'Q';	// string following tab after TYPE_Q
	public static final char TYPE_X2 = 'X';	// string following tab after TYPE_X
	
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
				// Log.dbg(file + " does not exist");
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
		//	Log.dbg("read_internal(): read "+ size() + " entries from " + file);
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
	
	@SuppressWarnings("unchecked")
	public void trim_content() {
		// remove duplicates
		HashSet<String> set = new HashSet<String> ((ArrayList <String>)clone());
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
			ArrayList <String>a = (ArrayList <String>) clone();
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
			int len = ss.length;
			if(len == 2) {
				if(ss[1].charAt(0) == type || type == TYPE_Z) a.add(new String(ss[0]));
			} else if(len == 3) {
				switch(type) {
					case TYPE_Q2: if(ss[1].charAt(0) == TYPE_Q) a.add(new String(ss[2])); break;
					case TYPE_X2: if(ss[1].charAt(0) == TYPE_X) a.add(new String(ss[2])); break;
					case TYPE_Q:  if(ss[1].charAt(0) == TYPE_Q) a.add(new String(ss[0])); break;
					case TYPE_X:  if(ss[1].charAt(0) == TYPE_X) a.add(new String(ss[0])); break;
				}
			} 
		}
		// Log.dbg("get_array() returning " + a.size());
		return a;
	}
}


