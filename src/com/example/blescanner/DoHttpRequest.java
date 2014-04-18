package com.example.blescanner;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

class DoHttpRequest extends AsyncTask<HttpRequestBase, Void, HttpResponse> {
	private final String ZONE = "Application";
	private final String TAG = this.getClass().getSimpleName();
	private final AndroidHttpClient httpclient = AndroidHttpClient
			.newInstance("android");
	private HttpRequestBase request = null;

	@Override
	protected HttpResponse doInBackground(HttpRequestBase... requests) {
		request = requests[0];

		HttpResponse response = null;

		try {
			LogManager.logD(ZONE, TAG, "Do http req");
			response = httpclient.execute(request);
			// StatusLine = response.getStatusLine();
			LogManager.logD(ZONE, TAG, "Got http reponse, status="
					+ response.getStatusLine().getStatusCode());
			HttpEntity entity = response.getEntity();
			if (entity != null) {

			}
			httpclient.close();

		} catch (IOException e) {
			LogManager.logE(ZONE, TAG, "Error in http request", e);
		}
		return response;
	}

	public void cancelRequest() {
		LogManager.logD(ZONE, TAG, "Cancel http req");
		if (request != null)
			request.abort();
		httpclient.close();
		cancel(true);
	}
}