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
package edu.cens.loci.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

public class GooglePlaceAPIHandler extends AsyncTask<Void, Void, Void>{

	public static final String URL = "https://maps.googleapis.com/maps/api/place/search/json?";
	public static final String API_KEY = "AIzaSyBo3S4CbBBZBwNn2-wMJ4mYSjAtTuvgdxs";
	
	private Context mContext;
	private Location mCenter;
	private float mRadius;
	private String mKeyword;
	
	public GooglePlaceAPIHandler(Context context, Location center, float radius, String keyword) {
		mContext = context;
		mCenter = center;
		mRadius = radius;
		mKeyword = keyword;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		
		String url = null;
		
		try {
				url= new String(URL
					+ "location=" + mCenter.getLatitude() + "," + mCenter.getLongitude()
					+ "&radius=" + mRadius 
					+ "&name=" + URLEncoder.encode(mKeyword, "UTF-8")
					+ "&sensor=true"
					+ "&key=" + API_KEY
			);
		
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		HttpGet httpGet = new HttpGet(url);
		HttpClient httpClient = new DefaultHttpClient();
		
		Log.d("D", url);
		
		try {
			
			HttpResponse response = httpClient.execute(httpGet);
			
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String serverResponse = "";
				String thisLine = "";
					
				while((thisLine = br.readLine()) != null) {
					serverResponse += thisLine;
				}
					
				Log.i("D", "" + serverResponse);
			
			}
	
		} catch (MalformedURLException me) {
	   	me.printStackTrace();
	  } catch (UnsupportedEncodingException ue) {
	  	ue.printStackTrace();
	  } catch (IOException ie) {
	   	ie.printStackTrace();
	  }

		
		return null;
	}

}
