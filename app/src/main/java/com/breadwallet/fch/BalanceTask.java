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

public class BalanceTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "BalanceTask";
    public static final String PREURL = "http://47.244.146.150:8422/api/utxo/findUtxoByAddress?address=";

    private Context mContext;
    private String mUrl;

    public BalanceTask(Context ctx, String address) {
        mContext = ctx;
        mUrl = PREURL + address;
    }

    private String getUtxo() {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException pException) {
            Log.e(TAG, String.format("Exception on getUtxo : %s", pException.toString()));
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
                    text = data.get("utxos").toString();
                    Log.e(TAG, "utxo = " + text);
                } else {
                    Log.e(TAG, "responseCode = " + responseCode);
                }
            }
        } catch (IOException | JSONException | NullPointerException pException) {
            Log.e(TAG, String.format("Exception on getUtxo : %s", pException.toString()));
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return text;
    }

    @Override
    protected String doInBackground(String... pValues) {
        return getUtxo();
    }

    @Override
    protected void onPostExecute(String utxo) {
        if (utxo != null && utxo != "") {
            Intent i = new Intent(SearchActivity.ACTIVITY_ACTION);
            i.setAction(SearchActivity.ACTION_UTXO);
            i.putExtra(HomeActivity.KEY_UTXO, utxo);
            mContext.sendBroadcast(i);
        }
        super.onPostExecute(utxo);
    }

}