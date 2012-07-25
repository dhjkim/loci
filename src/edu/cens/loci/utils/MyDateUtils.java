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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

public class MyDateUtils {

	public static Date getToday() {
		Calendar today = Calendar.getInstance();
  	today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		today.add(Calendar.DAY_OF_MONTH, 0);
	
		return today.getTime();
	}
	
	public static Date getTomorrow() {
		Calendar tomorrow = Calendar.getInstance();
		tomorrow.setTime(MyDateUtils.getToday());
		tomorrow.add(Calendar.DAY_OF_MONTH, 1);
		return tomorrow.getTime();
	}
	
	public static Date getMidnight(Date date) {
		Calendar day = Calendar.getInstance();
		day.setTime(date);
  	day.set(Calendar.HOUR_OF_DAY, 0);
		day.set(Calendar.MINUTE, 0);
		day.set(Calendar.SECOND, 0);
		day.set(Calendar.MILLISECOND, 0);
		day.add(Calendar.DAY_OF_MONTH, 0);
		
		return day.getTime();
	}
	
	public static Date getNextDay(Date date) {
		Calendar day = Calendar.getInstance();
		day.setTime(date);
		day.add(Calendar.DAY_OF_MONTH, 1);

		return day.getTime();
	}
	
	/**
	 * 
	 * @param aStayTime is duration in milliseconds
	 * @param type is 1 if cut-off on hours, 2 if cut-off on minutes.
	 * @return
	 */
	public static String humanReadableDuration(long aStayTime, int type) {
		
		if (aStayTime < 0) 
			return "invalid time";
		else if (aStayTime == 0)
			return "0 min";
		
		String stay = "";
		int stayTime = (int) aStayTime / 1000;
		
		String hh = null;
		String mm = null;
		String ss = null;
		
		if (type == 1) {
			return String.format("%.1fhr", ((float)stayTime)/3600);
		}
		
		int hhInt = stayTime / 3600;
		hh = String.format("%d", hhInt);
		
		if (hhInt >= 1)
			stay = hh + "hr ";
		
		stayTime = stayTime % 3600;
		int mmInt = stayTime / 60;
		mm = String.format("%2d", mmInt);
		
		//if (mmInt >= 1)
			stay = stay + mm + "min ";
				
		if (type == 2) {
			return stay;
		}
		
		stayTime = stayTime % 60;
		ss = String.format("%2d", stayTime);
	
		if (stayTime >= 1)
			stay = stay + ss + "sec";
		
		return stay;
	}
	
	public static String getFullDate(Date date) {
		String pattern = "EEEE, MMM d, yyyy";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(date);
	}
	
	// relative date is based on absolute difference between date and now
	// change it to be based on midnight
	public static String getRelativeDate(Context c, long date) {
 		int flags = DateUtils.FORMAT_ABBREV_RELATIVE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL;
 		
 		long today = getToday().getTime();
 		long tmrw = getTomorrow().getTime();
 		
 		//date = date - 3600000;
 		
 		if (date > today) {
 			//return "" + date + " " + today;
 		
 			String datestr = (String) DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags);
 			String timestr = (String) DateUtils.formatDateTime(c, date, DateUtils.FORMAT_SHOW_TIME);
 			return  datestr + ", " + timestr;
 		} else {
 			String datestr = (String) DateUtils.getRelativeTimeSpanString(date, tmrw, DateUtils.MINUTE_IN_MILLIS, flags);
 			String timestr = (String) DateUtils.formatDateTime(c, date, DateUtils.FORMAT_SHOW_TIME);
 			return  datestr + ", " + timestr;
 		}
 		
