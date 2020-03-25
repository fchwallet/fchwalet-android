package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreTransactionInput;
import com.breadwallet.core.BRCoreTransactionOutput;
import com.breadwallet.fch.Cid;
import com.breadwallet.fch.CidSpinnerAdapter;
import com.breadwallet.fch.DataCache;
import com.breadwallet.fch.SpUtil;
import com.breadwallet.fch.Utxo;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.SendManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.bitcoin.WalletFchManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CidActivity extends BRActivity {

    private final static String TARGET = "F9A9TgNE2ixYhQmEnB15BNYcEuCvZvzqxT";
    private final static String PRE_DATA = "FEIP|3|1|";
    private final static int MIN_BALANCE = 1010000;
    private final static int TARGET_BALANCE = 1000000;

    private BaseWalletManager mWalletManager;
    private DataCache mDataCache;

    private Spinner mSpinner;
    private CidSpinnerAdapter mAdapter;
    private TextView mTvTarget;
    private TextView mTvBalance;
    private EditText mEtName;
    private EditText mEtTag;
    private TextView mBtnCreate;

    private List<Utxo> mUtxos = new ArrayList<Utxo>();
    private String mAddress;
    private String mName = "";
    private String mTag = "";
    private int mTotal;
    private int mFee;
    private int mCharge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cid);

        mSpinner = findViewById(R.id.spinner);
        mTvTarget = findViewById(R.id.tv_target_address);
        mTvBalance = findViewById(R.id.tv_balance);
        mBtnCreate = findViewById(R.id.tv_create);
        mEtName = findViewById(R.id.et_name);
        mEtTag = findViewById(R.id.et_tag);

        mTvTarget.setText(TARGET);

        mWalletManager = WalletsMaster.getInstance().getCurrentWallet(this);
        mDataCache = DataCache.getInstance();

        List<String> addresses = mDataCache.getAddressList();
        mAdapter = new CidSpinnerAdapter(this, addresses);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                mAddress = addresses.get(pos);
                if (mDataCache.getBalance().containsKey(mAddress)) {
                    int balance = mDataCache.getBalance().get(mAddress);
                    BigDecimal bd = new BigDecimal(balance).divide(WalletFchManager.ONE_FCH_BD);
                    mTvBalance.setText(String.format(getString(R.string.balance_format), bd.doubleValue()));
                } else {
                    mTvBalance.setText(String.format(getString(R.string.balance_format), 0.0));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mBtnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Integer> map = mDataCache.getBalance();
                if (!map.containsKey(mAddress) || map.get(mAddress) < MIN_BALANCE) {
                    Toast.makeText(CidActivity.this, R.string.toast_balance, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!prepareUtxo()) {
                    Toast.makeText(CidActivity.this, R.string.toast_balance, Toast.LENGTH_SHORT).show();
                    return;
                }
                mName = mEtName.getText().toString();
                mTag = mEtTag.getText().toString();
                String data = PRE_DATA + mName + "|" + mTag;
                Log.e("####", "data = " + data);
                createCid(data);
            }
        });
    }

    private boolean prepareUtxo() {
        List<Utxo> list = mDataCache.getUtxoList();
        mTotal = 0;
        mFee = 1000;
        mUtxos.clear();
        for (Utxo u : list) {
            if (u.getAddress().equalsIgnoreCase(mAddress)) {
                mTotal += u.getAmount();
                mFee += 500;
                mUtxos.add(u);
            }
        }
        return mTotal >= (mFee + TARGET_BALANCE);
    }

    private void createCid(String data) {
        BRCoreTransaction tx = new BRCoreTransaction();
        byte[] empty = new byte[]{};
        long sequence = 4294967295L;

        BRCoreAddress inAddress = new BRCoreAddress(mAddress);
        byte[] inScript = inAddress.getPubKeyScript();
        for (Utxo u : mUtxos) {
            String s = Utils.reverse(u.getTxid());
            byte[] hash = Utils.hexToBytes(s);
            BRCoreTransactionInput in = new BRCoreTransactionInput(hash, u.getVout(), u.getAmount(), inScript, empty, empty, sequence);
            tx.addInput(in);
        }

        BRCoreAddress targetAddress = new BRCoreAddress(TARGET);
        byte[] targetScript = targetAddress.getPubKeyScript();
        BRCoreTransactionOutput out = new BRCoreTransactionOutput(TARGET_BALANCE, targetScript);
        tx.addOutput(out);

        mCharge = mTotal - mFee - TARGET_BALANCE;
        if (mCharge > WalletFchManager.DUST) {
            BRCoreTransactionOutput charge = new BRCoreTransactionOutput(mCharge, inScript);
            tx.addOutput(charge);
        }

        String hexData = stringToHex(data);
        int len = hexData.length() / 2;
        if (len < 16) {
            hexData = "6a0" + Integer.toHexString(len) + hexData;
        } else {
            hexData = "6a" + Integer.toHexString(len) + hexData;
        }
        BRCoreTransactionOutput dataOut = new BRCoreTransactionOutput(0, Utils.hexToBytes(hexData));
        tx.addOutput(dataOut);

        CryptoTransaction transaction = new CryptoTransaction(tx);
        final CryptoRequest item = new CryptoRequest.Builder().setAddress(TARGET).setAmount(new BigDecimal(TARGET_BALANCE)).build();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            SendManager.sendTransaction2(this, item, transaction, mWalletManager, (txHash, succeeded) -> {
                if (!Utils.isNullOrEmpty(txHash) && succeeded) {
                    UiUtils.showBreadSignal(this, getString(R.string.Alerts_sendSuccess),
                            getString(R.string.Alerts_sendSuccessSubheader), R.drawable.ic_check_mark_white, () -> UiUtils.killAllFragments(this));
                    Log.e("####", "txHash = " + txHash);
                    updateUtxo(txHash);
                    finish();
                } else {
                    UiUtils.showBreadSignal(this, getString(R.string.Alert_error),
                            getString(R.string.Alerts_sendFailure), R.drawable.ic_error_outline_black_24dp, () -> UiUtils.killAllFragments(this));
                }

            });
        });
    }

    private void updateUtxo(String txid) {
        List<String> txs = mDataCache.getSpendTxid();
        List<Utxo> pending = mDataCache.getPendingList();

        for (Utxo u : mUtxos) {
            String s = u.getTxid() + u.getVout();
            txs.add(s);

            if (pending.contains(u)) {
                pending.remove(u);
            }
        }
        mDataCache.setSpendTxid(txs);
        SpUtil.putTxid(this, txs);

        if (mCharge > WalletFchManager.DUST) {
            Utxo charge = new Utxo(txid, mAddress, mCharge, 1);
            pending.add(charge);
        }
        mDataCache.setPendingList(pending);
        SpUtil.putPending(this, pending);

        saveCid();
    }

    private void saveCid() {
        String name = mName + "_" + mAddress.substring(30);
        List<Cid> list = mDataCache.getCidList();

        Cid cid = new Cid(mAddress, name);
        for (Cid c : list) {
            if (c.getAddress().equalsIgnoreCase(mAddress)) {
                list.remove(c);
                break;
            }
        }

        if (mName.isEmpty() && mTag.isEmpty()) {

        } else {
            list.add(cid);
            mDataCache.setCidList(list);
            SpUtil.putCid(this, list);
        }
    }

    public String stringToHex(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }

}
