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
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRDialog;
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
    private View mView1, mView2, mView3;
    private TextView mTabSign, mTabVerify;

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
        mTabSign = findViewById(R.id.tab_sign);
        mTabVerify = findViewById(R.id.tab_verify);
        mView1 = findViewById(R.id.view1);
        mView2 = findViewById(R.id.view2);
        mView3 = findViewById(R.id.view3);

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

        mTabSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mView1.setVisibility(View.VISIBLE);
                mView2.setVisibility(View.VISIBLE);
                mSpinner.setVisibility(View.VISIBLE);
                mView3.setVisibility(View.GONE);
                mTabSign.setBackground(getDrawable(R.drawable.bg_top_corner));
                mTabVerify.setBackground(getDrawable(R.drawable.bg_top_corner_unselected));
                mBtnSign.setText(R.string.signature);
            }
        });
        mTabVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mView1.setVisibility(View.GONE);
                mView2.setVisibility(View.GONE);
                mSpinner.setVisibility(View.GONE);
                mView3.setVisibility(View.VISIBLE);
                mTabSign.setBackground(getDrawable(R.drawable.bg_top_corner_unselected));
                mTabVerify.setBackground(getDrawable(R.drawable.bg_top_corner));
                mBtnSign.setText(R.string.verify);
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
                if (mSpinner.getVisibility() == View.VISIBLE) {
                    mSigning = true;
                    try {
                        mRawPhrase = BRKeyStore.getPhrase(SignActivity.this, BRConstants.SIGN_CODE);
                        sign();
                    } catch (UserNotAuthenticatedException e) {
                        Log.e("####", "SignActivity: WARNING! Authentication Loop bug");
                        return;
                    }
                } else {
                    verify();
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

    private void verify() {
        String text = mEtMessage.getText().toString().trim();
        String[] ss = text.split("----");
        if (ss.length != 3) {
            BRDialog.showCustomDialog(this, "", getString(R.string.format_error),
                    getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismiss();
                        }
                    }, null, null, 0);
            return;
        }

        String message = ss[0];
        String address = ss[1];
        String sign = ss[2];
        byte[] data = BRBitId.getMessageHash(message);
        byte[] signature = Base64.decode(sign, 0);
        String res = mWalletManager.verifyMessage(data, signature);

        String title = res.equalsIgnoreCase(address) ? getString(R.string.verify_success) : getString(R.string.verify_fail);
        BRDialog.showCustomDialog(this, "", title,
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
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
