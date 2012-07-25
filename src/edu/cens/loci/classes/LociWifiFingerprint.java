/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.cens.loci.classes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class LociWifiFingerprint implements Serializable {

	private static final long	serialVersionUID	= 1L;
	private static final String TAG = "LociWifiFingerprint";
	
	private float pRepStep 				= LociConfig.pRepStep;
	private float pRepMin 				= LociConfig.pRepMin;
	private static double pMinRss = LociConfig.pMinRss;
	private static double pMaxRss = LociConfig.pMaxRss;
	
	private long mEnter;
	private long mExit;
	private int mScanCount = 0;
	private HashMap<String, APInfoMapItem> mAPs;
	
	public static class APInfoMapItem {
		public int rss;
		public int count;
		public String ssid;
		public int [] rssBuckets;
		public boolean isRep;

		private final int BucketValues[] = {-100, -90, -80, -70, -60, -50, -40};
		
		// BSSID is used as key
		public APInfoMapItem(int rss, String ssid) {
			this.rss = rss;
			this.count = 1;
			this.ssid = ssid;
			this.rssBuckets = new int[BucketValues.length+1];
			this.rssBuckets[bucketIndex(rss)] = 1;
		}
		
		public String toString() {
			String result = String.format("APInfoMapItem [rss=%d, count=%d, ssid=%s] ", this.rss, this.count, this.ssid);
			int sum = 0;
			for (int i=0; i<this.rssBuckets.length; i++) {
				result += String.format(" %3d", this.rssBuckets[i]);
				sum += this.rssBuckets[i];
			}
			result += " sum=" + sum;
			return result;
		}
		
		public void update(int rss) {
			this.rss = (int) runningAvg((double) this.count, (double) this.rss, (double) rss);
			this.count++;
			this.rssBuckets[bucketIndex(rss)]++;
		}
		
		private int bucketIndex(int rss) {
			int numBuckets = BucketValues.length;
			int index = numBuckets;
			for (int i=0; i<numBuckets; i++) {
				if (BucketValues[i] >= rss) {
					index = i;
					break;
				}
			}
			return index;
		}

		public APInfoMapItem(JSONObject jObj) throws JSONException {
			
			this.rss = jObj.getInt("rss");
			this.count = jObj.getInt("count");
			this.ssid = jObj.getString("ssid");
			JSONArray jArr = jObj.getJSONArray("rssBuckets");
			this.rssBuckets = new int[BucketValues.length+1];

			for (int i=0; i<jArr.length();i++)
				this.rssBuckets[i] = jArr.getInt(i);
			
			this.isRep = jObj.getBoolean("isRep");
		}
		
		public JSONObject toJsonObject() throws JSONException {
			
			JSONObject jObj = new JSONObject();
			JSONArray jArr = new JSONArray();
			
			jObj.put("rss", this.rss);
			jObj.put("count", this.count);
			jObj.put("ssid", this.ssid);
			
			for (int i=0; i<this.rssBuckets.length; i++) 
				jArr.put(this.rssBuckets[i]);
			
			jObj.put("rssBuckets", jArr);
			jObj.put("isRep", this.isRep);
			
			return jObj;
		}
	}
	
	public LociWifiFingerprint() {
		initialize();
	}
	
	public void initialize() {
		mScanCount = 0;
		mEnter = -1;
		mExit = -1;
		if (mAPs == null)
			mAPs = new HashMap<String, APInfoMapItem>();
		else
			mAPs.clear();
	}
	
	
	public String toString() {
		String header = String.format("LociWifiFingerprint [enter=%s, exit=%s, #APs=%d, #scans=%d]\n", MyDateUtils.getTimeFormatMedium(mEnter), MyDateUtils.getTimeFormatMedium(mExit), mAPs.size(),  mScanCount);
		
		Set<String> keys = mAPs.keySet();
		Iterator<String> iter = keys.iterator();
		
		while (iter.hasNext()) {
			String key = iter.next();
			header += (" " + key + ":");
			header += mAPs.get(key).toString();
			header += String.format("\n");
		}
		header += String.format("---------------------\n");
		return header;
	}
	
	public String getWifiInfoSubstring(int limit) {
		
		String substring = "Not available";
		ArrayList<APInfoMapItem> aps = getAPsSortRss();
		substring = "";
		int numAps = aps.size();

		for (int i=0; i<numAps; i++) {
			substring += aps.get(i).ssid;
			if (i+1 == numAps) 
				break;
			if (i+1 == limit) {
				substring += " ..."; 
				break;
			}
			substring += ", ";
		}
		return substring;
	}
	
	
	@SuppressWarnings("unchecked")
	public LociWifiFingerprint(String json) throws JSONException {
		JSONObject jObj = new JSONObject(json);
		this.mEnter = jObj.getLong("enter");
		this.mExit = jObj.getLong("exit");
		this.mScanCount = jObj.getInt("count");
		JSONObject jObjAPs = jObj.getJSONObject("aps");
		
		Iterator<String> keys = jObjAPs.keys();
		mAPs = new HashMap<String, APInfoMapItem>();
		while (keys.hasNext()) {
			String bssid = keys.next();
			mAPs.put(bssid, new APInfoMapItem(jObjAPs.getJSONObject(bssid)));
		}
		
		//Log.d(TAG, "constructing from json : " + this.toString());
	}
	
	public JSONObject toJsonObject() throws JSONException {
		JSONObject jObj = new JSONObject();
		
		jObj.put("enter", mEnter);
		jObj.put("exit", mExit);
		jObj.put("count", mScanCount);
		
		JSONObject jObjAPs = new JSONObject();
		
		Set<String> keys = mAPs.keySet();
		Iterator<String> iterateKeys = keys.iterator();
		
		while (iterateKeys.hasNext()) {
			String bssid = iterateKeys.next();
			jObjAPs.put(bssid, mAPs.get(bssid).toJsonObject());
		}
		jObj.put("aps", jObjAPs);
		
		return jObj;
	}
	
	public String toJsonString() {
		try {
			return this.toJsonObject().toString();
		} catch (JSONException e) {
			Log.e(TAG, "json failed");
			e.printStackTrace();
		}
		return "";
	}
	
	public void update(long time, HashMap<String, APInfoMapItem> scan) {

		if (mScanCount == 0) 
			mEnter = time; 
		mExit = time;
		
		Set<String> keys = scan.keySet();
		Iterator<String> iter = keys.iterator();
		
		while(iter.hasNext()) {
			String bssid = iter.next();
			
			if (mAPs.containsKey(bssid)) {
				mAPs.get(bssid).update(scan.get(bssid).rss);
			} else {
				mAPs.put(bssid, scan.get(bssid));
			}
		}
		mScanCount++;
	}
	
	public int getScanCount() {
		return mScanCount;
	}
	
	public long getEnter() {
		return mEnter;
	}
	
	public long getExit() {
		return mExit;
	}
	
	public HashMap<String, APInfoMapItem> getAps() {
		return mAPs;
	}
	
	public HashSet<String> getBssids() {
		HashSet<String> bssids = new HashSet<String>();
		Set<String> keys = mAPs.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String bssid = iter.next();
			bssids.add(bssid);
		}
		return bssids;
	}

	public String getSsid(String bssid) {
		APInfoMapItem ap = mAPs.get(bssid);
		if (ap != null)
			return ap.ssid;
		return null;
	}
	
	public ArrayList<APInfoMapItem> getAPsSortRss() {
		
		ArrayList<APInfoMapItem> aps = new ArrayList<APInfoMapItem>();
		
		Set<String> keys = mAPs.keySet();
		Iterator<String> iter = keys.iterator();
		while(iter.hasNext()) {
			String bssid = iter.next();
			
			APInfoMapItem ap = mAPs.get(bssid);
			
			int apsSize = aps.size();
			
			for (int i=0; i<apsSize; i++) {
				if (ap.rss > aps.get(i).rss) {
					aps.add(i, ap);
					break;
				}
			}
			if (aps.size() == apsSize) 
				aps.add(ap);
			
		}
		return aps;
	}
	
	public ArrayList<APInfoMapItem> getAPsSortCount() {
		
		ArrayList<APInfoMapItem> aps = new ArrayList<APInfoMapItem>();
		
		Set<String> keys = mAPs.keySet();
		Iterator<String> iter = keys.iterator();
		while(iter.hasNext()) {
			String bssid = iter.next();
			
			APInfoMapItem ap = mAPs.get(bssid);
			
			int apsSize = aps.size();
			
			for (int i=0; i<apsSize; i++) {
				if (ap.count > aps.get(i).count) {
					aps.add(i, ap);
					break;
				}
			}
			if (aps.size() == apsSize) 
				aps.add(ap);
			
		}
		return aps;
	}
	
	public int getNumAPs() {
		return mAPs.keySet().size();
	}
	
	// returns average RSS value of a particular bssid
	public int getAvgRss(String bssid) {
		if (mAPs.containsKey(bssid))
			return mAPs.get(bssid).rss;
		return 0;
	}
	
	public double getResponseRate(String bssid) {
		if (mAPs.containsKey(bssid))
			return ((double) mAPs.get(bssid).count) / ((double) mScanCount);
		return 0;
	}
	
	public int getCount(String bssid) {
		if (mAPs.containsKey(bssid)) {
			return mAPs.get(bssid).count;
		} else {
			return 0;
		}
	}
	
	public int [] getHistogram(String bssid) {
		if (mAPs.containsKey(bssid)) {
			return mAPs.get(bssid).rssBuckets;
		} else {
			return null;
		}
	}

	public HashSet<String> getRepAPs() {

		HashSet<String> repAPs = new HashSet<String>();
		
		Set<String> keys = mAPs.keySet();
		Iterator<String> iter = keys.iterator();
		
		while (iter.hasNext()) {
			String bssid = iter.next();
			if (mAPs.get(bssid).isRep)
				repAPs.add(bssid);
		}
		return repAPs;
	}
	
	public void setEnter(long time) {
		mEnter = time;
	}
	
	public void setExit(long time) {
		mExit = time;
	}
	
	public void setRepAPs(HashSet<String> repAPs) {
		
		Set<String> keys = mAPs.keySet();
		Iterator<String> iter = keys.iterator();
		
		while (iter.hasNext()) {
			String bssid = iter.next();
			if (repAPs.contains(bssid))
				mAPs.get(bssid).isRep = true;
			else
				mAPs.get(bssid).isRep = false;
		}
	}
	
	public void setRepresetnativeParameters(float step, float min) {
		pRepStep 	= step;
		pRepMin 	= min;
	}
	
	public void log() {
		
		String debugSsids = "    ";
		String debugRss = "ss: ";
		String debugCnt = "cn: ";
		String debugRR = "rr: ";
		
		Set<String> bssids = mAPs.keySet();
		Iterator<String> iter = bssids.iterator();
		
		while (iter.hasNext()) {
			String bssid = iter.next();
			
			APInfoMapItem ap = mAPs.get(bssid);
			
			int rss = ap.rss;
			int count = ap.count;
			double rr = count / mScanCount;
			String ssid = ap.ssid;
			
			if (ssid == null)
				ssid = "unknown";
						
			debugSsids 	= debugSsids + String.format("%10s", ssid.substring(0, Math.min(ssid.length(), 9)));
			debugRss 	= debugRss + String.format("%10d", rss);
			debugRR 		= debugRR + String.format("%10.2f", rr);
			debugCnt 		= debugCnt + String.format("%10d", count);
		}
		MyLog.d(LociConfig.D.Classes.LOG_WIFI, TAG, toString());
		MyLog.d(LociConfig.D.Classes.LOG_WIFI, TAG, debugSsids);
		MyLog.d(LociConfig.D.Classes.LOG_WIFI, TAG, debugRss);
		MyLog.d(LociConfig.D.Classes.LOG_WIFI, TAG, debugRR);
		MyLog.d(LociConfig.D.Classes.LOG_WIFI, TAG, debugCnt);
	}

	
	public boolean hasNewBssid(LociWifiFingerprint sig) {
		
		HashSet<String> diff = new HashSet<String>(sig.getBssids());
		diff.removeAll(this.getBssids());

		/*
		Iterator<String> iter = diff.iterator();
		String debugMsg = "";
		while (iter.hasNext()) {
			String key = iter.next();
			debugMsg = debugMsg + " " + this.getSsid(key);
		}
		*/
		
		return !diff.isEmpty();
	}
	
	public double getRepThreshold() { 
		
		Iterator<String> iter = this.getBssids().iterator();

		// get threshold
		double maxRR = 0;
		while (iter.hasNext()) {
			String bssid = iter.next();
			int rr = mAPs.get(bssid).rss / this.mScanCount;
			
			if (rr > maxRR) 
				maxRR = rr;
		}
		double threshold = maxRR = pRepStep;
		if (threshold < pRepMin)
			threshold = pRepMin - 0.1;
		return threshold;
	}
	
	/**
	 * Get APs that has a response rate higher than the given threshold
	 * @param threshold
	 * @return a set of APs (bbsid)
	 */
	public HashSet<String> getRepAPs(double threshold) {
		
		HashSet<String> repAPs = new HashSet<String>();
		int cntRepAPs = 0;

		// select representative beacons
		Iterator<String> iter = this.getBssids().iterator();
		while (iter.hasNext()) {
			String bssid = iter.next();
			int rr = mAPs.get(bssid).rss / this.mScanCount;
			
			if (rr >= threshold) {
				repAPs.add(bssid);
				cntRepAPs++;
			}
		}
		
		MyLog.d(LociConfig.D.Classes.LOG_WIFI, TAG, String.format("getRepAPs(): [number of APs with RR >= %6.3f] =%d)", threshold, cntRepAPs));
		return repAPs;
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	///// STATIC METHODS
	///////////////////////////////////////////////////////////////////////////////////
	
	public static double runningAvg(double scanCnt, double curAvg, double newRss) {
		double delta = newRss - curAvg;
		double newAvg = curAvg;
		double ddelta = delta / scanCnt;
		newAvg = newAvg + ddelta;
		return newAvg;
	}
	
	public static boolean hasCommonBeacons(LociWifiFingerprint sig1, LociWifiFingerprint sig2) {
		
		HashSet<String> bssid1 = sig1.getBssids();
		HashSet<String> bssid2 = sig2.getBssids();
		
		HashSet<String> intersection = new HashSet<String>(bssid1);
		intersection.retainAll(bssid2);
		
		if (intersection.isEmpty())
			return false;
		else
			return true;
	}
	
	/**
	 * Calculated Tanimoto Similarity between to arrays
	 * @param a is an array of doubles
	 * @param b is an array of doubles
	 * @return Tanimoto similarity between a and b
	 */
	public static double tanimotoSimilarity(double [] a, double [] b) {
		
		if (a.length != b.length) {
			MyLog.e(LociConfig.D.ERROR, TAG, "tanimoto similarity(): length mismatch a=" + a.length + ", b=" + b.length);
			return -1;
		}
		
		double a_mul_b = 0;
		double a_mul_a = 0;
		double b_mul_b = 0;
		
		String debugLnA = "a : ";
		String debugLnB = "b : ";
		
		for (int i=0; i<a.length; i++) {
			// (a * b) / (norm(a)^2 + norm(b)^2 - a*b) 
			a_mul_b = a_mul_b + a[i] * b[i];
			a_mul_a = a_mul_a + a[i] * a[i];
			b_mul_b = b_mul_b + b[i] * b[i];
		
			debugLnA = debugLnA + String.format("%10.2f", a[i]);
			debugLnB = debugLnB + String.format("%10.2f", b[i]);
		}
		
		double score = a_mul_b / (a_mul_a + b_mul_b - a_mul_b);
		
		//MyLog.d(LociConfig.D.PD.SCORE, TAG, debugLnA);
		//MyLog.d(LociConfig.D.PD.SCORE, TAG, debugLnB);
		MyLog.e(LociConfig.D.PD.SCORE, TAG, String.format(" [#AP=%2d] T-score=%10.2f", a.length, score));
		
		return score;
	}

	/**
	 * Calculated Tanimoto score between to Wifi fingerprints
	 * @param sig1 is a Wifi fingerprint
	 * @param sig2 is a Wifi fingerprint
	 * @param repAP1 
	 * @param repAP2
	 * @param rrThreshold is used to filter low response rate APs
	 * @param isDebugOn
	 * @return
	 */
	public static double tanimotoScore(LociWifiFingerprint sig1, LociWifiFingerprint sig2, HashSet<String> repAP1, HashSet<String> repAP2, double rrThreshold, boolean isDebugOn) {
			
		if (repAP1 == null)
			MyLog.i(LociConfig.D.Classes.LOG_WIFI, TAG, "tanimotoScore: use all.");
		else
			MyLog.i(LociConfig.D.Classes.LOG_WIFI, TAG, "tanimotoScore: use rep.");
		
		// use every beacon when repAP is empty
		if (repAP1 == null || repAP1.isEmpty())
			repAP1 = sig1.getBssids();
		if (repAP2 == null || repAP2.isEmpty())
			repAP2 = sig2.getBssids();
		
		// use only the union of both representative set
		HashSet<String> union = new HashSet<String>(repAP1);
		union.addAll(repAP2);

		// two arrays to save RSS values 
		double a [] = new double[union.size()];
		double b [] = new double[union.size()];
		
		// fill in the arrays with RSS values
		Iterator<String> iterator = union.iterator();
		int cnt = 0;
		
		String debugSsids = 	"   ";
		String debugRssA = 		"rss (a) : ";
		String debugRssB = 		"rss (b) : ";
		String debugNorA =    "nor (a) : ";
		String debugNorB = 	  "nor (b) : ";
		String debugCntA = 		"cnt (a) : ";
		String debugCntB =		"cnt (b) : ";
		String debugRRA  = 		"rr  (a) : ";
		String debugRRB  = 		"rr  (b) : ";
		
		while (iterator.hasNext()) {
			String bssid = iterator.next();

			int aIntVal = sig1.getAvgRss(bssid);
			int bIntVal = sig2.getAvgRss(bssid);
			double rr1 = sig1.getResponseRate(bssid);
			double rr2 = sig2.getResponseRate(bssid);
			
			if (rr1 >= rrThreshold)
				a[cnt] = raw2normal(aIntVal, pMinRss, pMaxRss);
			else
				a[cnt] = raw2normal(0, pMinRss, pMaxRss);
			
			if (rr2 >= rrThreshold)
				b[cnt] = raw2normal(bIntVal, pMinRss, pMaxRss);
			else
				b[cnt] = raw2normal(0, pMinRss, pMaxRss);

			cnt++;

			if (isDebugOn) {
				String ssid = sig1.getSsid(bssid);
				if (ssid == null)
					ssid = sig2.getSsid(bssid);
				if (ssid == null)
					ssid = "unknown";

				int count1 = sig1.getCount(bssid);
				int count2 = sig2.getCount(bssid);
				
				debugSsids = debugSsids + String.format("%10s", ssid.substring(0, Math.min(ssid.length(), 9)));
				debugRssA = debugRssA + String.format("%10d", aIntVal);
				debugRssB = debugRssB + String.format("%10d", bIntVal);
				debugNorA = debugNorA + String.format("%10.2f", a[cnt-1]);
				debugNorB = debugNorB + String.format("%10.2f", b[cnt-1]);
				debugCntA = debugCntA + String.format("%10d", count1);
				debugCntB = debugCntB + String.format("%10d", count2);
				debugRRA = debugRRA + String.format("%10.2f", rr1);
				debugRRB = debugRRB + String.format("%10.2f", rr2);
			}
		}
		
		MyLog.d(isDebugOn, TAG, debugSsids);
		MyLog.d(isDebugOn, TAG, debugRssA);
		MyLog.d(isDebugOn, TAG, debugRssB);
		MyLog.d(isDebugOn, TAG, debugNorA);
		MyLog.d(isDebugOn, TAG, debugNorB);
		MyLog.d(isDebugOn, TAG, debugCntA);
		MyLog.d(isDebugOn, TAG, debugCntB);
		MyLog.d(isDebugOn, TAG, debugRRA);
		MyLog.d(isDebugOn, TAG, debugRRB);
		
		return tanimotoSimilarity(a, b);
	}
	
	/**
	 * Maps RSS value in 0 to 1 scale
	 * @param min is mapped to 0
	 * @param max is mapped to 1
	 */
	public static double raw2normal(double rss, double min, double max) {
			if (rss == 0 || rss < min)
				rss = min;
			else if (rss > max)
				rss = max;
			return (-rss) / (min-max) - min / (max - min);
	}
}
