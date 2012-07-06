package edu.cens.loci.classes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.net.wifi.ScanResult;
import edu.cens.loci.classes.LociWifiFingerprint.APInfoMapItem;

public class WifiScanBuffer {
	
	//private static final String TAG = "WifiScanBuffer";
	//private static final boolean LOG_TEST = true;
	
	private int mBufSize;
	private List<ScanResult> [] mScans;
	
	private int mCursor;
	private long mLastTime;
	
	private HashSet<String> mBssids = new HashSet<String>();
	
	@SuppressWarnings("unchecked")
	public WifiScanBuffer(int bufSize) {
		mBufSize = bufSize;
		mScans = new List[mBufSize];
		clear();
	}
	
	public void clear() {
		for (int i=0; i<mBufSize; i++)
			mScans[i] = null;
		mScanRssSum.clear();
		mScanCnt.clear();
		mCursor = 0;
		mLastTime = -1;
	}
	
	public void updateScanBuffer(long date, List<ScanResult> scan) {

		mLastTime = date;
		mScans[mCursor] = scan;
		this.updateCursor();
		
	}
	
	public String toString() {
		return String.format("WifiScanBuffer: #APs=%d", getBssids().size());
	}
	
	public HashMap<String, APInfoMapItem> getAPInfoMap() {
		
		HashMap<String, APInfoMapItem> map = new HashMap<String, APInfoMapItem>();
		
		mScanRssSum.clear();
		mScanCnt.clear();
		
		int cnt = 0;
		int cntAdhoc = 0;
		
		for (int i=0;i<mBufSize;i++) {
			if (mScans[i] != null) {
				for (ScanResult scan: mScans[i]) {
					// filter ad-hoc APs (that has the first two bytes as 00)
					if (scan.BSSID.substring(0, 2).equals("00")) {
						if (mScanRssSum.containsKey(scan.BSSID)) {
							int newRss = mScanRssSum.get(scan.BSSID) + scan.level;
							mScanRssSum.put(scan.BSSID, newRss);
							int newCnt = mScanCnt.get(scan.BSSID) + 1;
							mScanCnt.put(scan.BSSID, newCnt);
							int avg = newRss / newCnt;
							map.put(scan.BSSID, new APInfoMapItem(avg, scan.SSID));
						} else {
							mScanRssSum.put(scan.BSSID, scan.level);
							mScanCnt.put(scan.BSSID, 1);
							//scanRssAvg.put(scan.BSSID, scan.level);
							map.put(scan.BSSID, new APInfoMapItem(scan.level, scan.SSID));
							cnt++;
						}
					} else {
						cntAdhoc++;
					}
				}
			}
		}
		
		return map;
	}
	
	public HashSet<String> getBssids() {

		mBssids.clear();
		
		for (int i=0; i<mBufSize; i++) {
			
			if (mScans[i] != null) {
				for (ScanResult scan: mScans[i]) {
					if (!mBssids.contains(scan.BSSID))
						mBssids.add(scan.BSSID);
					
					//Log.d(TAG, "AP:" + scan.SSID + " RSSI:" + scan.level + "\n");
				}
			}
		}
		return mBssids;
	}
	
	// updates the ssid map
	public HashMap<String, String> getSsidMap() {
		
		HashMap<String, String> ssidMap = new HashMap<String, String>();
		 
		int cnt = 0;
		
		for (int i=0; i<mBufSize; i++) {
			if (mScans[i] != null) {
				for (ScanResult scan: mScans[i]) {
					if (!ssidMap.containsKey(scan.BSSID)) {
						ssidMap.put(scan.BSSID, scan.SSID);
						cnt++;
						//Log.d(TAG, "AP:" + scan.SSID + " RSSI:" + scan.level + "\n");
					}
				}
			}
		}
		
		//MyLog.i(LOG_TEST, TAG, "getSsidMap: #APs = " + cnt);
		return ssidMap;
	}
	
	private HashMap<String, Integer> mScanRssSum = new HashMap<String, Integer>();
	private HashMap<String, Integer> mScanCnt = new HashMap<String, Integer>();
	
	
	public HashMap<String, Integer> getAvgRssBuf() {
		
		HashMap<String, Integer> scanRssAvg = new HashMap<String, Integer>();
		mScanRssSum.clear();
		mScanCnt.clear();
		
		int cnt = 0;
		int cntAdhoc = 0;
		
		for (int i=0;i<mBufSize;i++) {
			if (mScans[i] != null) {
				for (ScanResult scan: mScans[i]) {
					// filter ad-hoc APs (that has the first two bytes as 00)
					if (scan.BSSID.substring(0, 2).equals("00")) {
						if (mScanRssSum.containsKey(scan.BSSID)) {
							int newRss = mScanRssSum.get(scan.BSSID) + scan.level;
							mScanRssSum.put(scan.BSSID, newRss);
							int newCnt = mScanCnt.get(scan.BSSID) + 1;
							mScanCnt.put(scan.BSSID, newCnt);
							int avg = newRss / newCnt;
							scanRssAvg.put(scan.BSSID, avg);
						} else {
							mScanRssSum.put(scan.BSSID, scan.level);
							mScanCnt.put(scan.BSSID, 1);
							scanRssAvg.put(scan.BSSID, scan.level);
							cnt++;
						}
					} else {
						cntAdhoc++;
					}
				}
			}
		}
		
		//MyLog.i(LOG_TEST, TAG, "getAvgRssBuf: #APs = " + cnt + " (ad-hoc: " + cntAdhoc + ")");
		return scanRssAvg;
	}
	
	
	public long getScanTime() {
		return mLastTime;
	}
	
	private void updateCursor() {
		mCursor = (mCursor + 1) % mBufSize;
	}
}
