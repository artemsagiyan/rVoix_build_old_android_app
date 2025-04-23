package com.voix;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class Browser extends ListActivity {

 
    private final ArrayList<ListLine> recordings = new ArrayList<ListLine>();
    private final FContentList favs = new FContentList(FContentList.FAVS_FILE, this);
    public  static final String voixdir = "/sdcard/voix";
	public  static final String favsFile = "/sdcard/voix/.favourites";
	public  static final int dirlen = voixdir.length() + 1;  // + extra slash at the end
	private ListLineAdapter adapter;
	private ListView flist;
	private String filter = null;
	private boolean filter_only_incoming = false;
	private boolean filter_only_outgoing = false;
	private boolean filter_reverse_time = false;
	
	private boolean ac_search = false;
	private boolean search_results = false;
	private final ArrayList<String> ac_items = new ArrayList<String>();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.dbg("onCreate(): entry");
        Intent intie = getIntent();
        
        ac_search = intie.getBooleanExtra("ac_search",false);
        if(ac_search) {
        	  Log.dbg("onCreate(): loading search layout");
        	  setContentView(R.layout.ac_list);
        	  AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.autocomplete);
        	  getAcList();
        	  ArrayAdapter<String> ac_adapter = new ArrayAdapter<String>(this, R.layout.ac_list_item, ac_items);
        	  textView.setAdapter(ac_adapter);
        	  textView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
	        		Intent intent = new Intent().setClassName("com.voix", "com.voix.Browser");
	        		TextView txt = (TextView) v;
	        		String fname = ""+txt.getText();
	        		intent.putExtra("filter", fname);
	        		intent.putExtra("search_results", true);
	        		finish();
	        		Log.dbg("restarting with "+filter);
	        		startActivityForResult(intent,1);
	        		return;
				}}
        	  );
        	  flist = (ListView) findViewById(android.R.id.list);
        	  setListAdapter(ac_adapter);
        	  // android.R.layout.simple_list_item_2
        	  Log.dbg("onCreate(): exit");
        	  return;
        }
        Log.dbg("onCreate(): loading standard layout");
        setContentView(R.layout.list);
        flist = (ListView) findViewById(android.R.id.list);
        flist.setOnItemLongClickListener(longClick);
        flist.setOnScrollListener(sScr);
        favs.read();
        
        filter = intie.getStringExtra("filter");
        filter_only_incoming = intie.getBooleanExtra("filter_only_incoming",false);
        filter_only_outgoing = intie.getBooleanExtra("filter_only_outgoing",false);
        filter_reverse_time = intie.getBooleanExtra("filter_reverse_time",false);
        search_results = intie.getBooleanExtra("search_results",false);
        
        adapter = new ListLineAdapter(this);
        setAdapter(filter);
        Log.dbg("onCreate(): exit");
	}
	
	void setAdapter(String flt) {
		Log.dbg("setAdapter(): entry");
		File[] files = (new File(voixdir)).listFiles();
		recordings.clear();
	  
	  try {	
		for(int i = 0; i < files.length; i++) {
			String file = files[i].toString();
			if(file.length() < dirlen+18) continue;
			boolean isfav = favs.contains(file);
			if(file.endsWith(".wav") || file.endsWith(".mp3")) {
				long mtime = files[i].lastModified();
				long fsize = files[i].length();
				if(flt != null && !file.substring(dirlen+13).startsWith(flt)) continue;
				if(file.substring(dirlen).startsWith("O-")) { 
					if(filter_only_incoming) continue;
					recordings.add(new ListLine(trim(file,fsize),file,isfav,R.drawable.outgoing, fsize, mtime));
				} else if(file.substring(dirlen).startsWith("I-")) { 
					if(filter_only_outgoing) continue;
					recordings.add(new ListLine(trim(file,fsize),file,isfav,R.drawable.incoming, fsize, mtime));
				} else if(file.substring(dirlen).startsWith("A-")) {
					if(filter_only_outgoing) continue;
					recordings.add(new ListLine(trim(file,fsize),file,isfav,R.drawable.aaa, fsize, mtime));
				}
			}
		}
		if(recordings.size()> 0) {
			Collections.sort(recordings);			
			setListAdapter(adapter);	
		}
	  } catch (Exception e){
		  Log.err("exception in setAdapter()");
		  e.printStackTrace();
		  return;
	  }
	  Log.dbg("setAdapter(): exit");
	}
	
	void getAcList() {
		File[] files = (new File(voixdir)).listFiles();
		ac_items.clear();
		Log.dbg("getAcList(): entry");
		if(files == null) return;
		for(int i = 0; i < files.length; i++) {
			String file = files[i].toString();
			if(file.endsWith(".wav") || file.endsWith(".mp3")) {
				if(file.charAt(dirlen+1) == '-' && (file.charAt(dirlen)=='I' 
					|| file.charAt(dirlen)=='O' || file.charAt(dirlen)=='A')) { 
					String s = ac_trim(file);
					if(ac_items.contains(s)) continue;
					ac_items.add(s);
				}	
			}
		}
		if(ac_items.size()> 0) {
			Collections.sort(ac_items);			
		}
		Log.dbg("getAcList(): exit");
	}
	
	void resetAdapter(boolean full) {
		Log.dbg("resetAdapter(): entry");
		adapter = new ListLineAdapter(this);
		if(full) {
			Log.dbg("adapter: full reset");
			favs.read();
			adapter = new ListLineAdapter(this);
			setAdapter(filter);
			flist.invalidateViews();
			firstVisible = 0;
		} else {
			Log.dbg("adapter: partial reset");
			setListAdapter(adapter);
		}
		flist.setSelection(firstVisible);
		Log.dbg("resetAdapter(): exit");
	}
	
	///////////////////////
	public class ListLine implements Comparable<ListLine> {
		public String s;
		public String fname;
		public boolean is_fav;
		public int img_res;
		public long size;
		public long last_mod;
		ListLine(String dispname, String file, boolean fav, int img, long sz, long lm) {
			s = dispname; fname = file; is_fav = fav; img_res = img;
			last_mod = lm; size = sz;
		}
		@Override
		public int compareTo(ListLine another) {
			if(last_mod > another.last_mod) return filter_reverse_time ? 1 : -1;
			else if(last_mod < another.last_mod) return filter_reverse_time ? -1 :1;
			else return 0;
		}  
	}
	public class ListLineAdapter extends ArrayAdapter<ListLine> {
		public ListLineAdapter(Context context) {
			super(context, R.layout.list_item, recordings);
		}
		@Override
		public View getView(int i, View convertView, ViewGroup parent) { 
			View row = convertView;
			ListLine ll = recordings.get(i);
			if(row == null) row = getLayoutInflater().inflate(R.layout.list_item,(ViewGroup)findViewById(R.id.LayoutRoot));
			TextView txt = (TextView) row.findViewById(R.id.label);
			txt.setText(ll.s);
			((ImageView)row.findViewById(R.id.icon)).setImageResource(ll.img_res);
			row.setId(i);
			if(!ll.is_fav) row.setBackgroundColor(0xFF000000);
			else row.setBackgroundColor(0xFF800000);
			txt.setTextColor(0xFFFFFFFF);
			return row; 
		}
	}
	///////////////////////	

	/////////////////////// Click listeners && dialogs
	@Override
	public  void onListItemClick(ListView parent, View v, int pos, long id) { 
		Log.dbg("list item clicked");
		try {
        	if(ac_search) {
        		Intent intie = new Intent().setClassName("com.voix", "com.voix.Browser");
        		TextView txt = (TextView) v;
        		String fname = ""+txt.getText(); //ac_items.get((int)id);
        		intie.putExtra("filter", fname);
        		intie.putExtra("search_results", true);
        		finish();
        		Log.dbg("restarting with "+filter);
        		startActivityForResult(intie,1);
        		return;
        	}
        	String f = recordings.get(pos).fname;
        	Intent intie = new Intent(Intent.ACTION_VIEW); 
        	intie.setDataAndType(Uri.fromFile(new File(f)), f.endsWith("wav") ? "audio/wav" : "audio/mp3");
        	Log.msg("start playing " + f);
        	if(favs.dirty) favs.write();
        	startActivity(intie);
        } catch (Exception e) {
        	Log.err("exception in onListItemClick");
        	e.printStackTrace();
        }
	}  
	private int lpress = 0; 
	AdapterView.OnItemLongClickListener longClick= new OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> a, View v, int i, long k) {
       	 	Log.dbg("long click detected");
        	lpress = (int) k;
        	if(lpress >= recordings.size()) return false;
       	 	boolean fav = recordings.get(lpress).is_fav;
       	 	ctxMenu(fav);
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
   
   void ctxMenu(boolean isfav) {	   
	   final Dialog dialog = new Dialog(this);
	   final boolean fav = isfav;
	//  dialog.setContentView(fav ? R.layout.dialog_fav : 
	//	   (isnum ? R.layout.dialog_phone : R.layout.dialog));
	   
	// dialog.setContentView(isnum ? R.layout.dialog_phone : R.layout.dialog);
	   
	   dialog.setContentView(R.layout.dialog_phone);
	   
	   dialog.setTitle(R.string.SAction);
	   Button btn;
	   Log.dbg("in ctxMenu()");
	   btn = (Button) dialog.findViewById(R.id.ButtonMark);
	   btn.setText(fav ? R.string.SUnMarkFav:R.string.SMarkFav);
	   btn.setOnClickListener(new OnClickListener() {
   			public void onClick(View v) {
   				if(!fav) {
	   				Log.msg("adding " + lpress +" to favs");
	   				favs.add(recordings.get(lpress).fname);
	   				recordings.get(lpress).is_fav = true;
   				} else {
		       		Log.msg("removing " + lpress +" from favs");
		       		favs.remove(recordings.get(lpress).fname);
		       		recordings.get(lpress).is_fav = false;
   				}
   				resetAdapter(false);
   				favs.dirty = true;
   				setResult(1);
   				dialog.dismiss();
   			}
   	   });
	   
	   btn = (Button) dialog.findViewById(R.id.ButtonBlack);
	   btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
        	   dialog.dismiss();
        	   add_to_list();
           }
	   });
	   
	   btn = (Button) dialog.findViewById(R.id.ButtonFilter);
	   if(filter == null) btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
               dialog.dismiss();
               Intent intie = new Intent().setClassName("com.voix", "com.voix.Browser");
               Log.dbg("click on filter");
               try {
            	   String fname = recordings.get(lpress).fname.substring(dirlen+13);
            	   fname = fname.substring(0, fname.length()-4);
            	   fname = cut_chunk(fname);
            	   Log.msg("starting myself with filter " + fname);
            	   intie.putExtra("filter", fname);
            	   intie.putExtra("filter_only_incoming", filter_only_incoming);
            	   intie.putExtra("filter_only_outgoing", filter_only_outgoing);
            	   intie.putExtra("filter_reverse_time", filter_reverse_time);
            	   if(favs.dirty) favs.write();
            	   startActivityForResult(intie,1);
               } catch(Exception e) {
            	   Log.msg("Error in filter");
            	   e.printStackTrace();
               }
               Log.dbg("click on filter processed");
           }
	   }); else btn.setEnabled(false);
	   
	   btn = (Button) dialog.findViewById(R.id.ButtonCall);
	   btn.setOnClickListener(new OnClickListener() {
           public void onClick(View v) {
        	   //startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mEditText_number.getText())));
        	   Log.dbg("click on call");
        	   dialog.dismiss();
        	   	try {
               	 //  	   String fname = recordings.get(lpress).fname.substring(dirlen+13);        		   
        	   	   String sss = recordings.get(lpress).fname;
        	  	   String fname = (sss.charAt(dirlen+12) == '+') ? sss.substring(dirlen+12) : sss.substring(dirlen+13);
            	   fname = fname.substring(0, fname.length()-4);
            	   fname = cut_chunk(fname);
            	   if(fname.contains("#")) fname = fname.replace('#', '*');
            	   Contix ctxx = Contix.getContix();
                   ctxx.setContentResolver(getContentResolver());
            	   Uri uri = ctxx.getContactID(fname);
            	   if(favs.dirty) favs.write();
            	   if(uri != null) startActivityForResult(new Intent(Intent.ACTION_VIEW, uri),2);
            	   else startActivityForResult(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + fname)),2);
        		} catch(Exception e) {
        			e.printStackTrace();
        			//Log.msg("Error in filter");
               }
        		Log.dbg("click on call processed"); 	
           }
	   });
	   
	   btn = (Button) dialog.findViewById(R.id.ButtonDelete);
	   if(!fav) {
	   		btn.setOnClickListener(new OnClickListener() {
	   			public void onClick(View v) {
	   				Log.dbg("click on delete");
	   				(new File(recordings.get(lpress).fname)).delete();
	   				recordings.remove(lpress);
	   				resetAdapter(false);
	   				dialog.dismiss();
	   				Log.dbg("click on delete processed");
	   				setResult(1);
	   			}
	   		});
	   } else btn.setEnabled(false);
	   dialog.show();
   }
   @Override	
   protected void  onActivityResult  (int requestCode, int resultCode, Intent  data) {
	   super.onActivityResult(requestCode, resultCode, data);
	   Log.dbg("onActivityResult() code="+requestCode+", result=" +resultCode);
	   resetAdapter(requestCode == 2 || resultCode == 1);
   }
   
