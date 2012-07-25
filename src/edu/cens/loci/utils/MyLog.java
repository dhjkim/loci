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
package edu.cens.loci.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import edu.cens.loci.LociConfig;

public class MyLog {
	
	private static final String TAG = "MyLog";

	public static final String LABEL = "[loci] ";
	
	private static MyLog sSingleton = null;
	
	public static synchronized MyLog getInstance() {
		if (sSingleton == null) {
			sSingleton = new MyLog();
			if (createFiles()) {
				setTimer();
			} else {
				sSingleton = null;
			}
		}
		return sSingleton;
	}
	
	private MyLog() {
	}
	
	public static void e(boolean flag, String tag, String msg) {
		if (flag) {
			Log.e(tag, LABEL + msg);
			if (LociConfig.LOG) {
				getInstance();
				MyLog.write(String.format(" (%s)\t%s", tag, msg));
			}
		}
	}
	
	public static void w(boolean flag, String tag, String msg) {
		// TODO Auto-generated method stub
		if (flag) {
			Log.w(tag, LABEL + msg);
			if (LociConfig.LOG) {
				getInstance();
				write(String.format(" (%s)\t%s", tag, msg));
			}
		}
	}
	
	public static void d(boolean flag, String tag, String msg) {
		if (flag) {
			Log.d(tag, LABEL + msg);
			if (LociConfig.LOG) {
				getInstance();
				write(String.format(" (%s)\t%s", tag, msg));
			}
		}
	}
	
	public static void i(boolean flag, String tag, String msg) {
		if (flag) {
			Log.i(tag, LABEL + msg);
			if (LociConfig.LOG) {
				getInstance();
				write(String.format(" (%s)\t%s", tag, msg));
			}
		}
	}
	
	public static void v(boolean flag, String tag, String msg) {
		if (flag) {
			Log.v(tag, LABEL + msg);
			if (LociConfig.LOG) {
				getInstance();
				write(String.format(" (%s)\t%s", tag, msg));
			}
		}
	}
	
	
	private static File								mFiles = null;
	private static File								mDirFile = null;
	private static FileOutputStream 	mFileOutStreams = null;
	private static PrintStream				mPrintStreams = null;
	
	private static SimpleDateFormat 	formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
	private static SimpleDateFormat 	formatter2 = new SimpleDateFormat("yyyy-MM-dd");
	
	private static Timer mTimer = new Timer();
	
	public static synchronized void write(String data) {
		
		if (sSingleton == null) {
			Log.e(TAG, "MyLog is not initiated yet. not writting to file.");
			return;
		}
		
		Calendar.getInstance();
		long time = Calendar.getInstance().getTimeInMillis(); 
		String s;
		s = String.format("[%sT%s] %s\n",MyDateUtils.getDateFormatMedium(time), MyDateUtils.getTimeFormatMedium(time), data);
		mPrintStreams.print(s);
	}
	
	public static class FileSwitcher extends TimerTask {
	  public void run() {
	    System.out.println("Switching Files...");
	    close();
	    createFiles();
	    setTimer();
	  }
	}
	
	private static synchronized boolean createFiles() {
		Date date = Calendar.getInstance().getTime();
		String filename = formatter2.format(date);
		String pathname = "/sdcard/Loci2/logs/" + formatter2.format(date);
		return createFile(pathname, filename + ".dat");
	}
	
	private static synchronized boolean createFile(String path, String filename) {
		mFiles = new File(path, filename);
		
		Log.d(TAG, "[log] pathname=" + path + ",filename=" + filename);
	  Log.d(TAG, "[log] mFiles=" + mFiles.getAbsolutePath());
	  Log.d(TAG, "[log] mFiles getParent=" + mFiles.getParentFile());
		
		// Create dir if it does not exists
		createDir(mFiles.getParentFile());
		
		// Create newfile
		try {
			if (!mFiles.exists()) {
				Log.e(TAG, "[log] file does not exists, creating :" + mFiles.getName());
				mFiles.createNewFile();
			} else {
				Log.e(TAG, "[log] file exists, open.");
			}
		} catch (IOException e) {
			Log.e(TAG, "[log] createNewFile(): ...[n] creating file:" + mFiles.getName());
			e.printStackTrace();
			return false;
		}
		// Open a fileOutputStream
		try {
  		mFileOutStreams = new FileOutputStream(mFiles, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		// Create a PrinteStream
		try {
			mPrintStreams = new PrintStream(mFileOutStreams);
		} catch (NullPointerException ne) {
			ne.printStackTrace();
			//Toast.makeText(mContext, "[log] SD card is full", Toast.LENGTH_LONG);
			return false;
		}
		return true;
	}
	
	private static synchronized void createDir(File dir) {
		
		mDirFile = dir;
		
		if (!mDirFile.exists()) {
			Log.d(TAG, "[log] dirFile=" + mDirFile.getName() + " does not exists.. creating directory...");
			if (!mDirFile.mkdirs()) {
				Log.d(TAG, "[log] ...[n] creating dir:" + mDirFile.getAbsolutePath());
				if (!mDirFile.mkdir()) {
					Log.d(TAG, "[log] ......[n] creating dir (mkdir)");
				} else {
					Log.d(TAG, "[log] ......[y] creating dir (mkdir)");
				}
			} else {
				Log.d(TAG, "[log] ...[y] creating dir:" + mDirFile.getName());
			}
		}
	}
	
	private static void setTimer() {
		Calendar nextDay = Calendar.getInstance(); 	// get current time
		nextDay.add(Calendar.DAY_OF_YEAR, 1);				// add a day (next day)
		nextDay.set(Calendar.HOUR_OF_DAY, 0);				// set time to midnight
		nextDay.set(Calendar.MINUTE, 0);
		nextDay.set(Calendar.SECOND, 0);
		nextDay.set(Calendar.MILLISECOND, 0);
		
		Log.e(TAG, "schedule a timer at : " + formatter1.format(nextDay.getTime()));
		mTimer.schedule(new FileSwitcher(), nextDay.getTime(),1000*60*60*24*7);
	}
	
	public static synchronized void close() {
		if (mPrintStreams != null)
			mPrintStreams.close();
		try {
			if (mFileOutStreams != null)
				mFileOutStreams.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
