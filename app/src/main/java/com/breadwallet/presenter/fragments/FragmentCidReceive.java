package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.utils.ModalDialogFragment;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.util.CryptoUriParser;

import java.math.BigDecimal;

public class FragmentCidReceive extends ModalDialogFragment {

    public static final String EXTRA_RECEIVE = "com.breadwallet.presenter.fragments.FragmentCidReceive.EXTRA_RECEIVE";
    public TextView mAddress;
    public ImageView mQrImage;
    private String mReceiveAddress;
    private BRButton mShareButton;
    private BRLinearLayoutWithCaret mCopiedLayout;
    private ImageButton mCloseButton;
    private Handler mCopyHandler = new Handler();
    private ViewGroup mBackgroundLayout;
    private ViewGroup mSignalLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = assignRootView((ViewGroup) inflater.inflate(R.layout.fragment_cid_receive, container, false));
        mBackgroundLayout = assignBackgroundLayout((ViewGroup) rootView.findViewById(R.id.background_layout));
        mSignalLayout = assignSignalLayout((ViewGroup) rootView.findViewById(R.id.signal_layout));
        mAddress = rootView.findViewById(R.id.address_text);
        mQrImage = rootView.findViewById(R.id.qr_image);
        mShareButton = rootView.findViewById(R.id.share_button);
        mCopiedLayout = rootView.findViewById(R.id.copied_layout);
        mCloseButton = rootView.findViewById(R.id.close_button);
        setListeners();

        mSignalLayout.removeView(mCopiedLayout);
        mSignalLayout.setLayoutTransition(UiUtils.getDefaultTransition());
        mSignalLayout.setOnTouchListener(new SlideDetector(getContext(), mSignalLayout));
        return rootView;
    }

    private void setListeners() {
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(getActivity());
                CryptoRequest cryptoRequest = new CryptoRequest.Builder().setAddress(walletManager.decorateAddress(mReceiveAddress)).setAmount(BigDecimal.ZERO).build();
                Uri cryptoUri = CryptoUriParser.createCryptoUrl(getActivity(), walletManager, cryptoRequest);
                QRUtils.sendShareIntent(getActivity(), cryptoUri.toString(), cryptoRequest.getAddress());
            }
        });
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                copyText();
            }
        });

        mBackgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });
        mQrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                copyText();
            }
        });

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWithAnimation();
            }
        });
    }

    private void showCopiedLayout(boolean b) {
        if (!b) {
            mSignalLayout.removeView(mCopiedLayout);
            mCopyHandler.removeCallbacksAndMessages(null);
        } else {
            if (mSignalLayout.indexOfChild(mCopiedLayout) == -1) {
                mSignalLayout.addView(mCopiedLayout, mSignalLayout.indexOfChild(mShareButton));
                mCopyHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSignalLayout.removeView(mCopiedLayout);
                    }
                }, DateUtils.SECOND_IN_MILLIS * 2);
            } else {
                mCopyHandler.removeCallbacksAndMessages(null);
                mSignalLayout.removeView(mCopiedLayout);
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mReceiveAddress = getArguments().getString(EXTRA_RECEIVE, "");
        updateQr();
    }

    private void updateQr() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(getContext());
                walletManager.refreshAddress(getContext());
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        mAddress.setText(mReceiveAddress);
                        Utils.correctTextSizeIfNeeded(mAddress);
                        Uri uri = CryptoUriParser.createCryptoUrl(getActivity(), walletManager, new CryptoRequest.Builder().setAddress(mReceiveAddress).build());
                        if (!QRUtils.generateQR(getContext(), uri.toString(), mQrImage)) {
                            throw new RuntimeException("failed to generate qr image for address");
                        }
                    }
                });
            }
        });
    }

    private void copyText() {
        Activity app = getActivity();
        BRClipboardManager.putClipboard(app, mAddress.getText().toString());
        EventUtils.pushEvent(EventUtils.EVENT_RECEIVE_COPIED_ADDRESS);
        showCopiedLayout(true);
    }

}