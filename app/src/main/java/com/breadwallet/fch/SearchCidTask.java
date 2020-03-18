package com.breadwallet.fch;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.SearchActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SearchCidTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "SearchCidTask";
    public static final String PREURL = "http://sp.fchwallet.com:8422/api/cid/getcid";

    private Context mContext;
    private String mUrl;
    private int mSize = 0;

    public SearchCidTask(Context ctx, String text, int page) {
        mContext = ctx;
        mUrl = PREURL + "?cid=" + text + "&page=" + page;
    }

    private String search() {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException pException) {
            Log.e(TAG, String.format("Exception on search : %s", pException.toString()));
            return "";
        }

        String text = "";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            if (connection != null) {
                int responseCode = connection.getResponseCode();
                if (responseCode < 300) {
                    BufferedReader response = new BufferedReader(new InputStreamReader(
                            connection.getInputStream()));

                    String inputLine;
                    while ((inputLine = response.readLine()) != null) {
                        text += inputLine + '\n';
                    }

                    JSONObject jo = new JSONObject(text);
                    JSONObject data = new JSONObject(jo.get("data").toString());
                    text = data.get("contracts").toString();
                    mSize = Integer.parseInt(data.get("size").toString());
                    Log.e(TAG, "data = " + data.toString());
                } else {
                    Log.e(TAG, "responseCode = " + responseCode);
                }
            }
        } catch (IOException | JSONException | NullPointerException pException) {
            Log.e(TAG, String.format("Exception on search : %s", pException.toString()));
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return text;
    }

    @Override
    protected String doInBackground(String... pValues) {
        return search();
    }

    @Override
    protected void onPostExecute(String data) {
        if (data != null && data != "") {
            Intent i = new Intent(SearchActivity.ACTIVITY_ACTION);
            i.setAction(SearchActivity.ACTION_SEARCH);
            i.putExtra("search", data);
            i.putExtra("size", mSize);
            mContext.sendBroadcast(i);
        }
        super.onPostExecute(data);
    }

}