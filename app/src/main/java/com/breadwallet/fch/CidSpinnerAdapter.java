package com.breadwallet.fch;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;

import java.util.ArrayList;
import java.util.List;

public class CidSpinnerAdapter extends BaseAdapter {

    private Context mContext;
    private List<String> mList = new ArrayList<String>();

    public CidSpinnerAdapter(Context ctx, List<String> list) {
        mContext = ctx;
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public String getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.spinner_cid_item, null);
            new ViewHolder(convertView);
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.tv.setText(mList.get(position));
        return convertView;
    }

    class ViewHolder {
        TextView tv;

        public ViewHolder(View convertView){
            tv = (TextView) convertView.findViewById(R.id.item_spinner);
            convertView.setTag(this);
        }
    }
}



