package com.breadwallet.fch;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.model.PriceChange;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FchPriceTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "FchPriceTask";
    public static final String mUrl = "http://47.110.137.123:4080/api/price";

    private Context mContext;

    public FchPriceTask(Context context) {
        mContext = context;
    }

    private String getPrice() {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException w) {
            Log.e(TAG, String.format("Exception on getPrice : %s", w.toString()));
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
            Log.e(TAG, String.format("Exception on getPrice : %s", e.toString()));
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return data;
    }

    @Override
    protected String doInBackground(String... data) {
        return getPrice();
    }

    @Override
    protected void onPostExecute(String data) {
        if (data != null && data != "") {
            try {
                JSONObject obj = new JSONObject(data);
                String price = obj.getString("cny");
                String change = obj.getString("range");
                SpUtil.put(mContext, price, change);
            } catch (JSONException e) {

            }

            Intent i = new Intent(HomeActivity.ACTIVITY_ACTION);
            i.setAction(HomeActivity.ACTION_PRICE_UPDATED);
            mContext.sendBroadcast(i);
        }
        super.onPostExecute(data);
    }

}