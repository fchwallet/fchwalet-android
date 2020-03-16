package com.breadwallet.fch;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;

public class DownloadUtils {
    public interface DownLoadListener {
        void onProgress(int progress, int max);

        void onCancel();

        void onFinish();

        void onStart();
    }

    private DownloadManager downloadManager;
    private Context mContext;
    private long downloadId;
    private String name;
    private String pathstr;
    private DownLoadListener mListener;

    public DownloadUtils(Context context, String url, String pathstr, String name, DownLoadListener mListener) {
        this.mContext = context;
        this.name = name;
        this.pathstr = pathstr;
        this.mListener = mListener;
        downloadAPK(url, name);
    }

    private void downloadAPK(String url, String name) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedOverRoaming(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setVisibleInDownloadsUi(false);
        File file = new File(pathstr, name);
        request.setDestinationUri(Uri.fromFile(file));
        if (downloadManager == null)
            downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);

        if (downloadManager != null) {
            downloadId = downloadManager.enqueue(request);
        }

        mContext.registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mHandler.postDelayed(mQueryProgressRunnable, 10);
        mListener.onStart();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkStatus();
        }
    };

    private final QueryRunnable mQueryProgressRunnable = new QueryRunnable();
    private final Handler mHandler = new NoLeakHandler();

    private class QueryRunnable implements Runnable {
        @Override
        public void run() {
            checkStatus();
        }
    }

    private static class NoLeakHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private void checkStatus() {
        try {
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                        break;
                    case DownloadManager.STATUS_PENDING:
                        break;
                    case DownloadManager.STATUS_RUNNING:
                        int mDownload_so_far = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int mDownload_all = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        mListener.onProgress(mDownload_so_far, mDownload_all);
                        cursor.close();
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        mListener.onFinish();
                        mContext.unregisterReceiver(receiver);
                        mHandler.removeCallbacks(mQueryProgressRunnable);
                        cursor.close();
                        break;
                    case DownloadManager.STATUS_FAILED:
                        mListener.onCancel();
                        mContext.unregisterReceiver(receiver);
                        mHandler.removeCallbacks(mQueryProgressRunnable);
                        cursor.close();
                        break;
                }
            }
        } catch (Exception e) {
        }
    }

}

