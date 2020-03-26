package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.fch.DataCache;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.adapter.AddressListAdapter;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.BRClipboardManager;

import java.util.List;


public class AddressListActivity extends BRActivity {

    private RecyclerView mRecyclerView;
    private AddressListAdapter mAdapter;
    private List<String> mList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);
        mRecyclerView = findViewById(R.id.address_list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new AddressListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mList = DataCache.getInstance().getAddressList();
        mAdapter.setData(mList);

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                BRClipboardManager.putClipboard(AddressListActivity.this, mList.get(position));
                Toast.makeText(AddressListActivity.this, R.string.copy_address, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongItemClick(View view, int position) {
            }
        }));
    }

}