 		//return (String) DateUtils.getRelativeDateTimeString(c, date - 61200000, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, flags);
 		//return (String) DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags);
	}
	
	public static String getDateFullAbrv(Date date) {
		String pattern = "MMM d, yyyy (EEE)";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(date);
	}
	
	public static String getDateFullAbrv(long date) {
		
		String pattern = "MMM d, yyyy (EEE)";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		mDate.setTime(date);
		return sdf.format(mDate);
		
	}
	
	public static String getAbrv_MMM_d_h_m(long date) {
		String pattern = "MMM d (EEE), h:mm";
		String patternAMPM = "a";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		SimpleDateFormat sdf2 = new SimpleDateFormat(patternAMPM);
		mDate.setTime(date);
		Date date2 = new Date(date);
		return sdf.format(mDate).toString() + sdf2.format(date2).toLowerCase();
	}
	
	public static String getAbrv_h_m_MMM_d(long date) {
		String patternTime = "h:mm";
		String patternAMPM = "a, ";
		String patternDay = "MMM d (EEE)";
		
		String dateString = "";
		
		SimpleDateFormat sdf = new SimpleDateFormat(patternTime);
		mDate.setTime(date);
		
		dateString += sdf.format(mDate).toString();
		sdf.applyPattern(patternAMPM);
		dateString += sdf.format(mDate).toLowerCase();
		sdf.applyPattern(patternDay);
		dateString += sdf.format(mDate).toString();
		
		return dateString;
	}
	
	private static final DateFormat mDfTime = DateFormat.getTimeInstance();
	private static final DateFormat mDfTimeShort = DateFormat.getTimeInstance(DateFormat.SHORT);
	private static final DateFormat mDfTimeMedium = DateFormat.getTimeInstance(DateFormat.MEDIUM);
	private static final DateFormat mDfTimeLong = DateFormat.getTimeInstance(DateFormat.LONG);	
	
	private static final DateFormat mDfDateShort = DateFormat.getDateInstance(DateFormat.SHORT);
	private static final DateFormat mDfDateMedium = DateFormat.getDateInstance(DateFormat.MEDIUM);
	private static final DateFormat mDfDateLong = DateFormat.getDateInstance(DateFormat.LONG);

	private static final Date mDate = new Date();
	
	public static String getTimeFormat(Date date) {
		
		if (date == null)
		 return "";
		
		return mDfTime.format(date);
	}

	public static String getTimeFormatLong(Date date) {
		if (date == null)
			return "";
		return mDfTimeLong.format(date);
	}
	
	public static String getTimeFormatLong(long date) {
		mDate.setTime(date);
		return mDfTimeLong.format(mDate);
	}

	public static String getTimeFormatMedium(Date date) {
		if (date == null)
			return "";
		
		return mDfTimeMedium.format(date);
	}
	
	public static String getTimeFormatMedium(long date) {
		mDate.setTime(date);
		return mDfTimeMedium.format(mDate);
	}
	
	public static String getTimeFormatShort(Date date) {
		if (date == null)
			return "";
		
		return mDfTimeShort.format(date);
	}
	
	public static String getTimeFormatShort(long date) {
		mDate.setTime(date);
		return mDfTimeShort.format(mDate);
	}
	
	public static String getDateFormatShort(Date date) {
		if (date == null)
			return "";
		
		return mDfDateShort.format(date);
	}
	
	public static String getDateFormatShort(long date) {
		mDate.setTime(date);
		return mDfDateShort.format(mDate);
	}
	
	public static String getDateFormatMedium(Date date) {
		if (date == null)
			return "";
		
		return mDfDateMedium.format(date);
	}
	
	public static String getDateFormatMedium(long date) {
		mDate.setTime(date);
		return mDfDateMedium.format(mDate);
	}
	
	public static String getDateFormatLong(Date date) {
		if (date == null)
			return "";
		
		return mDfDateLong.format(date);
	}
	
	public static String getDateFormatLong(long date) {
		mDate.setTime(date);
		return mDfDateLong.format(mDate);
	}
	
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	public static String getDateFormatYYYYMMDD(Date date) {
		return formatter.format(date);
	}
	
	public static String getDateFormatYYYYMMDD(long date) {
		Date d = new Date(date);
		return formatter.format(d);
	}
}
