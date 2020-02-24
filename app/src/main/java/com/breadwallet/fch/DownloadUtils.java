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

    //下载器
    private DownloadManager downloadManager;
    private Context mContext;
    //下载的ID
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

    //下载apk
    private void downloadAPK(String url, String name) {
        //创建下载任务
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //移动网络情况下是否允许漫游
        request.setAllowedOverRoaming(false);
        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setVisibleInDownloadsUi(false);
        //设置下载的路径
        File file = new File(pathstr, name);
        request.setDestinationUri(Uri.fromFile(file));
        //获取DownloadManager
        if (downloadManager == null)
            downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载请求加入下载队列，加入下载队列后会给该任务返回一个long型的id，通过该id可以取消任务，重启任务、获取下载的文件等等
        if (downloadManager != null) {
            downloadId = downloadManager.enqueue(request);
        }

        //注册广播接收者，监听下载状态
        mContext.registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mHandler.postDelayed(mQueryProgressRunnable, 10);
        mListener.onStart();
    }

    //广播监听下载的各个状态
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

    //检查下载状态
    private void checkStatus() {
        try {
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    //下载暂停
                    case DownloadManager.STATUS_PAUSED:
                        break;
                    //下载延迟
                    case DownloadManager.STATUS_PENDING:
                        break;
                    //正在下载
                    case DownloadManager.STATUS_RUNNING:
                        int mDownload_so_far = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int mDownload_all = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        mListener.onProgress(mDownload_so_far, mDownload_all);
                        cursor.close();
                        break;
                    //下载完成
                    case DownloadManager.STATUS_SUCCESSFUL:
                        //下载完成安装APK
                        Log.e("#####", "xxxxxx");
                        mListener.onFinish();
                        mContext.unregisterReceiver(receiver);
                        mHandler.removeCallbacks(mQueryProgressRunnable);
                        cursor.close();
                        break;
                    //下载失败
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

