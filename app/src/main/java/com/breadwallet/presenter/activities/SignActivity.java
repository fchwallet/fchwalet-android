package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.fch.CidSpinnerAdapter;
import com.breadwallet.fch.DataCache;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.platform.tools.BRBitId;

import java.util.List;

public class SignActivity extends BRActivity {

    private BaseWalletManager mWalletManager;
    private DataCache mDataCache;

    private Spinner mSpinner;
    private TextView mTvPaste, mTvCopy, mBtnSign, mTvResult;
    private EditText mEtMessage;

    private CidSpinnerAdapter mAdapter;
    private String mAddress;
    private byte[] mRawPhrase;
    private boolean mSigning = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);
        mSpinner = findViewById(R.id.sign_spinner);
        mTvPaste = findViewById(R.id.sign_paste);
        mTvCopy = findViewById(R.id.sign_copy);
        mTvResult = findViewById(R.id.sign_result);
        mBtnSign = findViewById(R.id.sign_sign);
        mEtMessage = findViewById(R.id.sign_message);

        mWalletManager = WalletsMaster.getInstance().getCurrentWallet(this);
        mDataCache = mDataCache.getInstance();

        List<String> addresses = mDataCache.getAddressList();
        mAdapter = new CidSpinnerAdapter(this, addresses);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                mAddress = addresses.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mTvPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = BRClipboardManager.getClipboard(SignActivity.this);
                mEtMessage.setText(message);
            }
        });

        mTvCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRClipboardManager.putClipboard(SignActivity.this, mTvResult.getText().toString());
            }
        });

        mBtnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEtMessage.getText().toString().isEmpty()) {
                    return;
                }
                mSigning = true;
                try {
                    mRawPhrase = BRKeyStore.getPhrase(SignActivity.this, BRConstants.SIGN_CODE);
                    sign();
                } catch (UserNotAuthenticatedException e) {
                    Log.e("####", "SignActivity: WARNING! Authentication Loop bug");
                    return;
                }
            }
        });
    }

    private void sign() {
        String message = mEtMessage.getText().toString();
        byte[] script = new BRCoreAddress(mAddress).getPubKeyScript();
        byte[] data = BRBitId.getMessageHash(message);
        byte[] res = mWalletManager.signMessage(script, mRawPhrase, data);
        String signature = Base64.encodeToString(res, Base64.NO_WRAP);
        mTvResult.setText(signature);
        mSigning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mSigning) {
            return;
        }
        try {
            mRawPhrase = BRKeyStore.getPhrase(SignActivity.this, BRConstants.PAY_REQUEST_CODE);
            sign();
        } catch (UserNotAuthenticatedException e) {
            Log.e("####", "onResume: WARNING! Authentication Loop bug");
            return;
        }
    }
}
