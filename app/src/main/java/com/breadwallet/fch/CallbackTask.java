package com.breadwallet.fch;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.presenter.activities.SignActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CallbackTask extends AsyncTask<String, Integer, String> {

    public static final String TAG = "CallbackTask";

    private Context mContext;
    private String mUrl, mMessage, mAddress, mSignature;
    private String mParams;

    public CallbackTask(Context ctx, String url, String params) {
        mContext = ctx;
        mUrl = url;
        mParams = params;
    }

    public CallbackTask(Context ctx, String url, String msg, String address, String signature) {
        mContext = ctx;
        mUrl = url;
        mMessage = msg;
        mAddress = address;
        mSignature = signature;
    }

    private String post() {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException pException) {
            Log.e(TAG, String.format("Exception on post: %s", pException.toString()));
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
                    Log.e(TAG, "callback data = " + text);
                } else {
                    Log.e(TAG, "responseCode = " + responseCode);
                }
            }
        } catch (IOException | NullPointerException pException) {
            Log.e(TAG, String.format("Exception on post: %s", pException.toString()));
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return text;
    }

    private String post2() {
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
            connection.setRequestProperty("Content-Type", "application/json;");
            connection.setRequestProperty("accept", "application/json");

            JSONObject jo = new JSONObject();
            jo.put("data", mParams);
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
    protected String doInBackground(String... pValues) {
        return post2();
    }

    @Override
    protected void onPostExecute(String data) {
        if (data != null && data != "") {
            Intent i = new Intent(SignActivity.ACTIVITY_ACTION);
            i.setAction(SignActivity.ACTION_CALLBACK);
            i.putExtra("callback", data);
            mContext.sendBroadcast(i);
        }
        super.onPostExecute(data);
    }

}