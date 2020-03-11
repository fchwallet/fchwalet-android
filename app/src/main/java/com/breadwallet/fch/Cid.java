package com.breadwallet.fch;

import android.support.annotation.NonNull;

public class Cid {

    private String address;
    private String name;

    public Cid () {}

    public Cid (String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + address + "," + name + "]";
    }
}
