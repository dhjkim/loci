/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\donniehk\\Desktop\\Loci2\\src\\edu\\cens\\loci\\wifiPeriodicScan\\IWifiPeriodicScanManager.aidl
 */
package edu.cens.loci.wifiPeriodicScan;
public interface IWifiPeriodicScanManager extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager
{
private static final java.lang.String DESCRIPTOR = "edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager interface,
 * generating a proxy if needed.
 */
public static edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager))) {
return ((edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager)iin);
}
return new edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager.Stub.Proxy(obj);
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
case TRANSACTION_requestWifiUpdates:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
edu.cens.loci.wifiPeriodicScan.IWifiScanListener _arg1;
_arg1 = edu.cens.loci.wifiPeriodicScan.IWifiScanListener.Stub.asInterface(data.readStrongBinder());
this.requestWifiUpdates(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_removeWifiUpdates:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
edu.cens.loci.wifiPeriodicScan.IWifiScanListener _arg1;
_arg1 = edu.cens.loci.wifiPeriodicScan.IWifiScanListener.Stub.asInterface(data.readStrongBinder());
this.removeWifiUpdates(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager
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
public void requestWifiUpdates(long minTime, edu.cens.loci.wifiPeriodicScan.IWifiScanListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(minTime);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_requestWifiUpdates, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void removeWifiUpdates(long minTime, edu.cens.loci.wifiPeriodicScan.IWifiScanListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(minTime);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeWifiUpdates, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_requestWifiUpdates = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_removeWifiUpdates = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void requestWifiUpdates(long minTime, edu.cens.loci.wifiPeriodicScan.IWifiScanListener listener) throws android.os.RemoteException;
public void removeWifiUpdates(long minTime, edu.cens.loci.wifiPeriodicScan.IWifiScanListener listener) throws android.os.RemoteException;
}
