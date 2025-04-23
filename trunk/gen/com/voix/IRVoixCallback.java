/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/kmk/android_app/source-archive/rvoix/trunk/src/com/voix/IRVoixCallback.aidl
 */
package com.voix;
public interface IRVoixCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.voix.IRVoixCallback
{
private static final java.lang.String DESCRIPTOR = "com.voix.IRVoixCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.voix.IRVoixCallback interface,
 * generating a proxy if needed.
 */
public static com.voix.IRVoixCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.voix.IRVoixCallback))) {
return ((com.voix.IRVoixCallback)iin);
}
return new com.voix.IRVoixCallback.Stub.Proxy(obj);
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
case TRANSACTION_goodbye:
{
data.enforceInterface(DESCRIPTOR);
this.goodbye();
return true;
}
case TRANSACTION_recordingComplete:
{
data.enforceInterface(DESCRIPTOR);
this.recordingComplete();
return true;
}
case TRANSACTION_encodingComplete:
{
data.enforceInterface(DESCRIPTOR);
this.encodingComplete();
return true;
}
case TRANSACTION_recordingAboutToStart:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _arg1;
_arg1 = (0!=data.readInt());
java.lang.String _arg2;
_arg2 = data.readString();
this.recordingAboutToStart(_arg0, _arg1, _arg2);
return true;
}
case TRANSACTION_recordingStarted:
{
data.enforceInterface(DESCRIPTOR);
this.recordingStarted();
return true;
}
case TRANSACTION_aarecordingStarted:
{
data.enforceInterface(DESCRIPTOR);
this.aarecordingStarted();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.voix.IRVoixCallback
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
@Override public void goodbye() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_goodbye, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void recordingComplete() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_recordingComplete, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void encodingComplete() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_encodingComplete, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void recordingAboutToStart(boolean outgoing, boolean incall, java.lang.String phone) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((outgoing)?(1):(0)));
_data.writeInt(((incall)?(1):(0)));
_data.writeString(phone);
mRemote.transact(Stub.TRANSACTION_recordingAboutToStart, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void recordingStarted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_recordingStarted, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void aarecordingStarted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_aarecordingStarted, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_goodbye = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_recordingComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_encodingComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_recordingAboutToStart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_recordingStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_aarecordingStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
}
public void goodbye() throws android.os.RemoteException;
public void recordingComplete() throws android.os.RemoteException;
public void encodingComplete() throws android.os.RemoteException;
public void recordingAboutToStart(boolean outgoing, boolean incall, java.lang.String phone) throws android.os.RemoteException;
public void recordingStarted() throws android.os.RemoteException;
public void aarecordingStarted() throws android.os.RemoteException;
}
