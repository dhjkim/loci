/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\donniehk\\Desktop\\Loci2\\src\\edu\\cens\\loci\\wifiPeriodicScan\\IWifiScanListener.aidl
 */
package edu.cens.loci.wifiPeriodicScan;
public interface IWifiScanListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.cens.loci.wifiPeriodicScan.IWifiScanListener
{
private static final java.lang.String DESCRIPTOR = "edu.cens.loci.wifiPeriodicScan.IWifiScanListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.cens.loci.wifiPeriodicScan.IWifiScanListener interface,
 * generating a proxy if needed.
 */
public static edu.cens.loci.wifiPeriodicScan.IWifiScanListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.cens.loci.wifiPeriodicScan.IWifiScanListener))) {
return ((edu.cens.loci.wifiPeriodicScan.IWifiScanListener)iin);
}
return new edu.cens.loci.wifiPeriodicScan.IWifiScanListener.Stub.Proxy(obj);
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
case TRANSACTION_onWifiUpdated:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _arg0;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg0 = data.readArrayList(cl);
this.onWifiUpdated(_arg0);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.cens.loci.wifiPeriodicScan.IWifiScanListener
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
public void onWifiUpdated(java.util.List test) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeList(test);
mRemote.transact(Stub.TRANSACTION_onWifiUpdated, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onWifiUpdated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void onWifiUpdated(java.util.List test) throws android.os.RemoteException;
}
