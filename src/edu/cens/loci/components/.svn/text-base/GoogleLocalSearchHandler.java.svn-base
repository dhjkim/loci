package edu.cens.loci.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.MyLog;

public class GoogleLocalSearchHandler extends AsyncTask<String, Void, String>{

	public static final String TAG = "GoogleLocalSearchHandler";
	
	private Location mCenter;
	
	private Context mContext;
	private GoogleLocalSearchListener mListener;
	
	public interface GoogleLocalSearchListener {
		public void onSearchResults(ArrayList<GoogleLocalSearchResult> results);
	}	
	
	public class GoogleLocalSearchResult {
		public String title;
		public String address;
		public String city;
		public String region;
		public String country;
		public String url;
		public double latitude;
		public double longitude;
		
		public void debug() {
			Log.d(TAG, "name :" + title);
			Log.d(TAG, "address : " + address);
			Log.d(TAG, "city : " + city);
			Log.d(TAG, "region : " + region);
			Log.d(TAG, "country : " + country);
			Log.d(TAG, "lat : " + latitude);
			Log.d(TAG, "lng : " + longitude);
			Log.d(TAG, "url : " + url);
		}
	}
	
	public GoogleLocalSearchHandler(Context context, GoogleLocalSearchListener listener, Location center) {
		mContext = context;
		mListener = listener;
		mCenter = center;
	}

	@Override
	protected void onPreExecute() {
		//mProgressDialog = ProgressDialog.show(mContext, "", "Searching nearby places...", true);
	}
	
	@Override
	protected String doInBackground(String... params) {
		
		String url = null;
		String keyword = params[0];
		
		String serverResponse = "";
		String thisLine = "";
		
		try {
			url = new String(LociConfig.LOCAL_SEARCH_URL + "?v=1.0&sll=" + mCenter.getLatitude() + "," + mCenter.getLongitude() + "&q="
	        + URLEncoder.encode(keyword, "UTF-8")
	        + "&rsz=8" 
	        + "&key=" + LociConfig.MAP_KEY);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		
		HttpGet httpGet = new HttpGet(url);
		HttpClient httpClient = new DefaultHttpClient();
		
		try {
			HttpResponse response = httpClient.execute(httpGet);
			
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
					
				while((thisLine = br.readLine()) != null) {
					serverResponse += thisLine;
				}
			}
	
		} catch (MalformedURLException me) {
	   	me.printStackTrace();
	  } catch (UnsupportedEncodingException ue) {
	  	ue.printStackTrace();
	  } catch (IOException ie) {
	   	ie.printStackTrace();
	  }
	 
		return serverResponse;
	}

	@Override
	protected void onCancelled () {
		
		MyLog.i(LociConfig.D.UI.PLACE_SEARCH, TAG, "onCancelled");
		
		Toast.makeText(mContext, "Search has been canceled.", Toast.LENGTH_SHORT).show();
		//if (mProgressDialog != null)
		//	mProgressDialog.dismiss();
	}
	
	@Override
	protected void onPostExecute(String response) {

	  //mProgressDialog.dismiss();
	  //mProgressDialog = null;
		
		try {
			JSONObject jsonObj = new JSONObject(response);
			
			mListener.onSearchResults(getResults(jsonObj.getJSONObject("responseData")));
			
			//JSONObject jsonObj = jsonObj.getJSONObject("responseData");
			//JSONObject jCursor = jsonObj.getJSONObject("cursor");
			//if(jCursor.has("pages")) {
			//	JSONArray jPages = jCursor.getJSONArray("pages");
			//}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		 
		
	}
	
	private ArrayList<GoogleLocalSearchResult> getResults(JSONObject jsonObj) {
		
		ArrayList<GoogleLocalSearchResult> results = new ArrayList<GoogleLocalSearchResult>();
		
		JSONArray jsonArr;
		JSONObject jsonArrItem;
		try {
			jsonArr = jsonObj.getJSONArray("results");
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
			
		for(int i = 0 ; i < jsonArr.length() ; i++) {
			
			try {
				jsonArrItem = jsonArr.getJSONObject(i);
			} catch (JSONException e) {
				continue;
			}

			GoogleLocalSearchResult result = new GoogleLocalSearchResult();
			
			try {
				result.title = jsonArrItem.getString("titleNoFormatting");
			} catch (JSONException e) {
				continue;
			}
			try {
				result.address = jsonArrItem.getString("streetAddress");
				result.city = jsonArrItem.getString("city");
				result.region = jsonArrItem.getString("region");
				result.country = jsonArrItem.getString("country");
			} catch (JSONException e) {
				result.address = "";
				result.city = "";
				result.region = "";
				result.country = "";
			}
			
			try {
				result.latitude = jsonArrItem.getDouble("lat");
				result.longitude = jsonArrItem.getDouble("lng");
			} catch (JSONException e) {
				result.latitude = Double.MAX_VALUE;
				result.longitude = Double.MAX_VALUE;
			}
			try {
				result.url = URLDecoder.decode(jsonArrItem.getString("url"));
			} catch (JSONException e) {
				result.url = null;
			}
			
			results.add(result);
		}
			
		return results;
	}
}
