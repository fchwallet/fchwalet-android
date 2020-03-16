/**
 * BreadWallet
 * <p/>
 * Created by byfieldj on <jade@breadwallet.com> 1/17/18.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.breadwallet.presenter.activities;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.fch.AppUpdateTask;
import com.breadwallet.fch.Cid;
import com.breadwallet.fch.CidTask;
import com.breadwallet.fch.DataCache;
import com.breadwallet.fch.DownloadUtils;
import com.breadwallet.fch.FchPriceTask;
import com.breadwallet.fch.SpUtil;
import com.breadwallet.fch.UpdateUtil;
import com.breadwallet.fch.Utxo;
import com.breadwallet.fch.UtxoTask;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.presenter.viewmodels.HomeViewModel;
import com.breadwallet.tools.adapter.CidListAdapter;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.AppEntryPointHandler;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.ui.notification.InAppNotificationActivity;
import com.breadwallet.ui.wallet.WalletActivity;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletFchManager;
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    public static final String EXTRA_DATA = "com.breadwallet.presenter.activities.HomeActivity.EXTRA_DATA";
    public static final String EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID = "com.breadwallet.presenter.activities.HomeActivity.EXTRA_PUSH_CAMPAIGN_ID";
    private static final String NETWORK_TESTNET = "TESTNET";
    private static final String NETWORK_MAINNET = "MAINNET";

    private RecyclerView mWalletRecycler;
    private WalletListAdapter mAdapter;
    private BaseTextView mFiatTotal;
    private BRNotificationBar mNotificationBar;
    private LinearLayout mTradeLayout;
    private LinearLayout mMenuLayout;
    private LinearLayout mListGroupLayout;
    private HomeViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Show build info as a watermark on non prod builds like: TESTNET 3.10.1 build 1
        setUpBuildInfoLabel();

        mWalletRecycler = findViewById(R.id.rv_wallet_list);
        mFiatTotal = findViewById(R.id.total_assets_usd);
        mNotificationBar = findViewById(R.id.notification_bar);
        mTradeLayout = findViewById(R.id.trade_layout);
        mMenuLayout = findViewById(R.id.menu_layout);
        mListGroupLayout = findViewById(R.id.list_group_layout);
        mCidRecycler = findViewById(R.id.rv_cid_list);

        mTradeLayout.setOnClickListener(view -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
        mMenuLayout.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SETTINGS);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
        });
        mWalletRecycler.setLayoutManager(new LinearLayoutManager(this));
        mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (position >= mAdapter.getItemCount() || position < 0) {
                    return;
                }
                if (mAdapter.getItemViewType(position) == 0) {
                    String currencyCode = mAdapter.getItemAt(position).getCurrencyCode();
                    BRSharedPrefs.putCurrentWalletCurrencyCode(HomeActivity.this, currencyCode);
                    // Use BrdWalletActivity to show rewards view and animation if BRD and not shown yet.
                    if (WalletTokenManager.BRD_CURRENCY_CODE.equalsIgnoreCase(currencyCode)) {
                        if (!BRSharedPrefs.getRewardsAnimationShown(HomeActivity.this)) {
                            Map<String, String> attributes = new HashMap<>();
                            attributes.put(EventUtils.EVENT_ATTRIBUTE_CURRENCY, WalletTokenManager.BRD_CURRENCY_CODE);
                            EventUtils.pushEvent(EventUtils.EVENT_REWARDS_OPEN_WALLET, attributes);
                        }
                        BrdWalletActivity.start(HomeActivity.this, currencyCode);
                    } else {
                        WalletActivity.start(HomeActivity.this, currencyCode);
                    }
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                } else {
                    Intent intent = new Intent(HomeActivity.this, AddWalletsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {
            }
        }));
        processIntentData(getIntent());

        mAdapter = new WalletListAdapter(this);
        mWalletRecycler.setAdapter(mAdapter);

        mCidRecycler.setLayoutManager(new LinearLayoutManager(this));
        mCidRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, mCidRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (position >= mCidAdapter.getItemCount() || position < 0) {
                    return;
                }
                if (mCidAdapter.getItemViewType(position) == 0) {

                } else {
                    Intent intent = new Intent(HomeActivity.this, CidActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {
            }
        }));
        mCidAdapter = new CidListAdapter(this);
        mCidRecycler.setAdapter(mCidAdapter);

        // Get ViewModel, observe updates to Wallet and aggregated balance data
        mViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        mViewModel.getWallets().observe(this, wallets -> mAdapter.setWallets(wallets));

        mViewModel.getAggregatedFiatBalance().observe(this, aggregatedFiatBalance -> {
            if (aggregatedFiatBalance == null) {
                Log.e(TAG, "fiatTotalAmount is null");
                return;
            }
            mFiatTotal.setText(CurrencyUtils.getFormattedAmount(HomeActivity.this,
                    BRSharedPrefs.getPreferredFiatIso(HomeActivity.this), aggregatedFiatBalance));
        });
        mViewModel.getNotificationLiveData().observe(this, notification -> {
            if (notification != null) {
                InAppNotificationActivity.Companion.start(HomeActivity.this, notification);
            }
        });
        mViewModel.checkForInAppNotification();

        initData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntentData(intent);
    }

    private synchronized void processIntentData(Intent intent) {
        if (intent.hasExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID)) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put(EventUtils.EVENT_ATTRIBUTE_CAMPAIGN_ID, intent.getStringExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID));
            EventUtils.pushEvent(EventUtils.EVENT_MIXPANEL_APP_OPEN, attributes);
            EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATION_OPEN);
        }

        String data = intent.getStringExtra(EXTRA_DATA);
        if (Utils.isNullOrEmpty(data)) {
            data = intent.getDataString();
        }
        if (data != null) {
            AppEntryPointHandler.processDeepLink(this, data);
        }
    }

    private void showNextPromptIfNeeded() {
        PromptManager.PromptItem toShow = PromptManager.nextPrompt(this);
        if (toShow != null) {
            View promptView = PromptManager.promptInfo(this, toShow);
            if (mListGroupLayout.getChildCount() > 0) {
                mListGroupLayout.removeAllViews();
            }
            mListGroupLayout.addView(promptView, 0);
            EventUtils.pushEvent(EventUtils.EVENT_PROMPT_PREFIX
                    + PromptManager.getPromptName(toShow) + EventUtils.EVENT_PROMPT_SUFFIX_DISPLAYED);
        } else {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNextPromptIfNeeded();
        InternetManager.registerConnectionReceiver(this, this);
        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        getUtxo();
        refreshCid();
    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.GONE);
            }
        } else {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.bringToFront();
            }
        }
    }

    private void setUpBuildInfoLabel() {
        TextView buildInfoTextView = findViewById(R.id.testnet_label);
        String network = BuildConfig.BITCOIN_TESTNET ? NETWORK_TESTNET : NETWORK_MAINNET;
        String buildInfo = network + " " + BuildConfig.VERSION_NAME + " build " + BuildConfig.BUILD_VERSION;
        buildInfoTextView.setText(buildInfo);
        buildInfoTextView.setVisibility(BuildConfig.BITCOIN_TESTNET || BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
    }


    private void appUpdate() {
        new AppUpdateTask(getApplicationContext()).execute();
    }

    private void prepare(String url, String version) {
        int flag = ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (flag != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(HomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限
                Toast.makeText(HomeActivity.this, R.string.toast_update, Toast.LENGTH_LONG).show();
            } else {
                // 申请授权
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else {
            String apkName = "fchwallet-" + version + ".apk";
            UpdateUtil updateUtil = new UpdateUtil();
            Log.e(TAG, "apkName = " + apkName);
            if (!updateUtil.checkApk(HomeActivity.this, apkName)) {
                DownloadUtils downloadUtils = new DownloadUtils(HomeActivity.this, url, updateUtil.getFileDir(), apkName,
                        new DownloadUtils.DownLoadListener() {
                            @Override
                            public void onProgress(int progress, int max) {
                            }

                            @Override
                            public void onCancel() {
                            }

                            @Override
                            public void onFinish() {
                                Log.e(TAG, "onFinish ==================== ");
                                updateUtil.checkApk(HomeActivity.this, apkName);
                            }

                            @Override
                            public void onStart() {
                            }
                        });
            }
        }
    }

    private RecyclerView mCidRecycler;
    private CidListAdapter mCidAdapter;

    public static final String ACTIVITY_ACTION = "ACTIVITY_ACTION";
    public static final String ACTION_PRICE_UPDATE = "PRICE_UPDATE";
    public static final String ACTION_APP_UPDATE = "APP_UPDATE";
    public static final String ACTION_UTXO_UPDATE = "UTXO_UPDATE";
    public static final String ACTION_CID_UPDATE = "CID_UPDATE";
    public static final String KEY_UTXO = "api_utxo";
    public static final String KEY_CID = "api_cid";

    private BaseWalletManager mWalletManager;
    private DataCache mDateCache;
    private BroadcastReceiver mReceiver;

    private List<String> mAddressList = new ArrayList<String>();
    private String mAddressString = "";
    private List<Utxo> mUtxoList = new ArrayList<Utxo>();
    private Map<String, Integer> mBalanceMap = new HashMap<String, Integer>();
    private int mTotalBalance = 0;

    private void initBroadcast() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_PRICE_UPDATE)) {
                    mViewModel.refreshWallets();
                } else if (intent.getAction().equals(ACTION_APP_UPDATE)) {
                    String url = intent.getStringExtra("download");
                    String version = intent.getStringExtra("version");
                    Toast.makeText(HomeActivity.this, R.string.toast_version, Toast.LENGTH_LONG).show();
                    prepare(url, version);
                } else if (intent.getAction().equals(ACTION_UTXO_UPDATE)) {
                    String utxo = intent.getStringExtra(KEY_UTXO);
                    refreshUtxo(utxo);
                } else if (intent.getAction().equals(ACTION_CID_UPDATE)) {
                    String data = intent.getStringExtra(KEY_CID);
                    getCid(data);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTIVITY_ACTION);
        filter.addAction(ACTION_APP_UPDATE);
        filter.addAction(ACTION_APP_UPDATE);
        filter.addAction(ACTION_UTXO_UPDATE);
        filter.addAction(ACTION_CID_UPDATE);
        registerReceiver(mReceiver, filter);
    }

    private void initData() {
        initBroadcast();

        BRSharedPrefs.putPreferredFiatIso(this, "CNY");
        mWalletManager = WalletsMaster.getInstance().getCurrentWallet(this);
        mDateCache = DataCache.getInstance();

        loadAddress();

        mDateCache.setSpendTxid(SpUtil.getTxid(this));
        mDateCache.setPendingList(SpUtil.getPending(this));

        getLatestPrice();
        initCid();
        appUpdate();
    }

    private void loadAddress() {
        mAddressList = SpUtil.getAddress(this);

        if (mAddressList.isEmpty()) {
            Log.e("####", "load address from tx history");
            List<TxUiHolder> holders = mWalletManager.getTxUiHolders(this);
            if (holders != null) {
                for (TxUiHolder h : holders) {
                    if (h.isReceived() && !mAddressList.contains(h.getTo())) {
                        mAddressList.add(h.getTo());
                    }
                }
            }
        }

        String a = mWalletManager.getAddress(this);
        if (!mAddressList.contains(a)) {
            mAddressList.add(a);
            SpUtil.putAddress(this, mAddressList);
        }
        mDateCache.setAddressList(mAddressList);

        int size = mAddressList.size();
        for (int i = 0; i < size; ++i) {
            mAddressString += mAddressList.get(i);
            if (i != size - 1) {
                mAddressString += ",";
            }
        }
    }

    private void initCid() {
        List<Cid> list = SpUtil.getCid(this);
        if (list.isEmpty()) {
            LinkedBlockingQueue<Runnable> q = new LinkedBlockingQueue<Runnable>();
            ExecutorService e = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, q);
            new CidTask(getApplicationContext(), mAddressString).executeOnExecutor(e);
        } else {
            mDateCache.setCidList(list);
        }
    }

    private void getLatestPrice() {
        LinkedBlockingQueue<Runnable> q = new LinkedBlockingQueue<Runnable>();
        ExecutorService e = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, q);
        new FchPriceTask(getApplicationContext()).executeOnExecutor(e);
    }

    private void getUtxo() {
        if (mAddressString.isEmpty()) {
            Log.e("####", "ERROR getUtxo: mAddressString is empty");
            return;
        }

        LinkedBlockingQueue<Runnable> q = new LinkedBlockingQueue<Runnable>();
        ExecutorService e = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, q);
        new UtxoTask(HomeActivity.this, mAddressString).executeOnExecutor(e);
    }

    private void refreshUtxo(String ss) {
        List<Utxo> temp = new ArrayList<Utxo>();
        List<String> txList = mDateCache.getSpendTxid();

        try {
            JSONArray arr = new JSONArray(ss);
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject obj = new JSONObject(arr.get(i).toString());
                String txid = obj.getString("txid");
                int vout = obj.getInt("vout");

                if (txList.contains(txid + vout))
                    continue;

                String address = obj.getString("address");
                String a = obj.getString("amount");
                BigDecimal bd = new BigDecimal(a).multiply(WalletFchManager.ONE_FCH_BD);
                int amount = bd.intValue();
                Utxo u = new Utxo(txid, address, amount, vout);
                temp.add(u);
            }
        } catch (JSONException e) {
            Log.e("####", "ERROR refreshUtxo: " + e.toString());
        }

        List<Utxo> pList = mDateCache.getPendingList();
        List<Utxo> res = temp;
        List<Utxo> newPending = pList;
        for (Utxo p : pList) {
            if (temp.contains(p)) {
                newPending.remove(p);
            } else {
                res.add(p);
            }
        }

        if (newPending.isEmpty()) {
            pList.clear();
            txList.clear();
            mDateCache.setPendingList(pList);
            mDateCache.setSpendTxid(txList);
            SpUtil.putPending(this, pList);
            SpUtil.putTxid(this, txList);
        }
        mUtxoList = res;
        mDateCache.setUtxoList(mUtxoList);

        calculateBalance();
        refreshCid();
    }

    private void calculateBalance() {
        mBalanceMap.clear();
        mTotalBalance = 0;
        for (Utxo u : mUtxoList) {
            String address = u.getAddress();
            int amount = u.getAmount();
            int value = amount;
            if (mBalanceMap.containsKey(address)) {
                value += mBalanceMap.get(address);
            }
            mBalanceMap.put(address, value);

            mTotalBalance += amount;
        }
        mDateCache.setBalance(mBalanceMap);
        Log.e("####", "mTotalBalance = " + mTotalBalance);
        mDateCache.setTotalBalance(mTotalBalance);

        BigDecimal bd = new BigDecimal(mTotalBalance);
        String price = SpUtil.get(this, SpUtil.KEY_PRICE);
        bd = bd.multiply(new BigDecimal(price)).divide(WalletFchManager.ONE_FCH_BD);
        mFiatTotal.setText(CurrencyUtils.getFormattedAmount(this,
                BRSharedPrefs.getPreferredFiatIso(this), bd));
        mViewModel.refreshWallets();
    }

    private void getCid(String data) {
        List<Cid> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(data);
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject obj = new JSONObject(arr.get(i).toString());
                String address = obj.getString("address");
                String name = obj.getString("cid");
                list.add(new Cid(address, name));
            }
        } catch (JSONException e) {
            Log.e("####", "ERROR getCid: " + e.toString());
        }
        mDateCache.setCidList(list);
        SpUtil.putCid(this, list);
    }

    private void refreshCid() {
        mCidAdapter.setData(mDateCache.getCidList());
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
