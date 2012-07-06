/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\donniehk\\workspace\\Loci2\\src\\edu\\cens\\loci\\components\\ILociManager.aidl
 */
package edu.cens.loci.components;
public interface ILociManager extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.cens.loci.components.ILociManager
{
private static final java.lang.String DESCRIPTOR = "edu.cens.loci.components.ILociManager";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.cens.loci.components.ILociManager interface,
 * generating a proxy if needed.
 */
public static edu.cens.loci.components.ILociManager asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.cens.loci.components.ILociManager))) {
return ((edu.cens.loci.components.ILociManager)iin);
}
return new edu.cens.loci.components.ILociManager.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
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
case TRANSACTION_requestLocationUpdates:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
edu.cens.loci.components.ILociListener _arg1;
_arg1 = edu.cens.loci.components.ILociListener.Stub.asInterface(data.readStrongBinder());
this.requestLocationUpdates(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_removeUpdates:
{
data.enforceInterface(DESCRIPTOR);
edu.cens.loci.components.ILociListener _arg0;
_arg0 = edu.cens.loci.components.ILociListener.Stub.asInterface(data.readStrongBinder());
this.removeUpdates(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_locationCallbackFinished:
{
data.enforceInterface(DESCRIPTOR);
edu.cens.loci.components.ILociListener _arg0;
_arg0 = edu.cens.loci.components.ILociListener.Stub.asInterface(data.readStrongBinder());
this.locationCallbackFinished(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isPlaceDetectorOn:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPlaceDetectorOn();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isPathTrackerOn:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPathTrackerOn();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isMovementDetectorOn:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isMovementDetectorOn();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_addPlaceAlert:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _arg1;
_arg1 = data.readLong();
android.app.PendingIntent _arg2;
if ((0!=data.readInt())) {
_arg2 = android.app.PendingIntent.CREATOR.createFromParcel(data);
}
else {
_arg2 = null;
}
this.addPlaceAlert(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_removePlaceAlert:
{
data.enforceInterface(DESCRIPTOR);
android.app.PendingIntent _arg0;
if ((0!=data.readInt())) {
_arg0 = android.app.PendingIntent.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.removePlaceAlert(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.cens.loci.components.ILociManager
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void requestLocationUpdates(long minTime, edu.cens.loci.components.ILociListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(minTime);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_requestLocationUpdates, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void removeUpdates(edu.cens.loci.components.ILociListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeUpdates, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void locationCallbackFinished(edu.cens.loci.components.ILociListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_locationCallbackFinished, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public boolean isPlaceDetectorOn() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPlaceDetectorOn, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean isPathTrackerOn() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPathTrackerOn, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean isMovementDetectorOn() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isMovementDetectorOn, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void addPlaceAlert(long placeid, long expiration, android.app.PendingIntent intent) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(placeid);
_data.writeLong(expiration);
if ((intent!=null)) {
_data.writeInt(1);
intent.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_addPlaceAlert, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void removePlaceAlert(android.app.PendingIntent intent) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((intent!=null)) {
_data.writeInt(1);
intent.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_removePlaceAlert, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_requestLocationUpdates = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_removeUpdates = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_locationCallbackFinished = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_isPlaceDetectorOn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_isPathTrackerOn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_isMovementDetectorOn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_addPlaceAlert = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_removePlaceAlert = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
}
public void requestLocationUpdates(long minTime, edu.cens.loci.components.ILociListener listener) throws android.os.RemoteException;
public void removeUpdates(edu.cens.loci.components.ILociListener listener) throws android.os.RemoteException;
public void locationCallbackFinished(edu.cens.loci.components.ILociListener listener) throws android.os.RemoteException;
public boolean isPlaceDetectorOn() throws android.os.RemoteException;
public boolean isPathTrackerOn() throws android.os.RemoteException;
public boolean isMovementDetectorOn() throws android.os.RemoteException;
public void addPlaceAlert(long placeid, long expiration, android.app.PendingIntent intent) throws android.os.RemoteException;
public void removePlaceAlert(android.app.PendingIntent intent) throws android.os.RemoteException;
}
