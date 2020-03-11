package com.breadwallet.fch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataCache {

    private static DataCache mInstance;
    private List<Utxo> mUtxoList = new ArrayList<Utxo>();
    private List<Cid> mCidList = new ArrayList<Cid>();
    private List<String> mAddressList = new ArrayList<String>();
    private Map<String, Integer> mBalance = new HashMap<String, Integer>();
    private List<String> mSpendTxid = new ArrayList<String>();
    private List<Utxo> mPendingList = new ArrayList<Utxo>();
    private int mTotalBalance = 0;

    public static DataCache getInstance() {
        if (mInstance == null) {
            mInstance = new DataCache();
        }
        return mInstance;
    }

    public List<Utxo> getUtxoList() {
        return mUtxoList;
    }

    public List<Cid> getCidList() {
        return mCidList;
    }

    public List<String> getAddressList() {
        return mAddressList;
    }

    public Map<String, Integer> getBalance() {
        return mBalance;
    }

    public List<String> getSpendTxid() {
        return mSpendTxid;
    }

    public List<Utxo> getPendingList() {
        return mPendingList;
    }

    public int getTotalBalance() {
        return mTotalBalance;
    }

    public void setUtxoList(List<Utxo> list) {
        this.mUtxoList = list;
    }

    public void setCidList(List<Cid> list) {
        this.mCidList = list;
    }

    public void setAddressList(List<String> list) {
        this.mAddressList = list;
    }

    public void setBalance(Map<String, Integer> map) {
        this.mBalance = map;
    }

    public void setSpendTxid(List<String> list) {
        this.mSpendTxid = list;
    }

    public void setPendingList(List<Utxo> list) {
        this.mPendingList = list;
    }

    public void setTotalBalance(int balance) {
        this.mTotalBalance = balance;
    }
}