/////////////////////// Helper functions
   @SuppressWarnings("boxing")
   String trim(String file, long sz) {
		String s;
		long size = sz;
		try {
			String tm;
			if(file.endsWith("wav") && size != 0) {
				if(file.charAt(dirlen) == 'A') size = (size-44)/(8000*2);
				else size = (size-44)/(8000*4);
			} else {
				FileInputStream fip = new FileInputStream(new File(file));
				try {
					byte[] bb = new byte[12];
					fip.skip(size-195);
					fip.read(bb, 0, 12);
					if(bb[0]=='X' && bb[1] == 'i' && bb[2] == 'n' && bb[3]== 'g') {
						size =	((bb[8] & 0xff) << 24) + ((bb[9]& 0xff) << 16) + ((bb[10]& 0xff) << 8) + (bb[11]& 0xff);
						size = (size*576)/11025;
					} else size = size/8000;	
				} catch(Exception e) {
					e.printStackTrace();
					Log.err("exception in trim");
					size = size/8000;
				} finally {
					fip.close();
				}
			}
			tm = String.format("%02d:%02d", size/60, size % 60);
			s = file.substring(dirlen+2,file.length()-4); // skip "I-" or "O-" 
			s = s.substring(0,2) + "/" + s.substring(3,5)+ " - " 
				+ s.substring(6,8) + ":" +s.substring(8,10) + " - [" + tm + "]\n" 
				+ ((s.charAt(10) == '+') ? s.substring(10) : s.substring(11));
		} catch (Exception e) {
			Log.err("exception in trim()");
			e.printStackTrace();
			return file;
		}
		return s;
	}
	String cut_chunk(String s) {
		int len = s.length();
		try {
			if(len >= 3 && s.charAt(len-2)=='-' && s.charAt(len-1) >= '0' 
				&& s.charAt(len-1) <= '9') return s.substring(0, len-2);
			else if(len >= 4 && s.charAt(len-3)=='-' && s.charAt(len-1) >= '0' 
				&& s.charAt(len-1) <= '9' && s.charAt(len-2) >= '0' 
					&& s.charAt(len-2) <= '9') return s.substring(0, len-3);
		} catch (Exception e) {
			Log.err("exception in cut_chunk()");
			e.printStackTrace();
			return s;
		}
		return s;
	}
	String ac_trim(String file) {
		String s = file.substring(dirlen+13,file.length()-4); // skip "I-" or "O-"
		s = cut_chunk(s);
		return s;
	}
		
	@Override
	public void finish() {
		Log.dbg("finish()");
		super.onPause();
		if(favs.dirty) {
			favs.write();
			setResult(1);
		}
		super.finish();
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if(filter == null) inflater.inflate(R.menu.mmenu, menu);
        else inflater.inflate(R.menu.mmenu1, menu);
        Log.dbg("onCreateOptionsMenu()");
        return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intie = new Intent().setClassName("com.voix", "com.voix.Browser");    
  	   	intie.putExtra("filter", filter);
  	   	Log.dbg("onOptionsItemSelected()");
        switch (item.getItemId()) {
            case R.id.IncomingOnly:
             	if(filter_only_incoming) return false;   
            	intie.putExtra("filter_only_incoming", true);
             	intie.putExtra("filter_only_outgoing", false);
             	intie.putExtra("filter_reverse_time", filter_reverse_time);
                Log.dbg("filtering incoming only");
             	break;
            case R.id.OutgoingOnly:
            	if(filter_only_outgoing) return false;   
            	intie.putExtra("filter_only_incoming", false);
             	intie.putExtra("filter_only_outgoing", true);
             	intie.putExtra("filter_reverse_time", filter_reverse_time);
             	Log.dbg("filtering outgoing only");
             	break;
            case R.id.ReverseTime:
            	if(filter_reverse_time) return false;
            	intie.putExtra("filter_only_incoming", filter_only_incoming);
          	   	intie.putExtra("filter_only_outgoing", filter_only_outgoing);
          	   	intie.putExtra("filter_reverse_time", true);
          	   	Log.dbg("filtering back in time");
          	   	break;
            case R.id.SearchX:   	
            	intie.putExtra("ac_search", true);
            	Log.dbg("starting search dlg");
            	break;
            case R.id.DeleteAll:
            	if(favs.dirty) favs.write();
            	for(int i=0; i < recordings.size(); i++) {
            		ListLine ll = recordings.get(i);
            		if(!ll.is_fav) (new File(ll.fname)).delete();
            	}
            	setResult(1);
            	finish();
            	return true;
            default:
            	return false;
        }
        try {
        	if(favs.dirty) favs.write();
      	   	if(ac_search || search_results) {
      		   finish();
      		   startActivity(intie);
      	   	} else startActivityForResult(intie,1);
        } catch(Exception e) {
        	Log.msg("Error in filter");
      	   	e.printStackTrace();
        }		
        return true;
    }
	
    private char selected_type = FContentList.TYPE_NONE;
    private int list_color = FContentList.BMODE;

	private void set_button1(Dialog dlg, int id, int val) {
		final Button btn = (Button) dlg.findViewById(id);
		final int color = val;
		final Dialog dialog = dlg;
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss(); list_color = color;
				if(list_color == FContentList.WMODE) insert_to_list();
				else select_type_and_proceed();
			}
		});
	}
    private void add_to_list() {
    	final Dialog dialog = new Dialog(this);
		list_color = FContentList.BMODE;
		dialog.setContentView(R.layout.sel_lnames);
		dialog.setTitle(R.string.SSelList);
		dialog.setTitle(R.string.SOverType);
		set_button1(dialog, R.id.ButtonWlist, FContentList.WMODE);
		set_button1(dialog, R.id.ButtonBlist, FContentList.BMODE);
		set_button1(dialog, R.id.ButtonIElist, FContentList.IEMODE);
		set_button1(dialog, R.id.ButtonOElist, FContentList.OEMODE);
		dialog.show();
    }

    private void set_button2(Dialog dlg, int id, char t) {
		final Button btn = (Button) dlg.findViewById(id);
		final char type = t;
		final Dialog dialog = dlg;
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				selected_type = type;
				insert_to_list();
			}
		});
	}
    private void select_type_and_proceed() {	   

    	final Dialog dialog = new Dialog(this);
		selected_type = FContentList.TYPE_NONE;
		
		if(list_color == FContentList.BMODE) {
			dialog.setContentView(R.layout.dialog_bk_type);
			dialog.setTitle(R.string.SOverType);
			set_button2(dialog, R.id.ButtonH, FContentList.TYPE_H);
			set_button2(dialog, R.id.ButtonM, FContentList.TYPE_M);
		} else {
			dialog.setContentView(R.layout.dialog_over_type);
			dialog.setTitle(R.string.SOverType);
			set_button2(dialog, R.id.ButtonR, FContentList.TYPE_R);
			set_button2(dialog, R.id.ButtonA, FContentList.TYPE_A);
			set_button2(dialog, R.id.ButtonN, FContentList.TYPE_N);
			set_button2(dialog, R.id.ButtonI, FContentList.TYPE_I);
		}	
		if(list_color == FContentList.OEMODE) {
			dialog.findViewById(R.id.ButtonQ).setEnabled(false);
			dialog.findViewById(R.id.ButtonX).setEnabled(false);
		} else {
			set_button2(dialog, R.id.ButtonQ, FContentList.TYPE_Q);
			if(list_color != FContentList.BMODE) set_button2(dialog, R.id.ButtonX, FContentList.TYPE_X);
		}
		
		dialog.show();
	}
    private void insert_to_list() {
    	FContentList fc = new FContentList(FContentList.LIST_FILES[list_color],this);
 	   	int items_added = 0;    	
 	   	fc.read();
 	   	char tt = list_color != FContentList.WMODE ? FContentList.TYPE_Z : FContentList.TYPE_NONE;
 	   	ArrayList <String> a = fc.get_array(tt);

 	   	String number = recordings.get(lpress).fname.substring(dirlen+13);
 	   	number = number.substring(0, number.length()-4);
 	   	number = cut_chunk(number); 
 	   	Contix ctxx = Contix.getContix();
 	   	ctxx.setContentResolver(getContentResolver());
 	   	ArrayList<String>numbers =ctxx.getAllPhones(number); 
 	   	int nn = numbers.size();
 	   	if(nn > 0) {	// in contacts
 	   		for(int i= 0; i < nn; i++) 
 	   			items_added += insert_single_number(numbers.get(i),a,fc);
 	   		if(nn==1) {
 	   			Toast.makeText(this, items_added == 1 ? 
 	   				R.string.TNumAdded : R.string.TAlreadyPresent, Toast.LENGTH_SHORT).show();
 	   		} else {
 	   			String ss = getString(R.string.TNumsAdded) + " " + items_added;
 	   			if(items_added < nn) 
 	   				ss += ", " + (nn-items_added) + " " + getString(R.string.TAlreadyPresents); 
 	   			Toast.makeText(this, ss, Toast.LENGTH_LONG).show();
 	   		}
 	   	} else {		// this must be a number
 	   		number = recordings.get(lpress).fname.substring(dirlen+12);
 	 	   	number = number.substring(0, number.length()-4);
 	 	   	number = cut_chunk(number); 
 	   		items_added += insert_single_number(number,a,fc);
 	   		Toast.makeText(this, items_added == 1 ? 
	 	   			R.string.TNumAdded : R.string.TAlreadyPresent, Toast.LENGTH_SHORT).show();
 	   	}
 	   	if(items_added > 0) {
 	   		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);  
 	   		List<ActivityManager.RunningServiceInfo> ls = am.getRunningServices(1000);
 	   		for(int i = 0; i < ls.size(); i++) {
			   if(ls.get(i).process.compareTo("com.voix:remote")==0) {
				   Intent intent = new Intent().setClassName("com.voix", "com.voix.RVoixSrv");
				   if(startService(intent)!= null) Log.dbg("service restarted");
				   break;
			   }
 	   		}
 	   	}	
    }

    private int insert_single_number(String num, ArrayList <String> a, FContentList fc) {
 	   	String number = new String(num);
    	if(!a.contains(number)) {
 	   		Log.dbg("Addnig " + number + " to list " + list_color);
 	   		if(list_color != FContentList.WMODE) number = number + '\t'+selected_type; 
 	   		fc.add(number);
 	   		fc.write();
 	   		return 1;
    	} 
    	return 0;
    }

}



