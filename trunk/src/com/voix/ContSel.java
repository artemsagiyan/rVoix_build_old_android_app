package com.voix;

import java.util.ArrayList;
import java.util.Collections;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;

public class ContSel extends ListActivity {

	private final ArrayList<ListLine> contacts = new ArrayList<ListLine>();
	private ListLineAdapter adapter;
	private Contix ctx;
    private FContentList bwflist = null;
    private ListView flist;
    private int list_color = 0;
    private char type = FContentList.TYPE_NONE;
    private ArrayList<String> phones = null;
    private String aa_file = null;
    
	private class ListLine implements Comparable<ListLine> {
		public String dname;
		public String name;
		public String phone;
		boolean selected;
		ListLine(String dispname, String cname, String tel) {
			dname = dispname;
			name = cname;
			phone = tel;
			selected = false;
		}
		@Override
		public int compareTo(ListLine another) {
			int i = name.compareTo(another.name);
			if(i != 0) return i;
			i = phone.compareTo(another.phone);
			return i;
		}  
	}
	
	public static int ColorSel = 0xFF800000;
	public static int ColorUnSel = 0xFF000000;

	private class ListLineAdapter extends ArrayAdapter<ListLine> {
		public ListLineAdapter(Context context) {
			super(context, R.layout.list_item, contacts);
		}
		@Override
		public View getView(int i, View convertView, ViewGroup parent) { 
			View row = convertView;
			ListLine ll = contacts.get(i);
			if(row == null) row = getLayoutInflater().inflate(R.layout.list_item,(ViewGroup)findViewById(R.id.LayoutRoot));
			TextView txt = (TextView) row.findViewById(R.id.label);
			txt.setText(ll.dname);
			row.setId(i);
			if(!ll.selected) row.setBackgroundColor(ColorUnSel);
			else row.setBackgroundColor(ColorSel);
			txt.setTextColor(0xFFFFFFFF);
			return row; 
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intie = getIntent();
        list_color = intie.getIntExtra("list_color",0);
        type = intie.getCharExtra("type",FContentList.TYPE_NONE);
        if(list_color == FContentList.AEMODE) {
        	aa_file = intie.getStringExtra("aa_file");
        	if(aa_file == null) {
        		finish(); return;
        	}
        }
        
        Log.dbg("onCreate(): entry, col=" + list_color + ", type=" + type);
        
        ctx = Contix.getContix();
        ctx.setContentResolver(getContentResolver());

        bwflist = new FContentList(FContentList.LIST_FILES[list_color], this);
        bwflist.read();
        Log.dbg("read bwflist, size=" + bwflist.size());
        
        if(list_color == FContentList.WMODE) phones = bwflist.get_array(FContentList.TYPE_NONE);
        else phones = bwflist.get_array(FContentList.TYPE_Z);
        
        Log.dbg("read phones, size=" + phones.size());
        
        setContentView(R.layout.list);
        adapter = new ListLineAdapter(this);
        setAdapter();
        
        flist = (ListView) findViewById(android.R.id.list);
        flist.setOnItemLongClickListener(longClick);
        
        Log.dbg("onCreate(): exit");
	}

	private void setAdapter() {
		Log.dbg("setAdapter(): entry");
		ArrayList<String>cts = ctx.getAllContacts();
		contacts.clear();
		if(cts.size()> 0) {
			for(int i = 0; i < cts.size(); i+=2) {
				String p = cts.get(i+1);
				if(phones.contains(p)) continue;
				String n = cts.get(i);
				String d = n + "\n" + p;
				contacts.add(new ListLine(d,n,p));
			}
			Collections.sort(contacts);
			Log.dbg("added contacts, size=" + contacts.size());
			setListAdapter(adapter);
		} else {
			Log.err("empty contact list!");
			finish();
		}
		Log.dbg("setAdapter(): exit");	
	}
	
	@Override
	public  void onListItemClick(ListView parent, View v, int pos, long id) { 
		int i = (int) id;
		if(pos < contacts.size()) {
			boolean s = !contacts.get(i).selected;
			contacts.get(i).selected = s;
			if(s) v.setBackgroundColor(ColorSel);
			else v.setBackgroundColor(ColorUnSel);
			bwflist.dirty = true;
		}
	}
	
	AdapterView.OnItemLongClickListener longClick= new OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> a, View v, int i, long k) {
       	 	Log.dbg("long click detected");
        	int lpress = (int) k;
        	if(lpress >= contacts.size()) return false;
        	String name = contacts.get(lpress).name;
        	boolean selected = !contacts.get(lpress).selected;
        	for(ListLine ll : contacts) {
        		if(ll.name.equals(name)) {
        			ll.selected = selected;
        		}
        	}
        	flist.invalidateViews();
        	bwflist.dirty = true;
        	return true;
        }
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cmenu, menu);
        Log.dbg("onCreateOptionsMenu()");
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.dbg("onOptionsItemSelected()");
		switch (item.getItemId()) {
			case R.id.SelNone:
				bwflist.dirty = true;
				for(ListLine ll : contacts)	ll.selected = false;
				flist.invalidateViews();
				return true;
			case R.id.SelAll:
				bwflist.dirty = true;
				for(ListLine ll : contacts) ll.selected = true;
				flist.invalidateViews();
				return true;
		}
		return false; 
	}
	
	@Override
	public void finish() {
		if(bwflist != null && bwflist.dirty) {
			int i = 0;
			for(ListLine ll : contacts) {
				if(ll.selected && !phones.contains(ll.phone)) {
					Log.dbg("added " + ll.phone + " to "+ FContentList.LIST_FILES[list_color]);
					if(list_color == FContentList.WMODE) bwflist.add(ll.phone);
					else if(list_color == FContentList.AEMODE ) bwflist.add(ll.phone+'\t'+type+'\t'+aa_file);
					else bwflist.add(ll.phone+'\t'+type);
					phones.add(ll.phone);
					i++;
				}
			}
			Log.dbg("exiting, added "+ i+ " entries,  bwflist size="+bwflist.size());
			bwflist.write();
			setResult(1);
		} else Log.dbg("exiting, bwflist size="+bwflist.size());
		super.finish();
	}	

}
