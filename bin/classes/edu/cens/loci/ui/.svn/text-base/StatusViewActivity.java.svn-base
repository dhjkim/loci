package edu.cens.loci.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.components.ILociManager;
import edu.cens.loci.components.LociManagerService;
import edu.cens.loci.utils.MyLog;

public class StatusViewActivity extends ListActivity {

	private static final String TAG = "StatusViewActivity";
	
	private Drawable mGreenIcon;
	private Drawable mRedIcon;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Loci Service Status");
		mGreenIcon = this.getResources().getDrawable(R.drawable.status_circle_green_m);
		mRedIcon = this.getResources().getDrawable(R.drawable.status_circle_red_m);
	}

	@Override
	public void onRestart() {
		super.onRestart();
		bindToService();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		unbindToService();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		bindToService();
	}
	
	@Override
	public void onPause() {
		unbindToService();
		super.onPause();
	}
	
	@Override
	public void onStop() {
		unbindToService(); // safe-net
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		}
	}

	
	/**
	 * Check if LociManagerService is running
	 * @return true if running
	 */
	public boolean isServRunning() {
		boolean isRunning = false;
		
		ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> runningServ = am.getRunningServices(100);
		
		for (ActivityManager.RunningServiceInfo serv : runningServ) {
			if (serv.service.getClassName().equals(LociManagerService.SERV_NAME)) {
				isRunning = true;
			}
		}
		return isRunning;
	}
	
	
	private boolean mIsBound = false;
	private static ILociManager mService = null;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			//MyLog.d(LociConfig.Debug.UI.LOG_CALL, TAG, "onServiceConnected() <= ILociManager connected");
			mService = ILociManager.Stub.asInterface(service);
			mIsBound = true;
			updateView();
		}
		public void onServiceDisconnected(ComponentName className) {
			//MyLog.d(LociConfig.Debug.UI.LOG_CALL, TAG, "onServiceDisconnected()");
			mService = null;
			mIsBound = false;
		}
	};
	
	private void bindToService() {
		
		boolean isServiceOn = isServRunning();
		
		//String action = ILociManager.class.getName();
		//MyLog.d(true, TAG, "bindToService() : action = " + action);
		
		if (isServiceOn) {
      bindService(new Intent(ILociManager.class.getName()),
          mConnection, Context.BIND_AUTO_CREATE);
		} else {
			//MyLog.i(LociConfig.Debug.UI.LOG_CALL, TAG, "bindToService() : service is not running. not binding.");
		}

	}
	
	private void unbindToService() {
		if (mIsBound) {
      // Detach our existing connection.
      unbindService(mConnection);
      mIsBound = false;
		}		
	}
	
	////////////////////////
	//// ListView
	////////////////////////
	
	static final class StatusListItem implements Serializable {
		
		private static final long	serialVersionUID	= 1L;
		public String name = "";
		public boolean status = false;
		
		public StatusListItem(String name, boolean status) {
			this.name = name;
			this.status = status;
		}
		
		public String toString() {
			String s = "name=" + name + ", status=" + status; 
			return s;
		}
	}
	
	
	private void updateView() {
		//MyLog.d(LociConfig.LOG_ERROR, TAG, "updateView() : loadPowerState=" + loadPowerState(this));

		if (!mIsBound) {
			//MyLog.d(LociConfig.LOG_ERROR, TAG, "updateView() : Service in not bound.");
			setTitle("Loci Service Status (Service not conneted).");
			return;
		}
		
		ArrayList<StatusListItem> list = new ArrayList<StatusListItem>();
		try {
			boolean pdOn = mService.isPlaceDetectorOn();
			boolean ptOn = mService.isPathTrackerOn();
			boolean mdOn = mService.isMovementDetectorOn();
			
			list.add(new StatusListItem("Place Detector", pdOn));
			list.add(new StatusListItem("Path Tracker", ptOn));
			list.add(new StatusListItem("Movement Detector", mdOn));
		} catch (RemoteException e) {
			MyLog.e(LociConfig.D.UI.STATUS, TAG, "updateView() : Remote Exception while getting service status");
			e.printStackTrace();
			Toast.makeText(this, "Connecting to the service failed.", Toast.LENGTH_SHORT).show();
			return;
		}
		StatusItemAdapter adapter = new StatusItemAdapter(this, R.layout.status_list_item, list);
		setListAdapter(adapter);
	}

	public class StatusItemAdapter extends BaseAdapter {

		//private Context mCxt;
		private LayoutInflater mInflater;
		private ArrayList<StatusListItem> mList;
		private int mLayout;
		
		public StatusItemAdapter(Context context, int layout, ArrayList<StatusListItem> list) {
			//mCxt = context;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
			mList = list;
		}
		
		public int getCount() {
			return mList.size();
		}

		public Object getItem(int position) {
			return mList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			
			final int pos = position;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mLayout, parent, false);
			}

			ImageView img = (ImageView) convertView.findViewById(R.id.img);

			if (mList.get(pos).status) {
				img.setImageDrawable(mGreenIcon);
			} else {
				img.setImageDrawable(mRedIcon);
			}
			
			TextView txt1 = (TextView) convertView.findViewById(R.id.text1);
			txt1.setText(mList.get(pos).name);
			
			return convertView;
		}
	}
	
}
