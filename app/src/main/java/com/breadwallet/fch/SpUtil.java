package com.breadwallet.fch;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtil {

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
}
