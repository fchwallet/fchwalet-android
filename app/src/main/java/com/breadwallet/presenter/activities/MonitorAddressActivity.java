package com.breadwallet.presenter.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.fch.MonitorTask;
import com.breadwallet.fch.SpUtil;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.adapter.MonitorAddressAdapter;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitorAddressActivity extends BRActivity {

    public static final String ACTIVITY_ACTION = "ACTIVITY_ACTION";
    public static final String ACTION_UTXO = "ACTION_UTXO";

    private RecyclerView mRecycler;
    private ImageView mIvAdd;
    private LinearLayout mEditLayout;
    private EditText mEtAddress;
    private TextView mBtnPaste, mBtnAdd;

    private BaseWalletManager mWalletManager;
    private MonitorAddressAdapter mAdapter;
    private List<String> mAddressList = new ArrayList<String>();
    private String mAddressString = "";

    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor_address);
        mRecycler = findViewById(R.id.rv_address);
        mIvAdd = findViewById(R.id.iv_add);
        mEditLayout = findViewById(R.id.ll_edit);
        mEtAddress = findViewById(R.id.et_address);
        mBtnPaste = findViewById(R.id.monitor_paste);
        mBtnAdd = findViewById(R.id.monitor_add);

        initBroadcast();

        mWalletManager = WalletsMaster.getInstance().getCurrentWallet(this);

        mAdapter = new MonitorAddressAdapter(this);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

        mAddressList = SpUtil.getMonitorAddress(this);
        mAdapter.setData(mAddressList);

        for (int i = 0; i < mAddressList.size(); ++i) {
            mAddressString += mAddressList.get(i);
            if (i < mAddressList.size() - 1) {
                mAddressString += ",";
            }
        }
        if (!mAddressString.isEmpty()) {
            new MonitorTask(getApplication(), mAddressString).execute();
        }

        mIvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditView(true);
            }
        });
        mBtnPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = BRClipboardManager.getClipboard(MonitorAddressActivity.this);
                mEtAddress.setText(text);
            }
        });
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String address = mEtAddress.getText().toString();
                if (mWalletManager.isAddressValid(address)) {
                    if (mWalletManager.containsAddress(address)) {
                        BRDialog.showCustomDialog(MonitorAddressActivity.this, "", getResources().getString(R.string.own_address),
                                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismiss();
                                    }
                                }, null, null, 0);
                    } else {
                        if (mAddressString.isEmpty()) {
                            mAddressString += address;
                        } else {
                            mAddressString += ",";
                            mAddressString += address;
                        }
                        mAddressList.add(address);
                        mAdapter.setData(mAddressList);
                        SpUtil.putMonitorAddress(MonitorAddressActivity.this, mAddressList);
                        showEditView(false);
                        new MonitorTask(getApplication(), mAddressString).execute();
                    }
                } else {
                    BRDialog.showCustomDialog(MonitorAddressActivity.this, "", getResources().getString(R.string.Send_invalidAddressTitle),
                            getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                }
            }
        });
    }

    private void initBroadcast() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_UTXO)) {
                    String utxo = intent.getStringExtra(HomeActivity.KEY_UTXO);
                    refreshBalance(utxo);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTIVITY_ACTION);
        filter.addAction(ACTION_UTXO);
        registerReceiver(mReceiver, filter);
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
        mAdapter.setBalance(map);
    }

    private void showEditView(boolean visible) {
        if (visible) {
            mEditLayout.setVisibility(View.VISIBLE);
            mRecycler.setVisibility(View.GONE);
        } else {
            mEditLayout.setVisibility(View.GONE);
            mRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mEditLayout.getVisibility() == View.VISIBLE) {
            showEditView(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
