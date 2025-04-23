package com.voix;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

public abstract class Contix {
	private static Contix contix;
	public static Contix getContix() {
		if(contix == null) {
 			try {
 				Class<? extends Contix> cls = 
 					Class.forName((Integer.parseInt(Build.VERSION.SDK) >= 5 /*Build.VERSION_CODES.ECLAIR */) 
 							? "com.voix.Cont5" : "com.voix.Cont3").asSubclass(Contix.class);
 				contix = cls.newInstance();
 			} catch (Exception e) {
 				Log.dbg("exception in getContix()");
 				e.printStackTrace();
 				throw new IllegalStateException(e);
 			}
		}
		return contix;
	}
	public abstract String findInContacts(String s);
	public abstract void setContentResolver(ContentResolver cr);
	public abstract Bitmap getBitmap(String s, Context ctx);
	public abstract Uri getContactID(String phone);
	public abstract ArrayList<String> getAllContacts();
	public abstract ArrayList<String> getAllPhones(String name); 
}
