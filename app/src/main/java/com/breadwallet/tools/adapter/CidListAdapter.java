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
import java.util.List;
import java.util.Map;

public class CidListAdapter extends RecyclerView.Adapter<CidListAdapter.CidViewHolder> {
    public static final String TAG = CidListAdapter.class.getName();

    private static final int VIEW_TYPE_CID = 0;
    private static final int VIEW_TYPE_ADD_CID = 1;
    private final Context mContext;
    private List<Cid> mList;

    public CidListAdapter(Context context) {
        this.mContext = context;
        mList = new ArrayList<>();
    }

    public void setData(List<Cid> list) {
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public CidViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView;

        if (viewType == VIEW_TYPE_CID) {
            convertView = inflater.inflate(R.layout.cid_list_item, parent, false);
            return new DecoratedCidItemViewHolder(convertView);
        } else if (viewType == VIEW_TYPE_ADD_CID) {
            convertView = inflater.inflate(R.layout.add_cid_item, parent, false);
            return new CidViewHolder(convertView);
        } else {
            throw new IllegalArgumentException("Invalid type: " + viewType);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mList.size()) {
            return VIEW_TYPE_CID;
        } else {
            return VIEW_TYPE_ADD_CID;
        }
    }

    public Cid getItemAt(int position) {
        if (position < mList.size()) {
            return mList.get(position);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(CidViewHolder holderView, int position) {
        if (getItemViewType(position) == VIEW_TYPE_CID) {
            DecoratedCidItemViewHolder decoratedHolderView = (DecoratedCidItemViewHolder) holderView;
            Cid cid = mList.get(position);
            decoratedHolderView.mName.setText(cid.getName());

            Map<String, Integer> m = DataCache.getInstance().getBalance();
            if (m.containsKey(cid.getAddress())) {
                int balance = m.get(cid.getAddress());
                BigDecimal bd = new BigDecimal(balance).divide(WalletFchManager.ONE_FCH_BD);
                decoratedHolderView.mBalance.setText(bd.toString());
            } else {
                decoratedHolderView.mBalance.setText("0");
            }
        }
    }

    @Override
    public int getItemCount() {
        return mList.size() + 1;
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
