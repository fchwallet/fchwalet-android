package com.breadwallet.presenter.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.fch.BalanceTask;
import com.breadwallet.fch.Cid;
import com.breadwallet.fch.SearchCidTask;
import com.breadwallet.fch.SpUtil;
import com.breadwallet.fch.Utxo;
import com.breadwallet.fch.UtxoTask;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.adapter.CidSearchAdapter;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.wallet.wallets.bitcoin.WalletFchManager;
import com.github.jdsjlzx.interfaces.OnLoadMoreListener;
import com.github.jdsjlzx.recyclerview.LuRecyclerView;
import com.github.jdsjlzx.recyclerview.LuRecyclerViewAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SearchActivity extends BRActivity {

    public static final String ACTIVITY_ACTION = "ACTIVITY_ACTION";
    public static final String ACTION_SEARCH = "ACTION_SEARCH";
    public static final String ACTION_UTXO = "ACTION_UTXO";

    private EditText mEtSearch;
    private ImageView mIvSearch;
    private TextView mEmpty;
    private CidSearchAdapter mCidSearchAdapter;

    private SwipeRefreshLayout mSrlayout;
    private LuRecyclerView mLuRecycler;
    private LuRecyclerViewAdapter mLuAdapter;
    private List<Cid> mList = new ArrayList<Cid>();
    private String mAddress = "";
    private String mText;
    private int mPage = 1;
    private int mSize = 0;
    private boolean mLoadMore = false;

    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mEtSearch = findViewById(R.id.et_search);
        mIvSearch = findViewById(R.id.iv_search);
        mLuRecycler = findViewById(R.id.lurv);
        mSrlayout = findViewById(R.id.srl);
        mEmpty = findViewById(R.id.tv_empty);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_SEARCH)) {
                    String data = intent.getStringExtra("search");
                    mSize = intent.getIntExtra("size", 0);
                    updateList(data);
                } else if (intent.getAction().equals(ACTION_UTXO)) {
                    String utxo = intent.getStringExtra(HomeActivity.KEY_UTXO);
                    refreshBalance(utxo);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTIVITY_ACTION);
        filter.addAction(ACTION_SEARCH);
        filter.addAction(ACTION_UTXO);
        registerReceiver(mReceiver, filter);

        mIvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mText = mEtSearch.getText().toString();
                if (mText.isEmpty()) {
                    return;
                }
                mPage = 1;
                new SearchCidTask(getApplicationContext(), mText, mPage).execute();
            }
        });


        mLuRecycler.setLayoutManager(new LinearLayoutManager(this));
        mLuRecycler.setFooterViewColor(R.color.transparent, R.color.transparent, R.color.transparent);
        mLuRecycler.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mLoadMore = true;
                mPage += 1;
                new SearchCidTask(getApplicationContext(), mText, mPage).execute();
            }
        });

        mCidSearchAdapter = new CidSearchAdapter(this);
        mLuAdapter = new LuRecyclerViewAdapter(mCidSearchAdapter);
        mLuRecycler.setAdapter(mLuAdapter);

        mSrlayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPage = 1;
                mLoadMore = false;
                new SearchCidTask(getApplicationContext(), mText, mPage).execute();
            }
        });
    }

    private void updateList(String data) {
        if (!mLoadMore) {
            mList.clear();
            mAddress = "";
        }

        try {
            JSONArray arr = new JSONArray(data);
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject obj = new JSONObject(arr.get(i).toString());
                String address = obj.get("address").toString();
                String name = obj.get("cid").toString();
                mList.add(new Cid(address, name));

                if (i == 0 && !mAddress.isEmpty()) {
                    mAddress += ",";
                }
                mAddress += address;
                if (i < arr.length() - 1) {
                    mAddress += ",";
                }
            }
        } catch (JSONException e) {

        }
        mCidSearchAdapter.setData(mList);

        if (mList.isEmpty()) {
            mEmpty.setVisibility(View.VISIBLE);
            mSrlayout.setVisibility(View.GONE);
        } else {
            mEmpty.setVisibility(View.GONE);
            mSrlayout.setVisibility(View.VISIBLE);
            new BalanceTask(getApplication(), mAddress).execute();
        }

        mSrlayout.setRefreshing(false);
        if (mPage >= mSize) {
            mLuRecycler.setNoMore(true);
        } else {
            mLuRecycler.setNoMore(false);
        }
    }

    private void refreshBalance(String utxo) {
        Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
        try {
            JSONArray arr = new JSONArray(utxo);
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject obj = new JSONObject(arr.get(i).toString());
                String address = obj.getString("address");
                String amount = obj.getString("amount");

                BigDecimal value = new BigDecimal(amount);
                if (map.containsKey(address)) {
                    value = value.add(map.get(address));
                }
                map.put(address, value);
            }
        } catch (JSONException e) {

        }
        mCidSearchAdapter.setBalance(map);
    }
}
