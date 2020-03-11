package com.breadwallet.fch;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.presenter.activities.HomeActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CidTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "CidTask";
    public static final String PREURL = "http://47.244.146.150:8422/api/cid/getByAddress?address=";

    private Context mContext;
    private String mUrl;

    public CidTask(Context ctx, String address) {
        mContext = ctx;
        mUrl = PREURL + address;
    }

    private String getCid() {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException pException) {
            Log.e(TAG, String.format("Exception on getCid : %s", pException.toString()));
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
                    text = data.get("list").toString();
                    Log.e(TAG, "list = " + text);
                } else {
                    Log.e(TAG, "responseCode = " + responseCode);
                }
            }
        } catch (IOException | JSONException | NullPointerException pException) {
            Log.e(TAG, String.format("Exception on getCid : %s", pException.toString()));
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return text;
    }

    @Override
    protected String doInBackground(String... pValues) {
        return getCid();
    }

    @Override
    protected void onPostExecute(String list) {
        if (list != null && list != "") {
            Intent i = new Intent(HomeActivity.ACTIVITY_ACTION);
            i.setAction(HomeActivity.ACTION_CID_UPDATE);
            i.putExtra(HomeActivity.KEY_CID, list);
            mContext.sendBroadcast(i);
        }
        super.onPostExecute(list);
    }

}