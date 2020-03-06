package com.breadwallet.fch;

import android.support.annotation.NonNull;

public class Cid {

    private String address;
    private String name;
    private String txid;

    public Cid () {}

    public Cid (String address, String name, String txid) {
        this.address = address;
        this.name = name;
        this.txid = txid;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getTxid() {
        return txid;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + address + "," + name + "," + txid + "]";
    }
}
