package com.breadwallet.fch;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Utxo {

    private String txid;
    private String address;
    private int amount;
    private int vout;

    public Utxo(String txid, String address, int amount, int vout) {
        this.txid = txid;
        this.address = address;
        this.amount = amount;
        this.vout = vout;
    }

    public int getAmount() {
        return amount;
    }

    public int getVout() {
        return vout;
    }

    public String getAddress() {
        return address;
    }

    public String getTxid() {
        return txid;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public void setVout(int vout) {
        this.vout = vout;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Utxo) {
            Utxo u = (Utxo) obj;
            return u.getTxid().equalsIgnoreCase(txid) && u.getVout() == vout;
        } else {
            return super.equals(obj);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + txid + "," + address + "," + amount + "," + vout + "]";
    }
}
