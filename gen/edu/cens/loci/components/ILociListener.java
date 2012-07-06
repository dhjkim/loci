/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\donniehk\\workspace\\Loci2\\src\\edu\\cens\\loci\\components\\ILociListener.aidl
 */
package edu.cens.loci.components;
public interface ILociListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.cens.loci.components.ILociListener
{
private static final java.lang.String DESCRIPTOR = "edu.cens.loci.components.ILociListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.cens.loci.components.ILociListener interface,
 * generating a proxy if needed.
 */
public static edu.cens.loci.components.ILociListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.cens.loci.components.ILociListener))) {
return ((edu.cens.loci.components.ILociListener)iin);
}
return new edu.cens.loci.components.ILociListener.Stub.Proxy(obj);
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
case TRANSACTION_onLocationChanged:
{
data.enforceInterface(DESCRIPTOR);
android.location.Location _arg0;
if ((0!=data.readInt())) {
_arg0 = android.location.Location.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onLocationChanged(_arg0);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.cens.loci.components.ILociListener
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
public void onLocationChanged(android.location.Location location) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((location!=null)) {
_data.writeInt(1);
location.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onLocationChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onLocationChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void onLocationChanged(android.location.Location location) throws android.os.RemoteException;
}
