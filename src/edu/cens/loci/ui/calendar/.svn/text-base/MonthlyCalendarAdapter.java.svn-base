package edu.cens.loci.ui.calendar;

import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import edu.cens.loci.R;
import edu.cens.loci.provider.LociContract;

public class MonthlyCalendarAdapter extends BaseAdapter {

	private Context mContext;
	private Calendar mBaseDate;
	private int mStartPos;
	private int mEndPos;
	private int mDaysInMonth;
	private static final int CELL_WIDTH = 45;
	private static final int CELL_HEIGH = 45;
	private static final int[] mWeekTitleIds = { 
		    R.string.sun
		   ,R.string.mon
		   ,R.string.tue
		   ,R.string.wed
		   ,R.string.thr
		   ,R.string.fri
		   ,R.string.sat 
	};
	@SuppressWarnings("unused")
	private static final int[] mFullWeekTitleIds = { 
	    R.string.sunday
	   ,R.string.monday
	   ,R.string.tuesday
	   ,R.string.wednesday
	   ,R.string.thirsday
	   ,R.string.friday
	   ,R.string.saturday
};
	private static final int[] mWeekColorIds = { 
		    R.color.stalegray
		   ,R.color.white
		   ,R.color.white
		   ,R.color.white
		   ,R.color.white
		   ,R.color.white
		   ,R.color.stalegray
	};

	public MonthlyCalendarAdapter(Context c, Calendar cal) {
        mContext = c;
        setBaseDate(cal);
    }

    public void setBaseDate(Calendar cal) {
        mBaseDate = (Calendar)cal.clone();
        Calendar lastDayInMonth = (Calendar)cal.clone();
        lastDayInMonth.add(Calendar.MONTH, 1);
        lastDayInMonth.add(Calendar.DATE, -1);
        mDaysInMonth = lastDayInMonth.get(Calendar.DATE);
        mStartPos = 7  
                  + mBaseDate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY 
                  ;
        mEndPos = mStartPos 
                + mDaysInMonth;    	
        
        //Log.i("K", "mDaysInMonth : " + mDaysInMonth + ", s position : " + mStartPos + " , : " + mEndPos);
    }
    

	public int getCount() {
    	return (mEndPos % 7 == 0) ? mEndPos : mEndPos + 1 ; // 1Week + Week Button
	}

	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public View getView(int position, View oldView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View v;
		
		// Sun ~ Sat
		if (position < 7) { 
			if (oldView == null) {
				v = new TextView(mContext);
				((TextView)v).setGravity(Gravity.CENTER);
				((TextView)v).setText(mWeekTitleIds[position]);
				((TextView)v).setTextColor(mContext.getResources().getColor(mWeekColorIds[position]));
				((TextView)v).setClickable(false);
			}
			else {
				v = oldView;
			}
		}
		
		// show the digits
		else if (position >= mStartPos && position < mEndPos) { 
			
			if (oldView == null) {
				v = new TextView(mContext);
				
				((TextView)v).setGravity(Gravity.CENTER);
				//((TextView)v).setBackgroundResource(mContext.getResources().getIdentifier("cens.ucla.edu.budburst:drawable/bordergray.9", null, null));
				((TextView)v).setClickable(true);
				
				int nDay = getDayFromPosition(position);

				Calendar c = (Calendar) mBaseDate.clone();
				
				c.set(Calendar.DATE, nDay);
				v.setTag(c);
	    		
				//Log.i("K", "" + nDay);
				
				((TextView)v).setText(Integer.toString(nDay));
				
				Calendar cToday = Calendar.getInstance();
				
				
				/*
				 *  show today a bit differently. (different color and size)
				 */
				if((c.getTime().getDate() == cToday.getTime().getDate()) && (c.getTime().getMonth() == cToday.getTime().getMonth())) {
					((TextView)v).setTextColor(mContext.getResources().getColor(R.color.gold));
					((TextView)v).setTextSize(23);
				}
				else {
					((TextView)v).setTextColor(mContext.getResources().getColor(mWeekColorIds[c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]));
					((TextView)v).setTextSize(20);
				}
				
				/*
				 *  code for checking if the daily survey has been done.
				 */
				Calendar d = (Calendar)v.getTag();
            	d.set(Calendar.HOUR_OF_DAY, 0);
          		d.set(Calendar.MINUTE, 0);
          		d.set(Calendar.SECOND, 0);
          		d.set(Calendar.MILLISECOND, 0);
				
				//boolean hasDailySurvey = Loci.getInstance().getVisitDb().hasDailySurveyEntry(d.getTime().getTime());
				
				//if(hasDailySurvey) { 
				//	((TextView)v).setBackgroundResource(R.drawable.calendar_daily_border);
				//}
				
				// when click the textview, move to the daily schedule listview
				v.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {
		              
		            	// get Date object for the selected day
		            	Calendar c = (Calendar)v.getTag();
		            	c.set(Calendar.HOUR_OF_DAY, 0);
		          		c.set(Calendar.MINUTE, 0);
		          		c.set(Calendar.SECOND, 0);
		          		c.set(Calendar.MILLISECOND, 0);
		          		
		            	if (c == null) 
		            		return;
		            	
		            	Intent intent = new Intent(mContext, DailyCalendar.class);
		            	intent.putExtra("date", c.getTime());
		            	mContext.startActivity(intent);
		            	
		            }
		        	});
			}
			else {
				v = oldView;
			}
		} else { 
			v = new TextView(mContext);
		}

		if (oldView == null) {
			v.setLayoutParams(new GridView.LayoutParams(CELL_WIDTH, CELL_HEIGH));
		}

		return v;
	}

	private int getDayFromPosition(int position) {
		return position - mStartPos + 1;
	}
}

