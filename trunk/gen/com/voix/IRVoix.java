/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/kmk/android_app/source-archive/rvoix/trunk/src/com/voix/IRVoix.aidl
 */
package com.voix;
public interface IRVoix extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.voix.IRVoix
{
private static final java.lang.String DESCRIPTOR = "com.voix.IRVoix";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.voix.IRVoix interface,
 * generating a proxy if needed.
 */
public static com.voix.IRVoix asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.voix.IRVoix))) {
return ((com.voix.IRVoix)iin);
}
return new com.voix.IRVoix.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_force_record:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.force_record(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_call_telephony:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.call_telephony(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_is_recording:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.is_recording();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_start_rec:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.start_rec();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_stop_rec:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.stop_rec();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_wait_confirm:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.wait_confirm(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_cancel_autoanswer:
{
data.enforceInterface(DESCRIPTOR);
this.cancel_autoanswer();
reply.writeNoException();
return true;
}
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
com.voix.IRVoixCallback _arg0;
_arg0 = com.voix.IRVoixCallback.Stub.asInterface(data.readStrongBinder());
this.registerCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterCallback:
{
data.enforceInterface(DESCRIPTOR);
com.voix.IRVoixCallback _arg0;
_arg0 = com.voix.IRVoixCallback.Stub.asInterface(data.readStrongBinder());
this.unregisterCallback(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.voix.IRVoix
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void force_record(boolean r) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((r)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_force_record, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void call_telephony(java.lang.String s) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(s);
mRemote.transact(Stub.TRANSACTION_call_telephony, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean is_recording() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_is_recording, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean start_rec() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_start_rec, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean stop_rec() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stop_rec, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void wait_confirm(int r) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(r);
mRemote.transact(Stub.TRANSACTION_wait_confirm, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void cancel_autoanswer() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_cancel_autoanswer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void registerCallback(com.voix.IRVoixCallback c) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((c!=null))?(c.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void unregisterCallback(com.voix.IRVoixCallback c) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((c!=null))?(c.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_force_record = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_call_telephony = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_is_recording = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_start_rec = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_stop_rec = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_wait_confirm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_cancel_autoanswer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
}
public void force_record(boolean r) throws android.os.RemoteException;
public void call_telephony(java.lang.String s) throws android.os.RemoteException;
public boolean is_recording() throws android.os.RemoteException;
public boolean start_rec() throws android.os.RemoteException;
public boolean stop_rec() throws android.os.RemoteException;
public void wait_confirm(int r) throws android.os.RemoteException;
public void cancel_autoanswer() throws android.os.RemoteException;
public void registerCallback(com.voix.IRVoixCallback c) throws android.os.RemoteException;
public void unregisterCallback(com.voix.IRVoixCallback c) throws android.os.RemoteException;
}
