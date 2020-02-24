package com.breadwallet.fch;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.BuildConfig;
import com.breadwallet.presenter.activities.HomeActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AppUpdateTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "AppUpdateTask";
    public static final String mUrl = "http://api.fchwallet.com:4080/api/version";

    private Context mContext;

    public AppUpdateTask(Context context) {
        mContext = context;
    }

    private String getApp() {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException w) {
            Log.e(TAG, String.format("Exception on getApp : %s", w.toString()));
            return "";
        }

        String data = "";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (connection != null) {
                int responseCode = connection.getResponseCode();
                if (responseCode < 300) {
                    BufferedReader response = new BufferedReader(new InputStreamReader(
                            connection.getInputStream()));

                    String inputLine;
                    while ((inputLine = response.readLine()) != null) {
                        data += inputLine + '\n';
                    }
                    Log.e(TAG, "data = " + data);
                } else {
                    Log.e(TAG, "responseCode = " + responseCode);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, String.format("Exception on getApp : %s", e.toString()));
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return data;
    }

    @Override
    protected String doInBackground(String... data) {
        return getApp();
    }

    @Override
    protected void onPostExecute(String data) {
        if (data != null && data != "") {
            try {
                JSONObject obj = new JSONObject(data);
                String version = obj.getString("version");

                if (BuildConfig.VERSION_NAME.compareTo(version) < 0) {
                    Intent i = new Intent(HomeActivity.ACTIVITY_ACTION);
                    i.setAction(HomeActivity.ACTION_APP_UPDATE);
                    i.putExtra("download", obj.getString("download"));
                    i.putExtra("version", version);
                    mContext.sendBroadcast(i);
                }
            } catch (JSONException ex) {

            }
        }
        super.onPostExecute(data);
    }

}