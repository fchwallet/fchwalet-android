package com.breadwallet.fch;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.presenter.activities.CidDetailActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TxHistoryTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "TxHistoryTask";
    public static final String PREURL = "http://47.244.146.150:8422/api/history/list";

    private Context mContext;
    private String mUrl;

    public TxHistoryTask(Context ctx, String address, int page) {
        mContext = ctx;
        mUrl = PREURL + "?page=" + page + "&address=" + address;
    }

    private String getTx() {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException pException) {
            Log.e(TAG, String.format("Exception on getTx : %s", pException.toString()));
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
                    Log.e(TAG, "data = " + text);
                } else {
                    Log.e(TAG, "responseCode = " + responseCode);
                }
            }
        } catch (IOException | JSONException | NullPointerException pException) {
            Log.e(TAG, String.format("Exception on getTx : %s", pException.toString()));
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return text;
    }

    @Override
    protected String doInBackground(String... pValues) {
        return getTx();
    }

    @Override
    protected void onPostExecute(String data) {
        if (data != null && data != "") {
            Intent i = new Intent(CidDetailActivity.ACTIVITY_ACTION);
            i.setAction(CidDetailActivity.ACTION_HISTORY);
            i.putExtra("history", data);
            mContext.sendBroadcast(i);
        }
        super.onPostExecute(data);
    }

}