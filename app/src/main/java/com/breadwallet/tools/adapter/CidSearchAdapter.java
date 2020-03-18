package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.fch.Cid;
import com.breadwallet.fch.DataCache;
import com.breadwallet.wallet.wallets.bitcoin.WalletFchManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CidSearchAdapter extends RecyclerView.Adapter<CidSearchAdapter.CidViewHolder> {
    public static final String TAG = CidSearchAdapter.class.getName();

    private final Context mContext;
    private List<Cid> mList;
    private Map<String, BigDecimal> mBalance = new HashMap<String, BigDecimal>();

    public CidSearchAdapter(Context context) {
        this.mContext = context;
        mList = new ArrayList<>();
    }

    public void setData(List<Cid> list) {
        mList = list;
        notifyDataSetChanged();
    }

    public void setBalance(Map<String, BigDecimal> map) {
        mBalance = map;
        notifyDataSetChanged();
    }

    @Override
    public CidViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.cid_list_item, parent, false);
        return new DecoratedCidItemViewHolder(convertView);
    }

    public Cid getItemAt(int position) {
        return mList.get(position);
    }

    @Override
    public void onBindViewHolder(CidViewHolder holderView, int position) {
        DecoratedCidItemViewHolder decoratedHolderView = (DecoratedCidItemViewHolder) holderView;
        Cid cid = mList.get(position);
        decoratedHolderView.mName.setText(cid.getName());

        if (mBalance.containsKey(cid.getAddress())) {
            BigDecimal balance = mBalance.get(cid.getAddress());
            decoratedHolderView.mBalance.setText(balance + "");
        } else {
            decoratedHolderView.mBalance.setText("0");
        }
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
        private TextView mBalance;

        private DecoratedCidItemViewHolder(View view) {
            super(view);
            mName = view.findViewById(R.id.item_cid_name);
            mBalance = view.findViewById(R.id.item_cid_balance);
        }
    }
}
