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
	private Context cntx;
		
	private class ListLine implements Comparable<ListLine> {
		public String dname;
		public String name;
		public String phone;
		boolean selected;
		char type;
		ListLine(String dispname, String cname, String tel, char t) {
			dname = dispname;
			name = cname;
			phone = tel;
			selected = false;
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
        cntx = this;
        
        Log.dbg("onCreate(): exit");
	}

	private void setAdapter() {
		Log.dbg("setAdapter(): entry, size="+bwlist.size());
			lines.clear();
			phones.clear();
			String nic = getString(R.string.SNotInContacts);
			for(int i = 0; i < bwlist.size(); i++) {
					String p = bwlist.get(i);
					char type = 0;
					if(list_color > FContentList.WMODE) {
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
						p = ss[0].trim(); 
					}
					String n = ctx.findInContacts(p);
					if(n == null) n = nic;
					String d = p + "\n(" + n + ")";
					if(!phones.contains(p)) {
						phones.add(p);
						lines.add(new ListLine(d,n,p,type));
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
        return true;
    }
	private final int REQ_CODE_LOAD	= 11;
	private final int REQ_CODE_CONT = 12;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.dbg("onOptionsItemSelected()");
		
		switch (item.getItemId()) {
			case R.id.DelSelected:
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
				} else Toast.makeText(this, R.string.TNothingSelected, Toast.LENGTH_SHORT).show();
				return true;
			case R.id.AddFromContacts:
				if(bwlist.dirty) saveBWlist();
				if(list_color > FContentList.WMODE) {
					select_type_and_proceed(0);
				} else {
					Intent intent = new Intent().setClassName("com.voix", "com.voix.ContSel");
					intent.putExtra("list_color", list_color);
					intent.putExtra("type", 0);
					startActivityForResult(intent, REQ_CODE_CONT);
				}	
				return true;
			case R.id.AddManually:
				if(list_color > FContentList.WMODE) {
					select_type_and_proceed(1);
				} else input_dialog(0);
				return true;
			case R.id.SaveList:
				input_dialog(1);
				return true;
			case R.id.LoadList:
				if(bwlist.dirty) saveBWlist();
				Intent intent = new Intent().setClassName("com.voix", "com.voix.BWFilesBrowser");
				intent.putExtra("list_color", list_color);
				startActivityForResult(intent, REQ_CODE_LOAD);
				return true;
		}
		return false; 
	}
	
	private char selected_type = 0;

	private void callContSel() {
		Log.dbg("callContSel()");
		Intent intent = new Intent().setClassName("com.voix", "com.voix.ContSel");
		intent.putExtra("list_color", list_color);
		intent.putExtra("type", selected_type);
		startActivityForResult(intent, REQ_CODE_CONT);
	}
	
	private void chtype(View v) {
		Log.dbg("changed exception type to " + selected_type);
		lines.get(lpress).type = selected_type;
		bwlist.dirty = true;
		//v.invalidate();
 	   	flist.invalidateViews();
	}
	
	private void select_type_and_proceed(int id) {	   

		final Dialog dialog = new Dialog(this);
		final int this_id = id;
		Button btn;
	
		selected_type = 0;
		
		if(list_color == FContentList.BMODE) {
			dialog.setContentView(R.layout.dialog_bk_type);
			dialog.setTitle(R.string.SOverType);
			
			btn = (Button) dialog.findViewById(R.id.ButtonH);
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss(); selected_type = FContentList.TYPE_H;
					if(this_id == 1) input_dialog(0);
					else if(this_id == 2) chtype(v);
					else callContSel();
				}
			});
			btn = (Button) dialog.findViewById(R.id.ButtonM);
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss(); selected_type = FContentList.TYPE_M;
					if(this_id == 1) input_dialog(0);
					else if(this_id == 2) chtype(v);
					else callContSel();
				}
			});
		} else {
			
			dialog.setContentView(R.layout.dialog_over_type);
			dialog.setTitle(R.string.SOverType);
			
			btn = (Button) dialog.findViewById(R.id.ButtonR);
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss(); selected_type = FContentList.TYPE_R;
					if(this_id == 1) input_dialog(0);
					else if(this_id == 2) chtype(v);
					else callContSel();
				}
			});
			btn = (Button) dialog.findViewById(R.id.ButtonA);
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss(); selected_type = FContentList.TYPE_A;
					if(this_id == 1) input_dialog(0);
					else if(this_id == 2) chtype(v);
					else callContSel();
				}
			});
			btn = (Button) dialog.findViewById(R.id.ButtonN);
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss(); selected_type = FContentList.TYPE_N;
					if(this_id == 1) input_dialog(0);
					else if(this_id == 2) chtype(v);
					else callContSel();
				}
			});
			btn = (Button) dialog.findViewById(R.id.ButtonI);
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss(); selected_type = FContentList.TYPE_I;
					if(this_id == 1) input_dialog(0);
					else if(this_id == 2) chtype(v);
					else callContSel();
				}
			});
		}
		btn = (Button) dialog.findViewById(R.id.ButtonCancel);
		btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
               dialog.dismiss();
           }
		});
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
		
		dialog.setContentView(list_color > FContentList.WMODE ? R.layout.dialog_bw : R.layout.dialog_bw1);
		dialog.setTitle(R.string.SAction);

		btn = (Button) dialog.findViewById(R.id.ButtonDelete);
		btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
        	   lines.remove(lpress);
        	   phones.remove(lpress);
        	   flist.invalidateViews();
        	   if(lines.size()==0) {
        		   adapter = new ListLineAdapter(cntx);
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
               input_dialog(2);
           }
		});
		if(list_color > FContentList.WMODE) {
			btn = (Button) dialog.findViewById(R.id.ButtonChType);
			btn.setOnClickListener(new OnClickListener() {
	           public void onClick(View v) {
	               dialog.dismiss();
	               Log.dbg("calling select_type_and_proceed()");
	               select_type_and_proceed(2);
	           }
			});
		}
		btn = (Button) dialog.findViewById(R.id.ButtonCancel);
		btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
               dialog.dismiss();
           }
		});
		dialog.show();
	}

	private void input_dialog(int id) {	   
		
		if(id == 0 && selected_type == 0) return;
		final Dialog dialog = new Dialog(this);
		final Context context = this;
		
		switch(id) {
			case 0:	
				dialog.setContentView(R.layout.input_numbers); 
				dialog.setTitle(R.string.SAddManual);
				break;
			case 1: 
				dialog.setContentView(R.layout.input); 
				dialog.setTitle(R.string.SSaveList);
				break;
			case 2: 
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
			case 0:	/* add phone list */
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
								Toast.makeText(cntx, R.string.TAlreadyPresent, Toast.LENGTH_SHORT).show();
								continue;
							}
							if(s.length()>1) {
								String cn = ctx.findInContacts(s);
								if(cn == null) cn = nic; 
								lines.add(new ListLine(s+"\n("+cn+")",cn,s,selected_type));
								n++;
							}
						}
						if(n > 0) {
							bwlist.dirty = true;
							Collections.sort(lines);
							setListAdapter(adapter);
						}	
					}
					dialog.dismiss();
				  }
				});
				break;
			case 1:		/* save to file */
				btn.setOnClickListener(new OnClickListener() {
				  public void onClick(View v) {
					if(bwlist.dirty) saveBWlist();
					String name = FContentList.LIST_FILES_EXT_PREFIXES[list_color] + edt.getText().toString().trim();
					FContentList ff = new FContentList(name,context);
					ff.clear();
					ff.addAll(bwlist);
					ff.write();
					dialog.dismiss();
				   }
				});
				break;
			case 2:	/* edit single phone */
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
		btn = (Button) dialog.findViewById(R.id.ButtonNo);
		btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
               dialog.dismiss();
           }
		});
		dialog.show();
	}

	private void saveBWlist() {
		bwlist.clear();
		Log.dbg("saving list, size="+lines.size());
		for(ListLine ll : lines) {
			//	Log.dbg("wrote " + ll.phone);
			if(list_color > FContentList.WMODE ) bwlist.add(ll.phone+'\t'+ll.type);
			else bwlist.add(ll.phone);
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
	   Log.msg("onActivityResult() code="+requestCode+", result="+resultCode);
	   if(resultCode != 1) return; 
	   if(requestCode == REQ_CODE_CONT){	// load from contacts
	        bwlist.read();
	        adapter = new ListLineAdapter(this);
	        setAdapter();
	        flist.invalidateViews();
	   } else if(requestCode == REQ_CODE_LOAD){	// load from file
		   	if(fbPath == null) return;
           	String fname = BWFilesBrowser.prefix+"/"+BWFilesBrowser.st[list_color]+fbPath;
           	File f = new File(fname);
           	if(!f.exists() || !f.canRead()) return;
           	Log.msg("loading new list from " + fname);
           	FContentList bwlist1 = new FContentList(fname, this);
           	bwlist1.read();
           	bwlist.clear();
           	bwlist.addAll(bwlist1);
           	bwlist.dirty = true;
  			adapter = new ListLineAdapter(this);
		   	setAdapter();
	   }
	   Log.dbg("size onActivityResult="+bwlist.size());
	   setResult(1);
	}
}
	

