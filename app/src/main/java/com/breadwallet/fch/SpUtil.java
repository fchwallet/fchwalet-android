package com.breadwallet.fch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SpUtil {

    private final static String KEY_ADDRESS = "cid_address";
    private final static String KEY_CID = "cid_key";
    private final static String KEY_TXID = "cid_txid";

    public static void put(Context context, String price, String change) {
        SharedPreferences sp = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("price", price);
        editor.putString("change", change);
        editor.apply();
    }

    public static String get(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        if (preferences.contains(key)) {
            return preferences.getString(key, "0");
        }
        return "0";
    }

    public static void putAddress(Context context, List<String> addresses) {
        SharedPreferences sp = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String value = "";
        for (int i = 0; i < addresses.size(); ++i) {
            value += addresses.get(i);
            if (i < addresses.size() - 1) {
                value += ",";
            }
        }
        editor.putString(KEY_ADDRESS, value);
        editor.apply();
    }

    public static List<String> getAddress(Context context) {
        List<String> list = new ArrayList<String>();
        SharedPreferences preferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        if (preferences.contains(KEY_ADDRESS)) {
            String value = preferences.getString(KEY_ADDRESS, "");
            if (value.length() > 10) {
                String[] strs = value.split(",");
                for (String s: strs) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    public static void putCid(Context context, List<Cid> list) {
        SharedPreferences sp = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String value = "";
        for (int i = 0; i < list.size(); ++i) {
            Cid c = list.get(i);
            value += c.getAddress();
            value += ",";
            value += c.getName();
            value += ",";
            value += c.getTxid();
            if (i < list.size() - 1) {
                value += ",";
            }
        }
        Log.e("####", "putCid = " + value);
        editor.putString(KEY_CID, value);
        editor.apply();
    }

    public static List<Cid> getCid(Context context) {
        List<Cid> list = new ArrayList<Cid>();
        SharedPreferences preferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        if (preferences.contains(KEY_CID)) {
            String value = preferences.getString(KEY_CID, "");
            Log.e("####", "getCid = " + value);
            if (value.length() > 20) {
                String[] strs = value.split(",");
                for (int i = 0; i < strs.length; i += 3) {
                    Cid c = new Cid(strs[i], strs[i + 1], strs[i + 2]);
                    list.add(c);
                }
            }
        }
        return list;
    }

    public static void putTxid(Context context, List<String> ids) {
        SharedPreferences sp = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String value = "";
        for (int i = 0; i < ids.size(); ++i) {
            value += ids.get(i);
            if (i < ids.size() - 1) {
                value += ",";
            }
        }
        Log.e("####", "putTxid = " + value);
        editor.putString(KEY_TXID, value);
        editor.apply();
    }

    public static List<String> getTxid(Context context) {
        List<String> list = new ArrayList<String>();
        SharedPreferences preferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        if (preferences.contains(KEY_TXID)) {
            String value = preferences.getString(KEY_TXID, "");
            Log.e("####", "getTxid = " + value);
            String[] strs = value.split(",");
            for (String s: strs) {
                list.add(s);
            }
        }
        return list;
    }

}
