package com.breadwallet.fch;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.ui.wallet.WalletActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class KLineTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "KLineTask";
    public static final String mUrl = "http://api.fchwallet.com:4080/api/getKlin";

    private Context mContext;
    private String mInterval;

    public KLineTask(Context context, String interval) {
        mContext = context;
        mInterval = interval;
    }

    private String post() {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(mUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("accept", "application/json");

            JSONObject jo = new JSONObject();
            jo.put("type", mInterval);
            String json = jo.toString();

            // 往服务器里面发送数据
            byte[] writebytes = json.getBytes();
            // 设置文件长度
            connection.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(json.getBytes());
            outputStream.flush();
            outputStream.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection
                        .getInputStream()));
                String temp;
                while ((temp = reader.readLine()) != null) {
                    sb.append(temp);
                }
                reader.close();
                Log.e(TAG, "data = " + sb.toString());
                return sb.toString();
            } else {
                Log.e(TAG, "responseCode = " + connection.getResponseCode());
            }
            connection.disconnect();
        } catch (Exception e) {
            return e.toString();
        }
        return "";
    }

    @Override
    protected String doInBackground(String... data) {
        return post();
    }

    @Override
    protected void onPostExecute(String data) {
        if (data != null && data != "") {
            Intent i = new Intent(WalletActivity.WALLET_ACTION);
            i.setAction(WalletActivity.ACTION_KLINE_UPDATE);
            i.putExtra("kline", data);
            mContext.sendBroadcast(i);
        }
        super.onPostExecute(data);
    }

}