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
package edu.cens.loci.ui.calendar;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import edu.cens.loci.R;

public class MonthlyCalendar extends Activity {

	private MonthlyCalendarAdapter mCalendarAdapter;
	private static final String INTENT_KEY_YYYYMM = "YYYYMM";
	private Calendar cal = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_main);

        final Intent intent = getIntent();
        Bundle bundle = intent.getExtras();        
        
        if (bundle != null)
        	cal = (Calendar)bundle.getSerializable(INTENT_KEY_YYYYMM);
        if (cal == null)
            cal = Calendar.getInstance();                
    }
    
    @Override
    public void onResume() {
    	super.onResume();
      cal.set(Calendar.DATE, 1);  
			setCalendar(cal);
			
			setDateMoveButtonHandler(R.id.prevYear, Calendar.YEAR, -1);
			setDateMoveButtonHandler(R.id.prevMonth, Calendar.MONTH, -1);
			setDateMoveButtonHandler(R.id.nextYear, Calendar.YEAR, 1);
			setDateMoveButtonHandler(R.id.nextMonth, Calendar.MONTH, 1);
    }
    
    private void setCalendar(Calendar cal) {
    	
    	if (mCalendarAdapter == null)
    		mCalendarAdapter = new MonthlyCalendarAdapter(this, cal);
    	else
    		mCalendarAdapter.setBaseDate(cal);
    		
        Button btn = (Button) findViewById(R.id.prevYear);
        btn.setTag(cal);
        btn = (Button) findViewById(R.id.prevMonth);
        btn.setTag(cal);
        btn = (Button) findViewById(R.id.nextYear);
        btn.setTag(cal);
        btn = (Button) findViewById(R.id.nextMonth);
        btn.setTag(cal);

        TextView tv = (TextView) findViewById(R.id.textview);
        
        String yearMonth = String.format("%s - %s"
        							, Integer.toString(cal.get(Calendar.YEAR))
        							, Integer.toString(cal.get(Calendar.MONTH)+1));
        
        tv.setText(yearMonth);

        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(mCalendarAdapter);
    }
    
    private void setDateMoveButtonHandler(int id, final int YearOrMonth, final int direction) {
        Button btn = (Button) findViewById(id);
		btn.setOnClickListener(new View.OnClickListener() {			

			public void onClick(View v) {
				// TODO Auto-generated method stub
				Calendar cal = (Calendar)v.getTag();
				cal.add(YearOrMonth, direction);
				setCalendar(cal);
			}
		});
    }     
	
	/**
	 * Menu Button
	 */
  /* 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.calendar_menu, menu);
	    
	  	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_day:
			
			// get Date object for today.
        	Calendar c = Calendar.getInstance();
        	c.set(Calendar.HOUR_OF_DAY, 0);
      		c.set(Calendar.MINUTE, 0);
      		c.set(Calendar.SECOND, 0);
      		c.set(Calendar.MILLISECOND, 0);
      		
        	Intent intent = new Intent(MonthlyCalendar.this, DailyCalendar.class);
        	intent.putExtra("date", c.getTime());
        	startActivity(intent);
			
			return true;

		//case R.id.menu_login:
			//startActivity(new Intent(this, LoginActivity.class));
			//finish();
		//	return true;
		}
		return super.onOptionsItemSelected(item);
	}
	*/
}
