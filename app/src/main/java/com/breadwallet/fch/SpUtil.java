package com.breadwallet.fch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SpUtil {

    private final static String SP_NAME = "sp";
    public final static String KEY_PRICE = "price";
    public final static String KEY_CHANGE = "change";

    private final static String KEY_ADDRESS = "address_list";
    private final static String KEY_CID = "cid_list";
    private final static String KEY_TXID = "txid_list";
    private final static String KEY_PENDING = "pending_list";
    private final static String KEY_MONITOR = "monitor_list";

    public static void put(Context context, String price, String change) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_PRICE, price);
        editor.putString(KEY_CHANGE, change);
        editor.apply();
    }

    public static String get(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (preferences.contains(key)) {
            return preferences.getString(key, "0");
        }
        return "0";
    }

    public static void putAddress(Context context, List<String> list) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String value = "";
        for (int i = 0; i < list.size(); ++i) {
            value += list.get(i);
            if (i < list.size() - 1) {
                value += ",";
            }
        }
        Log.e("####", "putAddress = " + value);
        editor.putString(KEY_ADDRESS, value);
        editor.apply();
    }

    public static List<String> getAddress(Context context) {
        List<String> list = new ArrayList<String>();
        SharedPreferences preferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (preferences.contains(KEY_ADDRESS)) {
            String value = preferences.getString(KEY_ADDRESS, "");
            Log.e("####", "getAddress = " + value);
            if (value.length() > 10) {
                for (String s : value.split(",")) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    public static void putCid(Context context, List<Cid> list) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String value = "";
        for (int i = 0; i < list.size(); ++i) {
            Cid c = list.get(i);
            value += c.getAddress();
            value += ",";
            value += c.getName();
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
        SharedPreferences preferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (preferences.contains(KEY_CID)) {
            String value = preferences.getString(KEY_CID, "");
            Log.e("####", "getCid = " + value);
            if (value.length() > 20) {
                String[] ss = value.split(",");
                for (int i = 0; i < ss.length; i += 2) {
                    Cid c = new Cid(ss[i], ss[i + 1]);
                    list.add(c);
                }
            }
        }
        return list;
    }

    public static void putTxid(Context context, List<String> ids) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
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
        SharedPreferences preferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (preferences.contains(KEY_TXID)) {
            String value = preferences.getString(KEY_TXID, "");
            Log.e("####", "getTxid = " + value);
            for (String s : value.split(",")) {
                list.add(s);
            }
        }
        return list;
    }

    public static void putPending(Context context, List<Utxo> list) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String value = "";
        for (int i = 0; i < list.size(); ++i) {
            Utxo u = list.get(i);
            value += u.getTxid();
            value += ",";
            value += u.getAddress();
            value += ",";
            value += u.getAmount();
            value += ",";
            value += u.getVout();
            if (i < list.size() - 1) {
                value += ",";
            }
        }
        Log.e("####", "putPending = " + value);
        editor.putString(KEY_PENDING, value);
        editor.apply();
    }

    public static List<Utxo> getPending(Context context) {
        List<Utxo> list = new ArrayList<Utxo>();
        SharedPreferences preferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (preferences.contains(KEY_PENDING)) {
            String value = preferences.getString(KEY_PENDING, "");
            Log.e("####", "getPending = " + value);
            if (value.length() > 20) {
                String[] ss = value.split(",");
                for (int i = 0; i < ss.length; i += 4) {
                    long amount = Long.parseLong(ss[i + 2]);
                    int vout = Integer.parseInt(ss[i + 3]);
                    Utxo u = new Utxo(ss[i], ss[i + 1], amount, vout);
                    list.add(u);
                }
            }
        }
        return list;
    }

    public static void putMonitorAddress(Context context, List<String> list) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String value = "";
        for (int i = 0; i < list.size(); ++i) {
            value += list.get(i);
            if (i < list.size() - 1) {
                value += ",";
            }
        }
        editor.putString(KEY_MONITOR, value);
        editor.apply();
    }

    public static List<String> getMonitorAddress(Context context) {
        List<String> list = new ArrayList<String>();
        SharedPreferences preferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (preferences.contains(KEY_MONITOR)) {
            String value = preferences.getString(KEY_MONITOR, "");
            if (value.length() > 10) {
                for (String s : value.split(",")) {
                    list.add(s);
                }
            }
        }
        return list;
    }
}
