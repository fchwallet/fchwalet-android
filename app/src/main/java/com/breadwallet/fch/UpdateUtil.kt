package com.breadwallet.fch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.content.FileProvider
import com.breadwallet.BuildConfig
import java.io.File

class UpdateUtil {
    val fileDir = Environment.getExternalStorageDirectory().absolutePath + "/"

    init {
        val file = File(fileDir)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    fun checkApk(activity: Activity, apkName: String): Boolean {
        val apkFilePath = fileDir + apkName
        val apkFile = File(apkFilePath)
        if (apkFile.exists()) {
            installApk(activity, apkFile)
            return true
        }
        return false
    }

    private fun installApk(activity: Activity, apkFile: File) {
        if (Build.VERSION.SDK_INT >= 26) {
            val hasInstallPermissionWithApi26 = activity.getPackageManager().canRequestPackageInstalls()
            if (!hasInstallPermissionWithApi26) {
                requestInstallPermission(activity)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val apkUri: Uri
        if (Build.VERSION.SDK_INT >= 24) {
            apkUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID, apkFile)
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            apkUri = Uri.fromFile(apkFile)
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        activity.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestInstallPermission(activity: Activity) {
        val packageUri = Uri.parse("package:" + activity.getPackageName())
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivityForResult(intent, 0x100)
    }

}