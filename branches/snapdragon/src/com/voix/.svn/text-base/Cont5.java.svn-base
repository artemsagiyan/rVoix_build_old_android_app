package com.voix;

import java.io.InputStream;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

public class Cont5 extends Contix {

	private ContentResolver cr;
	
	@Override
	public void setContentResolver(ContentResolver c) {
		cr = c;
	}

	@Override
    public String findInContacts(String phoneNumber) {
        String name = null;
        Cursor cursor = null;
        //Log.dbg("findInContacts()");
        try {        
        	Uri ci = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
        		Uri.encode(phoneNumber));
        	cursor = cr.query(ci,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

            if(cursor.moveToFirst()) name = (cursor.getString(0));
        } catch (Exception e) {
        	Log.err("exception in findInContacts()");
        	e.printStackTrace();
        } finally {
            if(cursor != null) cursor.close();
        }
        return name;
    }

	@Override
	public Bitmap getBitmap(String s, Context ctx) {
		Bitmap bm = null;
		Log.dbg("getBitmap()");
		try {
			InputStream istr = null;
		    Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, 
		    		new String[]{BaseColumns._ID},	"DISPLAY_NAME = '" + s + "'", null, null);
		    if(cursor.moveToFirst()) {
		        String contactId = cursor.getString(0); //cursor.getColumnIndex(ContactsContract.Contacts._ID));
		        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
		            Long.parseLong(contactId));
		        istr = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
				if(istr != null) {
					BitmapDrawable bmd = new BitmapDrawable(istr);
					bm = bmd.getBitmap();
					istr.close();
					istr = null;
				}
		    }
			cursor.close();
		} catch (Exception e) {
			Log.err("exception in getBitmap()");
			e.printStackTrace();
			bm = null;
		}
		return bm;
	}

	@Override
	public Uri getContactID(String phone) {
		Log.dbg("getContactID()");
		try {
			String contactId  = null;
		    Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, 
		    		new String[]{BaseColumns._ID},	"DISPLAY_NAME = '" + phone + "'", null, null);
		    if(cursor.moveToFirst()) contactId = cursor.getString(0);
			cursor.close();
			if(contactId != null) return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
		} catch (Exception e) { 
			Log.err("exception in getContactID()");
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public ArrayList<String> getAllContacts() {
		ArrayList<String> ret = new ArrayList<String>();
	    Cursor cursor = null;
	    Cursor cursor1 = null;
        Log.dbg("getAllContacts()");
	    try {        
	        cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, 
	        		new String[]{BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME,ContactsContract.Contacts.HAS_PHONE_NUMBER}, 
	        		null, null, null);
	        while(cursor.moveToNext()) {
	        	if(cursor.getInt(2) > 0) {
	        		String id  = (cursor.getString(0));
	            	String name = (cursor.getString(1));
	            	//Log.dbg("found: name=" +name+", id=" + id);	
	            	cursor1 = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
	    	        		new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, 
	    	        		ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id, null, null);
	            	while(cursor1.moveToNext()) {
		            	String phone = cursor1.getString(0);
	            		ret.add(name);
		            	ret.add(phone);
		            	//Log.dbg(">>>>>> phone=" + phone);
	            	}
	            }	
	        }
	    } catch (Exception e) {
	        	Log.err("exception in getAllContacts()");
	        	e.printStackTrace();
	    } finally {
	    	if(cursor1 != null) cursor1.close();
	    	if(cursor != null) cursor.close();
	    }
		return ret;
	}
	@Override
	public ArrayList<String> getAllPhones(String name) {
		ArrayList<String> a= new ArrayList<String>();
        Cursor cc = null;
        try {
        	cc = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
       		new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, "DISPLAY_NAME = '" + name + "'", null, null);
        	while(cc.moveToNext()) {
        		a.add(cc.getString(0));
        	}
        	cc.close();
        } catch (Exception e) {
        	Log.err("exception in getAllPhones()");
        	e.printStackTrace();
        } finally {
	    	if(cc != null) cc.close();
	    }
        return a;
	}

}
