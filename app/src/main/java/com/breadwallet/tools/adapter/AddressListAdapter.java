package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;

import java.util.ArrayList;
import java.util.List;

public class AddressListAdapter extends RecyclerView.Adapter<AddressListAdapter.CidViewHolder> {
    public static final String TAG = AddressListAdapter.class.getName();

    private final Context mContext;
    private List<String> mList = new ArrayList<String>();

    public AddressListAdapter(Context context) {
        this.mContext = context;
    }

    public void setData(List<String> list) {
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public CidViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.address_list_item, parent, false);
        return new DecoratedCidItemViewHolder(convertView);
    }

    @Override
    public void onBindViewHolder(CidViewHolder holderView, int position) {
        DecoratedCidItemViewHolder decoratedHolderView = (DecoratedCidItemViewHolder) holderView;
        String address = mList.get(position);
        decoratedHolderView.mName.setText(address);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class CidViewHolder extends RecyclerView.ViewHolder {
        CidViewHolder(View view) {
            super(view);
        }
    }

    private class DecoratedCidItemViewHolder extends CidViewHolder {
        private TextView mName;

        private DecoratedCidItemViewHolder(View view) {
            super(view);
            mName = view.findViewById(R.id.tv_address);
        }
    }
}
