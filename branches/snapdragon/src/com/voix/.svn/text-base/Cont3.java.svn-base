package com.voix;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.PhonesColumns;

@SuppressWarnings("deprecation")
public class Cont3 extends Contix {

	private ContentResolver cr;
	@Override
	public void setContentResolver(ContentResolver c) {
		cr = c;
	}

	@Override
	public String findInContacts(String phoneNumber) {
		//Log.dbg("findInContacts()");
		try {
			String[] projection = new String[] { Contacts.Phones.DISPLAY_NAME, Contacts.Phones.NUMBER };
			Uri contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(phoneNumber));
			Cursor c = cr.query(contactUri, projection, null, null, null);
			if(c.moveToFirst()) return c.getString(c.getColumnIndex(Contacts.Phones.DISPLAY_NAME));
		} catch(Exception e) { 
			Log.err("exception in findInContacts()");
			e.printStackTrace(); 
		}	
		return null;
	}
	
	
	@Override
	public Bitmap getBitmap(String s, Context ctx) {
		Log.dbg("getBitmap()");
		try {
	    	Cursor cursor = cr.query(Contacts.People.CONTENT_URI, 
	    			new String[]{Contacts.Phones._ID},
	    			"DISPLAY_NAME = '" + s + "'", null, null);
	    	if(cursor.moveToFirst()) {
	    		String contactId = cursor.getString(0); 
	    		Uri uri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, Long.parseLong(contactId));
	    		return Contacts.People.loadContactPhoto(ctx, uri, R.drawable.android_normal, null);	    
	    	}
	    } catch(Exception e) { 
			Log.err("exception in getBitmap()");
	    	e.printStackTrace(); 
	    }
	    
	    return null;
	}
	@Override
	public Uri getContactID(String s) {
		Log.dbg("getContactID()");
		try {
	    	Cursor cursor = cr.query(Contacts.People.CONTENT_URI, //null,
	    			new String[]{Contacts.Phones._ID},
	    			"DISPLAY_NAME = '" + s + "'", null, null);
	    	if(cursor.moveToFirst()) {
	    		String contactId = cursor.getString(0); //cursor.getColumnIndex(Contacts.People._ID));
	    		if(contactId != null)
	    			return ContentUris.withAppendedId(Contacts.People.CONTENT_URI, Long.parseLong(contactId));
	    	}
	    } catch(Exception e) { 
			Log.err("exception in getContactID()");
	    	e.printStackTrace(); 
	    }	
		return null;
	}

	@Override
	public ArrayList<String> getAllContacts() {
		ArrayList<String> ret = new ArrayList<String>();
	    Cursor cursor = null;
	    Log.dbg("getAllContacts()");
	    try {        
	        cursor = cr.query(Contacts.Phones.CONTENT_URI, 
	        		new String[]{PeopleColumns.DISPLAY_NAME,PhonesColumns.NUMBER}, null, null, null);
	        while(cursor.moveToNext()) {
            		ret.add(cursor.getString(0));
	            	ret.add(cursor.getString(1));
	        }
	    } catch (Exception e) {
	        	Log.err("exception in getAllContacts()");
	        	e.printStackTrace();
	    } finally {
	    	if(cursor != null) cursor.close();
	    }
		return ret;
	}
	@Override
	public ArrayList<String> getAllPhones(String name) {
		Cursor cc = null;
		ArrayList<String> a= new ArrayList<String>();
		try {
			cc  = cr.query(Contacts.Phones.CONTENT_URI, new String[]{PhonesColumns.NUMBER}, 
					"DISPLAY_NAME = '" + name + "'", null, null);
			while(cc.moveToNext()) a.add(cc.getString(0));
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
