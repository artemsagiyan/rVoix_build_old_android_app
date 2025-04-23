package com.voix;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class BWList extends ListActivity {
	
	private final ArrayList<ListLine> lines = new ArrayList<ListLine>();
	private ListLineAdapter adapter;
	private ArrayList<String> phones = new ArrayList<String>();
	private Contix ctx;
	private FContentList bwlist = null;
	private ListView flist;
	public 	static String fbPath = null;
	private int list_color = 0; 
		
	private class ListLine implements Comparable<ListLine> {
		public String dname;
		public String name;
		public String phone;
		public String aa_file;
		boolean selected;
		char type;
		ListLine(String dispname, String cname, String tel, String file, char t) {
			dname = dispname;
			name = cname;
			phone = tel;
			selected = false;
			aa_file = file;
			type = t;
		}
		@Override
		public int compareTo(ListLine another) {
			int i = name.compareTo(another.name);
			if(i != 0) return i;
			i = phone.compareTo(another.phone);
			return i;
		}  
	}

	private class ListLineAdapter extends ArrayAdapter<ListLine> {
		public ListLineAdapter(Context context) {
			super(context, R.layout.list_item, lines);
		}
		@Override
		public View getView(int i, View convertView, ViewGroup parent) { 
			View row = convertView;
			ListLine ll = lines.get(i);
			if(row == null) row = getLayoutInflater().inflate(R.layout.list_item,(ViewGroup)findViewById(R.id.LayoutRoot));
			TextView txt = (TextView) row.findViewById(R.id.label);
			txt.setText(ll.dname);
			row.setId(i);
			if(!ll.selected) row.setBackgroundColor(ContSel.ColorUnSel);
			else row.setBackgroundColor(ContSel.ColorSel);
			txt.setTextColor(0xFFFFFFFF);
			int img_res = 0;
			switch(ll.type) {
				case FContentList.TYPE_NONE:
					img_res = R.drawable.white;
					break;
				case FContentList.TYPE_H:
					img_res = R.drawable.hup;
					break;
				case FContentList.TYPE_M:
					img_res = R.drawable.mute;
					break;
				case FContentList.TYPE_R:
					img_res = R.drawable.rec;
					break;
				case FContentList.TYPE_Q:
					img_res = R.drawable.aa;
					break;
				case FContentList.TYPE_X:
					img_res = R.drawable.ar;
					break;
				case FContentList.TYPE_A:
					img_res = R.drawable.ask;
					break;
				case FContentList.TYPE_N:
					img_res = (list_color == FContentList.OEMODE ? R.drawable.out_pass : R.drawable.inc_pass);
					break;
				case FContentList.TYPE_I:
					img_res = R.drawable.incall;
					break;
			}
			((ImageView)row.findViewById(R.id.icon)).setImageResource(img_res);
			return row; 
		}
	}
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	        
        Intent intie = getIntent();
        list_color = intie.getIntExtra("list_color",0);
        
        Log.dbg("onCreate(): entry " + list_color);
        
        ctx = Contix.getContix();
        ctx.setContentResolver(getContentResolver());

        bwlist = new FContentList(FContentList.LIST_FILES[list_color], this);
        bwlist.read();

	    setContentView(R.layout.list);
	    String []x = getResources().getStringArray(R.array.ListNames);
	    setTitle(x[list_color]);
        adapter = new ListLineAdapter(this);
        setAdapter();
        
        flist = (ListView) findViewById(android.R.id.list);
        flist.setOnItemLongClickListener(longClick);
        flist.setOnScrollListener(sScr);
        
        fbPath = null;
        
        Log.dbg("onCreate(): exit");
	}

	private void setAdapter() {
		Log.dbg("setAdapter(): entry, size="+bwlist.size());
			lines.clear();
			phones.clear();
			String nic = getString(R.string.SNotInContacts);
			for(int i = 0; i < bwlist.size(); i++) {
					String p = bwlist.get(i);
					String pp = null;
					char type = FContentList.TYPE_NONE;
					if(list_color == FContentList.AEMODE) {
						String ss[] = p.split("\t");
						if(ss.length != 3 || ss[2] == null) continue;
						if(!(new File("/sdcard/voix/sounds/"+ss[2])).exists()) continue;
						type = ss[1].charAt(0);
						pp = ss[2];
						p = ss[0]; //.trim();
					} else if(list_color != FContentList.WMODE) {	
						String ss[] = p.split("\t");
						if(ss.length != 2 || ss[1] == null) {
							switch(list_color) {
								case FContentList.BMODE:
									type = FContentList.TYPE_H; 
									break;
								case FContentList.IEMODE:
								case FContentList.OEMODE:
									type = FContentList.TYPE_R; 
									break;
							}
						} else type = ss[1].charAt(0);
						p = ss[0]; //.trim(); 
					}
					String n = ctx.findInContacts(p);
					if(n == null) n = nic;
					String d = p + "\n(" + n + ")";
					if(!phones.contains(p)) {
						if(list_color == FContentList.AEMODE) d += "\n" + pp;
						phones.add(p);
						// Log.dbg("adding *" + d + "*");
						lines.add(new ListLine(d,n,p,pp,type));
					} else {
						Log.dbg(p + " skipped, phone already exists" );
						bwlist.dirty = true;
					}
			}
			if(lines.size() > 0) {
				Collections.sort(lines);
				setListAdapter(adapter);
			}	
			Log.dbg("setAdapter(): exit");	
	}
		
 
		// menu: 		add1 add_cont del_selected
		
	@Override
	public  void onListItemClick(ListView parent, View v, int pos, long id) { 
		int i = (int) id;
		if(pos < lines.size()) {
			boolean s = !lines.get(i).selected;
			lines.get(i).selected = s;
			if(s) v.setBackgroundColor(ContSel.ColorSel);
			else v.setBackgroundColor(ContSel.ColorUnSel);
		}
	}

	int lpress = 0;
	AdapterView.OnItemLongClickListener longClick= new OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> a, View v, int i, long k) {
        	lpress = (int) k;
        	Log.dbg("long click: processing item "+ lpress + ", display name=" + lines.get(lpress).dname);
        	if(lpress >= lines.size()) return false;
        	ctx_menu();
        	return true;
        }
	};
	private int firstVisible = 0;
   	AbsListView.OnScrollListener sScr = new OnScrollListener() {
	   @Override
	   public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		   firstVisible = firstVisibleItem;
	   }
	  @Override
	  public void onScrollStateChanged(AbsListView view, int scrollState) {
		  // nothing doing.
	  }
   	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bwmenu, menu);
        Log.dbg("onCreateOptionsMenu()");
        if(list_color == FContentList.WMODE) menu.findItem(R.id.ChangeType).setEnabled(false); 
        else if(list_color == FContentList.AEMODE) menu.findItem(R.id.ChangeType).setTitle(R.string.SChFileAndType);
        return true;
    }
	private final int REQ_CODE_LOAD	= 11;
	private final int REQ_CODE_CONT = 12;
	private final int REQ_CODE_CALL_CONTS = 13;
	private final int REQ_CODE_INPUT_LIST = 14;
	private final int REQ_CODE_CHG_ONE = 15;
	private final int REQ_CODE_CHG_SEL = 16;
	
	private final int PROCEED_CALL_CONTS = 0;
	private final int PROCEED_INPUT_LIST = 1;
	private final int PROCEED_CHG_ONE = 2;
	private final int PROCEED_CHG_SEL = 3;
	
	private final int INP_ADD_PHONE_LIST = 0;
	private final int INP_SAVE_TO_FILE = 1;
	private final int INP_EDIT_PHONE = 2;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.dbg("onOptionsItemSelected()");
		
		switch (item.getItemId()) {
			case R.id.DelSelected:
				Log.dbg("DelSelected");
				bwlist.dirty = true;
				Iterator<String> it = phones.iterator();
				Iterator<ListLine> iter = lines.iterator();
				int kk = 0;
				while(iter.hasNext()) {
					ListLine ll = iter.next();
					it.next();
					if(ll.selected) {
						iter.remove();
						it.remove();
						kk++;
					}
				}
				if(kk > 0) {
					adapter = new ListLineAdapter(this);
					setListAdapter(adapter);
					flist.setSelection(firstVisible);
				} else Toast.makeText(this, R.string.TNothingSelected, Toast.LENGTH_SHORT).show();
				return true;
			case R.id.ChangeType:
				Log.dbg("ChangeType");
				for(int i = 0; i < lines.size(); i++) {
					if(lines.get(i).selected) {
						if(list_color == FContentList.AEMODE) select_file_and_proceed(REQ_CODE_CHG_SEL);
						else select_type_and_proceed(PROCEED_CHG_SEL);
						return true;
					}
				}
				Toast.makeText(this, R.string.TNothingSelected, Toast.LENGTH_SHORT).show();
				return true;
			case R.id.AddFromContacts:
				Log.dbg("AddFromContacts");
				if(bwlist.dirty) saveBWlist();
				if(list_color == FContentList.WMODE) {
					Intent intent = new Intent().setClassName("com.voix", "com.voix.ContSel");
					intent.putExtra("list_color", list_color);
					intent.putExtra("type", 0);
					if(list_color == FContentList.AEMODE) intent.putExtra("aa_file", selected_file);
					startActivityForResult(intent, REQ_CODE_CONT);
				} else if(list_color == FContentList.AEMODE) select_file_and_proceed(REQ_CODE_CALL_CONTS);
				  else select_type_and_proceed(PROCEED_CALL_CONTS);	
				return true;
			case R.id.AddManually:
				Log.dbg("AddManually");
				if(list_color == FContentList.WMODE) input_dialog(INP_ADD_PHONE_LIST);
				else if(list_color == FContentList.AEMODE) select_file_and_proceed(REQ_CODE_INPUT_LIST);
				else select_type_and_proceed(PROCEED_INPUT_LIST);
				return true;
			case R.id.SaveList:
				Log.dbg("SaveList");
				input_dialog(INP_SAVE_TO_FILE);
				return true;
			case R.id.LoadList:
				Log.dbg("LoadList");
				if(bwlist.dirty) saveBWlist();
				Intent intent = new Intent().setClassName("com.voix", "com.voix.BWFilesBrowser");
				intent.putExtra("list_color", list_color);
				startActivityForResult(intent, REQ_CODE_LOAD);
				return true;
		}
		return false; 
	}
	
	private char selected_type = FContentList.TYPE_NONE;
	private String selected_file = null;
	
	private void callContSel() {
		Log.dbg("callContSel()");
		Intent intent = new Intent().setClassName("com.voix", "com.voix.ContSel");
		intent.putExtra("list_color", list_color);
		intent.putExtra("type", selected_type);
		if(list_color == FContentList.AEMODE) intent.putExtra("aa_file", selected_file);
		startActivityForResult(intent, REQ_CODE_CONT);
	}
	
	private void ch_type_lpressed() {
		if(selected_type == FContentList.TYPE_NONE) return;
		lines.get(lpress).type = selected_type;
		bwlist.dirty = true;
 	   	flist.invalidateViews();
	}

	private void ch_type_selected() {
		if(selected_type == FContentList.TYPE_NONE) return;
		int qq = 0;
		for(int i = 0; i < lines.size(); i++) {
			if(lines.get(i).selected) {
				lines.get(i).type = selected_type;
				lines.get(i).selected = false;
				qq++;
			}
		}
		if(qq > 0) {
			bwlist.dirty = true;
			adapter = new ListLineAdapter(this);
			setListAdapter(adapter);
			flist.setSelection(firstVisible);
		} else Toast.makeText(this, R.string.TNothingSelected, Toast.LENGTH_SHORT).show();
	}

	private void ch_file(int i) {
		if(selected_file == null || list_color != FContentList.AEMODE) return;
		ListLine ll = lines.get(i);
		ll.aa_file = new String(selected_file);
		if(list_color != FContentList.AEMODE) ll.selected = false;
		ll.dname = ll.phone + "\n(" + ll.name + ")\n" + ll.aa_file;
	}
	
	private void ch_file_selected() {
		if(selected_file == null || list_color != FContentList.AEMODE) return;
		int qq = 0;
		for(int i = 0; i < lines.size(); i++) {
			if(lines.get(i).selected) {
				//lines.get(i).aa_file = new String(selected_file);
				//lines.get(i).selected = false;
				ch_file(i);
				qq++;
			}
		}
		if(qq > 0) {
			bwlist.dirty = true;
			adapter = new ListLineAdapter(this);
			setListAdapter(adapter);
			flist.setSelection(firstVisible);
		} else Toast.makeText(this, R.string.TNothingSelected, Toast.LENGTH_SHORT).show();
	}

	private void set_chtype_listener(Dialog dlg, Button b, char t, int proc_type) {
		final Dialog dialog = dlg;
		final char type = t;
		final int id = proc_type;

		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss(); selected_type = type;
				Log.dbg("Selected type: " + type);
				switch(id) {
					case PROCEED_CALL_CONTS: 	
						callContSel();
						break;
					case PROCEED_INPUT_LIST:
						input_dialog(INP_ADD_PHONE_LIST);
						break;
					case PROCEED_CHG_ONE:
						ch_type_lpressed();
						break;
					case PROCEED_CHG_SEL:
						ch_type_selected();
						break;
					default:
						break;
				}
			}
		});
	}

	private void select_file_and_proceed(int id) {
		selected_file = null;
		Intent intent = new Intent().setClassName("com.voix", "com.voix.BWFilesBrowser");
			intent.putExtra("list_color", -1);
			startActivityForResult(intent, id);
	}
	
	private void select_type_and_proceed(int proc_type) {	   

		final Dialog dialog = new Dialog(this);
		Button btn;
		final int id = proc_type;
	
		selected_type = FContentList.TYPE_NONE;
		
		if(list_color == FContentList.BMODE) {
			dialog.setContentView(R.layout.dialog_bk_type);
			dialog.setTitle(R.string.SSelectType);
			
			btn = (Button) dialog.findViewById(R.id.ButtonH);
			set_chtype_listener(dialog,btn,FContentList.TYPE_H, id);

			btn = (Button) dialog.findViewById(R.id.ButtonM);
			set_chtype_listener(dialog,btn,FContentList.TYPE_M, id);

		} else if(list_color == FContentList.AEMODE) {
			dialog.setContentView(R.layout.dialog_over_type_aa);
			dialog.setTitle(R.string.SSelectType);
			btn = (Button) dialog.findViewById(R.id.ButtonX);
			set_chtype_listener(dialog,btn,FContentList.TYPE_X, id);
		} else {
			
			dialog.setContentView(R.layout.dialog_over_type);
			dialog.setTitle(R.string.SSelectType);
			
			btn = (Button) dialog.findViewById(R.id.ButtonR);
			set_chtype_listener(dialog,btn,FContentList.TYPE_R, id);

			btn = (Button) dialog.findViewById(R.id.ButtonA);
			set_chtype_listener(dialog,btn,FContentList.TYPE_A, id);

			btn = (Button) dialog.findViewById(R.id.ButtonN);
			set_chtype_listener(dialog,btn,FContentList.TYPE_N, id);

			btn = (Button) dialog.findViewById(R.id.ButtonI);
			set_chtype_listener(dialog,btn,FContentList.TYPE_I, id);

			btn = (Button) dialog.findViewById(R.id.ButtonX);
			if(list_color == FContentList.IEMODE) set_chtype_listener(dialog,btn,FContentList.TYPE_X, id); 
			else btn.setEnabled(false); 
		}
		
		btn = (Button) dialog.findViewById(R.id.ButtonQ);
		if(list_color == FContentList.BMODE || list_color == FContentList.IEMODE
				|| list_color == FContentList.AEMODE) {
			set_chtype_listener(dialog,btn,FContentList.TYPE_Q, id);
		} else btn.setEnabled(false);
		
		dialog.show();
	}	

	// info balloon
	// add from browser -> checkboxes
	// hw menus
	// sorting
	// search
	
	private void ctx_menu() {
		final Dialog dialog = new Dialog(this);
		Button btn;
		int dlg = 0;
		switch(list_color) {
			case FContentList.WMODE:
				dlg = R.layout.dialog_bw1; break;
			case FContentList.AEMODE:
				dlg = R.layout.dialog_bw2; break;
			default:
				dlg = R.layout.dialog_bw; break;
		}		
		dialog.setContentView(dlg);
		dialog.setTitle(R.string.SAction);

		btn = (Button) dialog.findViewById(R.id.ButtonDelete);
		btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
        	   lines.remove(lpress);
        	   phones.remove(lpress);
        	   flist.invalidateViews();
        	   if(lines.size()==0) {
        		   adapter = new ListLineAdapter(v.getContext());
        		   setListAdapter(adapter);
        	   }
        	   bwlist.dirty = true;
        	   dialog.dismiss();
           }
		});
		btn = (Button) dialog.findViewById(R.id.ButtonEdit);
		btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
               dialog.dismiss();
               input_dialog(INP_EDIT_PHONE);
           }
		});
		if(list_color == FContentList.AEMODE) {
			btn = (Button) dialog.findViewById(R.id.ButtonChFile);
			btn.setOnClickListener(new OnClickListener() {
	           public void onClick(View v) {
	               dialog.dismiss();
	               Log.dbg("calling change file()");
	               select_file_and_proceed(REQ_CODE_CHG_ONE);
	           }
			});
		} 
		if(list_color > FContentList.WMODE) {
			btn = (Button) dialog.findViewById(R.id.ButtonChType);
			btn.setOnClickListener(new OnClickListener() {
	           public void onClick(View v) {
	               dialog.dismiss();
	               Log.dbg("calling select_type_and_proceed()");
	               select_type_and_proceed(PROCEED_CHG_ONE);
	           }
			});
		}
		dialog.show();
	}
	
	private void input_dialog(int id) {	   
		
		final Dialog dialog = new Dialog(this);
		final Context context = this;
		Log.dbg("Input dialog " + id);
		
		switch(id) {
			case INP_ADD_PHONE_LIST:
				if(list_color == FContentList.AEMODE) {
					if(selected_file == null) return;
				} else if(list_color != FContentList.WMODE) {
					if(selected_type == FContentList.TYPE_NONE) return;
				}
				dialog.setContentView(R.layout.input_numbers); 
				dialog.setTitle(R.string.SAddManual);
				break;
			case INP_SAVE_TO_FILE: 
				dialog.setContentView(R.layout.input); 
				dialog.setTitle(R.string.SSaveList);
				break;
			case INP_EDIT_PHONE: 
				dialog.setContentView(R.layout.dialog_edit_number); 
				dialog.setTitle(R.string.SEdit1);
				break;
			default: return;
		}
		
		Button btn;
		final EditText edt = (EditText) dialog.findViewById(R.id.EditText);
		
		Log.dbg("input_dialog "+id);
		 
		btn = (Button) dialog.findViewById(R.id.ButtonYes);
		switch (id) {
			case INP_ADD_PHONE_LIST:	/* add phone list */
				btn.setOnClickListener(new OnClickListener() {
				  public void onClick(View v) {
					String txt = edt.getText().toString();
					txt.trim();
					if(txt.length()>1) {
						String [] phonez = txt.split("\n");
						String nic = getString(R.string.SNotInContacts);
						int n = 0;
						for(String s : phonez) {
							s.trim();
							if(phones.contains(s)) {
								Toast.makeText(v.getContext(), R.string.TAlreadyPresent, Toast.LENGTH_SHORT).show();
								continue;
							}
							if(s.length()>1) {
								String cn = ctx.findInContacts(s);
								if(cn == null) cn = nic; 
								String dn = s+"\n("+cn+")";
								if(selected_file != null) dn += '\n' + selected_file;
								lines.add(new ListLine(dn,cn,s,selected_file,selected_type));
								phones.add(s);
								n++;
							}
						}
						if(n > 0) {
							bwlist.dirty = true;
							Collections.sort(lines);
							setListAdapter(adapter);
							String s = getString(R.string.TNumsAdded);
							s += " " + n;
							Toast.makeText(v.getContext(), s, Toast.LENGTH_SHORT).show();
						} else Toast.makeText(v.getContext(), R.string.TNothingAdded, Toast.LENGTH_SHORT).show();	
					}
					Log.dbg("lines = " + lines.size());
					dialog.dismiss();
				  }
				});
				break;
			case INP_SAVE_TO_FILE:		/* save to file */
				btn.setOnClickListener(new OnClickListener() {
				  public void onClick(View v) {
					if(bwlist.dirty) saveBWlist();
					String s = edt.getText().toString().trim();
					if(s.length()==0) return;
					String name = FContentList.LIST_FILES_EXT_PREFIXES[list_color] + s;
					FContentList ff = new FContentList(name,context);
					ff.clear();
					ff.addAll(bwlist);
					ff.write();
					dialog.dismiss();
				   }
				});
				break;
			case INP_EDIT_PHONE:	/* edit single phone */
				String s = lines.get(lpress).phone;
				edt.setText(s);
				btn.setOnClickListener(new OnClickListener() {
				  public void onClick(View v) {
					String txt = edt.getText().toString();
					txt.trim();
					if(txt.length()>1) {
						String nic = getString(R.string.SNotInContacts);
						txt.trim();
						if(txt.length()>1) {
							ListLine ll = lines.get(lpress); 
							if(txt.compareTo(ll.phone)==0) return;
							String cn = ctx.findInContacts(txt);
							if(cn == null) cn = nic;
							ll.dname = txt+"\n("+cn+")";
							ll.name = cn;
							ll.phone = txt;
							bwlist.dirty = true;
							Collections.sort(lines);
							setListAdapter(adapter);
							flist.setSelection(firstVisible);
						}
					}
					dialog.dismiss();
				  }
				});
				break;
		}
		dialog.show();
	}

	private void saveBWlist() {
		bwlist.clear();
		Log.dbg("saving list, size="+lines.size());
		for(ListLine ll : lines) {
			if(list_color == FContentList.WMODE ) bwlist.add(ll.phone);
			else if(list_color == FContentList.AEMODE) bwlist.add(ll.phone+'\t'+ll.type+'\t'+ll.aa_file);
			else bwlist.add(ll.phone+'\t'+ll.type);
		}
		bwlist.write();
		setResult(1);
	}
	
	@Override
	public void finish() {
		if(bwlist.dirty) saveBWlist();
		super.finish();
	}

	@Override
	protected void  onActivityResult(int requestCode, int resultCode, Intent  data) {
	   super.onActivityResult(requestCode, resultCode, data);
	   Log.msg("onActivityResult() code="+requestCode+", result="+resultCode+", fbPath="+fbPath);
	   if(resultCode != 1) return; 
	   switch(requestCode) {
	   	case REQ_CODE_CONT:
	   		// load from contacts
	        bwlist.read();
	        adapter = new ListLineAdapter(this);
	        setAdapter();
	        flist.invalidateViews();
	        break;
	   	case REQ_CODE_LOAD:	// load from file
		   	if(fbPath == null) return;
           	String fname = "/sdcard/voix/"+BWFilesBrowser.st[list_color]+fbPath;
           	File f = new File(fname);
           	if(!f.exists() || !f.canRead()) return;
           	Log.dbg("loading new list from " + fname);
           	FContentList bwlist1 = new FContentList(fname, this);
           	bwlist1.read();
           	bwlist.clear();
           	bwlist.addAll(bwlist1);
           	bwlist.dirty = true;
  			adapter = new ListLineAdapter(this);
		   	setAdapter();
		   	break;
	   	case REQ_CODE_CALL_CONTS:
		   	if(fbPath == null) return;	
		   	selected_file = fbPath; 
		   	select_type_and_proceed(PROCEED_CALL_CONTS);
		   	//	callContSel();
		   	break;
	   	case REQ_CODE_INPUT_LIST:
		   	if(fbPath == null || list_color != FContentList.AEMODE) return;
		   	selected_file = fbPath; 
		   	//input_dialog(INP_ADD_PHONE_LIST);
		   	select_type_and_proceed(PROCEED_INPUT_LIST);
		   	break;
	   	case REQ_CODE_CHG_ONE:
		   	if(fbPath == null || list_color != FContentList.AEMODE) return;	
		   	selected_file = fbPath; 
			ch_file(lpress);
			bwlist.dirty = true;
	 	   	flist.invalidateViews();
	   		break;
	   	case REQ_CODE_CHG_SEL:
		   	if(fbPath == null || list_color != FContentList.AEMODE) return;
		   	selected_file = fbPath; 
		   	ch_file_selected();
		   	select_type_and_proceed(PROCEED_CHG_SEL);
		   	break;
		default:
			 break;
	   }
	   Log.dbg("size onActivityResult="+bwlist.size());
	   setResult(1);
	}
	
	
}
	

