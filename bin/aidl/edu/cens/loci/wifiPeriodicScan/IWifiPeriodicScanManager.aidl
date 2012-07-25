package edu.cens.loci.wifiPeriodicScan;

import edu.cens.loci.wifiPeriodicScan.IWifiScanListener;

interface IWifiPeriodicScanManager
{
	void requestWifiUpdates(long minTime, in IWifiScanListener listener);
	void removeWifiUpdates(long minTime, in IWifiScanListener listener);
}