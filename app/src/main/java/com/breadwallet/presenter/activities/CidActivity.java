package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
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
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;

import java.util.ArrayList;
import java.util.List;

public class CidActivity extends BRActivity {

    private final static String TARGET = "F9A9TgNE2ixYhQmEnB15BNYcEuCvZvzqxT";
    private final static String PRE_DATA = "FEIP|3|1|";
    private final static double ONE_FCH = 100000000.0;
    private final static int MIN_BALANCE = 1000000;
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

    private List<String> txids = new ArrayList<String>();

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

        mDataCache = mDataCache.getInstance();
        List<String> addresses = mDataCache.getAddressList();
        mAdapter = new CidSpinnerAdapter(this, addresses);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                mAddress = addresses.get(pos);
                if (mDataCache.getBalance().containsKey(mAddress)) {
                    int balance = mDataCache.getBalance().get(mAddress);
                    double b = balance / ONE_FCH;
                    mTvBalance.setText("余额： " + b + " FCH");
                } else {
                    mTvBalance.setText("余额： 0.0 FCH");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mBtnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mDataCache.getBalance().containsKey(mAddress)) {
                    Toast.makeText(CidActivity.this, "余额不足", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mDataCache.getBalance().get(mAddress) < MIN_BALANCE) {
                    Toast.makeText(CidActivity.this, "余额不足", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!prepareUtxo()) {
                    Toast.makeText(CidActivity.this, "余额不足", Toast.LENGTH_SHORT).show();
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
                mFee += 1000;
                mUtxos.add(u);
                if (mTotal >= mFee + TARGET_BALANCE) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createCid(String data) {
        BRCoreTransaction tx = new BRCoreTransaction();
        byte[] empty = new byte[]{};
        long sequence = 4294967295L;

        BRCoreAddress inAddress = new BRCoreAddress(mAddress);
        byte[] inScript = inAddress.getPubKeyScript();
        for (Utxo u : mUtxos) {
            byte[] hash = Utils.hexToBytes(u.getTxid());
            BRCoreTransactionInput in = new BRCoreTransactionInput(hash, u.getVout(), u.getAmount(), inScript, empty, empty, sequence);
            tx.addInput(in);
            txids.add(u.getTxid());
        }

        BRCoreAddress targetAddress = new BRCoreAddress(TARGET);
        byte[] targetScript = targetAddress.getPubKeyScript();
        BRCoreTransactionOutput out = new BRCoreTransactionOutput(TARGET_BALANCE, targetScript);
        tx.addOutput(out);

        int left = mTotal - mFee - TARGET_BALANCE;
        if (left > 99) {
            BRCoreTransactionOutput charge = new BRCoreTransactionOutput(left, inScript);
            tx.addOutput(charge);
        }

        String hexData = stringToHex(data);
        int len = hexData.length() / 2;
        if (len < 16) {
            hexData = "6a0" + Integer.toHexString(len) + hexData;
        } else {
            hexData = "6a" + Integer.toHexString(len) + hexData;
        }
        Log.e("####", "data = " + hexData);
        BRCoreTransactionOutput dataOut = new BRCoreTransactionOutput(0, Utils.hexToBytes(hexData));
        tx.addOutput(dataOut);

        CryptoTransaction transaction = new CryptoTransaction(tx);

        AuthManager.getInstance().authPrompt(CidActivity.this, getString(R.string.VerifyPin_touchIdMessage), "", false, false, new BRAuthCompletion() {
            @Override
            public void onComplete() {
                final byte[] rawPhrase;
                try {
                    rawPhrase = BRKeyStore.getPhrase(CidActivity.this, BRConstants.PAY_REQUEST_CODE);
                } catch (UserNotAuthenticatedException e) {
                    Log.e("####", "UserNotAuthenticatedException");
                    return;
                }
                byte[] txid = mWalletManager.signAndPublishTransaction(transaction, rawPhrase);
                if (txid.length > 0) {
                    Toast.makeText(CidActivity.this, "创建成功", Toast.LENGTH_SHORT).show();
                    saveCid(Utils.bytesToHex(txid));
                    finish();
                } else {
                    txids.clear();
                    Toast.makeText(CidActivity.this, "创建失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancel() {

            }
        });
    }

    private void saveCid(String txid) {
        String name = mName + "_" + mAddress.substring(30);
        List<Cid> list = mDataCache.getCidList();

        Cid cid = new Cid(mAddress, name, txid);
        for (Cid c : list) {
            if (c.getAddress().equalsIgnoreCase(mAddress)) {
                list.remove(c);
                break;
            }
        }
        list.add(cid);
        mDataCache.setCidList(list);
        SpUtil.putCid(this, list);

        List<String> txs = mDataCache.getSpendTxid();
        txs.addAll(txids);
        mDataCache.setSpendTxid(txs);
        SpUtil.putTxid(this, txs);
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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}
